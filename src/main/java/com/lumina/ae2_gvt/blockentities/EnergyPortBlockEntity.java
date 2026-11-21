package com.lumina.ae2_gvt.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: the energy is the controller's of the chamber the port is built into (see {@link FusionControllerBlockEntity#energyInput}). */
public class EnergyPortBlockEntity extends BlockEntity {
    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.ENERGY_PORT.get(), pos, state);
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
                                                                            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY)
            return net.minecraftforge.common.util.LazyOptional.of(() -> FusionControllerBlockEntity.energyInput(getLevel(), getBlockPos())).cast();
        return super.getCapability(cap, side);
    }
}
