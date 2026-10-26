package com.lumina.twinstar.algorithm.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.lumina.twinstar.algorithm.model.Amount;
import com.lumina.twinstar.algorithm.model.InputKind;
import com.lumina.twinstar.algorithm.model.Pattern;
import com.lumina.twinstar.algorithm.model.PatternLocation;
import com.lumina.twinstar.algorithm.model.Rational;
import com.lumina.twinstar.storage.TwinstarContent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Turns the network's AE2 patterns into internal patterns: the first registered adapter that
 * handles one translates it, otherwise its declared lists are used as they are.
 * <p>
 * Default translation, slot by slot (input slot i = {@code getInputs()[i]}, so the executor can
 * build {@code pushPattern}'s per-slot inputs):
 * <ul>
 *   <li>item = the first possible input, amount = its amount × the slot multiplier; the other
 *   possible inputs become alternatives and mark the pattern as allowing substitution;</li>
 *   <li>a remaining key equal to the input (the item comes back unchanged) makes the slot a
 *   CONTAINER; a different remaining key (water bucket → bucket, damaged tool) stays CONSUME and the
 *   remainder is added as a byproduct: the pattern does not list it, so it is no route to that item,
 *   but the ledger keeps it for the order's later steps;</li>
 *   <li>AE2 patterns carry no durability, so TOOL only comes from adapters;</li>
 *   <li>an output listed in the pattern's {@link PatternChances} (written by {@code gen_patterns}
 *   from the recipe) comes only with that probability.</li>
 * </ul>
 */
public final class PatternCompiler {
    private final List<PatternAdapter> adapters = new ArrayList<>();
    private final ItemIndex index;
    private final AtomicInteger nextId = new AtomicInteger();

    public PatternCompiler(ItemIndex index) {
        this.index = index;
    }

    public void register(PatternAdapter adapter) {
        adapters.add(adapter);
    }

    /**
     * @param priority    the provider's pattern priority (AE2, higher first)
     * @param hubInternal the pattern belongs to the twin-star hub itself (finished by the black hole)
     */
    public List<Pattern> compile(IPatternDetails details, int priority, boolean hubInternal) {
        PatternLocation where = hubInternal ? PatternLocation.HUB_INTERNAL
                : details instanceof AEProcessingPattern ? PatternLocation.EXTERNAL_PROCESS
                : PatternLocation.EXTERNAL_CRAFT;
        for (PatternAdapter a : adapters)
            if (a.handles(details))
                return a.translate(details, index, where, priority, nextId::getAndIncrement);
        return List.of(translateDeclared(details, where, priority));
    }

    private Pattern translateDeclared(IPatternDetails details, PatternLocation where, int priority) {
        Pattern.Builder b = new Pattern.Builder().where(where).priority(priority).source(details);
        boolean subst = false;
        Rational energy = Rational.ZERO;
        List<GenericStack> remainders = new ArrayList<>();
        for (IPatternDetails.IInput in : details.getInputs()) {
            GenericStack[] possible = in.getPossibleInputs();
            AEKey key = possible[0].what();
            long amount = Math.multiplyExact(possible[0].amount(), in.getMultiplier());
            int[] alts = new int[possible.length - 1];
            for (int j = 1; j < possible.length; j++)
                alts[j - 1] = index.idOf(possible[j].what());
            if (alts.length > 0)
                subst = true;
            AEKey rest = in.getRemainingKey(key);
            InputKind kind = rest != null && rest.equals(key) ? InputKind.CONTAINER : InputKind.CONSUME;
            if (rest != null && !rest.equals(key))
                remainders.add(new GenericStack(rest, amount));
            b.input(index.idOf(key), Amount.of(amount), kind, 0, alts);
            energy = energy.add(Rational.of(java.math.BigInteger.valueOf(amount),
                    java.math.BigInteger.valueOf(Math.max(1, key.getAmountPerOperation()))));
        }
        PatternChances chances = details.getDefinition().get(TwinstarContent.PATTERN_CHANCES.get());
        for (GenericStack out : details.getOutputs()) {
            double chance = chances == null ? 1 : chances.chanceOf(out.what());
            b.output(index.idOf(out.what()), Amount.of(out.amount()), chance > 0 && chance < 1 ? chance : 1);
        }
        for (GenericStack r : remainders)
            b.byproduct(index.idOf(r.what()), r.amount());
        return b.allowSubst(subst).energyPerRun(energy).build(nextId.getAndIncrement());
    }
}
