package com.lumina.twinstar.compat.mixin;

import appeng.api.networking.IGrid;
import com.lumina.twinstar.api.ITwinStarService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Neo ECO opens its own crafting status screen instead of AE2's on a network with its computation system. That
 * screen only reads its own CPUs and AE2's: a twin-star job shows as an empty table. On a network with a
 * twin-star hub AE2's screen stays, which shows both (Neo ECO supports it for its own CPUs).
 */
@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.gui.computation.ECOCraftingStatusUI", remap = false)
public abstract class EcoStatusScreenMixin {
    @Inject(method = "hasFunctionalHost", at = @At("HEAD"), cancellable = true, remap = false)
    private static void twinstarCompat$keepAe2Screen(IGrid grid, CallbackInfoReturnable<Boolean> cir) {
        ITwinStarService twinStar = grid.getService(ITwinStarService.class);
        if (twinStar != null && twinStar.hasHub())
            cir.setReturnValue(false);
    }
}
