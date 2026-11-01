package com.lumina.ae2_gvt.blockentities;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 引力子收集器 (on its bottom block): gravitons and anti-gravitons released by crystals growing on
 * holes (or a hole blown up) within {@link #RANGE} blocks go into the ME network it is cabled to, for
 * every collector in range. Only a collector on a powered network catches anything; what the network
 * can't take (cells full) is lost.
 */
public class GravitonCollector extends AENetworkedBlockEntity {
    /** How far from a hole a collector still catches its gravitons (blocks, along each axis). */
    public static final int RANGE = 16;

    private static final Set<GravitonCollector> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<GravitonCollector> SERVER = Collections.newSetFromMap(new WeakHashMap<>());

    public GravitonCollector(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntities.GRAVITON_COLLECTOR.get(), worldPosition, blockState);
        getMainNode().setVisualRepresentation(Blocks.GRAVITON_COLLECTOR.get());
    }

    public static Set<GravitonCollector> loadedOnClient() {
        return CLIENT;
    }

    /**
     * Gravitons released at {@code pos}: every collector on a powered network within {@link #RANGE}
     * catches the whole {@code amount} (a gravitational wave passes through; one collector doesn't use
     * it up), so two collectors catch twice as much.
     */
    public static void release(Level level, BlockPos pos, GravitonKey key, long amount) {
        for (GravitonCollector c : SERVER) {
            BlockPos p = c.getBlockPos();
            if (!c.isRemoved() && c.getLevel() == level && Math.abs(p.getX() - pos.getX()) <= RANGE
                    && Math.abs(p.getY() - pos.getY()) <= RANGE && Math.abs(p.getZ() - pos.getZ()) <= RANGE)
                c.collect(key, amount);
        }
    }

    /** Puts {@code amount} of {@code key} into the network; what doesn't fit is lost. */
    private void collect(GravitonKey key, long amount) {
        var grid = getMainNode().getGrid();
        if (grid != null && getMainNode().isActive())
            grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, IActionSource.ofMachine(this));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null)
            (level.isClientSide() ? CLIENT : SERVER).add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT.remove(this);
        SERVER.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT.remove(this);
        SERVER.remove(this);
    }
}
