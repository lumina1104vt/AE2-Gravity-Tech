package com.lumina.ae2_gvt.hole;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredRegister;

/**
 * Natural black holes, placed like AE2's meteorites (same spread and biomes: see the structure set
 * {@code ae2_gvt:black_hole}): a black hole that fell from the sky, floating in the middle of an
 * empty spherical hollow - it ate the rock it hit. Three kinds as the meteorites: a crater open to the
 * sky, half buried (a smaller opening), or fully buried. The 引力波探測器 points to them through the
 * structure tag {@link #TAG}.
 */
public final class BlackHoleCrater extends Structure {
    public static final com.mojang.serialization.Codec<BlackHoleCrater> CODEC = simpleCodec(BlackHoleCrater::new);
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE,
            new ResourceLocation(AE2GravityTech.MODID, "black_holes"));
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE,
            new ResourceLocation(AE2GravityTech.MODID, "black_hole"));

    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, AE2GravityTech.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<BlackHoleCrater>> TYPE =
            TYPES.register("black_hole", () -> () -> CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
            PIECES.register("black_hole", () -> (StructurePieceType) (context, tag) -> new Piece(tag));

    /** crater open to the sky, half buried, buried */
    private static final int CRATER = 0, HALF = 1, BURIED = 2;

    public BlackHoleCrater(StructureSettings settings) {
        super(settings);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        RandomSource random = context.random();
        int radius = 4 + random.nextInt(5);     // the hollow, 4 … 8
        int kind = random.nextInt(3);
        int centre = switch (kind) {
            case CRATER -> surface - radius + 2;
            case HALF -> surface - radius - 1;
            default -> surface - radius * 2 - random.nextInt(12);
        };
        int floor = context.heightAccessor().getMinBuildHeight() + radius + 6;   // clear of the bedrock
        if (centre < floor)
            centre = floor;
        BlockPos at = new BlockPos(x, centre, z);
        int mode = kind;
        return Optional.of(new GenerationStub(at, pieces -> pieces.addPiece(new Piece(at, radius, mode, surface))));
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }

    /** The hollow, the crater over it and the hole. */
    public static final class Piece extends StructurePiece {
        private final BlockPos centre;
        private final int radius, kind, surface;

        Piece(BlockPos centre, int radius, int kind, int surface) {
            super(PIECE.get(), 0, box(centre, radius, kind, surface));
            this.centre = centre;
            this.radius = radius;
            this.kind = kind;
            this.surface = surface;
        }

        Piece(CompoundTag tag) {
            super(PIECE.get(), tag);
            this.centre = NbtUtils.readBlockPos(tag.getCompound("centre"));
            this.radius = tag.getInt("radius");
            this.kind = tag.getInt("kind");
            this.surface = tag.getInt("surface");
        }

        /** Where the black hole is. */
        public BlockPos centre() {
            return centre;
        }

        /** How far out the crater reaches at the surface. */
        private static int craterRadius(int radius, int kind) {
            return kind == CRATER ? radius * 2 : kind == HALF ? radius + 2 : 0;
        }

        private static BoundingBox box(BlockPos centre, int radius, int kind, int surface) {
            int out = Math.max(radius, craterRadius(radius, kind)) + 1;
            int top = kind == BURIED ? centre.getY() + radius + 1 : Math.max(centre.getY() + radius, surface) + 16;
            return new BoundingBox(centre.getX() - out, centre.getY() - radius - 1, centre.getZ() - out,
                    centre.getX() + out, top, centre.getZ() + out);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.put("centre", NbtUtils.writeBlockPos(centre));
            tag.putInt("radius", radius);
            tag.putInt("kind", kind);
            tag.putInt("surface", surface);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int crater = craterRadius(radius, kind);
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
                for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
                    int dx = x - centre.getX(), dz = z - centre.getZ();
                    double h = Math.sqrt(dx * dx + dz * dz);
                    // the crater's bowl: down to the hole's height in the middle, up to the surface at its rim
                    double bowl = h < crater ? centre.getY() + (surface - centre.getY()) * Mth.square(h / crater) : Double.MAX_VALUE;
                    for (int y = boundingBox.minY(); y <= boundingBox.maxY(); y++) {
                        int dy = y - centre.getY();
                        boolean hollow = dx * dx + dy * dy + dz * dz <= radius * radius;
                        if (!hollow && y < bowl)
                            continue;
                        p.set(x, y, z);
                        BlockState there = level.getBlockState(p);
                        if (!there.isAir() && !there.is(Blocks.BEDROCK))
                            level.setBlock(p, air, 2);
                    }
                }
            }
            if (chunkBox.isInside(centre))
                level.setBlock(centre, GravityHole.BLACK_HOLE.get().defaultBlockState(), 2);
        }
    }
}
