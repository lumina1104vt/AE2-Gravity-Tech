package com.lumina.ae2_gvt.prototype.fusion;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.blocks.GravityConstrainer;
import org.jspecify.annotations.Nullable;

import com.lumina.ae2_gvt.prototype.frame.StructureFrameBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fusion generator's chamber: a 9x9x9 cube around a 7x7x7 inside.
 * <ul>
 *   <li>the 12 edges (corners included): structure frame</li>
 *   <li>the faces: containment glass, matter ports, energy ports or the controller itself, in any
 *       mix - the controller and ports may take any glass position</li>
 *   <li>inside, 7x7x7: six gravity constrainers, one against the middle of each wall on the
 *       inside, pointing at the centre (turned to do so when it forms, so they can be placed any
 *       way round); everything else empty, for the plasma</li>
 * </ul>
 * The controller must be on a face (not an edge). The chamber is found by trying every cube that
 * would put the controller there: 6 faces x 7 x 7 positions, each rejected at its first wrong block.
 */
public final class FusionStructure {
    public static final int SIZE = 9;
    private static final int LAST = SIZE - 1, MID = SIZE / 2;

    private FusionStructure() {
    }

    /** The low corner of a complete chamber around this controller, or null. */
    public static @Nullable BlockPos find(Level level, BlockPos controller, @Nullable BlockPos hint) {
        if (hint != null && matches(level, hint, controller)) {
            return hint;
        }
        for (Direction out : Direction.values()) {                      // the face the controller sits in
            Direction.Axis axis = out.getAxis();
            int along = out.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -LAST : 0;
            Direction.Axis[] others = otherAxes(axis);
            for (int a = 1; a < LAST; a++) {
                for (int b = 1; b < LAST; b++) {
                    BlockPos min = offset(offset(offset(controller, axis, along), others[0], -a), others[1], -b);
                    if (matches(level, min, controller)) {
                        return min;
                    }
                }
            }
        }
        return null;
    }

    private static boolean matches(Level level, BlockPos min, BlockPos controller) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    p.setWithOffset(min, x, y, z);
                    if (!fits(level.getBlockState(p), role(x, y, z), p.equals(controller))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private enum Role { EDGE, FACE, CONSTRAINER, INSIDE }

    private static Role role(int x, int y, int z) {
        int onBoundary = (x == 0 || x == LAST ? 1 : 0) + (y == 0 || y == LAST ? 1 : 0) + (z == 0 || z == LAST ? 1 : 0);
        if (onBoundary >= 2) {
            return Role.EDGE;
        }
        if (onBoundary == 1) {
            return Role.FACE;
        }
        // inside: the cell just in from the middle of each wall holds a constrainer
        int centred = (x == MID ? 1 : 0) + (y == MID ? 1 : 0) + (z == MID ? 1 : 0);
        boolean nextToWall = x == 1 || x == LAST - 1 || y == 1 || y == LAST - 1 || z == 1 || z == LAST - 1;
        return centred == 2 && nextToWall ? Role.CONSTRAINER : Role.INSIDE;
    }

    private static boolean fits(BlockState state, Role role, boolean isController) {
        return switch (role) {
            case EDGE -> state.getBlock() instanceof StructureFrameBlock;
            case CONSTRAINER -> state.is(Blocks.GRAVITY_CONSTRAINER.get());
            case FACE -> isController || state.is(FusionGenerator.GLASS.get()) || state.is(FusionGenerator.MATTER_PORT.get())
                    || state.is(FusionGenerator.ENERGY_PORT.get());
            case INSIDE -> state.isAir();
        };
    }

    /**
     * Forming: the frames take their formed look and the constrainers turn to face the centre.
     * Unforming: the frames go back. Only blocks still in place are touched.
     */
    public static void setFormed(Level level, BlockPos min, boolean formed) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    p.setWithOffset(min, x, y, z);
                    BlockState state = level.getBlockState(p);
                    Role role = role(x, y, z);
                    BlockState next = state;
                    if (role == Role.EDGE && state.getBlock() instanceof StructureFrameBlock) {
                        next = state.setValue(StructureFrameBlock.FORMED, formed);
                    } else if (role == Role.CONSTRAINER && formed && state.is(Blocks.GRAVITY_CONSTRAINER.get())) {
                        next = state.setValue(GravityConstrainer.FACING, inward(x, y, z));
                    }
                    if (next != state) {
                        level.setBlock(p, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    /** From a constrainer's cell (next to a wall) towards the middle of the cube. */
    private static Direction inward(int x, int y, int z) {
        if (x < MID) return Direction.EAST;
        if (x > MID) return Direction.WEST;
        if (y < MID) return Direction.UP;
        if (y > MID) return Direction.DOWN;
        return z < MID ? Direction.SOUTH : Direction.NORTH;
    }

    private static Direction.Axis[] otherAxes(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static BlockPos offset(BlockPos pos, Direction.Axis axis, int by) {
        return pos.relative(axis, by);
    }
}
