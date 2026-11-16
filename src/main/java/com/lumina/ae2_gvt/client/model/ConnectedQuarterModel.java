package com.lumina.ae2_gvt.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * A block connected 8 ways: every face is drawn as four quarters; each quarter looks at its two
 * sides and the diagonal between them (in the face's own up / right / down / left - the same
 * orientation AE2 uses for its connected quartz glass frame) and takes that quarter from one of
 * these tiles:
 * <ul>
 *   <li>both sides open: the outer corner (tile N with both border bits)</li>
 *   <li>one side open: that straight edge</li>
 *   <li>both sides continue but the diagonal does not: the inside corner (tile 16)</li>
 *   <li>all continue: plain (tile 0)</li>
 * </ul>
 * Border bits: up 1, right 2, down 4, left 8. The tiles share one sheet (tools/ctm_sheet.py):
 * column = variant, row = tile, each on the 16px grid so mipmapping never mixes neighbouring tiles
 * (tall, because 1.21.1's stitcher can silently drop a wide sprite).
 * Each block picks a variant by position. The neighbours are read when the chunk is built
 * ({@link #getModelData}); without them (an item) every quarter is open.
 */
abstract class ConnectedQuarterModel implements IDynamicBakedModel {
    private static final int INNER = 16, TILES = 17;
    /** [0] = variant row, then 4 tiles per face in Direction order. */
    private static final ModelProperty<int[]> LAYOUT = new ModelProperty<>();
    private static final EnumMap<Direction, List<Vector3f>> CORNERS = faceCorners();

    private final TextureAtlasSprite sheet;
    private final ChunkRenderTypeSet renderTypes;
    /** Columns in the sheet (grain variants); 1 for an animated sheet such as the joined crystals. */
    private final int variants;

    ConnectedQuarterModel(TextureAtlasSprite sheet, RenderType renderType) {
        this(sheet, renderType, 4);
    }

    ConnectedQuarterModel(TextureAtlasSprite sheet, RenderType renderType, int variants) {
        this.sheet = sheet;
        this.renderTypes = ChunkRenderTypeSet.of(renderType);
        this.variants = variants;
    }

    /** The face on {@code side} continues onto the block at {@code pos}. */
    protected abstract boolean connects(BlockAndTintGetter level, BlockPos pos, Direction side);

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        int[] layout = new int[1 + 6 * 4];
        layout[0] = variant(pos);
        for (Direction side : Direction.values()) {
            Direction[] dirs = faceDirections(side);   // up, right, down, left
            Direction up = dirs[0], right = dirs[1], down = dirs[2], left = dirs[3];
            int at = 1 + side.ordinal() * 4;
            layout[at] = tile(level, pos, side, left, up, 8, 1);
            layout[at + 1] = tile(level, pos, side, right, up, 2, 1);
            layout[at + 2] = tile(level, pos, side, left, down, 8, 4);
            layout[at + 3] = tile(level, pos, side, right, down, 2, 4);
        }
        return ModelData.builder().with(LAYOUT, layout).build();
    }

    private int tile(BlockAndTintGetter level, BlockPos pos, Direction side, Direction sideways, Direction vertical, int sidewaysBit,
                     int verticalBit) {
        boolean s = connects(level, pos.relative(sideways), side);
        boolean v = connects(level, pos.relative(vertical), side);
        if (!s || !v) {
            return (s ? 0 : sidewaysBit) | (v ? 0 : verticalBit);   // outer corner or straight edge
        }
        return connects(level, pos.relative(sideways).relative(vertical), side) ? 0 : INNER;   // plain or inside corner
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                                    @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();   // every quad belongs to a face and is culled with it
        }
        int[] layout = data.get(LAYOUT);
        int row = layout == null ? 0 : layout[0];
        int at = 1 + side.ordinal() * 4;
        int[] open = {9, 3, 12, 6};   // the four outer corners, for an item
        List<Vector3f> c = CORNERS.get(side);
        // four quarters of the same tile make the whole tile: one quad instead of four (most faces of a big structure are plain),
        // a quarter of the vertices to build, keep and draw
        if (layout != null && layout[at] == layout[at + 1] && layout[at] == layout[at + 2] && layout[at] == layout[at + 3])
            return List.of(quad(side, c, layout[at], row, 0, 0, 1, 1));
        List<BakedQuad> quads = new ArrayList<>(4);
        float[][] quarters = {{0, 0}, {.5f, 0}, {0, .5f}, {.5f, .5f}};
        for (int q = 0; q < 4; q++) {
            int tile = layout == null ? open[q] : layout[at + q];
            float u0 = quarters[q][0], v0 = quarters[q][1];
            quads.add(quad(side, c, tile, row, u0, v0, u0 + .5f, v0 + .5f));
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return renderTypes;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return sheet;
    }

    /** Still abstract in BakedModel, though NeoForge asks for the ModelData version above. */
    @Override
    @Deprecated
    public TextureAtlasSprite getParticleIcon() {
        return sheet;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /** Which variant a block uses: fixed per position, so neighbours rarely share one. */
    private int variant(BlockPos pos) {
        long h = pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY();
        h = h * h * 42317861L + h * 11L;
        return (int) Math.floorMod(h >> 16, (long) variants);
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
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sheet);
        builder.setDirection(side);
        builder.setShade(true);
        vertex(builder, side, c, tile, row, u0, v0);
        vertex(builder, side, c, tile, row, u0, v1);
        vertex(builder, side, c, tile, row, u1, v1);
        vertex(builder, side, c, tile, row, u1, v0);
        return builder.bakeQuad();
    }

    // corner 0 is texture (0,0), corner 1 is (0,1), corner 3 is (1,0): the face is a parallelogram on those
    private void vertex(QuadBakingVertexConsumer builder, Direction side, List<Vector3f> c, int tile, int row, float u, float v) {
        Vector3f p = new Vector3f(c.get(0))
                .add(new Vector3f(c.get(3)).sub(c.get(0)).mul(u))
                .add(new Vector3f(c.get(1)).sub(c.get(0)).mul(v));
        builder.addVertex(p.x(), p.y(), p.z());
        builder.setColor(255, 255, 255, 255);
        builder.setUv(sheet.getU((row + u) / variants), sheet.getV((tile + v) / TILES));
        builder.setUv2(0, 0);
        builder.setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
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
}
