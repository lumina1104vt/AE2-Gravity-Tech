package com.lumina.ae2_gvt.items.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * When an ME interface mode machine access interface (with a crafting card) asks the network to craft, and when it must not.
 * Pure bookkeeping, no Minecraft classes: {@link MachineAccessCore} feeds it what it sees and does what it says.
 * <p>
 * Example: a furnace is set to keep 64 iron ingots, holds 10, the network has 20: 34 are missing, so one calculation for 34 is
 * started. While that job runs, or is being calculated, nothing more is asked for that item. If the network has no pattern for it
 * (or lacks the materials) the try fails, and the next one waits 5 seconds, then 10, 20 ... up to 5 minutes, so a missing
 * pattern does not make a calculation every half second. A job that ended (done or cancelled) waits a short time too.
 * <p>
 * One entry per item (not per slot): two machines that both want iron ingots share one request for the sum of what is missing.
 */
public final class StockCrafting<K> {
    /** ticks: the first wait after a failed try; doubles each failure up to {@link #MAX_BACKOFF} */
    public static final long BASE_BACKOFF = 100;
    public static final long MAX_BACKOFF = 6000;
    /** ticks to wait after a job ended before the same item is asked for again */
    public static final long AFTER_JOB = 40;
    /** calculations running at once for one interface (AE2 plans on a worker thread; a few at a time is enough) */
    public static final int MAX_CALCULATING = 2;

    public enum Phase {
        /** nothing going on (or a short wait after a job) */
        IDLE,
        /** the plan is being calculated */
        CALCULATING,
        /** a job is running on a crafting CPU (or the hub) */
        RUNNING,
        /** the last try failed: no pattern, missing materials, no CPU free; it tries again when the wait is over */
        RETRY
    }

    public enum Step {
        NOTHING, CALCULATE
    }

    private static final class Entry {
        Phase phase = Phase.IDLE;
        int failures;
        long retryAt;
        long amount;
    }

    private final Map<K, Entry> entries = new HashMap<>();

    /** How many of an item are missing: what is wanted, less what the machines hold and what the network has. Never negative. */
    public static long shortfall(long wanted, long held, long inNetwork) {
        if (wanted <= 0)
            return 0;
        long have = Math.max(0, held) + Math.max(0, inNetwork);
        if (have < 0)
            have = Long.MAX_VALUE;      // a sum too big for a long is plenty
        return Math.max(0, wanted - have);
    }

    /** Ticks to wait after the {@code failures}-th failure in a row (1, 2, 3 ...): 100, 200, 400 ... capped. */
    public static long backoff(int failures) {
        if (failures <= 0)
            return 0;
        int shift = Math.min(failures - 1, 16);
        return Math.min(MAX_BACKOFF, BASE_BACKOFF << shift);
    }

    private Entry entry(K key) {
        return entries.computeIfAbsent(key, k -> new Entry());
    }

    /**
     * Asks whether to start calculating a craft of {@code need} of {@code key} now. {@link Step#CALCULATE} also moves the item to
     * {@link Phase#CALCULATING}: the caller must start the calculation.
     */
    public Step poll(K key, long need, long now) {
        if (need <= 0) {
            forgetIfQuiet(key);
            return Step.NOTHING;
        }
        Entry e = entry(key);
        if (e.phase == Phase.CALCULATING || e.phase == Phase.RUNNING)
            return Step.NOTHING;            // already asked: never twice for the same item
        if (now < e.retryAt)
            return Step.NOTHING;
        if (calculating() >= MAX_CALCULATING)
            return Step.NOTHING;
        e.phase = Phase.CALCULATING;
        e.amount = need;
        return Step.CALCULATE;
    }

    /**
     * The calculation of {@code key} finished. {@code usable}: the plan can run (not a simulation with missing items);
     * {@code stillNeeded}: the item is still missing now. Returns whether to submit the plan.
     */
    public boolean planFinished(K key, boolean usable, boolean stillNeeded, long now) {
        Entry e = entries.get(key);
        if (e == null || e.phase != Phase.CALCULATING)
            return false;
        if (!usable) {
            fail(e, now);
            return false;
        }
        if (!stillNeeded) {                 // the network got it in the meantime: drop the plan, no failure
            e.phase = Phase.IDLE;
            e.failures = 0;
            e.retryAt = 0;
            return false;
        }
        return true;
    }

    /** The plan was submitted: it is running, or the submit was refused (no CPU, too small ...). */
    public void submitted(K key, boolean ok, long now) {
        Entry e = entries.get(key);
        if (e == null || e.phase != Phase.CALCULATING)
            return;
        if (!ok) {
            fail(e, now);
            return;
        }
        e.phase = Phase.RUNNING;
        e.failures = 0;
        e.retryAt = 0;
    }

    /** The job of {@code key} ended: done, or cancelled (a cancel counts as a failure so a cancelled job is not restarted at once). */
    public void jobEnded(K key, boolean done, long now) {
        Entry e = entries.get(key);
        if (e == null || e.phase != Phase.RUNNING)
            return;
        if (done) {
            e.phase = Phase.IDLE;
            e.failures = 0;
            e.retryAt = now + AFTER_JOB;
        } else {
            fail(e, now);
        }
    }

    /** A job that was running when the world was saved: it is still running, nothing is asked for again. */
    public void restore(K key, long amount) {
        Entry e = entry(key);
        e.phase = Phase.RUNNING;
        e.amount = amount;
        e.failures = 0;
        e.retryAt = 0;
    }

    private void fail(Entry e, long now) {
        e.phase = Phase.RETRY;
        e.failures++;
        e.retryAt = now + backoff(e.failures);
    }

    /** An item nobody wants any more and nothing is going on for: forget it (its failures too). */
    private void forgetIfQuiet(K key) {
        Entry e = entries.get(key);
        if (e != null && (e.phase == Phase.IDLE || e.phase == Phase.RETRY))
            entries.remove(key);
    }

    /** Forget everything (the crafting card came out). */
    public void clear() {
        entries.clear();
    }

    /** Forget one item. */
    public void forget(K key) {
        entries.remove(key);
    }

    private int calculating() {
        int n = 0;
        for (Entry e : entries.values())
            if (e.phase == Phase.CALCULATING)
                n++;
        return n;
    }

    public Phase phaseOf(K key) {
        Entry e = entries.get(key);
        return e == null ? Phase.IDLE : e.phase;
    }

    /** Ticks until the next try of {@code key} (0 when it is not waiting). */
    public long retryIn(K key, long now) {
        Entry e = entries.get(key);
        return e == null || e.phase != Phase.RETRY ? 0 : Math.max(0, e.retryAt - now);
    }

    public int failuresOf(K key) {
        Entry e = entries.get(key);
        return e == null ? 0 : e.failures;
    }

    public long amountOf(K key) {
        Entry e = entries.get(key);
        return e == null ? 0 : e.amount;
    }

    /** The items with something going on (calculating or running), which need looking after even when no longer wanted. */
    public List<K> active() {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Entry> m : entries.entrySet())
            if (m.getValue().phase == Phase.CALCULATING || m.getValue().phase == Phase.RUNNING)
                out.add(m.getKey());
        return out;
    }

    /** Every item with an entry (for looking after the ones nobody wants any more). */
    public List<K> keys() {
        return new ArrayList<>(entries.keySet());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
