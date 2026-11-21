package com.lumina.ae2_gvt.terminal;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.core.AELog;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 合成處理樣板: what the terminal encodes. Works like AE2's processing pattern (same data, same decoding), plus the machine
 * data the hub reads; the hub sends it to that machine through a machine access interface.
 */
public class SynthesisPatternItem extends EncodedPatternItem {
    public SynthesisPatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        if (stack.getItem() != this || !stack.hasTag())
            return null;
        return decode(AEItemKey.of(stack), level);
    }

    @Override
    public @Nullable IPatternDetails decode(AEItemKey what, Level level) {
        if (what == null || !what.hasTag())
            return null;
        try {
            return new AEProcessingPattern(what);
        } catch (Exception e) {
            AELog.warn("Could not decode an invalid synthesis pattern %s: %s", what.getTag(), e);
            return null;
        }
    }
}
