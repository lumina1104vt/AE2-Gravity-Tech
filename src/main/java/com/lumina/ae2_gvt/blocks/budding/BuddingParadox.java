package com.lumina.ae2_gvt.blocks.budding;

import appeng.core.definitions.AEBlocks;
import appeng.decorative.solid.BuddingCertusQuartzBlock;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class BuddingParadox extends BuddingCertusQuartzBlock {
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final DeferredItem<BlockItem> SMALL_BUD = AE2GravityTech.PARADOX.smallBud();
    private static final DeferredItem<BlockItem> MEDIUM_BUD = AE2GravityTech.PARADOX.mediumBud();
    private static final DeferredItem<BlockItem> LARGE_BUD = AE2GravityTech.PARADOX.largeBud();
    private static final DeferredItem<BlockItem> CLUSTER = AE2GravityTech.PARADOX.cluster();

    private static final DeferredBlock<BuddingParadox> ALMOST_FLAWLESS = Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX;
    private static final DeferredBlock<BuddingParadox> FLAWED = Blocks.FLAWED_BUDDING_PARADOX;
    private static final DeferredBlock<BuddingParadox> CHIPPED = Blocks.CHIPPED_BUDDING_PARADOX;
    private static final DeferredBlock<BuddingParadox> DAMAGED = Blocks.DAMAGED_BUDDING_PARADOX;

    public BuddingParadox(Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextInt(5) == 0) {
            Direction direction = Util.getRandom(DIRECTIONS, randomSource);
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            Block newCluster = null;
            if (canClusterGrowAtState(targetState)) {
                newCluster = SMALL_BUD.get().getBlock();
            } else if (targetState.is(SMALL_BUD.get().getBlock()) && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
                newCluster = MEDIUM_BUD.get().getBlock();
            } else if (targetState.is(MEDIUM_BUD.get().getBlock()) && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
                newCluster = LARGE_BUD.get().getBlock();
            } else if (targetState.is(LARGE_BUD.get().getBlock()) && targetState.getValue(AmethystClusterBlock.FACING) == direction) {
                newCluster = CLUSTER.get().getBlock();
            }

            if (newCluster != null) {
                BlockState newClusterState = newCluster.defaultBlockState().setValue(AmethystClusterBlock.FACING, direction).setValue(AmethystClusterBlock.WATERLOGGED, targetState.getFluidState().getType() == Fluids.WATER);
                level.setBlockAndUpdate(targetPos, newClusterState);
                if (randomSource.nextInt(12) == 0) {
                    Block newBlock;
                    if (this == ALMOST_FLAWLESS.get()) {
                        newBlock = FLAWED.get();
                    } else if (this == FLAWED.get()) {
                        newBlock = CHIPPED.get();
                    } else if (this == CHIPPED.get()) {
                        newBlock = DAMAGED.get();
                    } else {
                        if (this != DAMAGED.get()) {
                            throw new IllegalStateException("Unexpected block: " + this);
                        }

                        newBlock = AEBlocks.FLUIX_BLOCK.block();
                    }

                    level.setBlockAndUpdate(pos, newBlock.defaultBlockState());
                }
            }
        }
    }
}
