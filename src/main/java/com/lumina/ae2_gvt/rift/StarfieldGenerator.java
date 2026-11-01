package com.lumina.ae2_gvt.rift;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * 破碎星空: nothing but broken pieces of sky stone adrift among the stars. One noise makes the large
 * masses, a finer one cracks them apart into shards.
 */
public final class StarfieldGenerator extends ChunkGenerator {
    public static final MapCodec<StarfieldGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(StarfieldGenerator::getBiomeSource)).apply(i, StarfieldGenerator::new));
    private static final ResourceLocation SEED = ResourceLocation.fromNamespaceAndPath("ae2_gvt", "shattered_starfield");
    private static final int HEIGHT = 256, MARGIN = 32;
    /** The masses: noise over this; the cracks: fine noise within this of zero. */
    private static final double SOLID = 0.42, CRACK = 0.07;

    public StarfieldGenerator(BiomeSource biomes) {
        super(biomes);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        RandomSource seeds = random.getOrCreateRandomFactory(SEED).at(0, 0, 0);
        SimplexNoise masses = new SimplexNoise(seeds), cracks = new SimplexNoise(seeds);
        BlockState rock = AEBlocks.SKY_STONE_BLOCK.block().defaultBlockState();
        BlockState smooth = AEBlocks.SMOOTH_SKY_STONE_BLOCK.block().defaultBlockState();
        ChunkPos at = chunk.getPos();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int wx = at.getMinBlockX() + x, wz = at.getMinBlockZ() + z;
                for (int y = 0; y < HEIGHT; y++) {
                    // thins out towards the top and the bottom of the world
                    double edge = Mth.clamp(Math.min(y, HEIGHT - 1 - y) / (double) MARGIN, 0, 1);
                    double mass = masses.getValue(wx / 72.0, y / 44.0, wz / 72.0) + (edge - 1) * 0.6;
                    if (mass < SOLID || Math.abs(cracks.getValue(wx / 15.0, y / 15.0, wz / 15.0)) < CRACK)
                        continue;
                    BlockState state = mass > SOLID + 0.2 ? smooth : rock;
                    chunk.setBlockState(p.set(x, y, z), state, false);
                    ocean.update(x, y, z, state);
                    surface.update(x, y, z, state);
                }
            }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }
}
