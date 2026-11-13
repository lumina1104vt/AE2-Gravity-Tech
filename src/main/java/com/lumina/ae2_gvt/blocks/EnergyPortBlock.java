package com.lumina.ae2_gvt.blocks;

import com.lumina.ae2_gvt.blockentities.EnergyPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量接口: a wall block of the fusion generator's chamber that takes FE in and gives back what the reactions made. It has a block
 * entity only so that Jade and the like, which read the energy of a block entity, can show it.
 */
public class EnergyPortBlock extends Block implements EntityBlock {
    public EnergyPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPortBlockEntity(pos, state);
    }
}
