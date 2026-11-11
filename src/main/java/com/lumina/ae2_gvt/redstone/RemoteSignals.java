package com.lumina.ae2_gvt.redstone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The redstone pulse of a machine access interface at a machine bound from afar, with no block: the machine's position is kept
 * here, and every redstone question asked about one of its sides (a mixin on the block state) is answered "15". The machine
 * is told its neighbours changed when the pulse starts and ends, as it would be by a real signal. A pulse ends by itself after a
 * while, so one whose interface unloaded does not stay on.
 */
public final class RemoteSignals {
    private static final long MAX_TICKS = 100;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> ACTIVE = new ConcurrentHashMap<>();
    private static volatile boolean any;

    private RemoteSignals() {
    }

    /** Starts or ends the pulse at {@code machine}. */
    public static void set(ServerLevel level, BlockPos machine, boolean on) {
        BlockPos at = machine.immutable();
        if (on) {
            ACTIVE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(at, level.getGameTime() + MAX_TICKS);
            any = true;
        } else {
            var map = ACTIVE.get(level.dimension());
            if (map == null || map.remove(at) == null)
                return;
            if (map.isEmpty())
                ACTIVE.remove(level.dimension());
            any = !ACTIVE.isEmpty();
        }
        if (level.isLoaded(at))
            level.neighborChanged(at, Blocks.REDSTONE_BLOCK, at.above());
    }

    /** Whether the machine at {@code machine} is being pulsed (asked by the redstone queries, many times a tick: cheap when nothing is). */
    public static boolean powers(net.minecraft.world.level.BlockGetter getter, BlockPos machine) {
        if (!any || !(getter instanceof Level level) || level.isClientSide())
            return false;
        var map = ACTIVE.get(level.dimension());
        if (map == null)
            return false;
        Long until = map.get(machine);
        if (until == null)
            return false;
        if (level.getGameTime() > until) {
            map.remove(machine);
            return false;
        }
        return true;
    }
}
