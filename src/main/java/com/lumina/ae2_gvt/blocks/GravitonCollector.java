package com.lumina.ae2_gvt.blocks;

import com.lumina.ae2_gvt.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GravitonCollector extends appeng.block.AEBaseEntityBlock<com.lumina.ae2_gvt.blockentities.GravitonCollector> {
    public static final int HEIGHT = 5;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, HEIGHT - 1);

    private static final VoxelShape[] SHAPES = {
            Shapes.block(),
            Shapes.block(),
            Block.box(2, 0, 2, 14, 16, 14),
            Block.box(2, 0, 2, 14, 16, 14),
            Shapes.or(Block.box(3, 0, 3, 13, 10, 13), Block.box(0, 10, 0, 16, 16, 16)),
    };

    public GravitonCollector(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() + HEIGHT - 1 > level.getMaxBuildHeight() + 1)
            return null;

        for (int i = 1; i < HEIGHT; i++) {
            if (!level.getBlockState(pos.above(i)).canBeReplaced(context))
                return null;
        }

        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);     // AE2: who owns the network node
        for (int i = 1; i < HEIGHT; i++) {
            level.setBlock(pos.above(i), state.setValue(PART, i), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        int part = state.getValue(PART);
        if (dir == Direction.UP && part < HEIGHT - 1 && !isPart(neighbour, part + 1) ||
                dir == Direction.DOWN && part > 0 && !isPart(neighbour, part - 1)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            int part = state.getValue(PART);
            BlockPos basePos = pos.below(part);
            BlockState baseState = level.getBlockState(basePos);
            if (part != 0 && isPart(baseState, 0)) {
                BlockUtils.preventDropFromBase(level, basePos, baseState, player);
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? super.newBlockEntity(pos, state) : null;
    }

    private boolean isPart(BlockState state, int part) {
        return state.is(this) && state.getValue(PART) == part;
    }
}
