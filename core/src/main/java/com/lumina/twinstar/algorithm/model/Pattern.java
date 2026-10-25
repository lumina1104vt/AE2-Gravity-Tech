package com.lumina.twinstar.algorithm.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One internal pattern: what goes in and what comes out of ONE run. Immutable; arrays are copied on
 * construction and never handed out.
 * <p>
 * All outputs are equal (no main product / byproduct). Input slot {@code i} matches AE2's
 * {@code IPatternDetails.getInputs()[i]} for patterns translated from AE2, so the executor can build
 * the per-slot {@code KeyCounter[]} for {@code pushPattern} directly.
 */
public final class Pattern {
    public final int id;

    private final int[] inputItems;
    private final Amount[] inputAmounts;
    private final InputKind[] inputKinds;
    private final int[] inputDurability;        // TOOL only: runs one tool lasts
    private final int[][] inputAlternatives;    // other items accepted in the slot (allowSubst only)

    private final int[] outputItems;
    private final Amount[] outputAmounts;

    public final PatternLocation where;
    public final int priority;                  // provider priority (AE2, higher first)
    public final boolean allowSubst;
    public final Rational energyPerRun;         // AE energy per run, exact (AE2: sum of inputs / amount per operation)
    /** The original AE2 {@code IPatternDetails}; null for patterns built from ids in tests. */
    private final @Nullable Object source;

    private Pattern(Builder b, int id) {
        this.id = id;
        int n = b.inItems.size();
        this.inputItems = new int[n];
        this.inputAmounts = new Amount[n];
        this.inputKinds = new InputKind[n];
        this.inputDurability = new int[n];
        this.inputAlternatives = new int[n][];
        for (int i = 0; i < n; i++) {
            inputItems[i] = b.inItems.get(i);
            inputAmounts[i] = b.inAmounts.get(i);
            inputKinds[i] = b.inKinds.get(i);
            inputDurability[i] = b.inDurability.get(i);
            inputAlternatives[i] = b.inAlternatives.get(i).clone();
        }
        // outputs: merge repeated items so outputAmount(x) is a single lookup
        List<Integer> oi = new ArrayList<>();
        List<Amount> oa = new ArrayList<>();
        for (int i = 0; i < b.outItems.size(); i++) {
            int item = b.outItems.get(i);
            int at = oi.indexOf(item);
            if (at >= 0)
                oa.set(at, oa.get(at).add(b.outAmounts.get(i)));
            else {
                oi.add(item);
                oa.add(b.outAmounts.get(i));
            }
        }
        this.outputItems = oi.stream().mapToInt(Integer::intValue).toArray();
        this.outputAmounts = oa.toArray(new Amount[0]);
        this.where = b.where;
        this.priority = b.priority;
        this.allowSubst = b.allowSubst;
        this.energyPerRun = b.energyPerRun;
        this.source = b.source;
    }

    public int inputCount() {
        return inputItems.length;
    }

    public int inputItem(int i) {
        return inputItems[i];
    }

    public Amount inputAmount(int i) {
        return inputAmounts[i];
    }

    public InputKind inputKind(int i) {
        return inputKinds[i];
    }

    public int inputDurability(int i) {
        return inputDurability[i];
    }

    public int alternativeCount(int i) {
        return inputAlternatives[i].length;
    }

    public int alternative(int i, int j) {
        return inputAlternatives[i][j];
    }

    public int outputCount() {
        return outputItems.length;
    }

    public int outputItem(int i) {
        return outputItems[i];
    }

    public Amount outputAmount(int i) {
        return outputAmounts[i];
    }

    /** Amount of {@code item} one run produces; ZERO if it is not an output. */
    public Amount outputAmountOf(int item) {
        for (int i = 0; i < outputItems.length; i++)
            if (outputItems[i] == item)
                return outputAmounts[i];
        return Amount.ZERO;
    }

    public boolean produces(int item) {
        for (int o : outputItems)
            if (o == item)
                return true;
        return false;
    }

    /** Amount of {@code item} one run consumes (CONSUME slots only). */
    public Amount consumedAmountOf(int item) {
        Amount sum = Amount.ZERO;
        for (int i = 0; i < inputItems.length; i++)
            if (inputItems[i] == item && inputKinds[i] == InputKind.CONSUME)
                sum = sum.add(inputAmounts[i]);
        return sum;
    }

    public boolean hasInput(int item) {
        for (int in : inputItems)
            if (in == item)
                return true;
        return false;
    }

    public @Nullable Object source() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("P").append(id).append('[');
        for (int i = 0; i < inputItems.length; i++) {
            if (i > 0)
                sb.append(" + ");
            sb.append(inputAmounts[i]).append('×').append(inputItems[i]);
            if (inputKinds[i] != InputKind.CONSUME)
                sb.append('(').append(inputKinds[i]).append(')');
        }
        sb.append(" → ");
        for (int i = 0; i < outputItems.length; i++) {
            if (i > 0)
                sb.append(" + ");
            sb.append(outputAmounts[i]).append('×').append(outputItems[i]);
        }
        return sb.append(']').toString();
    }

    public static final class Builder {
        private final List<Integer> inItems = new ArrayList<>();
        private final List<Amount> inAmounts = new ArrayList<>();
        private final List<InputKind> inKinds = new ArrayList<>();
        private final List<Integer> inDurability = new ArrayList<>();
        private final List<int[]> inAlternatives = new ArrayList<>();
        private final List<Integer> outItems = new ArrayList<>();
        private final List<Amount> outAmounts = new ArrayList<>();
        private PatternLocation where = PatternLocation.HUB_INTERNAL;
        private int priority;
        private boolean allowSubst;
        private Rational energyPerRun = Rational.ZERO;
        private @Nullable Object source;

        public Builder input(int item, long amount) {
            return input(item, Amount.of(amount), InputKind.CONSUME, 0, new int[0]);
        }

        public Builder input(int item, Amount amount, InputKind kind, int durability, int[] alternatives) {
            if (amount.signum() <= 0)
                throw new IllegalArgumentException("input amount must be positive");
            inItems.add(item);
            inAmounts.add(amount);
            inKinds.add(kind);
            inDurability.add(durability);
            inAlternatives.add(alternatives.clone());
            return this;
        }

        public Builder catalyst(int item, long amount) {
            return input(item, Amount.of(amount), InputKind.CATALYST, 0, new int[0]);
        }

        public Builder container(int item, long amount) {
            return input(item, Amount.of(amount), InputKind.CONTAINER, 0, new int[0]);
        }

        public Builder tool(int item, int durability) {
            return input(item, Amount.ONE, InputKind.TOOL, durability, new int[0]);
        }

        public Builder output(int item, long amount) {
            return output(item, Amount.of(amount));
        }

        public Builder output(int item, Amount amount) {
            if (amount.signum() <= 0)
                throw new IllegalArgumentException("output amount must be positive");
            outItems.add(item);
            outAmounts.add(amount);
            return this;
        }

        public Builder where(PatternLocation where) {
            this.where = where;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder allowSubst(boolean allowSubst) {
            this.allowSubst = allowSubst;
            return this;
        }

        public Builder energyPerRun(Rational energyPerRun) {
            this.energyPerRun = energyPerRun;
            return this;
        }

        public Builder source(@Nullable Object source) {
            this.source = source;
            return this;
        }

        public Pattern build(int id) {
            if (outItems.isEmpty())
                throw new IllegalStateException("a pattern needs at least one output");
            return new Pattern(this, id);
        }
    }
}
