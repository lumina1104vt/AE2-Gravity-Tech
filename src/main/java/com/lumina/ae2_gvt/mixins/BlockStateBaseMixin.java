package com.lumina.ae2_gvt.mixins;

import com.lumina.ae2_gvt.redstone.RemoteSignals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A machine bound from afar to a machine access interface gets its redstone pulse without any block: when a block next to it is
 * asked for its signal towards the machine, it answers 15 for as long as the pulse lasts.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void ae2_gvt$remoteSignal(BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() < 15 && RemoteSignals.powers(level, pos.relative(direction.getOpposite())))
            cir.setReturnValue(15);
    }

    @Inject(method = "getDirectSignal", at = @At("RETURN"), cancellable = true)
    private void ae2_gvt$remoteDirectSignal(BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() < 15 && RemoteSignals.powers(level, pos.relative(direction.getOpposite())))
            cir.setReturnValue(15);
    }
}
