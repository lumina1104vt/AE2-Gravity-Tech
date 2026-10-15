package com.lumina.ae2_gvt.blocks;

import com.lumina.ae2_gvt.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import org.jspecify.annotations.Nullable;

public class GravitonCollector extends Block implements EntityBlock {
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
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)];
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() + HEIGHT - 1 > level.getMaxY())
            return null;

        for (int i = 1; i < HEIGHT; i++) {
            if (!level.getBlockState(pos.above(i)).canBeReplaced(context))
                return null;
        }

        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        for (int i = 1; i < HEIGHT; i++) {
            level.setBlock(pos.above(i), state.setValue(PART, i), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        int part = state.getValue(PART);
        if (dir == Direction.UP && part < HEIGHT - 1 && !isPart(neighbour, part + 1) ||
            dir == Direction.DOWN && part > 0 && !isPart(neighbour, part - 1)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            int part = state.getValue(PART);
            BlockPos basePos = pos.below(part);
            BlockState baseState = level.getBlockState(basePos);
            if (part != 0 && isPart(baseState, 0)) {
                BlockUtils.preventDropFromBase(level, basePos, baseState, player);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new com.lumina.ae2_gvt.blockentities.GravitonCollector(pos, state) : null;
    }

    private boolean isPart(BlockState state, int part) {
        return state.is(this) && state.getValue(PART) == part;
    }
}
