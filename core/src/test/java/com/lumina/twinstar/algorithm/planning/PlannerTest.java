package com.lumina.twinstar.algorithm.planning;

import com.lumina.twinstar.algorithm.TestGraphs;
import com.lumina.twinstar.algorithm.cache.Snapshot;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.Deadline;
import com.lumina.twinstar.algorithm.model.OrderMode;
import com.lumina.twinstar.algorithm.model.Overlay;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerTest {
    static final int ORE = 0, INGOT = 1, PLATE = 2, GEAR = 3, NUGGET = 4;

    private static TestGraphs gears() {
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(ORE, 1).output(INGOT, 1));                       // P0
        g.pattern(b -> b.input(INGOT, 2).output(PLATE, 1));                     // P1
        g.pattern(b -> b.input(PLATE, 1).input(INGOT, 2).output(GEAR, 1));      // P2
        return g;
    }

    private static Plan plan(TestGraphs g, int target, long amount) {
        Snapshot s = g.flush();
        return Planner.plan(s, g.stockSnapshot(), target, Amount.of(amount), new Overlay(), Deadline.NONE);
    }

    @Test
    void sharedIntermediateIsCountedOnce() {
        TestGraphs g = gears().stock(ORE, 100);
        Plan p = plan(g, GEAR, 3);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.of(12), p.patternTimes.get(0));   // 6 ingots for plates + 6 for gears, one task
        assertEquals(Amount.of(3), p.patternTimes.get(1));
        assertEquals(Amount.of(3), p.patternTimes.get(2));
        assertEquals(Amount.of(12), p.pulled.get(ORE));
        // usage = pulled 12 + made (12 ingots + 3 plates + 3 gears)
        assertEquals(Amount.of(30), p.usage);
    }

    @Test
    void stockOfIntermediatesIsUsedFirst() {
        TestGraphs g = gears().stock(ORE, 100).stock(PLATE, 2).stock(INGOT, 1);
        Plan p = plan(g, GEAR, 3);
        assertTrue(p.isComplete());
        assertEquals(Amount.of(2), p.pulled.get(PLATE));
        // plates: 1 to make (2 ingots); gears: 6 ingots; 8 ingots − 1 in stock = 7 smelted
        assertEquals(Amount.of(7), p.patternTimes.get(0));
    }

    /** Bug 10-23: 2 already in storage made an order for 100 craft only 98. The order is always crafted in full (as in AE2). */
    @Test
    void orderedItemInStorageIsNotCountedAsCrafted() {
        TestGraphs g = gears().stock(ORE, 100).stock(GEAR, 2);
        Plan p = plan(g, GEAR, 3);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.of(3), p.patternTimes.get(2));
        assertEquals(Amount.ZERO, p.pulled.get(GEAR));
    }

    /** A remainder the pattern does not list (the bucket of a water bucket) is a byproduct: it comes out, but it is no route. */
    @Test
    void byproductIsNoRoute() {
        final int WATER = 10, BUCKET = 11, WATER_BUCKET = 12, CAKE = 13;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(INGOT, 3).output(BUCKET, 1));                                   // P0
        g.pattern(b -> b.input(BUCKET, 1).input(WATER, 1).output(WATER_BUCKET, 1));            // P1
        g.pattern(b -> b.input(WATER_BUCKET, 1).input(ORE, 1).output(CAKE, 1).byproduct(BUCKET, 1));   // P2
        g.stock(INGOT, 100).stock(WATER, 100).stock(ORE, 100);
        Snapshot s = g.flush();
        assertEquals(1, s.producers(BUCKET).length, "the cake pattern is no route to the bucket");
        assertEquals(0, s.producers(BUCKET)[0]);
        Plan p = plan(g, CAKE, 1);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.ONE, p.patternTimes.get(2));
    }

    @Test
    void roundingGoesToTheLedger() {
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(ORE, 1).output(NUGGET, 4));
        g.stock(ORE, 10);
        Plan p = plan(g, NUGGET, 5);
        assertEquals(Amount.of(2), p.patternTimes.get(0));
        assertEquals(Amount.of(3), p.leftover.get(NUGGET));
    }

    @Test
    void shortageIsReportedAtRawLevel() {
        TestGraphs g = gears().stock(ORE, 5);
        Plan p = plan(g, GEAR, 3);
        assertEquals(Amount.of(7), p.missing.get(ORE));
        assertEquals(1, p.missing.size());
    }

    @Test
    void catalystIsAHoldingNotASum() {
        int mold = 5, wire = 6, cable = 7;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(INGOT, 1).catalyst(mold, 1).output(wire, 1));
        g.pattern(b -> b.input(wire, 1).catalyst(mold, 1).output(cable, 1));
        g.stock(INGOT, 100).stock(mold, 1);
        Plan p = plan(g, cable, 10);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.ONE, p.pulled.get(mold));
        assertEquals(Amount.ONE, p.leftover.get(mold));

        g.stock(mold, 0);
        assertEquals(Amount.ONE, plan(g, cable, 10).missing.get(mold));
    }

    @Test
    void durabilityTool() {
        int hammer = 8, dust = 9;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(INGOT, 1).tool(hammer, 4).output(dust, 1));
        g.stock(INGOT, 100).stock(hammer, 5);
        Plan p = plan(g, dust, 10);
        assertEquals(Amount.of(3), p.pulled.get(hammer));     // ceil(10 / 4)
    }

    /** The design page's P1/P2 example: A = 10, B = 5, make 7. Makeable, so it must not report a shortage. */
    @Test
    void p1p2RepairMakesAllSeven() {
        int a = 10, b = 11, t = 12;
        TestGraphs g = new TestGraphs();
        g.pattern(x -> x.input(a, 2).output(t, 1));            // P1
        g.pattern(x -> x.input(a, 1).input(b, 1).output(t, 1)); // P2
        g.stock(a, 10).stock(b, 5);
        Plan p = plan(g, t, 7);
        assertTrue(p.isComplete(), p.toString());
        Amount p1 = p.patternTimes.get(0);
        Amount p2 = p.patternTimes.get(1);
        assertEquals(Amount.of(7), p1.add(p2));
        assertTrue(p1.compareTo(Amount.of(2)) >= 0 && p1.compareTo(Amount.of(3)) <= 0, p.toString());
    }

    @Test
    void growthLoopUsesStockAsSeed() {
        int seed = 20, dirt = 21;
        TestGraphs g = new TestGraphs();
        int grow = g.pattern(b -> b.input(seed, 1).input(dirt, 1).output(seed, 2));
        g.stock(dirt, 100).stock(seed, 1);
        Plan p = plan(g, seed, 10);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.of(10), p.patternTimes.get(grow));
        assertEquals(Amount.of(10), p.pulled.get(dirt));
        assertEquals(Amount.ONE, p.leftover.get(seed));     // the seed goes back

        g.stock(seed, 0);
        assertEquals(Amount.ONE, plan(g, seed, 10).missing.get(seed));
    }

    @Test
    void backEdgeUsesTheOtherDirection() {
        int a = 30, b2 = 31, r = 32;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(a, 1).output(b2, 1));
        int bToA = g.pattern(b -> b.input(b2, 1).output(a, 1));
        g.pattern(b -> b.input(r, 1).output(a, 1));
        g.stock(b2, 5);
        Plan p = plan(g, a, 3);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.of(3), p.patternTimes.get(bToA));
        assertEquals(Amount.of(3), p.pulled.get(b2));

        g.stock(b2, 0);
        Plan none = plan(g, a, 3);     // must terminate and report a shortage, not loop
        assertTrue(!none.isComplete());
    }

    @Test
    void substitutionTakesOtherItemsMostStockedFirst() {
        int oak = 40, birch = 41, spruce = 43, stick = 42;
        TestGraphs g = new TestGraphs();
        g.pattern(b -> b.input(oak, com.lumina.twinstar.algorithm.model.Amount.of(2),
                com.lumina.twinstar.algorithm.model.InputKind.CONSUME, 0, new int[]{birch, spruce})
                .allowSubst(true).output(stick, 4));
        g.stock(oak, 1).stock(birch, 10).stock(spruce, 2);
        Plan p = plan(g, stick, 8);
        assertTrue(p.isComplete(), p.toString());
        assertEquals(Amount.ONE, p.pulled.get(oak));
        assertEquals(Amount.of(3), p.pulled.get(birch));
    }

    @Test
    void bestEffortFindsTheLargestMakeableAmount() {
        TestGraphs g = gears().stock(ORE, 9);
        Snapshot s = g.flush();
        OrderResult r = Planner.order(s, g.stockSnapshot(), GEAR, Amount.of(5), OrderMode.BEST_EFFORT,
                Amount.of(Long.MAX_VALUE), new Overlay(), Deadline.NONE);
        Plan p = assertInstanceOf(OrderResult.Accepted.class, r).plan();
        assertTrue(p.isComplete());
        assertEquals(Amount.of(2), p.amount);
    }

    @Test
    void overCapacityIsRejectedWithTheLargestAmountThatFits() {
        TestGraphs g = gears().stock(ORE, 100);
        Snapshot s = g.flush();
        OrderResult r = Planner.order(s, g.stockSnapshot(), GEAR, Amount.of(3), OrderMode.EXACT,
                Amount.of(10), new Overlay(), Deadline.NONE);
        OrderResult.Rejected rej = assertInstanceOf(OrderResult.Rejected.class, r);
        assertEquals(Amount.ONE, rej.suggested());     // one gear: 4 ore + 4 ingots + 1 plate + 1 gear = 10
        assertEquals(Amount.of(30), rej.usage());
    }

    @Test
    void astronomicalAmounts() {
        TestGraphs g = gears().infinite(ORE).stock(ORE, 1);
        Amount huge = Amount.of(BigInteger.TEN.pow(1000));
        Snapshot s = g.flush();
        Plan p = Planner.plan(s, g.stockSnapshot(), GEAR, huge, new Overlay(), Deadline.NONE);
        assertTrue(p.isComplete());
        assertEquals(huge.mul(Amount.of(4)), p.patternTimes.get(0));
        assertEquals(huge, p.patternTimes.get(2));
        // an infinite source is drawn while the job runs, never pulled at submit
        assertEquals(huge.mul(Amount.of(4)), p.drawLater.get(ORE));
        assertEquals(p.pulled.get(ORE), p.drawLater.get(ORE));
    }

    @Test
    void finiteStockIsNotDrawnLater() {
        Plan p = plan(gears().stock(ORE, 100), GEAR, 3);
        assertTrue(p.drawLater.isEmpty());
    }

    @Test
    void reservedMaterialPushesTheOtherRouteFirst() {
        int copper = 50, tin = 51, wire = 52;
        TestGraphs g = new TestGraphs();
        int viaCopper = g.pattern(b -> b.input(copper, 1).output(wire, 1));
        int viaTin = g.pattern(b -> b.input(tin, 1).output(wire, 1));
        g.stock(copper, 1000).stock(tin, 10);
        Plan normal = plan(g, wire, 5);
        assertEquals(Amount.of(5), normal.patternTimes.get(viaCopper));
        Overlay o = new Overlay();
        o.markReserved(copper);
        Snapshot s = g.flush();
        Plan marked = Planner.plan(s, g.stockSnapshot(), wire, Amount.of(5), o, Deadline.NONE);
        assertEquals(Amount.of(5), marked.patternTimes.get(viaTin));
    }
}
