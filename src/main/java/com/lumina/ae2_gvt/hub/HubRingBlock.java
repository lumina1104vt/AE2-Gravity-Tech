package com.lumina.ae2_gvt.hub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A block of the hub's 3-wide rings. Placed like a log: {@code axis} is the direction ACROSS the
 * ring's width. Each block then picks its tile from its neighbours along that axis, so the three
 * blocks across a ring read as one band with an edge only on the outside: high / middle / low,
 * or single when it stands alone.
 * <p>
 * The blockstate rotates the axis=y model like a log (z: x90, x: x90 y90), so the model's "up"
 * is UP for y, NORTH for z and EAST for x - that is the side the high tile's edge faces.
 */
public class HubRingBlock extends RotatedPillarBlock {
    public enum Part implements StringRepresentable {
        SINGLE, LOW, MIDDLE, HIGH;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Never set in the world: the look the lib draws while the hub is formed (a violet line along the band's edges). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public HubRingBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(PART, Part.SINGLE).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state.setValue(PART, partAt(context.getLevel(), context.getClickedPos(), state.getValue(AXIS)));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() != state.getValue(AXIS)) {
            return state;
        }
        return state.setValue(PART, partAt(level, pos, state.getValue(AXIS)));
    }

    /** {@code state} with the part its current neighbours call for (after placing blocks without updates). */
    public BlockState withPart(BlockGetter level, BlockPos pos, BlockState state) {
        return state.setValue(PART, partAt(level, pos, state.getValue(AXIS)));
    }

    /** The model's "up" for each axis, after the blockstate's log-style rotation. */
    static Direction modelUp(Direction.Axis axis) {
        return switch (axis) {
            case Y -> Direction.UP;
            case Z -> Direction.NORTH;
            case X -> Direction.EAST;
        };
    }

    private Part partAt(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction up = modelUp(axis);
        boolean above = sameRing(level.getBlockState(pos.relative(up)), axis);
        boolean below = sameRing(level.getBlockState(pos.relative(up.getOpposite())), axis);
        if (above && below) {
            return Part.MIDDLE;
        }
        return above ? Part.LOW : below ? Part.HIGH : Part.SINGLE;
    }

    private boolean sameRing(BlockState state, Direction.Axis axis) {
        return state.is(this) && state.getValue(AXIS) == axis;
    }
}
