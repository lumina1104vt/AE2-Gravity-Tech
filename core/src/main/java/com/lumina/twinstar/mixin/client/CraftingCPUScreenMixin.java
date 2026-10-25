package com.lumina.twinstar.mixin.client;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.TimeUnit;

/**
 * The estimated time in the CPU status title: when AE2's estimate overflows (an astronomical order
 * at today's speed never finishes) it shows ∞ instead of 2562047:47:16.
 */
@Mixin(CraftingCPUScreen.class)
public abstract class CraftingCPUScreenMixin {
    @Unique
    private static final long twinstar$SATURATED_MILLIS = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE);

    @WrapOperation(method = "updateBeforeRender", at = @At(value = "INVOKE",
            target = "Lorg/apache/commons/lang3/time/DurationFormatUtils;formatDuration(JLjava/lang/String;)Ljava/lang/String;"))
    private String twinstar$infiniteEta(long millis, String format, Operation<String> original) {
        if (millis >= twinstar$SATURATED_MILLIS)
            return "∞";
        return original.call(millis, format);
    }
}
