package com.lumina.ae2_gvt.mixins;

import java.util.Collection;
import java.util.Set;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.pathfinding.ControllerValidator;
import com.lumina.ae2_gvt.quantum.QuantumChannels;
import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A slave quantum controller, and every controller built onto it, is not a controller structure of its own: it gives channels
 * like any controller, but AE2's check for "two controller structures in one network" (a conflict) leaves it out, both when it
 * counts the controllers and when it walks the network to find them.
 */
@Mixin(value = ControllerValidator.class, remap = false)
public abstract class ControllerValidatorMixin {
    /** the slave structures of the check being made (the check runs on the server thread, one at a time) */
    private static final ThreadLocal<Set<ControllerBlockEntity>> ae2_gvt$slaves = new ThreadLocal<>();

    @ModifyVariable(method = "calculateState", at = @At("HEAD"), argsOnly = true)
    private static Collection<ControllerBlockEntity> ae2_gvt$withoutSlaves(Collection<ControllerBlockEntity> controllers) {
        Set<ControllerBlockEntity> slaves = QuantumChannels.slaveStructures(controllers);
        ae2_gvt$slaves.set(slaves.isEmpty() ? null : slaves);
        if (slaves.isEmpty())
            return controllers;
        return controllers.stream().filter(c -> !slaves.contains(c)).toList();
    }

    /** Quantum controllers do not join up with AE2's: one of each in a network is a conflict (the red state). */
    @Inject(method = "calculateState", at = @At("HEAD"), cancellable = true)
    private static void ae2_gvt$mixed(Collection<ControllerBlockEntity> controllers, CallbackInfoReturnable<ControllerState> cir) {
        boolean quantum = false, ae2 = false;
        for (ControllerBlockEntity c : controllers) {
            if (c instanceof QuantumControllerBlockEntity)
                quantum = true;
            else
                ae2 = true;
        }
        if (quantum && ae2) {
            ae2_gvt$slaves.remove();
            cir.setReturnValue(ControllerState.CONTROLLER_CONFLICT);
        }
    }

    @Inject(method = "calculateState", at = @At("RETURN"))
    private static void ae2_gvt$done(Collection<ControllerBlockEntity> controllers, CallbackInfoReturnable<ControllerState> cir) {
        ae2_gvt$slaves.remove();
    }

    /** walking the network: a slave's controllers are passed through, not counted */
    @Inject(method = "visitNode", at = @At("HEAD"), cancellable = true)
    private void ae2_gvt$skipSlaves(IGridNode node, CallbackInfoReturnable<Boolean> cir) {
        Set<ControllerBlockEntity> slaves = ae2_gvt$slaves.get();
        if (slaves != null && node.getOwner() instanceof ControllerBlockEntity c && slaves.contains(c))
            cir.setReturnValue(true);
    }
}
