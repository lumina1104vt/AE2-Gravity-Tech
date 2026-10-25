package com.lumina.twinstar.algorithm.cache;

import com.lumina.twinstar.algorithm.TestGraphs;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.Pattern;
import com.lumina.twinstar.algorithm.model.SccKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheoryLayerTest {
    static final int ORE = 0, INGOT = 1, PLATE = 2, GEAR = 3;

    @Test
    void chainCostsLevelsAndBom() {
        TestGraphs g = new TestGraphs();
        int smelt = g.pattern(b -> b.input(ORE, 1).output(INGOT, 1));
        g.pattern(b -> b.input(INGOT, 2).output(PLATE, 1));
        g.pattern(b -> b.input(PLATE, 1).input(INGOT, 2).output(GEAR, 1));
        g.stock(ORE, 100);      // tier 2 → price 0.01
        Snapshot s = g.flush();

        assertEquals(-2.0, s.logCost(ORE), 1e-9);
        assertEquals(-2.0, s.logCost(INGOT), 1e-9);
        assertEquals(Math.log10(0.02), s.logCost(PLATE), 1e-9);
        assertEquals(Math.log10(0.04), s.logCost(GEAR), 1e-9);
        assertEquals(0, s.level(ORE));
        assertEquals(1, s.level(INGOT));
        assertEquals(2, s.level(PLATE));
        assertEquals(3, s.level(GEAR));
        assertEquals(smelt, s.best(INGOT));
        // a gear flattens to 4 ore
        assertEquals(ORE, s.bomItems(GEAR)[0]);
        assertEquals(Math.log10(4), s.bomLog(GEAR)[0], 1e-9);
    }

    @Test
    void cheaperRouteWinsAndPriorityComesFirst() {
        TestGraphs g = new TestGraphs();
        int cheap = g.pattern(b -> b.input(ORE, 1).output(INGOT, 2));
        g.pattern(b -> b.input(ORE, 1).output(INGOT, 1));
        g.stock(ORE, 50);
        Snapshot s = g.flush();
        assertEquals(cheap, s.best(INGOT));
        assertEquals(cheap, s.producers(INGOT)[0]);

        TestGraphs h = new TestGraphs();
        h.pattern(b -> b.input(ORE, 1).output(INGOT, 2));
        int prio = h.pattern(b -> b.input(ORE, 1).output(INGOT, 1).priority(5));
        h.stock(ORE, 50);
        assertEquals(prio, h.flush().best(INGOT));
    }

    @Test
    void growthLoopIsAPolicyCycle() {
        int seed = 20, dirt = 21;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(seed, 1).input(dirt, 1).output(seed, 2));
        g.stock(dirt, 100).stock(seed, 1);
        Snapshot s = g.flush();
        int scc = s.scc(seed);
        assertTrue(scc >= 0);
        assertEquals(SccKind.GROWTH, s.sccAt(scc).kind);
        // c = (c + dirt) / 2  →  c = dirt
        assertEquals(s.logCost(dirt), s.logCost(seed), 1e-9);
        assertEquals(1, s.sccAt(scc).seedCount());
        assertEquals(Amount.ONE, s.sccAt(scc).seedAmount(0));
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void growthLoopAddedLaterIsFoundIncrementally() {
        // the smithing-template duplication recipe added to a network that already has patterns
        int diamond = 30, template = 31, netherrack = 32, ore = 33, ingot = 34;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(ore, 1).output(ingot, 1));
        g.stock(diamond, 64).stock(template, 1).stock(netherrack, 64).stock(ore, 10);
        g.flush();
        g.pattern(b -> b.input(diamond, 7).input(template, 1).input(netherrack, 1).output(template, 2));
        Snapshot s = g.flush();
        assertTrue(s.scc(template) >= 0, "a self-loop added later is a policy cycle");
        assertEquals(SccKind.GROWTH, s.sccAt(s.scc(template)).kind);
    }

    @Test
    void reversibleRecipesDoNotPropEachOtherUp() {
        int a = 30, b2 = 31, r = 32;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(a, 1).output(b2, 1));
        g.pattern(b -> b.input(b2, 1).output(a, 1));
        int fromRaw = g.pattern(b -> b.input(r, 1).output(a, 1));
        g.stock(r, 1000);
        Snapshot s = g.flush();
        assertEquals(fromRaw, s.best(a));
        assertEquals(-3.0, s.logCost(a), 1e-9);
        assertEquals(-3.0, s.logCost(b2), 1e-9);
        assertEquals(-1, s.scc(a));
    }

    @Test
    void stockTierHasHysteresis() {
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(ORE, 1).output(INGOT, 1));
        g.stock(ORE, 100);
        assertEquals(-2.0, g.flush().logCost(ORE), 1e-9);
        g.stock(ORE, 99);       // below 100 but above 0.8 × 100: stays tier 2
        assertEquals(-2.0, g.flush().logCost(ORE), 1e-9);
        g.stock(ORE, 80);
        assertEquals(-1.0, g.flush().logCost(ORE), 1e-9);
        g.stock(ORE, 0);
        assertEquals(TheoryLayer.LOG_HIGH, g.flush().logCost(INGOT), 1e-9);
    }

    /** The most important A test: after random changes, incremental results equal a full rebuild. */
    @Test
    void incrementalEqualsRebuild() {
        int rounds = Integer.getInteger("rounds", 200);
        Random rnd = new Random(1234);
        for (int round = 0; round < rounds; round++) {
            int items = 30;
            List<Pattern> all = new ArrayList<>();
            TheoryLayer inc = new TheoryLayer();
            // price tiers have hysteresis, so the rebuild replays each item's stock history
            List<List<Integer>> history = new ArrayList<>();
            for (int i = 0; i < items; i++) {
                history.add(new ArrayList<>());
                history.get(i).add(rnd.nextInt(3) == 0 ? 0 : 1 + rnd.nextInt(100_000));
            }
            int nextId = 0;
            for (int k = 0; k < 40; k++)
                all.add(randomPattern(rnd, items, nextId++));
            for (Pattern p : all)
                inc.addPattern(p);
            for (int i = 0; i < items; i++)
                inc.onStockChanged(i, Amount.of(history.get(i).get(0)), false);
            inc.flush();
            // random edits, flushed incrementally
            for (int step = 0; step < 15; step++) {
                int what = rnd.nextInt(3);
                if (what == 0 && !all.isEmpty()) {
                    Pattern p = all.remove(rnd.nextInt(all.size()));
                    inc.removePattern(p.id);
                } else if (what == 1) {
                    Pattern p = randomPattern(rnd, items, nextId++);
                    all.add(p);
                    inc.addPattern(p);
                } else {
                    int i = rnd.nextInt(items);
                    int v = rnd.nextInt(4) == 0 ? 0 : 1 + rnd.nextInt(1_000_000);
                    history.get(i).add(v);
                    inc.onStockChanged(i, Amount.of(v), false);
                }
                Snapshot a = inc.flush();
                TheoryLayer fresh = new TheoryLayer();
                for (Pattern p : all)
                    fresh.addPattern(p);
                for (int i = 0; i < items; i++)
                    for (int v : history.get(i))
                        fresh.onStockChanged(i, Amount.of(v), false);
                Snapshot b = fresh.flush();
                for (int i = 0; i < items; i++) {
                    double ca = a.logCost(i), cb = b.logCost(i);
                    if (Double.isInfinite(ca) || Double.isInfinite(cb))
                        assertEquals(cb, ca, "round " + round + " step " + step + " item " + i);
                    else
                        assertEquals(cb, ca, 1e-6, "round " + round + " step " + step + " item " + i);
                }
            }
        }
    }
    /**
     * Acyclic by construction (inputs have lower ids than outputs) half of the time; otherwise any
     * inputs, plus one raw (ids 0–4 are never outputs).
     * <p>
     * Two kinds of graph are left out on purpose, because there the answer depends on where the
     * search starts and incremental and rebuild can both be "right" yet differ:
     * <ul>
     *   <li>provider priorities (all 0 here): two priority routes that together close a loop that
     *   cannot run — either one may win;</li>
     *   <li>loops with no outside input at all (every cyclic pattern takes a raw here): the design
     *   counts them as impossible, which makes the best policy a combinatorial choice.</li>
     * </ul>
     */
    private static Pattern randomPattern(Random rnd, int items, int id) {
        Pattern.Builder b = new Pattern.Builder();
        boolean dag = rnd.nextBoolean();
        int out = 5 + rnd.nextInt(items - 5);
        int ins = 1 + rnd.nextInt(3);
        for (int k = 0; k < ins; k++) {
            int in = dag ? rnd.nextInt(out) : rnd.nextInt(items);
            if (in == out)
                in = (in + 1) % items;
            b.input(in, 1 + rnd.nextInt(4));
        }
        if (!dag)
            b.input(rnd.nextInt(5), 1 + rnd.nextInt(4));
        b.output(out, 1 + rnd.nextInt(4));
        return b.build(id);
    }
}
