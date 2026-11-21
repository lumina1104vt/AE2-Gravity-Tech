package com.lumina.ae2_gvt.items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import com.lumina.ae2_gvt.hole.BlackHoleCrater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

/**
 * 引力波探測器: like AE2's meteorite compass, its pointer turns to the nearest natural black hole
 * ({@link BlackHoleCrater}) within {@link #SEARCH_CHUNKS} chunks, and spins when there is none. Held
 * in a hand, it looks again every 2 seconds; the target is kept on the stack as a lodestone tracker
 * (the client draws the pointer from it).
 * <p>
 * Not vanilla's locate: that returns the first start it scans in the nearest ring of placement cells,
 * not the nearest, so the target flips as the player crosses cells. Here every cell in range is
 * checked and the truly nearest hole wins; what each cell holds is remembered per level.
 */
public class GravityWaveDetectorItem extends Item {
    /** How far it looks, in chunks (about 1000 blocks). */
    public static final int SEARCH_CHUNKS = 64;

    /** per level: chunk (as long) of a placement cell → its black hole, or null when it has none */
    private static final Map<ServerLevel, Map<Long, BlockPos>> HOLES = Collections.synchronizedMap(new WeakHashMap<>());

    public GravityWaveDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || entity.tickCount % 40 != 0)
            return;
        if (!selected && player.getOffhandItem() != stack)
            return;
        BlockPos found = nearest(server, entity.blockPosition());
        GlobalPos target = found == null ? null : GlobalPos.of(level.dimension(), found);
        GlobalPos now = target(stack);
        if (target == null ? now != null : !target.equals(now))
            setTarget(stack, target);
    }

    /** Where the detector points (kept in the stack as a compass keeps its lodestone: 1.20.1 has no tracker component). */
    public static @Nullable GlobalPos target(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains("LodestonePos") || !tag.contains("LodestoneDimension"))
            return null;
        var dimension = net.minecraft.world.level.Level.RESOURCE_KEY_CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("LodestoneDimension")).result();
        return dimension.map(d -> GlobalPos.of(d, net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("LodestonePos")))).orElse(null);
    }

    public static void setTarget(ItemStack stack, @Nullable GlobalPos target) {
        if (target == null) {
            var tag = stack.getTag();
            if (tag != null) {
                tag.remove("LodestonePos");
                tag.remove("LodestoneDimension");
                if (tag.isEmpty())
                    stack.setTag(null);
            }
            return;
        }
        var tag = stack.getOrCreateTag();
        tag.put("LodestonePos", net.minecraft.nbt.NbtUtils.writeBlockPos(target.pos()));
        net.minecraft.world.level.Level.RESOURCE_KEY_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, target.dimension())
                .result().ifPresent(t -> tag.put("LodestoneDimension", t));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.lumina.ae2_gvt.client.WaveDetectorClient.renderer();
            }
        });
    }

    /** The nearest natural black hole within {@link #SEARCH_CHUNKS} chunks of {@code from}, or null. */
    private static @Nullable BlockPos nearest(ServerLevel level, BlockPos from) {
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(BlackHoleCrater.KEY).orElse(null);
        if (structure == null)
            return null;
        var state = level.getChunkSource().getGeneratorState();
        Map<Long, BlockPos> known = HOLES.computeIfAbsent(level, l -> new HashMap<>());
        long range = (long) SEARCH_CHUNKS * 16;
        BlockPos best = null;
        long bestDistance = range * range;
        for (var placement : state.getPlacementsForStructure(structure)) {
            if (!(placement instanceof RandomSpreadStructurePlacement spread))
                continue;
            int spacing = spread.spacing();
            int cellX = Math.floorDiv(from.getX() >> 4, spacing), cellZ = Math.floorDiv(from.getZ() >> 4, spacing);
            int cells = SEARCH_CHUNKS / spacing + 1;
            for (int x = cellX - cells; x <= cellX + cells; x++) {
                for (int z = cellZ - cells; z <= cellZ + cells; z++) {
                    ChunkPos chunk = spread.getPotentialStructureChunk(state.getLevelSeed(), x * spacing, z * spacing);
                    BlockPos hole;
                    if (known.containsKey(chunk.toLong())) {
                        hole = known.get(chunk.toLong());
                    } else {
                        hole = holeIn(level, chunk, structure.value(), spread);
                        known.put(chunk.toLong(), hole);
                    }
                    if (hole == null)
                        continue;
                    long dx = hole.getX() - from.getX(), dz = hole.getZ() - from.getZ();
                    if (dx * dx + dz * dz < bestDistance) {
                        bestDistance = dx * dx + dz * dz;
                        best = hole;
                    }
                }
            }
        }
        return best;
    }

    /** The black hole that starts in {@code chunk}, if one does (loads the chunk's structure starts if need be). */
    private static @Nullable BlockPos holeIn(ServerLevel level, ChunkPos chunk,
                                             net.minecraft.world.level.levelgen.structure.Structure structure,
                                             RandomSpreadStructurePlacement placement) {
        var structures = level.structureManager();
        if (!placement.isStructureChunk(level.getChunkSource().getGeneratorState(), chunk.x, chunk.z))
            return null;
        if (structures.checkStructurePresence(chunk, structure, false) == StructureCheckResult.START_NOT_PRESENT)
            return null;
        var access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = structures.getStartForStructure(SectionPos.bottomOf(access), structure, access);
        if (start == null || !start.isValid() || start.getPieces().isEmpty()
                || !(start.getPieces().get(0) instanceof BlackHoleCrater.Piece piece))
            return null;
        return piece.centre();
    }

    /** No enchantment glint from the lodestone tracker. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
