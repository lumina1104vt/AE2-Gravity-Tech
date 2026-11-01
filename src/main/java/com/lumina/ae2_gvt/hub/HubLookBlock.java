package com.lumina.ae2_gvt.hub;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A plain hub block with a formed look (the helix strands). {@code formed} is never set in the world: it is
 * the look the lib draws while the hub is formed.
 */
public class HubLookBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public HubLookBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
