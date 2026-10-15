package com.lumina.ae2_gvt.blockentities.cpu;

import appeng.blockentity.crafting.CraftingBlockEntity;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DummyWhiteHoleCPUBlockEntity extends CraftingBlockEntity {
    public DummyWhiteHoleCPUBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntities.WHITE_HOLE_CPU.get(), pos, blockState);
    }
}
