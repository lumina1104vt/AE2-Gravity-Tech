package com.lumina.ae2_gvt.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: the energy is the controller's of the chamber the port is built into (see {@link FusionControllerBlockEntity#energyInput}). */
public class EnergyPortBlockEntity extends BlockEntity {
    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.ENERGY_PORT.get(), pos, state);
    }
}
