package com.lumina.twinstar.algorithm.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One internal pattern: what goes in and what comes out of ONE run. Immutable; arrays are copied on
 * construction and never handed out.
 * <p>
 * Outputs are main products, except byproducts: what a recipe leaves behind without the pattern
 * saying so (the bucket of a water bucket). A byproduct comes out like any output (the ledger and the
 * executor take it), but it is no route to the item: costs, routes and the theory layer's graph only
 * see main products. Input slot {@code i} matches AE2's
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
    private final boolean[] outputByproduct;

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
        // (an item that is also a main output is a main output for the whole amount)
        List<Integer> oi = new ArrayList<>();
        List<Amount> oa = new ArrayList<>();
        List<Boolean> ob = new ArrayList<>();
        for (int i = 0; i < b.outItems.size(); i++) {
            int item = b.outItems.get(i);
            int at = oi.indexOf(item);
            if (at >= 0) {
                oa.set(at, oa.get(at).add(b.outAmounts.get(i)));
                ob.set(at, ob.get(at) && b.outByproduct.get(i));
            } else {
                oi.add(item);
                oa.add(b.outAmounts.get(i));
                ob.add(b.outByproduct.get(i));
            }
        }
        this.outputItems = oi.stream().mapToInt(Integer::intValue).toArray();
        this.outputAmounts = oa.toArray(new Amount[0]);
        this.outputByproduct = new boolean[ob.size()];
        for (int i = 0; i < ob.size(); i++)
            outputByproduct[i] = ob.get(i);
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

    public boolean isByproduct(int i) {
        return outputByproduct[i];
    }

    /** Amount of {@code item} one run produces, byproducts included; ZERO if it is not an output. */
    public Amount outputAmountOf(int item) {
        for (int i = 0; i < outputItems.length; i++)
            if (outputItems[i] == item)
                return outputAmounts[i];
        return Amount.ZERO;
    }

    /** Amount of {@code item} one run makes as a main product: what a route to {@code item} counts; ZERO otherwise. */
    public Amount mainOutputAmountOf(int item) {
        for (int i = 0; i < outputItems.length; i++)
            if (outputItems[i] == item)
                return outputByproduct[i] ? Amount.ZERO : outputAmounts[i];
        return Amount.ZERO;
    }

    /** Any output, byproducts included. */
    public boolean produces(int item) {
        for (int o : outputItems)
            if (o == item)
                return true;
        return false;
    }

    /** A main product: the pattern is a route to {@code item}. */
    public boolean producesMain(int item) {
        return mainOutputAmountOf(item).signum() > 0;
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
            if (outputByproduct[i])
                sb.append("(BYPRODUCT)");
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
        private final List<Boolean> outByproduct = new ArrayList<>();
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
            return output(item, amount, false);
        }

        /** What the recipe leaves behind without the pattern saying so: comes out, but is no route to the item. */
        public Builder byproduct(int item, long amount) {
            return output(item, Amount.of(amount), true);
        }

        public Builder byproduct(int item, Amount amount) {
            return output(item, amount, true);
        }

        private Builder output(int item, Amount amount, boolean byproduct) {
            if (amount.signum() <= 0)
                throw new IllegalArgumentException("output amount must be positive");
            outItems.add(item);
            outAmounts.add(amount);
            outByproduct.add(byproduct);
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
            if (!outByproduct.contains(false))
                throw new IllegalStateException("a pattern needs at least one main output");
            return new Pattern(this, id);
        }
    }
}
