package com.lumina.ae2_gvt.prototype.frame.client;

import java.util.EnumMap;
import java.util.List;

import org.joml.Vector3f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.frame.StructureFrameBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Formed structure frame, connected 8 ways. Every face is drawn as four quarters; each quarter
 * looks at its two sides and the diagonal between them (in the face's own up / right / down / left
 * - the same orientation AE2 uses for its connected quartz glass frame) and takes that quarter
 * from one of these tiles:
 * <ul>
 *   <li>both sides open: the outer corner (tile N with both border bits)</li>
 *   <li>one side open: that straight edge</li>
 *   <li>both sides continue but the diagonal does not: the inside corner (tile 16)</li>
 *   <li>all continue: plain (tile 0)</li>
 * </ul>
 * Border bits: up 1, right 2, down 4, left 8. The tiles share one sheet (structure_frame_formed,
 * tools/ctm_sheet.py), each on the 16px grid so mipmapping never mixes neighbouring tiles. Faces
 * between two frames are culled as usual.
 */
public final class StructureFrameModel implements DynamicBlockStateModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "structure_frame_formed");
    private static final int INNER = 16;
    /** Grain variants of every tile (tools/frame_textures.py); each block picks one by position. */
    private static final int VARIANTS = 4;
    /** All tiles in one sheet (tools/ctm_sheet.py): column = tile, row = variant, 16px cells. */
    private static final Material SHEET = new Material(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "block/structure_frame_formed"));
    private static final int TILES = 17;
    private static final EnumMap<Direction, List<Vector3f>> CORNERS = faceCorners();

    private final Material.Baked sheet;

    private StructureFrameModel(Material.Baked sheet) {
        this.sheet = sheet;
    }

    /** Which grain variant a block uses: fixed per position, so neighbours rarely share one. */
    private static int variant(BlockPos pos) {
        long h = pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY();
        h = h * h * 42317861L + h * 11L;
        return (int) Math.floorMod(h >> 16, (long) VARIANTS);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        QuadCollection.Builder quads = new QuadCollection.Builder();
        int row = variant(pos);
        for (Direction side : Direction.values()) {
            Direction[] dirs = faceDirections(side);   // up, right, down, left
            Direction up = dirs[0], right = dirs[1], down = dirs[2], left = dirs[3];
            List<Vector3f> c = CORNERS.get(side);
            // quarters: (u0, v0) of the quarter, its horizontal and vertical neighbours, border bits
            quarter(quads, row, side, c, level, pos, 0f, 0f, left, up, 8, 1);
            quarter(quads, row, side, c, level, pos, .5f, 0f, right, up, 2, 1);
            quarter(quads, row, side, c, level, pos, 0f, .5f, left, down, 8, 4);
            quarter(quads, row, side, c, level, pos, .5f, .5f, right, down, 2, 4);
        }
        parts.add(new SimpleModelWrapper(quads.build(), true, sheet));
    }

    private void quarter(QuadCollection.Builder quads, int row, Direction side, List<Vector3f> c, BlockAndTintGetter level, BlockPos pos,
                         float u0, float v0, Direction sideways, Direction vertical, int sidewaysBit, int verticalBit) {
        boolean s = connects(level, pos.relative(sideways), side);
        boolean v = connects(level, pos.relative(vertical), side);
        int tile;
        if (!s || !v) {
            tile = (s ? 0 : sidewaysBit) | (v ? 0 : verticalBit);   // outer corner or straight edge
        } else if (!connects(level, pos.relative(sideways).relative(vertical), side)) {
            tile = INNER;                                            // inside corner
        } else {
            tile = 0;
        }
        quads.addCulledFace(side, quad(side, c, tile, row, u0, v0, u0 + .5f, v0 + .5f));
    }

    @Override
    public Material.Baked particleMaterial() {
        return sheet;
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    /**
     * The face continues onto the block at {@code pos}: it is a formed frame and its own face on the
     * same side is showing (not buried under another frame, as on a stepped surface).
     */
    private static boolean connects(BlockAndTintGetter level, BlockPos pos, Direction side) {
        return StructureFrameBlock.isFormedFrame(level.getBlockState(pos))
                && !StructureFrameBlock.isFormedFrame(level.getBlockState(pos.relative(side)));
    }

    /** The face's own up / right / down / left, matching how its texture is laid on (see faceCorners). */
    private static Direction[] faceDirections(Direction side) {
        return switch (side) {
            case DOWN -> new Direction[] {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
            case UP -> new Direction[] {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
            case NORTH -> new Direction[] {Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST};
            case SOUTH -> new Direction[] {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};
            case WEST -> new Direction[] {Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
            case EAST -> new Direction[] {Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH};
        };
    }

    /** Part of a face: texture-space rectangle (u0, v0)-(u1, v1), positioned on the face to match. */
    private BakedQuad quad(Direction side, List<Vector3f> c, int tile, int row, float u0, float v0, float u1, float v1) {
        Vec3 n = side.getUnitVec3();
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sheet);
        builder.setDirection(side);
        vertex(builder, n, c, tile, row, u0, v0);
        vertex(builder, n, c, tile, row, u0, v1);
        vertex(builder, n, c, tile, row, u1, v1);
        vertex(builder, n, c, tile, row, u1, v0);
        return builder.bakeQuad();
    }

    // corner 0 is texture (0,0), corner 1 is (0,1), corner 3 is (1,0): the face is a parallelogram on those
    private void vertex(QuadBakingVertexConsumer builder, Vec3 n, List<Vector3f> c, int tile, int row, float u, float v) {
        Vector3f p = new Vector3f(c.get(0))
                .add(new Vector3f(c.get(3)).sub(c.get(0)).mul(u))
                .add(new Vector3f(c.get(1)).sub(c.get(0)).mul(v));
        builder.addVertex(p.x(), p.y(), p.z());
        builder.setColor(1f, 1f, 1f, 1f);
        builder.setNormal((float) n.x, (float) n.y, (float) n.z);
        builder.setUv(sheet.sprite().getU((tile + u) / TILES), sheet.sprite().getV((row + v) / VARIANTS));
    }

    // the same corner order AE2's RenderHelper uses, so the edge bits above line up with the texture
    private static EnumMap<Direction, List<Vector3f>> faceCorners() {
        EnumMap<Direction, List<Vector3f>> result = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            float o = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0f : 1f;
            List<Vector3f> corners = switch (facing.getAxis()) {
                case X -> Lists.newArrayList(new Vector3f(o, 1, 1), new Vector3f(o, 0, 1), new Vector3f(o, 0, 0), new Vector3f(o, 1, 0));
                case Y -> Lists.newArrayList(new Vector3f(1, o, 1), new Vector3f(1, o, 0), new Vector3f(0, o, 0), new Vector3f(0, o, 1));
                case Z -> Lists.newArrayList(new Vector3f(0, 1, o), new Vector3f(0, 0, o), new Vector3f(1, 0, o), new Vector3f(1, 1, o));
            };
            if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                corners = Lists.reverse(corners);
            }
            result.put(facing, ImmutableList.copyOf(corners));
        }
        return result;
    }

    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            ModelDebugName name = StructureFrameModel.class::toString;
            return new StructureFrameModel(baker.materials().get(SHEET, name));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }

        @Override
        public MapCodec<Unbaked> codec() {
            return MAP_CODEC;
        }
    }
}
