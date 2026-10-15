package com.lumina.ae2_gvt.prototype.frame;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The block megastructures are built from. While part of a formed structure it switches to the
 * formed look: neighbouring formed frames join up into one panel with AE2's ring frame running round
 * the outside only (see client.StructureFrameModel). The structure sets {@link #FORMED}.
 */
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
