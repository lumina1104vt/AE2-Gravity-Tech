package com.lumina.ae2_gvt.structure;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.blocks.FusionController;
import com.lumina.ae2_gvt.blocks.GravityConstrainer;
import com.lumina.ae2_gvt.blocks.StructureFrameBlock;
import com.lumina.ae2_gvt.hub.HexagramHub;
import com.lumina.ae2_gvt.hub.HubStructureBlocks;
import com.lumina.lmb.api.BlockMatchers;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.api.RoleBlocks;
import com.lumina.lmb.core.Definition;
import com.lumina.lmb.core.Match;
import com.lumina.lmb.core.Matcher;
import com.lumina.lmb.core.Role;
import com.lumina.lmb.core.RoleField;
import com.lumina.lmb.core.Shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fusion generator's chamber: a 9x9x9 cube around a 7x7x7 inside.
 * <ul>
 *   <li>the 12 edges (corners included): structure frame</li>
 *   <li>the faces: containment glass or structure frame, at least one structure port in any such cell, energy
 *       ports for FE as wanted; the
 *       controller in the middle of one of the four side walls, facing out</li>
 *   <li>inside, 7x7x7: six gravity constrainers, one against the middle of each wall on the
 *       inside, pointing at the centre (turned to do so when it forms, so they can be placed any
 *       way round); everything else empty, for the plasma</li>
 * </ul>
 * The multiblock lib finds and watches it (definition
 * {@link #ID}); this class only says what it looks like, and does what forming changes in the world.
 */
public final class FusionStructure {
    public static final String ID = AE2GravityTech.MODID + ":fusion_chamber";
    /** The kind of an energy port among the chamber's members. */
    public static final String ENERGY_PORT_KIND = AE2GravityTech.MODID + ":energy_port";
    public static final int SIZE = 9;
    private static final int LAST = SIZE - 1, MID = SIZE / 2;

    private FusionStructure() {
    }

    /** Registers the chamber with the multiblock lib (during mod construction). */
    public static void register() {
        Role<BlockState> controller = Role.of("controller", block(Blocks.FUSION_CONTROLLER));
        Role<BlockState> frame = Role.of("frame", s -> s.getBlock() instanceof StructureFrameBlock ? Match.of(BlockMatchers.id(s)) : null);
        // the walls: glass or frame, a structure port where an ME cable comes in, an energy port where FE does
        Role<BlockState> wall = Role.of("wall", block(Blocks.CONTAINMENT_GLASS, Blocks.STRUCTURE_FRAME, () -> HubStructureBlocks.PORT_BLOCK.get(), Blocks.ENERGY_PORT));
        RoleBlocks.prefer(wall, Blocks.CONTAINMENT_GLASS);
        Role<BlockState> constrainer = Role.of("constrainer", block(Blocks.GRAVITY_CONSTRAINER));
        Role<BlockState> inside = Role.of("inside", BlockMatchers.air());
        Map<Cell, Role<BlockState>> roles = Map.of(Cell.EDGE, frame, Cell.FACE, wall, Cell.CONSTRAINER, constrainer, Cell.INSIDE, inside);
        // the controller is in the middle of a side wall and faces out of it, so where it stands says where the chamber goes
        RoleField.Builder<BlockState> b = RoleField.builder(SIZE, SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    b.set(x, y, z, x == MID && y == MID && z == 0 ? controller : roles.get(cell(x, y, z)));
                }
            }
        }
        Multiblocks.register(Definition.builder(ID, Shape.fixed(b.anchor(MID, MID, 0).build()))
                .facing(new int[]{0, 0, -1}, FusionStructure::frontOf)
                .hatchKinds(HexagramHub.PORT_KIND, ENERGY_PORT_KIND)
                .limit(HexagramHub.PORT_KIND, 1, Integer.MAX_VALUE)
                .build());
    }

    private static int[] frontOf(BlockState state) {
        if (!state.hasProperty(FusionController.FACING))
            return null;
        Direction d = state.getValue(FusionController.FACING);
        return new int[]{d.getStepX(), d.getStepY(), d.getStepZ()};
    }

    @SafeVarargs
    private static Matcher<BlockState> block(Supplier<? extends Block>... blocks) {
        return s -> Arrays.stream(blocks).anyMatch(b -> s.is(b.get())) ? Match.of(BlockMatchers.id(s)) : null;
    }

    private enum Cell { EDGE, FACE, CONSTRAINER, INSIDE }

    private static Cell cell(int x, int y, int z) {
        int onBoundary = (x == 0 || x == LAST ? 1 : 0) + (y == 0 || y == LAST ? 1 : 0) + (z == 0 || z == LAST ? 1 : 0);
        if (onBoundary >= 2) {
            return Cell.EDGE;
        }
        if (onBoundary == 1) {
            return Cell.FACE;
        }
        // inside: the cell just in from the middle of each wall holds a constrainer
        int centred = (x == MID ? 1 : 0) + (y == MID ? 1 : 0) + (z == MID ? 1 : 0);
        boolean nextToWall = x == 1 || x == LAST - 1 || y == 1 || y == LAST - 1 || z == 1 || z == LAST - 1;
        return centred == 2 && nextToWall ? Cell.CONSTRAINER : Cell.INSIDE;
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
                    Cell cell = cell(x, y, z);
                    BlockState next = state;
                    if (cell != Cell.CONSTRAINER && state.getBlock() instanceof StructureFrameBlock) {
                        next = state.setValue(StructureFrameBlock.FORMED, formed);
                    } else if (cell == Cell.CONSTRAINER && formed && state.is(Blocks.GRAVITY_CONSTRAINER.get())) {
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
}
