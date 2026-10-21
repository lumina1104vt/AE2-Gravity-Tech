package com.lumina.ae2_gvt.blocks.cpu;

import appeng.block.crafting.AbstractCraftingUnitBlock;
import appeng.block.crafting.ICraftingUnitType;
import com.lumina.ae2_gvt.blockentities.cpu.DummyWhiteHoleCPUBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DummyWhiteHoleCPU extends Block implements EntityBlock {
    public DummyWhiteHoleCPU(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }
}
