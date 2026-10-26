package com.lumina.twinstar.algorithm.execution;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.lumina.twinstar.algorithm.cache.Snapshot;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.AmountMap;
import com.lumina.twinstar.algorithm.model.ChanceSampler;
import com.lumina.twinstar.algorithm.model.InputKind;
import com.lumina.twinstar.algorithm.model.Pattern;
import com.lumina.twinstar.algorithm.model.PatternLocation;
import com.lumina.twinstar.algorithm.model.Rational;
import com.lumina.twinstar.algorithm.planning.CycleContract;
import com.lumina.twinstar.algorithm.planning.Plan;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * F — the executor, one per network (held by TwinStarService). Runs every job each tick, round
 * robin; each job gets the full white-hole parallelism P, the black-hole capacity is one pool shared
 * by all jobs and released as items leave them.
 */
public final class HubState {
    /** stuck threshold = max(median round trip × 4, 10 s); 60 s for machines without data (建議) */
    public static final int STUCK_RTT_FACTOR = 4;
    public static final long STUCK_MIN_NANOS = 10_000_000_000L;
    public static final long STUCK_DEFAULT_NANOS = 60_000_000_000L;

    private final List<Job> jobs = new ArrayList<>();
    /** returns waiting inside the hub; other devices on the network cannot take them */
    private final AmountMap pool = new AmountMap();
    private Amount used = Amount.ZERO;
    private final Map<WindowKey, Window> windows = new LinkedHashMap<>();
    private int rotate;
    private long nextJobId = 1;
    /** draws of the hub's own runs for outputs that come with a probability */
    private final RandomGenerator random = new SplittableRandom();

    record WindowKey(ICraftingProvider provider, int pattern) {
    }

    // ───────────────────────────── queries ─────────────────────────────

    public List<Job> jobs() {
        return Collections.unmodifiableList(jobs);
    }

    public Amount used() {
        return used;
    }

    public Amount freeCapacity(Amount capacity) {
        return capacity.sub(used).clampNonNegative();
    }

    /** For AE2's isRequesting / getRequestedAmount: what jobs still expect minus what already waits in the pool. */
    public Amount requestedAmount(int item) {
        Amount sum = Amount.ZERO;
        for (Job j : jobs)
            sum = sum.add(j.outstanding.get(item));
        return sum.sub(pool.get(item)).clampNonNegative();
    }

    public boolean isRequestingAny() {
        for (Job j : jobs)
            if (!j.outstanding.isEmpty())
                return true;
        return false;
    }

    // ───────────────────────────── submit ─────────────────────────────

    /**
     * Pulls everything the plan needs into a new job's own inventory, all or nothing. Null when the
     * network no longer has it: the caller re-plans once with fresh stock, then gives up.
     */
    public @Nullable Job submit(Plan plan, Snapshot snap, ExecutionEnv env) {
        if (!plan.isComplete())
            return null;
        // infinite sources are drawn while the job runs; everything else is pulled now
        AmountMap now = plan.pulled.copy();
        plan.drawLater.forEach(now::sub);
        for (Map.Entry<Integer, Amount> e : now.entries())
            if (env.index().keyOf(e.getKey()) == null)
                return null;
        // taken for real and put back when short: big storages have no long-sized simulate to ask first
        AmountMap taken = new AmountMap();
        for (Map.Entry<Integer, Amount> e : now.entries()) {
            AEKey key = env.index().keyOf(e.getKey());
            Amount got = env.store().extract(key, e.getValue());
            taken.add(e.getKey(), got);
            if (got.compareTo(e.getValue()) < 0) {
                taken.forEach((id, a) -> insertAll(env, id, a));
                return null;
            }
        }
        Job job = new Job(nextJobId++, plan, snap, env.nowNanos());
        used = used.add(job.occupancy);
        jobs.add(job);
        for (Task t : job.tasks)
            checkReady(job, t);
        return job;
    }

    // ───────────────────────────── tick ─────────────────────────────

    public void hubTick(ExecutionEnv env) {
        if (jobs.isEmpty())
            return;
        long deadline = System.nanoTime() + env.tickBudgetNanos();
        int n = jobs.size();
        for (int i = 0; i < n; i++) {
            Job job = jobs.get((rotate + i) % n);
            if (job.cancelled) {
                // a full network kept part of a cancelled job: keep trying to give it back
                if (!job.finished && returnInventory(job, env))
                    job.finished = true;
                continue;
            }
            pullReturns(job, env);      // returns are taken even while paused
            if (job.finished)
                continue;
            if (!job.suspended)
                runJob(job, env, deadline);
            if (allDone(job))
                finish(job, env);
        }
        rotate++;
        checkStuck(env.nowNanos());
        jobs.removeIf(j -> j.finished);
    }

    private void runJob(Job job, ExecutionEnv env, long deadline) {
        // runs this job may still send this tick, summed over all patterns; null = unlimited
        Amount p = env.parallelism();
        deliverFinals(job, env);
        // tasks that had their materials but no free machine or power last tick try again now
        List<Task> again = new ArrayList<>(job.retry);
        job.retry.clear();
        for (Task t : again)
            checkReady(job, t);
        while (!job.ready.isEmpty() && (p == null || p.signum() > 0) && System.nanoTime() < deadline) {
            Task t = job.ready.poll();
            t.queued = false;
            if (t.remaining.signum() <= 0)
                continue;
            Pattern pat = job.snap.pattern(t.pattern);
            Amount supported = supportable(job, t, pat, true);
            if (supported.signum() <= 0) {
                waitFor(job, t, pat);      // materials taken first: an inventory change wakes it
                continue;
            }
            Amount want = p == null ? t.remaining : t.remaining.min(p);
            ChanceProgress chance = job.chances.get(t.pattern);
            if (chance != null)
                want = want.min(chance.nextBatch(pat));     // no more than the rest needs on average
            Amount n = contractCap(job, t, want.min(supported));
            if (n.signum() <= 0)
                continue;                  // not this pattern's turn in the loop's contract; advancing it wakes it
            n = energyCap(pat, n, env);
            if (n.signum() <= 0) {
                retryNextTick(job, t);     // no power right now
                continue;
            }
            if (!job.drawLater.isEmpty()) {
                drawLazily(job, pat, n, env);
                n = n.min(supportable(job, t, pat, false));
                if (n.signum() <= 0) {
                    retryNextTick(job, t); // the infinite source did not deliver this tick
                    continue;
                }
            }
            if (pat.where == PatternLocation.HUB_INTERNAL) {
                consume(job, pat, n);
                produce(job, pat, n, chance);   // later tasks may become ready within this tick
            } else {
                n = dispatchEven(job, t, pat, n, env);
                if (n.signum() <= 0) {
                    // materials are here but every machine is busy or refused: no inventory change
                    // will wake this task, so it must try again next tick
                    retryNextTick(job, t);
                    continue;
                }
            }
            payEnergy(pat, n, env);
            t.remaining = t.remaining.sub(n);
            release(job, n);            // one byte per run
            if (chance != null)
                chanceCheck(job, t, chance, env.nowNanos());
            if (p != null)
                p = p.sub(n);
            advanceContract(job, t, n);
            if (t.scc != -1)
                releaseSeedsIfLoopDone(job, t.scc);
            releaseRecycleIfLoopDone(job, t.pattern);
            if (t.remaining.signum() > 0)
                checkReady(job, t);
            deliverFinals(job, env);    // finished products go out batch by batch
        }
    }

    private static void retryNextTick(Job job, Task t) {
        if (!job.retry.contains(t))
            job.retry.add(t);
    }

    private boolean allDone(Job job) {
        for (Task t : job.tasks)
            if (t.remaining.signum() > 0)
                return false;
        return job.outstanding.isEmpty();
    }

    // ───────────────────────────── readiness ─────────────────────────────

    private void checkReady(Job job, Task t) {
        if (t.queued || t.remaining.signum() <= 0)
            return;
        Pattern pat = job.snap.pattern(t.pattern);
        if (supportable(job, t, pat, true).signum() > 0) {
            t.queued = true;
            job.ready.add(t);
        } else
            waitFor(job, t, pat);
    }

    private void waitFor(Job job, Task t, Pattern pat) {
        for (int s = 0; s < pat.inputCount(); s++) {
            List<Task> l = job.waitingOn.computeIfAbsent(pat.inputItem(s), k -> new ArrayList<>());
            if (!l.contains(t))
                l.add(t);
            for (int a = 0; a < pat.alternativeCount(s); a++) {
                List<Task> la = job.waitingOn.computeIfAbsent(pat.alternative(s, a), k -> new ArrayList<>());
                if (!la.contains(t))
                    la.add(t);
            }
        }
    }

    private void notifyItem(Job job, int item) {
        List<Task> waiting = job.waitingOn.remove(item);
        if (waiting != null)
            for (Task t : waiting)
                checkReady(job, t);
    }

    /**
     * Runs the job's stock supports (seeds of running loops are off limits to everyone else).
     * {@code withLater}: count infinite-source items not drawn yet as available.
     */
    private Amount supportable(Job job, Task t, Pattern pat, boolean withLater) {
        Amount max = null;
        for (int s = 0; s < pat.inputCount(); s++) {
            Amount have = usable(job, t, pat.inputItem(s), withLater);
            if (pat.allowSubst)
                for (int a = 0; a < pat.alternativeCount(s); a++)
                    have = have.add(usable(job, t, pat.alternative(s, a), withLater));
            Amount lim;
            switch (pat.inputKind(s)) {
                case CONSUME -> lim = have.div(pat.inputAmount(s));
                case TOOL -> {
                    int dur = pat.inputDurability(s);
                    if (dur <= 0)
                        lim = have.div(pat.inputAmount(s));
                    else {
                        Amount usedRuns = job.toolUses.get(pat.inputItem(s));
                        Amount partLeft = usedRuns.ceilDiv(dur).mul(dur).sub(usedRuns);   // runs left on the tool in use
                        lim = have.mul(dur).add(partLeft);
                    }
                }
                default -> {
                    if (pat.where != PatternLocation.HUB_INTERNAL)
                        lim = have.div(pat.inputAmount(s));   // sent along and expected back
                    else if (have.compareTo(pat.inputAmount(s)) < 0)
                        return Amount.ZERO;
                    else
                        continue;
                }
            }
            max = max == null ? lim : max.min(lim);
        }
        return max == null ? UNBOUNDED : max.clampNonNegative();
    }

    private static final Amount UNBOUNDED = Amount.of(Long.MAX_VALUE).mul(Amount.of(Long.MAX_VALUE));

    private Amount usable(Job job, Task t, int item, boolean withLater) {
        Amount have = job.inventory.get(item);
        if (withLater)
            have = have.add(job.drawLater.get(item));
        boolean owner = t.scc != -1 && job.sccSeeds.getOrDefault(t.scc, new AmountMap()).contains(item);
        for (Plan.Recycle r : job.recycleGuards)
            owner |= r.item() == item && r.has(t.pattern);
        if (!owner)
            have = have.sub(job.protectedSeeds.get(item));
        return have.clampNonNegative();
    }

    // ───────────────────────────── hub-internal runs ─────────────────────────────

    private void consume(Job job, Pattern pat, Amount n) {
        for (int s = 0; s < pat.inputCount(); s++) {
            int item = pat.inputItem(s);
            switch (pat.inputKind(s)) {
                case CONSUME -> takeSlot(job, pat, s, pat.inputAmount(s).mul(n));
                case TOOL -> {
                    int dur = pat.inputDurability(s);
                    if (dur <= 0) {
                        takeSlot(job, pat, s, pat.inputAmount(s).mul(n));
                        break;
                    }
                    Amount before = job.toolUses.get(item);
                    Amount after = before.add(n);
                    Amount tools = after.ceilDiv(dur).sub(before.ceilDiv(dur));
                    job.toolUses.set(item, after);
                    takeSlot(job, pat, s, tools);
                }
                default -> {
                    // catalysts and containers stay in the CPU
                }
            }
        }
    }

    /** Takes from the slot's own item first, then (substitution allowed) from the others, most stocked first. */
    private void takeSlot(Job job, Pattern pat, int s, Amount amount) {
        Amount left = amount;
        left = left.sub(removeFromJob(job, pat.inputItem(s), left));
        if (left.signum() > 0 && pat.allowSubst) {
            List<Integer> alts = new ArrayList<>();
            for (int a = 0; a < pat.alternativeCount(s); a++)
                alts.add(pat.alternative(s, a));
            alts.sort((x, y) -> job.inventory.get(y).compareTo(job.inventory.get(x)));
            for (int alt : alts) {
                if (left.signum() <= 0)
                    break;
                left = left.sub(removeFromJob(job, alt, left));
            }
        }
    }

    /** Items leaving the job (used up or sent out) release capacity. */
    private Amount removeFromJob(Job job, int item, Amount want) {
        Amount got = job.inventory.get(item).min(want).clampNonNegative();
        if (got.signum() > 0) {
            job.inventory.sub(item, got);
            releaseItems(job, item, got);
        }
        return got;
    }

    /**
     * {@code amount} of {@code item} left the job: its bytes. Keys counted in fractions of a byte
     * (fluids) release whole bytes as their amounts add up; the rest goes at the end.
     */
    private void releaseItems(Job job, int item, Amount amount) {
        int apb = job.snap.units().amountPerByte(item);
        if (apb == 8 || apb <= 0) {
            release(job, amount);
            return;
        }
        Amount units = job.releasedUnits.get(item).add(amount);
        job.releasedUnits.set(item, units);
        Amount bytes = units.mul(8).div(Amount.of(apb));
        release(job, bytes.sub(job.releasedBytes.get(item)));
        job.releasedBytes.set(item, bytes);
    }

    /** Items back in the job (a machine refused them): {@link #releaseItems} undone. */
    private void reoccupy(Job job, int item, Amount amount) {
        int apb = job.snap.units().amountPerByte(item);
        Amount back = amount;
        if (apb != 8 && apb > 0) {
            Amount units = job.releasedUnits.get(item).sub(amount).clampNonNegative();
            job.releasedUnits.set(item, units);
            Amount bytes = units.mul(8).div(Amount.of(apb));
            back = job.releasedBytes.get(item).sub(bytes);
            job.releasedBytes.set(item, bytes);
        }
        job.occupancy = job.occupancy.add(back);
        used = used.add(back);
    }

    private void release(Job job, Amount a) {
        Amount r = a.min(job.occupancy);
        job.occupancy = job.occupancy.sub(r);
        used = used.sub(r).clampNonNegative();
    }

    /** Outputs that come with a probability are drawn: each run gives all of its amount or nothing. */
    private void produce(Job job, Pattern pat, Amount n, @Nullable ChanceProgress chance) {
        for (int o = 0; o < pat.outputCount(); o++) {
            Amount runs = pat.isChance(o) ? ChanceSampler.successes(n, pat.outputChance(o), random) : n;
            Amount made = pat.outputAmount(o).mul(runs);
            if (chance != null)
                chance.gave(pat.outputItem(o), made);
            if (made.signum() <= 0)
                continue;
            job.inventory.add(pat.outputItem(o), made);
            notifyItem(job, pat.outputItem(o));
        }
        if (chance != null)
            chance.ran(n);
    }

    /**
     * After runs of a pattern run for probable outputs: stop it once it has what the plan counts on
     * (the materials of the runs left over go back at the end), or, when every planned run is done
     * and it is still short, mark 機率產物不足 and wait for the gap like for a machine's return. No
     * runs are added on top of the plan.
     */
    private void chanceCheck(Job job, Task t, ChanceProgress chance, long now) {
        if (chance.satisfied()) {
            t.remaining = Amount.ZERO;
            return;
        }
        if (t.remaining.signum() > 0)
            return;
        AmountMap gap = chance.shortfall();
        gap.forEach((item, a) -> {
            job.chanceShort.add(item, a);
            expect(job, item, a, now);
        });
    }

    // ───────────────────────────── dispatch ─────────────────────────────

    /**
     * Even split within the highest-priority group of machines that still have window room; leftover
     * copies go one each from the rotation start; only then the next group. N copies = N operations.
     * Returns the runs actually sent.
     */
    private Amount dispatchEven(Job job, Task t, Pattern pat, Amount n, ExecutionEnv env) {
        if (!(pat.source() instanceof IPatternDetails details))
            return Amount.ZERO;
        TreeListByPriority groups = new TreeListByPriority();
        for (ICraftingProvider prov : env.providers().apply(details))
            if (!prov.isBusy())
                groups.add(prov.getPatternPriority(), prov);
        long want = n.saturatedLong();
        long sent = 0;
        for (List<ICraftingProvider> group : groups.highestFirst()) {
            List<ICraftingProvider> open = new ArrayList<>(group);
            while (sent < want && !open.isEmpty()) {
                long left = want - sent;
                int m = open.size();
                long share = left / m;
                long extra = left % m;
                boolean progress = false;
                List<ICraftingProvider> full = new ArrayList<>();
                for (int i = 0; i < m; i++) {
                    ICraftingProvider prov = open.get((i + rotate) % m);
                    long k = share + (i < extra ? 1 : 0);
                    if (k <= 0)
                        continue;
                    Window w = windows.computeIfAbsent(new WindowKey(prov, t.pattern), key -> new Window());
                    long cap = w.size.saturatedLong();
                    k = Math.min(k, cap);
                    long pushed = 0;
                    boolean outOfStock = false;
                    for (long c = 0; c < k; c++) {
                        KeyCounter[] inputs = extractInputs(job, pat, env);
                        if (inputs == null) {
                            outOfStock = true;
                            break;
                        }
                        if (!prov.pushPattern(details, inputs)) {
                            returnInputs(job, inputs, env);
                            break;
                        }
                        pushed++;
                    }
                    w.onPushResult(Amount.of(k), Amount.of(pushed));
                    if (pushed > 0) {
                        w.lastSendNanos = env.nowNanos();
                        expectOutputs(job, t, pat, pushed, env.nowNanos());
                        sent += pushed;
                        progress = true;
                    }
                    if (outOfStock)
                        return Amount.of(sent);
                    if (pushed < k || pushed >= cap)
                        full.add(prov);     // refused, or its window is used up for this tick
                }
                open.removeAll(full);
                if (!progress)
                    break;
            }
            if (sent >= want)
                break;
        }
        return Amount.of(sent);
    }

    /** Providers grouped by pattern priority. */
    private static final class TreeListByPriority {
        private final java.util.TreeMap<Integer, List<ICraftingProvider>> map = new java.util.TreeMap<>(Collections.reverseOrder());

        void add(int priority, ICraftingProvider p) {
            map.computeIfAbsent(priority, k -> new ArrayList<>()).add(p);
        }

        Iterable<List<ICraftingProvider>> highestFirst() {
            return map.values();
        }
    }

    /** One run's inputs, slot by slot as AE2 expects; null (nothing taken) when the job lacks them. */
    private KeyCounter @Nullable [] extractInputs(Job job, Pattern pat, ExecutionEnv env) {
        KeyCounter[] out = new KeyCounter[pat.inputCount()];
        AmountMap taken = new AmountMap();
        for (int s = 0; s < pat.inputCount(); s++) {
            if (pat.inputAmount(s).isBig())
                return rollback(job, taken);
            Amount amt = pat.inputAmount(s);
            int item = pat.inputItem(s);
            if (job.inventory.get(item).compareTo(amt) < 0 && pat.allowSubst) {
                for (int a = 0; a < pat.alternativeCount(s); a++)
                    if (job.inventory.get(pat.alternative(s, a)).compareTo(amt) >= 0) {
                        item = pat.alternative(s, a);
                        break;
                    }
            }
            AEKey key = env.index().keyOf(item);
            if (key == null || job.inventory.get(item).compareTo(amt) < 0)
                return rollback(job, taken);
            job.inventory.sub(item, amt);
            taken.add(item, amt);
            out[s] = new KeyCounter();
            out[s].add(key, amt.longValue());
        }
        taken.forEach((id, a) -> releaseItems(job, id, a));
        return out;
    }

    private static KeyCounter @Nullable [] rollback(Job job, AmountMap taken) {
        taken.forEach(job.inventory::add);
        return null;
    }

    private void returnInputs(Job job, KeyCounter[] inputs, ExecutionEnv env) {
        for (KeyCounter kc : inputs) {
            if (kc == null)
                continue;
            for (Object2LongMap.Entry<AEKey> e : kc) {
                int id = env.index().idOf(e.getKey());
                Amount a = Amount.of(e.getLongValue());
                job.inventory.add(id, a);
                reoccupy(job, id, a);
            }
        }
    }

    /** Only the products the pattern states count as completion; containers sent along come back too. */
    private void expectOutputs(Job job, Task t, Pattern pat, long runs, long now) {
        Amount r = Amount.of(runs);
        for (int o = 0; o < pat.outputCount(); o++)
            expect(job, pat.outputItem(o), pat.outputAmount(o).mul(r), now);
        for (int s = 0; s < pat.inputCount(); s++)
            if (pat.inputKind(s) == InputKind.CONTAINER || pat.inputKind(s) == InputKind.CATALYST)
                expect(job, pat.inputItem(s), pat.inputAmount(s).mul(r), now);
        t.inFlight = t.inFlight.add(r);
    }

    private static void expect(Job job, int item, Amount a, long now) {
        if (!job.outstanding.contains(item))
            job.outstandingSince.put(item, now);
        job.outstanding.add(item, a);
    }

    // ───────────────────────────── returns ─────────────────────────────

    /**
     * AE2's insertIntoCpus (called through CraftingServiceStorage, mounted at the highest priority):
     * keep only what jobs still expect; the rest flows on into the network. Returns the amount kept.
     */
    public Amount onInsert(int item, Amount amount, long nowNanos) {
        Amount room = requestedAmount(item);
        Amount take = amount.min(room);
        if (take.signum() <= 0)
            return Amount.ZERO;
        pool.add(item, take);
        for (Map.Entry<WindowKey, Window> e : windows.entrySet()) {
            Window w = e.getValue();
            if (w.lastSendNanos >= 0 && producesItem(e.getKey().pattern(), item))
                w.recordRoundTrip(nowNanos - w.lastSendNanos);
        }
        return take;
    }

    private boolean producesItem(int pattern, int item) {
        for (Job j : jobs) {
            Pattern p = j.snap.pattern(pattern);
            if (p != null)
                return p.produces(item);
        }
        return false;
    }

    /** The job takes what it expects from the pool (jobs take turns: the tick's rotation order). */
    private void pullReturns(Job job, ExecutionEnv env) {
        for (int item : job.outstanding.keys()) {
            Amount got = pool.get(item).min(job.outstanding.get(item));
            if (got.signum() <= 0)
                continue;
            pool.sub(item, got);
            job.outstanding.sub(item, got);
            job.inventory.add(item, got);
            job.chanceShort.sub(item, got.min(job.chanceShort.get(item)));
            job.stuck.remove(item);
            if (job.outstanding.contains(item))
                job.outstandingSince.put(item, env.nowNanos());
            else
                job.outstandingSince.remove(item);
            for (Task t : job.tasks) {
                Pattern p = job.snap.pattern(t.pattern);
                Amount per = p.outputAmountOf(item);
                if (per.signum() > 0 && t.inFlight.signum() > 0)
                    t.inFlight = t.inFlight.sub(got.ceilDiv(per).min(t.inFlight));
            }
            notifyItem(job, item);
        }
    }

    /** Finished products go to the network batch by batch (unless a remaining task still needs them). */
    private void deliverFinals(Job job, ExecutionEnv env) {
        int target = job.plan.target;
        for (Task t : job.tasks)
            if (t.remaining.signum() > 0 && job.snap.pattern(t.pattern).hasInput(target))
                return;
        Amount have = job.inventory.get(target);
        if (have.signum() <= 0)
            return;
        Amount in = Amount.ZERO;
        AEKey key = env.index().keyOf(target);
        if (job.sink != null && key != null) {
            // the requester first (AE2 does the same through the crafting link)
            Amount left = have;
            for (int c = 0; c < MAX_CHUNKS_PER_CALL && left.signum() > 0; c++) {
                long chunk = left.saturatedLong();
                long took = job.sink.insert(key, chunk);
                in = in.add(Amount.of(took));
                left = left.sub(Amount.of(took));
                if (took < chunk)
                    break;
            }
        }
        if (in.compareTo(have) < 0)
            in = in.add(insertAll(env, target, have.sub(in)));
        job.inventory.sub(target, in);
        job.delivered = job.delivered.add(in);
        releaseItems(job, target, in);
    }

    /** The requester's crafting link takes longs: an astronomical amount goes to it a few chunks per call. */
    private static final int MAX_CHUNKS_PER_CALL = 16;

    /** Into network storage (exactly, when the grid has a big storage); returns what went in. */
    private static Amount insertAll(ExecutionEnv env, int item, Amount amount) {
        AEKey key = env.index().keyOf(item);
        return key == null ? Amount.ZERO : env.store().insert(key, amount);
    }

    // ───────────────────────────── end, cancel, problems ─────────────────────────────

    /** Everything left goes back: ledger extras, rounding, containers, catalysts, tools, loop seeds. */
    private void finish(Job job, ExecutionEnv env) {
        if (returnInventory(job, env)) {
            job.finished = true;
            release(job, job.occupancy);
        }
    }

    /** Cancel: inventory back as it is (intermediates are not taken apart), expectations dropped, capacity freed. */
    public void cancel(Job job, ExecutionEnv env) {
        job.cancelled = true;
        job.outstanding.clear();
        job.outstandingSince.clear();
        returnInventory(job, env);
        release(job, job.occupancy);
        if (job.inventory.isEmpty())
            job.finished = true;
        jobs.removeIf(j -> j.finished);
    }

    /** True when all of it went in (a full network keeps the rest in the job for the next tick). */
    private boolean returnInventory(Job job, ExecutionEnv env) {
        for (int item : job.inventory.keys()) {
            Amount in = insertAll(env, item, job.inventory.get(item));
            job.inventory.sub(item, in);
        }
        return job.inventory.isEmpty();
    }

    private void checkStuck(long now) {
        for (Job job : jobs) {
            if (job.suspended)
                continue;       // paused: the stuck timer stops
            for (int item : job.outstanding.keys()) {
                Long since = job.outstandingSince.get(item);
                if (since == null)
                    continue;
                int pattern = -1;
                long median = -1;
                for (Task t : job.tasks)
                    if (job.snap.pattern(t.pattern).produces(item)) {
                        pattern = t.pattern;
                        for (Map.Entry<WindowKey, Window> e : windows.entrySet())
                            if (e.getKey().pattern() == t.pattern)
                                median = Math.max(median, e.getValue().medianRoundTrip());
                    }
                long threshold = median >= 0 ? Math.max(median * STUCK_RTT_FACTOR, STUCK_MIN_NANOS) : STUCK_DEFAULT_NANOS;
                long waited = now - since;
                if (waited > threshold)
                    job.stuck.put(item, new StuckMark(item, pattern, waited));
            }
        }
    }

    /** F9: the hub was down; jobs with things out get a notice and the manual "pull from network" button. */
    public void onHubResume() {
        for (Job job : jobs)
            if (!job.outstanding.isEmpty())
                job.unloaded = true;
    }

    /** Player pressed the button: take at most the gap from network storage; real returns beyond that flow on. */
    public void pullFromNetwork(Job job, ExecutionEnv env) {
        for (int item : job.outstanding.keys()) {
            Amount gap = job.outstanding.get(item).sub(pool.get(item).min(job.outstanding.get(item)));
            AEKey key = env.index().keyOf(item);
            if (gap.signum() <= 0 || key == null)
                continue;
            Amount a = env.store().extract(key, gap);
            if (a.signum() <= 0)
                continue;
            job.outstanding.sub(item, a);
            job.inventory.add(item, a);
            job.chanceShort.sub(item, a.min(job.chanceShort.get(item)));
            notifyItem(job, item);
        }
        if (job.outstanding.isEmpty())
            job.unloaded = false;
    }

    // ───────────────────────────── loops ─────────────────────────────

    /**
     * Runs the available energy pays for. Energy is counted exactly (fractions of AE): the hub's own
     * storage first, the AE2 network (double) for the rest.
     */
    private Amount energyCap(Pattern pat, Amount n, ExecutionEnv env) {
        Rational per = pat.energyPerRun;
        if (per.signum() <= 0 || n.signum() <= 0)
            return n;
        Rational want = per.mul(Rational.of(n));
        Rational have = Rational.ZERO;
        if (env.hubEnergy() != null)
            have = env.hubEnergy().extractEnergy(want, true);
        if (have.compareTo(want) < 0) {
            double rest = want.sub(have).toDoubleSaturated();
            double got = env.energy().extractAEPower(rest, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            // AE2 energy is a double: a need beyond its range is asked as Double.MAX_VALUE. A network
            // that still gives all of that (creative energy cell) is infinite, like infinite item
            // sources, and does not cap the runs
            if (rest == Double.MAX_VALUE && got >= rest)
                return n;
            have = have.add(Rational.of(Double.isFinite(got) ? got : Double.MAX_VALUE));
        }
        if (have.compareTo(want) >= 0)
            return n;
        return n.min(Amount.of(have.div(per).floor()));
    }

    private void payEnergy(Pattern pat, Amount n, ExecutionEnv env) {
        Rational per = pat.energyPerRun;
        if (per.signum() <= 0 || n.signum() <= 0)
            return;
        Rational cost = per.mul(Rational.of(n));
        if (env.hubEnergy() != null)
            cost = cost.sub(env.hubEnergy().extractEnergy(cost, false));
        if (cost.signum() > 0)
            env.energy().extractAEPower(cost.toDoubleSaturated(), Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    /**
     * Infinite-source items come in as the job needs them (they could not be pulled at submit):
     * enough for {@code n} runs, any amount, while the source is still infinite.
     */
    private void drawLazily(Job job, Pattern pat, Amount n, ExecutionEnv env) {
        for (int s = 0; s < pat.inputCount(); s++) {
            int item = pat.inputItem(s);
            Amount later = job.drawLater.get(item);
            if (later.signum() <= 0)
                continue;
            Amount need = switch (pat.inputKind(s)) {
                case CONSUME -> pat.inputAmount(s).mul(n);
                case TOOL -> pat.inputDurability(s) > 0 ? n.ceilDiv(pat.inputDurability(s)) : pat.inputAmount(s).mul(n);
                default -> pat.where == PatternLocation.HUB_INTERNAL ? pat.inputAmount(s) : pat.inputAmount(s).mul(n);
            };
            need = need.sub(job.inventory.get(item)).min(later);
            AEKey key = env.index().keyOf(item);
            if (need.signum() <= 0 || key == null)
                continue;
            // an infinite source does not run out, and AE2 extracts at most one long per call: while it
            // is still infinite, the whole need is credited in one go
            if (env.store().isInfinite(key)) {
                job.inventory.add(item, need);
                job.drawLater.sub(item, need);
                continue;
            }
            // the source is gone (cell removed): take what real storage gives
            Amount got = env.store().extract(key, need);
            if (got.signum() > 0) {
                job.inventory.add(item, got);
                job.drawLater.sub(item, got);
            }
        }
    }

    /** A loop with a contract runs in the contract's order and batch sizes. */
    private Amount contractCap(Job job, Task t, Amount n) {
        if (t.scc == -1 || !job.contractStep.containsKey(t.scc))
            return n;
        CycleContract c = job.plan.contracts.get(t.scc);
        int step = job.contractStep.get(t.scc);
        if (c.patterns()[step] != t.pattern)
            return Amount.ZERO;
        return n.min(job.contractLeft.get(t.scc));
    }

    private void advanceContract(Job job, Task t, Amount n) {
        if (t.scc == -1 || !job.contractStep.containsKey(t.scc))
            return;
        Amount left = job.contractLeft.get(t.scc).sub(n);
        if (left.signum() > 0) {
            job.contractLeft.put(t.scc, left);
            return;
        }
        CycleContract c = job.plan.contracts.get(t.scc);
        int step = job.contractStep.get(t.scc) + 1;
        if (step >= c.patterns().length) {
            job.contractStep.remove(t.scc);    // done: plain rules from here on
            job.contractLeft.remove(t.scc);
        } else {
            job.contractStep.put(t.scc, step);
            job.contractLeft.put(t.scc, c.batches()[step]);
        }
        for (Task other : job.tasks)
            if (other.scc == t.scc)
                checkReady(job, other);
    }

    /** A loop that uses an item again holds its seed until every pattern of the loop has run. */
    private void releaseRecycleIfLoopDone(Job job, int pattern) {
        for (Iterator<Plan.Recycle> it = job.recycleGuards.iterator(); it.hasNext(); ) {
            Plan.Recycle r = it.next();
            if (!r.has(pattern))
                continue;
            boolean done = true;
            for (Task t : job.tasks)
                if (r.has(t.pattern) && t.remaining.signum() > 0)
                    done = false;
            if (!done)
                continue;
            it.remove();
            job.protectedSeeds.sub(r.item(), r.seed());
            notifyItem(job, r.item());
        }
    }

    /** Seeds stay protected until the whole loop has run; then they are ordinary stock again. */
    private void releaseSeedsIfLoopDone(Job job, int scc) {
        for (Task t : job.tasks)
            if (t.scc == scc && t.remaining.signum() > 0)
                return;
        AmountMap seeds = job.sccSeeds.remove(scc);
        if (seeds == null)
            return;
        seeds.forEach(job.protectedSeeds::sub);
        for (int item : seeds.keys())
            notifyItem(job, item);
    }

    /** Snapshot of the window table, for debugging screens. */
    public Map<String, Amount> windowSizes() {
        Map<String, Amount> m = new HashMap<>();
        windows.forEach((k, w) -> m.put(k.provider() + "#" + k.pattern(), w.size));
        return m;
    }
}
