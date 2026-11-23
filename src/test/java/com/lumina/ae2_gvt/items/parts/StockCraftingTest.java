package com.lumina.ae2_gvt.items.parts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.lumina.ae2_gvt.items.parts.StockCrafting.Phase;
import com.lumina.ae2_gvt.items.parts.StockCrafting.Step;

/**
 * A furnace set to keep 64 iron ingots; the interface has a crafting card. These walk through what the interface asks the network
 * for, and when it keeps quiet.
 */
class StockCraftingTest {
    @Test
    void shortfallCountsWhatTheMachinesHoldAndTheNetworkHas() {
        assertEquals(34, StockCrafting.shortfall(64, 10, 20));
        assertEquals(0, StockCrafting.shortfall(64, 64, 0), "the machine already holds all of it");
        assertEquals(0, StockCrafting.shortfall(64, 0, 5000), "the network has plenty: the interface just moves it");
        assertEquals(64, StockCrafting.shortfall(64, 0, 0));
        assertEquals(0, StockCrafting.shortfall(0, 0, 0));
        assertEquals(0, StockCrafting.shortfall(-5, 0, 0));
        assertEquals(0, StockCrafting.shortfall(64, Long.MAX_VALUE, Long.MAX_VALUE), "a sum past long is still plenty");
    }

    @Test
    void enoughInStockAsksForNothing() {
        var c = new StockCrafting<String>();
        assertEquals(Step.NOTHING, c.poll("iron", 0, 0));
        assertEquals(Phase.IDLE, c.phaseOf("iron"));
    }

    @Test
    void aShortageStartsOneCalculationAndNoSecondWhileItRuns() {
        var c = new StockCrafting<String>();
        assertEquals(Step.CALCULATE, c.poll("iron", 34, 0));
        assertEquals(Phase.CALCULATING, c.phaseOf("iron"));
        assertEquals(34, c.amountOf("iron"));
        // asked every half second while the plan is worked out: never a second calculation
        for (int t = 10; t < 200; t += 10)
            assertEquals(Step.NOTHING, c.poll("iron", 34, t));
        assertTrue(c.planFinished("iron", true, true, 200));
        c.submitted("iron", true, 200);
        assertEquals(Phase.RUNNING, c.phaseOf("iron"));
        // the job runs for minutes and the shortage is still there: still no second request
        for (int t = 210; t < 5000; t += 10)
            assertEquals(Step.NOTHING, c.poll("iron", 34, t));
    }

    @Test
    void aFinishedJobWaitsAMomentThenAsksAgainIfStillShort() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        assertTrue(c.planFinished("iron", true, true, 5));
        c.submitted("iron", true, 5);
        c.jobEnded("iron", true, 100);
        assertEquals(Phase.IDLE, c.phaseOf("iron"));
        assertEquals(Step.NOTHING, c.poll("iron", 10, 100 + StockCrafting.AFTER_JOB - 1));
        assertEquals(Step.CALCULATE, c.poll("iron", 10, 100 + StockCrafting.AFTER_JOB));
    }

    @Test
    void aFailedTryWaitsLongerEachTimeUpToACap() {
        var c = new StockCrafting<String>();
        long now = 0;
        long[] expected = {100, 200, 400, 800, 1600, 3200, 6000, 6000};
        for (long wait : expected) {
            assertEquals(Step.CALCULATE, c.poll("iron", 10, now));
            assertFalse(c.planFinished("iron", false, true, now), "no pattern: the plan only simulates");
            assertEquals(Phase.RETRY, c.phaseOf("iron"));
            assertEquals(wait, c.retryIn("iron", now));
            assertEquals(Step.NOTHING, c.poll("iron", 10, now + wait - 1), "still waiting");
            now += wait;
        }
        assertEquals(8, c.failuresOf("iron"));
    }

    @Test
    void aRefusedSubmitBacksOffToo() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        assertTrue(c.planFinished("iron", true, true, 3));
        c.submitted("iron", false, 3);     // every CPU busy
        assertEquals(Phase.RETRY, c.phaseOf("iron"));
        assertEquals(StockCrafting.BASE_BACKOFF, c.retryIn("iron", 3));
        assertEquals(Step.NOTHING, c.poll("iron", 10, 50));
        assertEquals(Step.CALCULATE, c.poll("iron", 10, 3 + StockCrafting.BASE_BACKOFF));
    }

    @Test
    void successForgetsEarlierFailures() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        c.planFinished("iron", false, true, 0);
        c.poll("iron", 10, 100);
        c.planFinished("iron", false, true, 100);
        assertEquals(2, c.failuresOf("iron"));
        c.poll("iron", 10, 300);
        assertTrue(c.planFinished("iron", true, true, 300));
        c.submitted("iron", true, 300);
        assertEquals(0, c.failuresOf("iron"));
        c.jobEnded("iron", true, 400);
        c.planFinished("iron", false, true, 0);     // not calculating: ignored
        assertEquals(0, c.failuresOf("iron"));
    }

    @Test
    void aCancelledJobIsNotRestartedAtOnce() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        c.planFinished("iron", true, true, 0);
        c.submitted("iron", true, 0);
        c.jobEnded("iron", false, 50);      // the player cancelled it on the CPU screen
        assertEquals(Phase.RETRY, c.phaseOf("iron"));
        assertEquals(Step.NOTHING, c.poll("iron", 10, 60));
        assertEquals(Step.CALCULATE, c.poll("iron", 10, 50 + StockCrafting.BASE_BACKOFF));
    }

    @Test
    void shortageGoneBeforeThePlanIsReadyDropsThePlan() {
        var c = new StockCrafting<String>();
        c.poll("iron", 34, 0);
        // the network received 5000 ingots while the plan was being calculated
        assertFalse(c.planFinished("iron", true, false, 20));
        assertEquals(Phase.IDLE, c.phaseOf("iron"));
        assertEquals(0, c.failuresOf("iron"));
        assertEquals(Step.CALCULATE, c.poll("iron", 34, 20), "short again later: asks again at once");
    }

    @Test
    void noLongerWantedForgetsTheBackoff() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        c.planFinished("iron", false, true, 0);
        assertEquals(1, c.failuresOf("iron"));
        assertEquals(Step.NOTHING, c.poll("iron", 0, 10));        // stock set back or filled
        assertEquals(0, c.failuresOf("iron"));
        assertTrue(c.isEmpty());
    }

    @Test
    void itemsAreIndependent() {
        var c = new StockCrafting<String>();
        assertEquals(Step.CALCULATE, c.poll("iron", 10, 0));
        assertEquals(Step.CALCULATE, c.poll("gold", 10, 0));
        c.planFinished("iron", false, true, 0);
        assertEquals(Phase.RETRY, c.phaseOf("iron"));
        assertEquals(Phase.CALCULATING, c.phaseOf("gold"));
    }

    @Test
    void atMostAFewCalculationsAtOnce() {
        var c = new StockCrafting<String>();
        assertEquals(Step.CALCULATE, c.poll("a", 1, 0));
        assertEquals(Step.CALCULATE, c.poll("b", 1, 0));
        assertEquals(Step.NOTHING, c.poll("c", 1, 0), "two plans at a time is the limit");
        assertTrue(c.planFinished("a", true, true, 5));
        c.submitted("a", true, 5);
        assertEquals(Step.CALCULATE, c.poll("c", 1, 10), "a slot is free once a plan is done");
    }

    @Test
    void aJobKeptThroughASaveIsNotAskedForAgain() {
        // the world was saved with the iron job running and loaded again: the link comes back, so the item is running
        var c = new StockCrafting<String>();
        c.restore("iron", 34);
        assertEquals(Phase.RUNNING, c.phaseOf("iron"));
        assertEquals(Step.NOTHING, c.poll("iron", 34, 0));
        assertEquals(java.util.List.of("iron"), c.active());
        c.jobEnded("iron", true, 100);
        assertTrue(c.active().isEmpty());
    }

    @Test
    void clearForgetsAll() {
        var c = new StockCrafting<String>();
        c.poll("iron", 10, 0);
        c.restore("gold", 5);
        c.clear();
        assertTrue(c.isEmpty());
        assertEquals(Step.CALCULATE, c.poll("gold", 5, 0));
    }
}
