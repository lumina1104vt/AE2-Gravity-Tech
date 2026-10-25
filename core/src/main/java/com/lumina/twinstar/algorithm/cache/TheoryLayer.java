package com.lumina.twinstar.algorithm.cache;

import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.AmountMap;
import com.lumina.twinstar.algorithm.model.InputKind;
import com.lumina.twinstar.algorithm.model.LogMath;
import com.lumina.twinstar.algorithm.model.Pattern;
import com.lumina.twinstar.algorithm.model.Rational;
import com.lumina.twinstar.algorithm.model.SccKind;
import com.lumina.twinstar.algorithm.model.StockSnapshot;
import com.lumina.twinstar.algorithm.planning.ExactSimplex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;

/**
 * A — the theory layer. Keeps, for every item, the cheapest way to make it in theory (cost, best
 * pattern, flattened BOM, level, policy cycles), independent of any order. Stock only enters through
 * raw prices, which change when a raw's stock crosses a power of ten.
 * <p>
 * Built bottom-up once; afterwards only the affected paths are repaired, level by level, stopping
 * where nothing changed. Changes that touch a policy cycle, or that would close a new one, fall back
 * to a full rebuild (rare; the rebuild is linear apart from the policy iteration of cycles).
 * <p>
 * Not thread-safe by itself: every public method is synchronized. {@link #snapshot()} returns the
 * last published immutable version without locking.
 */
public final class TheoryLayer {
    /** log10 of the price of a raw with no stock (10: using it always means a shortage). */
    public static final double LOG_HIGH = 1.0;
    /** A tier only drops once the stock falls to 0.8 × 10^tier (no flapping at the boundary). */
    public static final double HYSTERESIS = 0.8;
    /** Relative change treated as "unchanged" (stops propagation; policy switches need more). 【待定】 */
    public static final double EPS = 1e-9;
    private static final int MAX_POLICY_ITERATIONS = 64;
    private static final int[] EMPTY = new int[0];

    // ── patterns (index = id) ──
    private final ArrayList<Pattern> patterns = new ArrayList<>();
    private double[] patRunLogCost = new double[0];
    private int[][] patBomItems = new int[0][];
    private double[][] patBomLog = new double[0][];

    // ── items (index = id) ──
    private int items;
    private int[] level = new int[0];
    private int[] scc = new int[0];
    private double[] logCost = new double[0];
    private int[] best = new int[0];
    private int[][] bomItems = new int[0][];
    private double[][] bomLog = new double[0][];
    private int[][] producers = new int[0][];
    private int[][] consumers = new int[0][];
    private int[] priceTier = new int[0];
    private final BitSet noStock = new BitSet();
    private final BitSet infinite = new BitSet();
    /**
     * Items only loops without an outside input make, loops that do not grow (raw iron ⇄ raw iron
     * block, wheat ⇄ hay bale): they are mined, so they are priced like raws by their own stock;
     * their recipes stay as conversions. Decided by a rebuild only.
     */
    private final BitSet mined = new BitSet();
    /** a pattern was added or removed since the last flush */
    private boolean patternsChanged;
    private final ArrayList<Scc> sccs = new ArrayList<>();

    private final DirtySet dirty = new DirtySet();
    private boolean needRebuild = true;
    private long version;
    private volatile Snapshot published = null;
    private volatile FlushStats lastFlush = new FlushStats(0, false, false, 0, 0);
    private int processed;

    // ─────────────────────────────── public API ───────────────────────────────

    /** The last published version (null before the first flush). */
    public Snapshot snapshot() {
        return published;
    }

    public synchronized void addPattern(Pattern p) {
        ensurePatterns(p.id + 1);
        if (patterns.get(p.id) != null)
            removePattern(p.id);
        int maxItem = -1;
        for (int i = 0; i < p.inputCount(); i++) {
            maxItem = Math.max(maxItem, p.inputItem(i));
            for (int j = 0; j < p.alternativeCount(i); j++)
                maxItem = Math.max(maxItem, p.alternative(i, j));
        }
        for (int i = 0; i < p.outputCount(); i++)
            maxItem = Math.max(maxItem, p.outputItem(i));
        ensureItems(maxItem + 1);
        patterns.set(p.id, p);
        for (int i = 0; i < p.outputCount(); i++)
            producers[p.outputItem(i)] = append(producers[p.outputItem(i)], p.id);
        for (int in : distinctInputs(p))
            consumers[in] = append(consumers[in], p.id);
        patternsChanged = true;     // flush checks whether the mined items moved
        if (!needRebuild) {
            computeRunCost(p.id);
            computePatBom(p.id);
            for (int i = 0; i < p.outputCount(); i++)
                markDirty(p.outputItem(i));
        }
    }

    public synchronized void removePattern(int id) {
        if (id < 0 || id >= patterns.size() || patterns.get(id) == null)
            return;
        Pattern p = patterns.get(id);
        for (int i = 0; i < p.outputCount(); i++)
            producers[p.outputItem(i)] = without(producers[p.outputItem(i)], id);
        for (int in : distinctInputs(p))
            consumers[in] = without(consumers[in], id);
        patterns.set(id, null);
        patBomItems[id] = EMPTY;
        patBomLog[id] = new double[0];
        if (needRebuild)
            return;
        patternsChanged = true;
        for (Scc s : sccs)
            for (int j = 0; j < s.patternCount(); j++)
                if (s.pattern(j) == id)
                    needRebuild = true;
        for (int i = 0; i < p.outputCount(); i++)
            markDirty(p.outputItem(i));
    }

    /** Reads a whole stock snapshot; only raws whose price tier changed become dirty. */
    public synchronized void applyStock(StockSnapshot s) {
        ensureItems(s.size());
        for (int k = 0; k < items; k++)
            onStockChanged(k, s.stock(k), s.isInfinite(k));
    }

    /** Price tiers have a buffer: up at 10^(t+1), down only at 0.8 × 10^t. */
    public synchronized void onStockChanged(int k, Amount stock, boolean isInfinite) {
        ensureItems(k + 1);
        boolean changed = false;
        if (isInfinite != infinite.get(k)) {
            infinite.set(k, isInfinite);
            changed = true;
        }
        boolean none = stock.signum() <= 0;
        if (none != noStock.get(k)) {
            noStock.set(k, none);
            changed = true;
        }
        if (!none) {
            double lg = stock.log10();
            int t = priceTier[k];
            while (lg >= t + 1)
                t++;
            while (t >= 1 && lg <= Math.log10(HYSTERESIS) + t)
                t--;
            if (t != priceTier[k]) {
                priceTier[k] = t;
                changed = true;
            }
        }
        if (changed && (producers[k].length == 0 || mined.get(k)))
            markDirty(k);
    }

    /** Applies all pending changes (incrementally when possible) and publishes a new version. */
    public synchronized Snapshot flush() {
        long t0 = System.nanoTime();
        processed = 0;
        if (patternsChanged && !needRebuild) {
            // a pattern can merge or split loops and so move which items are mined (cheap to
            // recompute, linear in the patterns); when it does, values around them change everywhere
            BitSet before = (BitSet) mined.clone();
            computeMined();
            if (!before.equals(mined))
                needRebuild = true;
        }
        patternsChanged = false;
        boolean rebuilt = needRebuild;
        boolean fellBack = false;
        if (needRebuild)
            build();
        else if (!incremental()) {
            fellBack = true;
            rebuilt = true;
            build();
        }
        Snapshot s = publish();
        lastFlush = new FlushStats(s.version(), rebuilt, fellBack, processed, System.nanoTime() - t0);
        return s;
    }

    /**
     * What the last flush did, for logs.
     *
     * @param rebuilt   built everything again
     * @param fellBack  started incremental but had to rebuild (a policy cycle, a new loop, or something not makeable)
     * @param processed dirty items repaired incrementally (0 for a rebuild)
     */
    public record FlushStats(long version, boolean rebuilt, boolean fellBack, int processed, long nanos) {
    }

    public FlushStats lastFlushStats() {
        return lastFlush;
    }

    /** Throws the incremental state away and builds everything again (tests compare the two). */
    public synchronized Snapshot rebuild() {
        needRebuild = true;
        return flush();
    }

    // ─────────────────────────────── storage ───────────────────────────────

    private void ensurePatterns(int n) {
        while (patterns.size() < n)
            patterns.add(null);
        if (patRunLogCost.length < n) {
            int old = patRunLogCost.length;
            patRunLogCost = Arrays.copyOf(patRunLogCost, n);
            patBomItems = Arrays.copyOf(patBomItems, n);
            patBomLog = Arrays.copyOf(patBomLog, n);
            for (int i = old; i < n; i++) {
                patRunLogCost[i] = LogMath.INF;
                patBomItems[i] = EMPTY;
                patBomLog[i] = new double[0];
            }
        }
    }

    private void ensureItems(int n) {
        if (n <= items)
            return;
        int old = items;
        items = n;
        level = Arrays.copyOf(level, n);
        scc = Arrays.copyOf(scc, n);
        logCost = Arrays.copyOf(logCost, n);
        best = Arrays.copyOf(best, n);
        bomItems = Arrays.copyOf(bomItems, n);
        bomLog = Arrays.copyOf(bomLog, n);
        producers = Arrays.copyOf(producers, n);
        consumers = Arrays.copyOf(consumers, n);
        priceTier = Arrays.copyOf(priceTier, n);
        for (int i = old; i < n; i++) {
            scc[i] = -1;
            best[i] = -1;
            producers[i] = EMPTY;
            consumers[i] = EMPTY;
            noStock.set(i);
            logCost[i] = logRawPrice(i);
            bomItems[i] = new int[]{i};
            bomLog[i] = new double[]{0};
        }
    }

    private Snapshot publish() {
        version++;
        Snapshot s = new Snapshot(version, items, level.clone(), scc.clone(), logCost.clone(), rawPrices(), best.clone(),
                bomItems.clone(), bomLog.clone(), producers.clone(), consumers.clone(),
                patterns.toArray(new Pattern[0]), Arrays.copyOf(patRunLogCost, patterns.size()),
                Arrays.copyOf(patBomItems, patterns.size()), Arrays.copyOf(patBomLog, patterns.size()),
                sccs.toArray(new Scc[0]));
        published = s;
        return s;
    }

    private double[] rawPrices() {
        double[] r = new double[items];
        for (int i = 0; i < items; i++)
            r[i] = logRawPrice(i);
        return r;
    }

    private double logRawPrice(int k) {
        if (infinite.get(k))
            return LogMath.ZERO;
        if (noStock.get(k))
            return LOG_HIGH;
        return -priceTier[k];
    }

    private void markDirty(int x) {
        dirty.push(x, level[x]);
    }

    // ─────────────────────────────── costs & BOM ───────────────────────────────

    /** log10 of the material cost of one run: consumed inputs + tools ÷ durability; catalysts and containers are free. */
    private void computeRunCost(int q) {
        Pattern p = patterns.get(q);
        double sum = LogMath.ZERO;
        for (int i = 0; i < p.inputCount(); i++) {
            int y = p.inputItem(i);
            double term;
            switch (p.inputKind(i)) {
                case CONSUME -> term = LogMath.mul(p.inputAmount(i).log10(), logCost[y]);
                case TOOL -> term = toolDurability(p, i) > 0
                        ? LogMath.mul(logCost[y], -Math.log10(toolDurability(p, i)))
                        : LogMath.mul(p.inputAmount(i).log10(), logCost[y]);
                default -> term = LogMath.ZERO;
            }
            sum = LogMath.add(sum, term);
        }
        patRunLogCost[q] = sum;
    }

    private static int toolDurability(Pattern p, int slot) {
        return p.inputDurability(slot);
    }

    private double unitCost(int q, int x) {
        Amount out = patterns.get(q).outputAmountOf(x);
        if (out.isZero())
            return LogMath.INF;
        double c = patRunLogCost[q];
        if (c == LogMath.ZERO || c == LogMath.INF)
            return c;
        return c - out.log10();
    }

    /** bom_p for one run: Σ consumed (amount × bom[input]) + Σ tools (bom[tool] ÷ durability). */
    private void computePatBom(int q) {
        Pattern p = patterns.get(q);
        TreeMap<Integer, Double> acc = new TreeMap<>();
        for (int i = 0; i < p.inputCount(); i++) {
            int y = p.inputItem(i);
            double scale;
            switch (p.inputKind(i)) {
                case CONSUME -> scale = p.inputAmount(i).log10();
                case TOOL -> scale = toolDurability(p, i) > 0 ? -Math.log10(toolDurability(p, i)) : p.inputAmount(i).log10();
                default -> {
                    continue;
                }
            }
            int[] ks = bomItems[y];
            double[] vs = bomLog[y];
            for (int j = 0; j < ks.length; j++)
                acc.merge(ks[j], vs[j] + scale, LogMath::add);
        }
        patBomItems[q] = acc.keySet().stream().mapToInt(Integer::intValue).toArray();
        patBomLog[q] = acc.values().stream().mapToDouble(Double::doubleValue).toArray();
    }

    private void setItemBomFrom(int x, int q) {
        double out = patterns.get(q).outputAmountOf(x).log10();
        int[] ks = patBomItems[q];
        double[] vs = patBomLog[q].clone();
        for (int j = 0; j < vs.length; j++)
            vs[j] -= out;
        bomItems[x] = ks;
        bomLog[x] = vs;
    }

    private void setRaw(int x) {
        logCost[x] = logRawPrice(x);
        best[x] = -1;
        bomItems[x] = new int[]{x};
        bomLog[x] = new double[]{0};
    }

    /** Route order: possible routes first, then provider priority, then cost, location, id. */
    private int compareRoutes(int a, int b, int x) {
        double ca = unitCost(a, x);
        double cb = unitCost(b, x);
        boolean fa = ca != LogMath.INF;
        boolean fb = cb != LogMath.INF;
        if (fa != fb)
            return fa ? -1 : 1;
        Pattern pa = patterns.get(a);
        Pattern pb = patterns.get(b);
        if (pa.priority != pb.priority)
            return pa.priority > pb.priority ? -1 : 1;
        int c = Double.compare(ca, cb);
        if (c != 0)
            return c;
        // a tie keeps the current best first: reversible recipes (block ⇄ 9 ingots) cost exactly the
        // same as the real source, and the id must not put "take a block apart" ahead of smelting
        if (a == best[x] || b == best[x])
            return a == best[x] ? -1 : 1;
        c = Integer.compare(pa.where.ordinal(), pb.where.ordinal());
        if (c != 0)
            return c;
        return Integer.compare(a, b);
    }

    /** Policy switches need a real gain (ties never switch). */
    private boolean strictlyBetter(int a, int b, int x) {
        if (b < 0)
            return true;
        double ca = unitCost(a, x);
        double cb = unitCost(b, x);
        boolean fa = ca != LogMath.INF;
        boolean fb = cb != LogMath.INF;
        if (fa != fb)
            return fa;
        if (!fa)
            return false;
        int prA = patterns.get(a).priority;
        int prB = patterns.get(b).priority;
        if (prA != prB)
            return prA > prB;
        if (cb == LogMath.ZERO)
            return false;
        return ca < cb + Math.log10(1 - EPS);
    }

    private void sortProducers(int x) {
        int[] ps = producers[x];
        if (ps.length < 2)
            return;
        Integer[] boxed = new Integer[ps.length];
        for (int i = 0; i < ps.length; i++)
            boxed[i] = ps[i];
        Arrays.sort(boxed, (a, b) -> compareRoutes(a, b, x));
        int[] sorted = new int[ps.length];
        for (int i = 0; i < ps.length; i++)
            sorted[i] = boxed[i];
        producers[x] = sorted;
    }

    private int maxInputLevel(int q) {
        Pattern p = patterns.get(q);
        int m = 0;
        for (int i = 0; i < p.inputCount(); i++)
            m = Math.max(m, level[p.inputItem(i)]);
        return m;
    }

    // ─────────────────────────────── full build ───────────────────────────────

    private void build() {
        dirty.clear();
        sccs.clear();
        for (int i = 0; i < items; i++) {
            // a rebuild must not depend on history: start every producer list in id order
            producers[i] = producers[i].clone();
            Arrays.sort(producers[i]);
            scc[i] = -1;
            level[i] = 0;
            best[i] = -1;
            logCost[i] = LogMath.INF;
        }
        computeMined();
        int[] all = new int[items];
        for (int i = 0; i < items; i++)
            all[i] = i;
        // the complete graph (every pattern, input → output); only the first build walks all of it
        List<int[]> comps = Tarjan.components(all, this::successors);
        for (int[] comp : comps) {
            if (comp.length == 1 && !selfLoop(comp[0]))
                processSingle(comp[0]);
            else
                policyIteration(comp);
        }
        // B and C1 take the first route as the policy: policy iteration picks best[] on its own
        // (ties never switch), so put it in front where the sorted order differs
        for (int i = 0; i < items; i++)
            bestFirst(i);
        needRebuild = false;
    }

    private void bestFirst(int x) {
        int[] ps = producers[x];
        int b = best[x];
        if (b < 0 || ps.length < 2 || ps[0] == b)
            return;
        int[] out = new int[ps.length];
        out[0] = b;
        int k = 1;
        for (int q : ps)
            if (q != b)
                out[k++] = q;
        producers[x] = out;
    }

    /**
     * Which items are mined, from the recipes alone (prices never change it, so stock changes stay
     * incremental). Starting from the raws, everything a pattern makes from items already
     * obtainable is obtainable. What is left is only reachable through loops; the loops among it
     * that nothing else left feeds are the cause. A loop that grows (1 X → 2 X, seed + dirt →
     * 2 seeds) stays a growth loop needing a seed; one that does not (raw iron ⇄ raw iron block)
     * was mined. Both count as obtainable from then on, and the pass goes on until all is settled.
     */
    private void computeMined() {
        mined.clear();
        BitSet ok = new BitSet(items);
        int[] waiting = new int[patterns.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int q = 0; q < patterns.size(); q++)
            if (patterns.get(q) != null)
                waiting[q] = distinctInputs(patterns.get(q)).length;
        for (int i = 0; i < items; i++)
            if (producers[i].length == 0) {
                ok.set(i);
                queue.add(i);
            }
        for (int q = 0; q < patterns.size(); q++)
            if (patterns.get(q) != null && waiting[q] == 0)
                obtain(patterns.get(q), ok, queue);
        while (true) {
            while (!queue.isEmpty())
                for (int q : consumers[queue.poll()])
                    if (--waiting[q] == 0)
                        obtain(patterns.get(q), ok, queue);
            List<Integer> rest = new ArrayList<>();
            for (int i = 0; i < items; i++)
                if (!ok.get(i))
                    rest.add(i);
            if (rest.isEmpty())
                return;
            List<int[]> loops = sourceLoops(rest);
            if (loops.isEmpty())
                return;     // cannot happen: a source that is no loop would have only obtainable inputs
            for (int[] loop : loops) {
                if (!grows(loop, ok))
                    for (int m : loop)
                        mined.set(m);
                for (int m : loop) {
                    ok.set(m);
                    queue.add(m);
                }
            }
        }
    }

    private static void obtain(Pattern p, BitSet ok, ArrayDeque<Integer> queue) {
        for (int o = 0; o < p.outputCount(); o++)
            if (!ok.get(p.outputItem(o))) {
                ok.set(p.outputItem(o));
                queue.add(p.outputItem(o));
            }
    }

    /**
     * Does the loop grow: is there a mix of its recipes (those whose inputs are members or
     * obtainable) after which no member is down and the members together are up? Exact (a small
     * linear feasibility problem in fractions), so branching loops (crystal ⇄ block ⇄ cut block ⇄
     * smooth block) are judged by what they can do, not by one chosen order. 1 X → 2 X and
     * seed + dirt → 2 seeds grow; raw iron ⇄ raw iron block does not.
     */
    private boolean grows(int[] loop, BitSet ok) {
        Map<Integer, Integer> row = new HashMap<>();
        for (int i = 0; i < loop.length; i++)
            row.put(loop[i], i);
        LinkedHashSet<Integer> usable = new LinkedHashSet<>();
        for (int m : loop)
            for (int q : producers[m]) {
                Pattern p = patterns.get(q);
                boolean all = true;
                for (int s = 0; s < p.inputCount() && all; s++)
                    all = row.containsKey(p.inputItem(s)) || ok.get(p.inputItem(s));
                if (all)
                    usable.add(q);
            }
        if (usable.isEmpty())
            return false;
        int k = loop.length;
        int n = usable.size();
        Rational[][] a = new Rational[k + 1][n];
        Rational[] b = new Rational[k + 1];
        for (Rational[] r : a)
            Arrays.fill(r, Rational.ZERO);
        int j = 0;
        for (int q : usable) {
            Pattern p = patterns.get(q);
            for (int o = 0; o < p.outputCount(); o++) {
                Integer i = row.get(p.outputItem(o));
                if (i != null)
                    a[i][j] = a[i][j].add(Rational.of(p.outputAmount(o)));
            }
            for (int s = 0; s < p.inputCount(); s++) {
                Integer i = row.get(p.inputItem(s));
                if (i == null)
                    continue;
                if (p.inputKind(s) == InputKind.CONSUME || (p.inputKind(s) == InputKind.TOOL && p.inputDurability(s) <= 0))
                    a[i][j] = a[i][j].sub(Rational.of(p.inputAmount(s)));
                else if (p.inputKind(s) == InputKind.TOOL)
                    a[i][j] = a[i][j].sub(Rational.of(java.math.BigInteger.ONE, java.math.BigInteger.valueOf(p.inputDurability(s))));
            }
            for (int i = 0; i < k; i++)
                a[k][j] = a[k][j].add(a[i][j]);
            j++;
        }
        Arrays.fill(b, Rational.ZERO);
        b[k] = Rational.ONE;        // scale-free: any growing mix can be scaled to a total of 1
        return ExactSimplex.feasible(a, b);
    }

    private int[] successors(int u) {
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        for (int q : consumers[u]) {
            Pattern p = patterns.get(q);
            for (int i = 0; i < p.outputCount(); i++)
                out.add(p.outputItem(i));
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean selfLoop(int u) {
        for (int q : consumers[u])
            if (patterns.get(q).produces(u))
                return true;
        return false;
    }

    private void processSingle(int x) {
        if (producers[x].length == 0) {
            setRaw(x);
            level[x] = 0;
            return;
        }
        for (int q : producers[x]) {
            computeRunCost(q);
            computePatBom(q);
        }
        sortProducers(x);
        int p = producers[x][0];
        best[x] = p;
        logCost[x] = unitCost(p, x);
        level[x] = 1 + maxInputLevel(p);
        setItemBomFrom(x, p);
    }

    // ─────────────────────────────── policy cycles ───────────────────────────────

    /**
     * Loops among {@code impossible} (edges input → output over every pattern) that no other
     * item of it feeds.
     */
    private List<int[]> sourceLoops(List<Integer> impossible) {
        Set<Integer> set = new HashSet<>(impossible);
        IntFunction<int[]> succ = u -> {
            LinkedHashSet<Integer> out = new LinkedHashSet<>();
            for (int q : consumers[u]) {
                Pattern p = patterns.get(q);
                for (int i = 0; i < p.outputCount(); i++)
                    if (set.contains(p.outputItem(i)))
                        out.add(p.outputItem(i));
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        };
        int[] nodes = impossible.stream().mapToInt(Integer::intValue).toArray();
        List<int[]> comps = Tarjan.components(nodes, succ);
        Map<Integer, Integer> compOf = new HashMap<>();
        for (int c = 0; c < comps.size(); c++)
            for (int m : comps.get(c))
                compOf.put(m, c);
        boolean[] fed = new boolean[comps.size()];
        for (int u : nodes)
            for (int v : succ.apply(u))
                if (!compOf.get(u).equals(compOf.get(v)))
                    fed[compOf.get(v)] = true;
        List<int[]> out = new ArrayList<>();
        for (int c = 0; c < comps.size(); c++) {
            int[] members = comps.get(c);
            boolean loop = members.length > 1 || Arrays.stream(succ.apply(members[0])).anyMatch(v -> v == members[0]);
            if (!fed[c] && loop)
                out.add(members);
        }
        return out;
    }

    /**
     * A strongly connected part of the full graph. Old values inside it cannot be trusted (two
     * convertible items would prop each other up), so start from ∞ and iterate policies: evaluate the
     * current choice exactly (cycles solved as linear equations), then switch any member whose route
     * is strictly better. Ties never switch. Mined members stay raws throughout.
     */
    private void policyIteration(int[] comp) {
        Set<Integer> in = new HashSet<>();
        for (int m : comp) {
            in.add(m);
            logCost[m] = mined.get(m) ? logRawPrice(m) : LogMath.INF;
            best[m] = -1;
            scc[m] = -1;
        }
        // value iteration (Bellman–Ford) for a first policy: the cheapest finite route, which never loops
        int[] pol = new int[comp.length];
        Arrays.fill(pol, -1);
        for (int it = 0; it <= comp.length; it++) {
            boolean changed = false;
            for (int i = 0; i < comp.length; i++) {
                int m = comp[i];
                if (mined.get(m))
                    continue;
                for (int q : producers[m]) {
                    computeRunCost(q);
                    double c = unitCost(q, m);
                    if (c < logCost[m]) {
                        logCost[m] = c;
                        pol[i] = q;
                        changed = true;
                    }
                }
            }
            if (!changed)
                break;
        }
        // Bellman–Ford cannot enter a loop that only runs once it is seeded (growth loops). For
        // members still impossible, guess a route twice — cheapest with members counted as free,
        // and the one needing the fewest members per output (most likely to converge) — solve each
        // exactly, improve it by single switches, and keep the guess that makes more members possible
        boolean[] stuck = new boolean[comp.length];
        boolean anyStuck = false;
        for (int i = 0; i < comp.length; i++) {
            stuck[i] = logCost[comp[i]] == LogMath.INF && producers[comp[i]].length > 0;
            anyStuck |= stuck[i];
        }
        if (anyStuck) {
            int[] cheap = pol.clone();
            int[] lean = pol.clone();
            for (int i = 0; i < comp.length; i++) {
                if (!stuck[i])
                    continue;
                int m = comp[i];
                double bestC = LogMath.INF;
                double bestGain = Double.POSITIVE_INFINITY;
                double bestGainCost = LogMath.INF;
                for (int q : producers[m]) {
                    double c = optimisticUnitCost(q, m, in);
                    if (c < bestC) {
                        bestC = c;
                        cheap[i] = q;
                    }
                    double g = memberInputsPerOutput(q, m, in);
                    if (c != LogMath.INF && (g < bestGain || (g == bestGain && c < bestGainCost))) {
                        bestGain = g;
                        bestGainCost = c;
                        lean[i] = q;
                    }
                }
                if (cheap[i] < 0)
                    cheap[i] = producers[m][0];
                if (lean[i] < 0)
                    lean[i] = cheap[i];
            }
            // third guess: value iteration from below (stuck members start free); its argmin
            // policy usually enters the loops the two guesses above miss
            int[] below = pol.clone();
            for (int i = 0; i < comp.length; i++)
                if (stuck[i])
                    logCost[comp[i]] = LogMath.ZERO;
            for (int sweep = 0; sweep < MAX_BELOW_SWEEPS; sweep++) {
                boolean changed = false;
                for (int i = 0; i < comp.length; i++) {
                    int m = comp[i];
                    if (mined.get(m))
                        continue;
                    double bestC = LogMath.INF;
                    for (int q : producers[m]) {
                        computeRunCost(q);
                        double c = unitCost(q, m);
                        if (c < bestC) {
                            bestC = c;
                            below[i] = q;
                        }
                    }
                    if (below[i] >= 0 && !LogMath.sameWithin(bestC, logCost[m], 1e-12)) {
                        logCost[m] = bestC;
                        changed = true;
                    }
                }
                if (!changed)
                    break;
            }
            int[][] guesses = {cheap, lean, below};
            int bestFinite = -1;
            for (int[] g : guesses) {
                evaluate(comp, in, g);
                seedLoops(comp, in, g);
                int f = countFinite(costsOf(comp));
                if (f > bestFinite) {
                    bestFinite = f;
                    pol = g;
                }
            }
        }
        evaluate(comp, in, pol);

        Set<Long> banned = new HashSet<>();
        Set<String> seen = new HashSet<>();
        seen.add(Arrays.toString(pol));
        for (int it = 0; it < MAX_POLICY_ITERATIONS; it++) {
            int[] prev = pol.clone();
            double[] prevCost = costsOf(comp);
            boolean changed = false;
            for (int i = 0; i < comp.length; i++) {
                int m = comp[i];
                if (producers[m].length == 0 || mined.get(m))
                    continue;
                for (int q : producers[m])
                    computeRunCost(q);
                sortProducers(m);
                int cand = firstAllowed(m, i, banned);
                if (cand >= 0 && cand != pol[i] && strictlyBetter(cand, pol[i], m)) {
                    pol[i] = cand;
                    changed = true;
                }
            }
            if (!changed)
                break;
            evaluate(comp, in, pol);
            if (regressed(comp, prevCost)) {
                // switching together closed a loop that cannot run: retry the switches one by one;
                // one that makes something impossible on its own is banned — but only while the rest
                // of the policy stays as it is (any accepted switch lifts the old bans)
                int[] next = pol;
                pol = prev.clone();
                evaluate(comp, in, pol);
                boolean accepted = false;
                Set<Long> rejected = new HashSet<>();
                for (int i = 0; i < comp.length; i++) {
                    if (next[i] == prev[i])
                        continue;
                    double[] before = costsOf(comp);
                    pol[i] = next[i];
                    evaluate(comp, in, pol);
                    if (regressed(comp, before)) {
                        pol[i] = prev[i];
                        rejected.add(((long) i << 32) | next[i]);
                        evaluate(comp, in, pol);
                    } else
                        accepted = true;
                }
                if (accepted)
                    banned.clear();
                banned.addAll(rejected);
            } else
                banned.clear();
            if (!seen.add(Arrays.toString(pol) + banned))
                break;      // been here with the same bans: no progress possible
        }
        finish(comp, in, pol);
    }

    private static final int MAX_SEED_EVALUATIONS = 2_000;
    private static final int MAX_BELOW_SWEEPS = 200;

    /**
     * The optimistic guess can pick a loop that does not converge. For members still impossible,
     * try their other routes one at a time and keep any switch that makes more members possible
     * (without losing any). Rare in real packs; bounded by {@link #MAX_SEED_EVALUATIONS}.
     */
    private void seedLoops(int[] comp, Set<Integer> in, int[] pol) {
        int evaluations = 0;
        boolean improved = true;
        while (improved && evaluations < MAX_SEED_EVALUATIONS) {
            improved = false;
            for (int i = 0; i < comp.length && evaluations < MAX_SEED_EVALUATIONS; i++) {
                int m = comp[i];
                if (logCost[m] != LogMath.INF)
                    continue;
                double[] before = costsOf(comp);
                int finite = countFinite(before);
                int old = pol[i];
                for (int q : producers[m]) {
                    if (q == old)
                        continue;
                    pol[i] = q;
                    evaluate(comp, in, pol);
                    evaluations++;
                    if (!regressed(comp, before) && countFinite(costsOf(comp)) > finite) {
                        improved = true;
                        break;
                    }
                    pol[i] = old;
                }
                if (pol[i] == old)
                    evaluate(comp, in, pol);
            }
        }
    }

    private static int countFinite(double[] costs) {
        int n = 0;
        for (double c : costs)
            if (c != LogMath.INF)
                n++;
        return n;
    }

    /** Members used up per output of {@code m}: a loop converges when this multiplies to less than 1 around it. */
    private double memberInputsPerOutput(int q, int m, Set<Integer> in) {
        Pattern p = patterns.get(q);
        double sum = 0;
        for (int s = 0; s < p.inputCount(); s++) {
            if (!in.contains(p.inputItem(s)))
                continue;
            switch (p.inputKind(s)) {
                case CONSUME -> sum += Math.pow(10, p.inputAmount(s).log10());
                case TOOL -> sum += p.inputDurability(s) > 0 ? 1.0 / p.inputDurability(s) : Math.pow(10, p.inputAmount(s).log10());
                default -> {
                }
            }
        }
        return sum / Math.pow(10, p.outputAmountOf(m).log10());
    }

    /** Unit cost with every input inside the component taken as free (only outside inputs count). */
    private double optimisticUnitCost(int q, int m, Set<Integer> in) {
        Pattern p = patterns.get(q);
        double sum = LogMath.ZERO;
        for (int s = 0; s < p.inputCount(); s++) {
            int y = p.inputItem(s);
            if (in.contains(y))
                continue;
            switch (p.inputKind(s)) {
                case CONSUME -> sum = LogMath.add(sum, LogMath.mul(p.inputAmount(s).log10(), logCost[y]));
                case TOOL -> sum = LogMath.add(sum, p.inputDurability(s) > 0
                        ? LogMath.mul(logCost[y], -Math.log10(p.inputDurability(s)))
                        : LogMath.mul(p.inputAmount(s).log10(), logCost[y]));
                default -> {
                    if (logCost[y] == LogMath.INF)
                        return LogMath.INF;
                }
            }
        }
        if (sum == LogMath.ZERO || sum == LogMath.INF)
            return sum;
        return sum - p.outputAmountOf(m).log10();
    }

    private int firstAllowed(int m, int i, Set<Long> banned) {
        for (int q : producers[m])
            if (!banned.contains(((long) i << 32) | q))
                return q;
        return -1;
    }

    private double[] costsOf(int[] comp) {
        double[] c = new double[comp.length];
        for (int i = 0; i < comp.length; i++)
            c[i] = logCost[comp[i]];
        return c;
    }

    /** Some member that could be made before can no longer be made. */
    private boolean regressed(int[] comp, double[] before) {
        for (int i = 0; i < comp.length; i++)
            if (before[i] != LogMath.INF && logCost[comp[i]] == LogMath.INF)
                return true;
        return false;
    }

    /** Groups of the policy graph inside {@code comp}, sources first. */
    private List<int[]> policyGroups(int[] comp, Set<Integer> in, int[] pol) {
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < comp.length; i++)
            idx.put(comp[i], i);
        Map<Integer, List<Integer>> succ = new HashMap<>();
        for (int i = 0; i < comp.length; i++) {
            if (pol[i] < 0)
                continue;
            Pattern p = patterns.get(pol[i]);
            for (int s = 0; s < p.inputCount(); s++) {
                int y = p.inputItem(s);
                if (in.contains(y))
                    succ.computeIfAbsent(y, k -> new ArrayList<>()).add(comp[i]);
            }
        }
        return Tarjan.components(comp, u -> {
            List<Integer> l = succ.get(u);
            return l == null ? EMPTY : l.stream().mapToInt(Integer::intValue).toArray();
        });
    }

    private void evaluate(int[] comp, Set<Integer> in, int[] pol) {
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < comp.length; i++)
            idx.put(comp[i], i);
        for (int[] group : policyGroups(comp, in, pol)) {
            if (group.length == 1) {
                int m = group[0];
                int q = pol[idx.get(m)];
                if (q < 0) {
                    logCost[m] = mined.get(m) ? logRawPrice(m) : LogMath.INF;
                    continue;
                }
                if (!patterns.get(q).hasInput(m)) {
                    computeRunCost(q);
                    logCost[m] = unitCost(q, m);
                    continue;
                }
            }
            solveGroupCost(group, idx, pol);
        }
    }

    /** c = A c + b for a cycle of the policy; no external source (or a non-convergent loop) → ∞. */
    private void solveGroupCost(int[] group, Map<Integer, Integer> idx, int[] pol) {
        int k = group.length;
        Map<Integer, Integer> local = new HashMap<>();
        for (int i = 0; i < k; i++)
            local.put(group[i], i);
        double[][] a = new double[k][k];
        double[] bLog = new double[k];
        boolean external = false;
        for (int i = 0; i < k; i++) {
            int m = group[i];
            int q = pol[idx.get(m)];
            if (q < 0) {
                setAll(group, LogMath.INF);
                return;
            }
            Pattern p = patterns.get(q);
            double outLog = p.outputAmountOf(m).log10();
            double b = LogMath.ZERO;
            for (int s = 0; s < p.inputCount(); s++) {
                int y = p.inputItem(s);
                InputKind kind = p.inputKind(s);
                double perUnitLog;
                if (kind == InputKind.CONSUME || (kind == InputKind.TOOL && p.inputDurability(s) <= 0))
                    perUnitLog = p.inputAmount(s).log10() - outLog;
                else if (kind == InputKind.TOOL)
                    perUnitLog = -Math.log10(p.inputDurability(s)) - outLog;
                else
                    continue;
                Integer j = local.get(y);
                if (j != null)
                    a[i][j] += Math.pow(10, perUnitLog);
                else {
                    b = LogMath.add(b, LogMath.mul(perUnitLog, logCost[y]));
                    external = true;
                }
            }
            bLog[i] = b;
        }
        if (!external) {
            setAll(group, LogMath.INF);     // a loop with no outside source at all cannot be made
            return;
        }
        double l0 = LogMath.ZERO;
        for (double b : bLog) {
            if (b == LogMath.INF) {
                setAll(group, LogMath.INF);
                return;
            }
            l0 = Math.max(l0, b);
        }
        double[][] mtx = new double[k][k];
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++)
                mtx[i][j] = (i == j ? 1 : 0) - a[i][j];
        DenseLu lu = DenseLu.factor(mtx);
        if (lu == null) {
            setAll(group, LogMath.INF);
            return;
        }
        if (l0 == LogMath.ZERO) {
            setAll(group, LogMath.ZERO);   // everything external is free
            return;
        }
        double[] rhs = new double[k];
        for (int i = 0; i < k; i++)
            rhs[i] = bLog[i] == LogMath.ZERO ? 0 : Math.pow(10, bLog[i] - l0);
        double[] c = lu.solve(rhs);
        double maxAbs = 0;
        for (double v : c)
            maxAbs = Math.max(maxAbs, Math.abs(v));
        for (double v : c)
            if (!Double.isFinite(v) || v < -1e-9 * maxAbs) {
                setAll(group, LogMath.INF);
                return;
            }
        for (int i = 0; i < k; i++)
            logCost[group[i]] = c[i] <= 0 ? LogMath.ZERO : Math.log10(c[i]) + l0;
    }

    private void setAll(int[] group, double v) {
        for (int m : group)
            logCost[m] = v;
    }

    /** Writes best / level / BOM / policy cycles for the final policy of {@code comp}. */
    private void finish(int[] comp, Set<Integer> in, int[] pol) {
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < comp.length; i++)
            idx.put(comp[i], i);
        for (int[] group : policyGroups(comp, in, pol)) {
            int first = group[0];
            boolean single = group.length == 1 && (pol[idx.get(first)] < 0 || !patterns.get(pol[idx.get(first)]).hasInput(first));
            if (single) {
                int q = pol[idx.get(first)];
                if (q < 0) {
                    setRaw(first);
                    if (!mined.get(first))
                        logCost[first] = LogMath.INF;
                    level[first] = 0;
                    continue;
                }
                best[first] = q;
                level[first] = 1 + maxInputLevel(q);
                computeRunCost(q);
                computePatBom(q);
                setItemBomFrom(first, q);
                continue;
            }
            makeScc(group, idx, pol);
        }
        // every producer of a member now sees final member costs
        for (int m : comp) {
            for (int q : producers[m]) {
                computeRunCost(q);
                computePatBom(q);
            }
            sortProducers(m);
        }
    }

    private void makeScc(int[] group, Map<Integer, Integer> idx, int[] pol) {
        int k = group.length;
        Set<Integer> members = new HashSet<>();
        for (int m : group)
            members.add(m);
        int lvl = 0;
        int[] policy = new int[k];
        LinkedHashSet<Integer> distinct = new LinkedHashSet<>();
        for (int i = 0; i < k; i++) {
            policy[i] = pol[idx.get(group[i])];
            distinct.add(policy[i]);
            Pattern p = patterns.get(policy[i]);
            for (int s = 0; s < p.inputCount(); s++)
                if (!members.contains(p.inputItem(s)))
                    lvl = Math.max(lvl, level[p.inputItem(s)]);
        }
        lvl += 1;
        int index = sccs.size();
        for (int i = 0; i < k; i++) {
            level[group[i]] = lvl;
            best[group[i]] = policy[i];
            scc[group[i]] = index;
        }
        int[] pats = distinct.stream().mapToInt(Integer::intValue).toArray();

        // net[i][j]: what one run of pats[j] adds to member i, exactly
        Rational[][] net = new Rational[k][pats.length];
        for (int i = 0; i < k; i++)
            for (int j = 0; j < pats.length; j++) {
                Pattern p = patterns.get(pats[j]);
                Rational v = Rational.of(p.outputAmountOf(group[i]));
                for (int s = 0; s < p.inputCount(); s++) {
                    if (p.inputItem(s) != group[i])
                        continue;
                    if (p.inputKind(s) == InputKind.CONSUME || (p.inputKind(s) == InputKind.TOOL && p.inputDurability(s) <= 0))
                        v = v.sub(Rational.of(p.inputAmount(s)));
                    else if (p.inputKind(s) == InputKind.TOOL)
                        v = v.sub(Rational.of(java.math.BigInteger.ONE, java.math.BigInteger.valueOf(p.inputDurability(s))));
                }
                net[i][j] = v;
            }

        // seeds: run each pattern once, try every rotation, keep the one needing the fewest members up front
        AmountMap bestSeeds = null;
        Amount bestTotal = null;
        boolean growth = false;
        List<AmountMap> options = new ArrayList<>();
        for (int r = 0; r < pats.length; r++) {
            AmountMap seeds = new AmountMap();
            AmountMap inv = new AmountMap();
            for (int t = 0; t < pats.length; t++) {
                Pattern p = patterns.get(pats[(r + t) % pats.length]);
                for (int s = 0; s < p.inputCount(); s++) {
                    int y = p.inputItem(s);
                    if (!members.contains(y))
                        continue;
                    Amount need = p.inputKind(s) == InputKind.TOOL ? Amount.ONE : p.inputAmount(s);
                    Amount have = inv.get(y);
                    if (have.compareTo(need) < 0) {
                        seeds.add(y, need.sub(have));
                        inv.set(y, need);
                    }
                    if (p.inputKind(s) == InputKind.CONSUME)
                        inv.sub(y, need);
                }
                for (int o = 0; o < p.outputCount(); o++)
                    if (members.contains(p.outputItem(o)))
                        inv.add(p.outputItem(o), p.outputAmount(o));
            }
            Amount total = seeds.total();
            if (options.stream().noneMatch(o -> o.toString().equals(seeds.toString())))
                options.add(seeds);
            if (bestTotal == null || total.compareTo(bestTotal) < 0) {
                bestTotal = total;
                bestSeeds = seeds;
                growth = inv.total().compareTo(total) > 0;
            }
        }
        int[] seedItems = bestSeeds.keys();
        Amount[] seedAmounts = new Amount[seedItems.length];
        for (int i = 0; i < seedItems.length; i++)
            seedAmounts[i] = bestSeeds.get(seedItems[i]);
        AmountMap first = bestSeeds;
        options.remove(first);
        options.sort(java.util.Comparator.comparing(AmountMap::total));     // stable: rotation order on ties
        options.add(0, first);

        sccs.add(new Scc(index, group.clone(), policy, pats, growth ? SccKind.GROWTH : SccKind.ENTANGLED,
                seedItems, seedAmounts, options.toArray(AmountMap[]::new), net));
        sccBom(group, policy, members);
    }

    /** BOM of cycle members: (I − A) B = B_ext, one raw at a time (estimates). */
    private void sccBom(int[] group, int[] policy, Set<Integer> members) {
        int k = group.length;
        Map<Integer, Integer> local = new HashMap<>();
        for (int i = 0; i < k; i++)
            local.put(group[i], i);
        double[][] a = new double[k][k];
        List<TreeMap<Integer, Double>> ext = new ArrayList<>();
        Set<Integer> raws = new java.util.TreeSet<>();
        for (int i = 0; i < k; i++) {
            Pattern p = patterns.get(policy[i]);
            double outLog = p.outputAmountOf(group[i]).log10();
            TreeMap<Integer, Double> e = new TreeMap<>();
            for (int s = 0; s < p.inputCount(); s++) {
                InputKind kind = p.inputKind(s);
                double perUnitLog;
                if (kind == InputKind.CONSUME || (kind == InputKind.TOOL && p.inputDurability(s) <= 0))
                    perUnitLog = p.inputAmount(s).log10() - outLog;
                else if (kind == InputKind.TOOL)
                    perUnitLog = -Math.log10(p.inputDurability(s)) - outLog;
                else
                    continue;
                int y = p.inputItem(s);
                Integer j = local.get(y);
                if (j != null) {
                    a[i][j] += Math.pow(10, perUnitLog);
                    continue;
                }
                for (int t = 0; t < bomItems[y].length; t++) {
                    e.merge(bomItems[y][t], bomLog[y][t] + perUnitLog, LogMath::add);
                    raws.add(bomItems[y][t]);
                }
            }
            ext.add(e);
        }
        double[][] mtx = new double[k][k];
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++)
                mtx[i][j] = (i == j ? 1 : 0) - a[i][j];
        DenseLu lu = DenseLu.factor(mtx);
        List<TreeMap<Integer, Double>> result = new ArrayList<>();
        for (int i = 0; i < k; i++)
            result.add(new TreeMap<>());
        if (lu != null) {
            for (int raw : raws) {
                double l0 = LogMath.ZERO;
                for (TreeMap<Integer, Double> e : ext)
                    l0 = Math.max(l0, e.getOrDefault(raw, LogMath.ZERO));
                if (l0 == LogMath.ZERO)
                    continue;
                double[] rhs = new double[k];
                for (int i = 0; i < k; i++) {
                    double v = ext.get(i).getOrDefault(raw, LogMath.ZERO);
                    rhs[i] = v == LogMath.ZERO ? 0 : Math.pow(10, v - l0);
                }
                double[] sol = lu.solve(rhs);
                for (int i = 0; i < k; i++)
                    if (Double.isFinite(sol[i]) && sol[i] > 0)
                        result.get(i).put(raw, Math.log10(sol[i]) + l0);
            }
        }
        for (int i = 0; i < k; i++) {
            TreeMap<Integer, Double> r = result.get(i);
            bomItems[group[i]] = r.keySet().stream().mapToInt(Integer::intValue).toArray();
            bomLog[group[i]] = r.values().stream().mapToDouble(Double::doubleValue).toArray();
        }
        for (int q : policy)
            computePatBom(q);
    }

    // ─────────────────────────────── incremental ───────────────────────────────

    /**
     * Repairs the dirty items lowest level first, so every input is final when an item is taken out:
     * getting cheaper and getting more expensive use the same path, and each item is handled at most
     * once per change. Returns false when a full rebuild is needed instead.
     */
    private boolean incremental() {
        // each item settles in a few passes; far more means values keep feeding back (an undetected
        // loop): give up and rebuild rather than spin on the engine thread
        long limit = processed + 64L * (items + patterns.size() + 16);
        while (!dirty.isEmpty()) {
            int x = dirty.popMin(level);
            if (++processed > limit)
                return false;
            if (x >= items)
                continue;
            if (scc[x] != -1)
                return false;   // a policy cycle: do not trust old values inside it

            if (producers[x].length == 0 || mined.get(x)) {
                double c = logRawPrice(x);
                if (best[x] == -1 && LogMath.sameWithin(c, logCost[x], EPS))
                    continue;
                setRaw(x);
                propagate(x);
                continue;
            }

            for (int q : producers[x])
                computeRunCost(q);
            sortProducers(x);
            int p = producers[x][0];
            double c = unitCost(p, x);
            if (c == LogMath.INF)
                return false;   // only a loop could still make it (a growth loop needs the exact solve)

            if (patterns.get(p).hasInput(x))
                return false;       // the route takes x itself (2 templates from 1): a growth loop
            if (p != best[x]) {
                int maxIn = maxInputLevel(p);
                if (maxIn >= level[x] && !raiseLevel(x, maxIn + 1, p))
                    return false;   // the new route closes a loop through x
            }
            if (p == best[x] && LogMath.sameWithin(c, logCost[x], EPS))
                continue;           // unchanged: stop here
            logCost[x] = c;
            best[x] = p;
            computePatBom(p);
            setItemBomFrom(x, p);
            propagate(x);
        }
        return true;
    }

    private void propagate(int x) {
        for (int q : consumers[x]) {
            computeRunCost(q);
            computePatBom(q);
            Pattern p = patterns.get(q);
            for (int i = 0; i < p.outputCount(); i++) {
                int o = p.outputItem(i);
                // watch edges only pass on "now ahead of the best route" (cheaper, or a priority
                // route that became possible); the best route's own changes always pass
                // an item that cannot be made yet is always rechecked: a new input may open a loop for it
                if (best[o] == q || best[o] < 0 || patterns.get(best[o]) == null || logCost[o] == LogMath.INF
                        || compareRoutes(q, best[o], o) < 0)
                    markDirty(o);
            }
        }
    }

    /**
     * Raises x (and everything whose best route uses it) to at least {@code target}, like sifting up
     * in a heap. Returns false if this reaches an input of {@code newBest}: the policy would loop.
     */
    private boolean raiseLevel(int x, int target, int newBest) {
        Set<Integer> loopInputs = new HashSet<>();
        Pattern np = patterns.get(newBest);
        for (int s = 0; s < np.inputCount(); s++)
            loopInputs.add(np.inputItem(s));
        ArrayList<int[]> stack = new ArrayList<>();
        stack.add(new int[]{x, target});
        while (!stack.isEmpty()) {
            int[] e = stack.remove(stack.size() - 1);
            int u = e[0];
            int l = e[1];
            if (level[u] >= l)
                continue;
            if (u != x && loopInputs.contains(u))
                return false;
            // without loops no level exceeds the item count: still rising means the best routes
            // already go round in a loop elsewhere (seen with a real modpack: levels rose until
            // the heap ran out) - rebuild instead
            if (l > items)
                return false;
            if (scc[u] != -1)
                return false;
            level[u] = l;
            for (int q : consumers[u]) {
                Pattern p = patterns.get(q);
                for (int i = 0; i < p.outputCount(); i++) {
                    int o = p.outputItem(i);
                    if (best[o] == q && o != x)
                        stack.add(new int[]{o, l + 1});
                }
            }
        }
        return true;
    }

    // ─────────────────────────────── helpers ───────────────────────────────

    private static int[] distinctInputs(Pattern p) {
        LinkedHashSet<Integer> s = new LinkedHashSet<>();
        for (int i = 0; i < p.inputCount(); i++) {
            s.add(p.inputItem(i));
            for (int j = 0; j < p.alternativeCount(i); j++)
                s.add(p.alternative(i, j));
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] append(int[] a, int v) {
        for (int x : a)
            if (x == v)
                return a;
        int[] r = Arrays.copyOf(a, a.length + 1);
        r[a.length] = v;
        return r;
    }

    private static int[] without(int[] a, int v) {
        int n = 0;
        for (int x : a)
            if (x != v)
                n++;
        if (n == a.length)
            return a;
        int[] r = new int[n];
        int k = 0;
        for (int x : a)
            if (x != v)
                r[k++] = x;
        return r;
    }
}
