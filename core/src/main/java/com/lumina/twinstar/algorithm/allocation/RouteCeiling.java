package com.lumina.twinstar.algorithm.allocation;

import com.lumina.twinstar.algorithm.cache.Snapshot;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.LogMath;
import com.lumina.twinstar.algorithm.model.Pattern;
import com.lumina.twinstar.algorithm.planning.DemandPass;
import com.lumina.twinstar.algorithm.planning.Split;

import java.util.ArrayList;
import java.util.List;

/**
 * C1 — when an item has several routes: how much each route can make on its own, then a greedy
 * split in route order. The ceilings answer "at most how many with this route alone"; they cannot
 * simply be added up (routes may share raws) — whoever really gets what is C2's job.
 */
public final class RouteCeiling {
    /** Slack for the BOM fast check (the BOM is a log-domain estimate). */
    private static final double FAST_CHECK_SLACK = 1e-9;

    private RouteCeiling() {
    }

    /** Routes are already in route-key order. Whatever no route can cover goes to the first route and shows up as a raw shortage. */
    public static List<Split> greedySplit(DemandPass pass, int x, Amount d, int[] routes) {
        return greedySplit(pass, x, d, routes, true);
    }

    /** {@code forceRest}: what no route can make goes to the first route anyway (its raws are then missing). */
    public static List<Split> greedySplit(DemandPass pass, int x, Amount d, int[] routes, boolean forceRest) {
        List<Split> out = new ArrayList<>();
        Amount left = d;
        for (int p : routes) {
            if (left.signum() <= 0)
                break;
            Amount m = routeCeiling(pass, p, x, left);
            if (m.signum() <= 0)
                continue;
            out.add(new Split(p, m.ceilDiv(pass.snapshot().pattern(p).outputAmountOf(x)), m));
            left = left.sub(m);
        }
        if (left.signum() > 0 && forceRest)
            out = pass.mergeInto(out, routes[0], x, left);
        return out;
    }

    /** The most of {@code d} that route {@code p} can make alone with the stock and ledger left in this pass. */
    public static Amount routeCeiling(DemandPass pass, int p, int x, Amount d) {
        Snapshot snap = pass.snapshot();
        if (snap.unitLogCost(p, x) == LogMath.INF)
            return Amount.ZERO;
        if (fastCheck(pass, p, x, d))
            return d;       // most orders stop here
        // past the soft limit: stop improving (design) - no more trial runs, whatever no route
        // proves goes to the first route (found with a real pack: trials alone took over 1 s)
        if (pass.deadline().softPassed())
            return Amount.ZERO;
        // the BOM gives the most the raws allow; check it with one B run, then step down a little
        // (fixed overheads and rounding make the estimate a bit high)
        Amount m = bomEstimate(pass, p, x, d);
        if (m.signum() <= 0)
            return stockedIntermediateInput(pass, p) ? fromIntermediates(pass, p, x, d) : Amount.ZERO;
        for (int step = 0; step < DOWN_STEPS && m.signum() > 0; step++) {
            if (pass.trial(x, m, p))
                return refineUp(pass, p, x, m, d);
            if (pass.deadline().softPassed())
                return Amount.ZERO;
            Amount next = m.mul(9).div(Amount.of(10));
            m = next.compareTo(m) < 0 ? next : m.sub(Amount.ONE);
        }
        // the estimate was far off (stock of intermediates, catalysts): binary search below it
        Amount lo = Amount.ZERO;
        Amount hi = m;
        while (hi.sub(lo).compareTo(Amount.ONE) > 0 && !pass.deadline().softPassed()) {
            Amount mid = lo.add(hi).half();
            if (pass.trial(x, mid, p))
                lo = mid;
            else
                hi = mid;
        }
        return lo;
    }

    /**
     * The raws allow nothing, but intermediates in stock may (the BOM only counts raws). Searched
     * upwards from 1: what stock allows is small next to an astronomical order, so stepping down
     * from the order would take thousands of trials.
     */
    private static Amount fromIntermediates(DemandPass pass, int p, int x, Amount d) {
        Amount lo = Amount.ONE.min(d);
        if (!pass.trial(x, lo, p))
            return Amount.ZERO;     // one trial for the common case: the route really is out
        if (pass.trial(x, d, p))
            return d;
        Amount hi = d;
        while (!pass.deadline().softPassed()) {
            Amount next = lo.mul(2);
            if (next.compareTo(hi) >= 0)
                break;
            if (pass.trial(x, next, p))
                lo = next;
            else {
                hi = next;
                break;
            }
        }
        while (hi.sub(lo).compareTo(Amount.ONE) > 0 && !pass.deadline().softPassed()) {
            Amount mid = lo.add(hi).half();
            if (pass.trial(x, mid, p))
                lo = mid;
            else
                hi = mid;
        }
        return lo;
    }

    /**
     * Is any direct input of p an intermediate (not raw) with stock or ledger left? Only then can a
     * route whose raws allow nothing still make some: trying every such route cost a real pack
     * (30 levels, little stock) hundreds of trial runs per order. Stock deeper down is not seen;
     * that demand then goes to the first route.
     */
    private static boolean stockedIntermediateInput(DemandPass pass, int p) {
        Snapshot snap = pass.snapshot();
        Pattern pat = snap.pattern(p);
        for (int s = 0; s < pat.inputCount(); s++) {
            int y = pat.inputItem(s);
            if (!snap.isRaw(y) && pass.available(y).signum() > 0)
                return true;
        }
        return false;
    }

    private static final int DOWN_STEPS = 8;
    private static final int UP_STEPS = 4;

    /** Intermediates in stock can make the raw-only estimate too low: a few doubling steps up, capped by d. */
    private static Amount refineUp(DemandPass pass, int p, int x, Amount ok, Amount d) {
        Amount lo = ok;
        for (int i = 0; i < UP_STEPS && lo.compareTo(d) < 0 && !pass.deadline().softPassed(); i++) {
            Amount next = lo.mul(2).min(d);
            if (!pass.trial(x, next, p))
                break;
            lo = next;
        }
        return lo;
    }

    /** min over raws k of available(k) ÷ (k per item through p), capped by d. */
    private static Amount bomEstimate(DemandPass pass, int p, int x, Amount d) {
        Snapshot snap = pass.snapshot();
        double best = d.log10();
        for (int k : snap.patBomItems(p)) {
            double per = snap.patBomUnitLog(p, x, k);
            if (per == LogMath.ZERO || pass.isInfinite(k))
                continue;       // an infinite source sets no ceiling, however large the order
            best = Math.min(best, pass.available(k).log10() - per);
        }
        return Amount.floorOfLog10(best + 1e-12).min(d);
    }

    /** Every raw in bom_p × d is covered by what is left: no need to run B. */
    private static boolean fastCheck(DemandPass pass, int p, int x, Amount d) {
        Snapshot snap = pass.snapshot();
        int[] ks = snap.patBomItems(p);
        double logD = d.log10();
        for (int k : ks) {
            if (pass.isInfinite(k))
                continue;
            double needLog = snap.patBomUnitLog(p, x, k) + logD;
            double haveLog = pass.available(k).log10();
            if (needLog > haveLog + FAST_CHECK_SLACK)
                return false;
        }
        // the BOM is only an estimate (rounding, catalysts, loops): confirm with one B run when it is close
        return ks.length > 0;
    }
}
