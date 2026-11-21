package com.lumina.ae2_gvt.mixins;

import java.util.HashSet;
import java.util.Set;

import appeng.api.networking.IGridNode;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.Grid;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A grid files its machines under the exact class of their owner, so a subclass of AE2's controller is not found when AE2 asks for
 * its controllers (the pathing starts from them: without this the channels never start). Asked for AE2's controllers, the quantum
 * ones are added to the answer.
 */
@Mixin(value = Grid.class, remap = false)
public abstract class GridMixin {
    @Shadow
    @Final
    private SetMultimap<Class<?>, IGridNode> machines;

    @Inject(method = "getMachineNodes", at = @At("RETURN"), cancellable = true)
    private void ae2_gvt$quantumNodes(Class<?> clazz, CallbackInfoReturnable<Iterable<IGridNode>> cir) {
        if (clazz == ControllerBlockEntity.class)
            cir.setReturnValue(Iterables.concat(cir.getReturnValue(), machines.get(QuantumControllerBlockEntity.class)));
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "getMachines", at = @At("RETURN"), cancellable = true)
    private <T> void ae2_gvt$quantumMachines(Class<T> clazz, CallbackInfoReturnable<Set<T>> cir) {
        if (clazz == ControllerBlockEntity.class) {
            Set<T> all = new HashSet<>(cir.getReturnValue());
            for (IGridNode node : machines.get(QuantumControllerBlockEntity.class))
                all.add((T) node.getOwner());
            cir.setReturnValue(all);
        }
    }
}
