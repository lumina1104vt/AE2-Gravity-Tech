package com.lumina.ae2_gvt.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;


public class StructureFrameBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public StructureFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    public static boolean isFormedFrame(BlockState state) {
        return state.getBlock() instanceof StructureFrameBlock && state.getValue(FORMED);
    }
}
