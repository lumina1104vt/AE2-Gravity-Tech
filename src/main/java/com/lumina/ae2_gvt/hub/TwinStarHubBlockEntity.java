package com.lumina.ae2_gvt.hub;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.cpu.TwinStarHostBlockEntity;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.core.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The hexagram hub's controller: the twin-star engine's host. One black-hole slot (crafting units:
 * the capacity) and one white-hole slot (computing units: the parallel count), each one stack of one
 * tier, up to 64. Takes over crafting on its network while the hub is formed and both slots hold
 * units; otherwise AE2's own CPUs do it.
 * <p>
 * Its grid node is not reachable by cable: ME cables join the hub at its ports on the outer circle,
 * and the controller links its node to each port's while the hub is formed (looked at once a
 * second). The client also draws the formed hub's sphere from this block entity.
 */
public class TwinStarHubBlockEntity extends TwinStarHostBlockEntity {
    public static final int BLACK_SLOT = 0, WHITE_SLOT = 1;
    private static final int CHECK_EVERY_TICKS = 20;

    private final AppEngInternalInventory units = new AppEngInternalInventory(this, 2, 64);
    private boolean formed;
    private final Map<BlockPos, IGridConnection> links = new HashMap<>();
    private int ticks;

    public TwinStarHubBlockEntity(BlockPos pos, BlockState state) {
        super(HubStructureBlocks.CONTROLLER_ENTITY.get(), pos, state);
        getMainNode().setExposedOnSides(EnumSet.noneOf(Direction.class));
        getMainNode().setVisualRepresentation(HubStructureBlocks.CONTROLLER.get());
        units.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return slot == BLACK_SLOT ? HubUnits.craftingTier(stack) >= 0 : HubUnits.computingTier(stack) >= 0;
            }
        });
    }

    public InternalInventory getUnits() {
        return units;
    }

    /** Formed, as last looked at (server). */
    public boolean isFormed() {
        return formed;
    }

    /** Ports linked now (server). */
    public int linkedPorts() {
        return links.size();
    }

    // ── engine host ──

    @Override
    public boolean isHostActive() {
        return formed && getMainNode().isActive() && !units.getStackInSlot(BLACK_SLOT).isEmpty()
                && !units.getStackInSlot(WHITE_SLOT).isEmpty();
    }

    @Override
    public @Nullable BigInteger getHostCapacity() {
        ItemStack s = units.getStackInSlot(BLACK_SLOT);
        int tier = HubUnits.craftingTier(s);
        return tier < 0 ? BigInteger.ZERO : HubUnits.capacity(tier, s.getCount());
    }

    @Override
    public @Nullable BigInteger getHostParallelism() {
        ItemStack s = units.getStackInSlot(WHITE_SLOT);
        int tier = HubUnits.computingTier(s);
        return tier < 0 ? BigInteger.ZERO : HubUnits.parallel(tier, s.getCount());
    }

    // ── structure and ports ──

    public static void serverTick(Level level, BlockPos pos, BlockState state, TwinStarHubBlockEntity hub) {
        if (++hub.ticks % CHECK_EVERY_TICKS == 0 && level instanceof ServerLevel server)
            hub.check(server);
    }

    /** Formed or not, and a link to each port of the formed hub (none otherwise). */
    private void check(ServerLevel level) {
        Instance<BlockState> hub = Multiblocks.at(level, getBlockPos());
        boolean nowFormed = hub != null && hub.isFormed();
        if (nowFormed != formed) {
            formed = nowFormed;
            AE2GravityTech.LOGGER.debug("twin-star hub at {}: {}", getBlockPos(), formed ? "formed" : "not formed");
        }
        Set<BlockPos> ports = new HashSet<>();
        if (formed)
            for (long p : hub.members(HexagramHub.PORT_KIND))
                ports.add(BlockPos.of(p));
        // links whose port left the structure, or that AE2 dropped (a port unloaded or broken)
        for (Iterator<Map.Entry<BlockPos, IGridConnection>> it = links.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, IGridConnection> e = it.next();
            IGridConnection link = e.getValue();
            boolean alive = link.a().getConnections().contains(link);
            if (!ports.contains(e.getKey()) || !alive) {
                if (alive)
                    link.destroy();
                it.remove();
            }
        }
        IGridNode own = getMainNode().getNode();
        if (own == null)
            return;
        for (BlockPos p : ports) {
            if (links.containsKey(p) || !level.isLoaded(p) || !(level.getBlockEntity(p) instanceof HubPortBlockEntity port))
                continue;
            IGridNode other = port.getMainNode().getNode();
            if (other == null)
                continue;
            try {
                links.put(p, GridHelper.createConnection(own, other));
            } catch (RuntimeException e) {
                // already joined (another link between the same nodes): nothing to add
                AE2GravityTech.LOGGER.debug("twin-star hub at {}: no link to port {}: {}", getBlockPos(), p, e.getMessage());
            }
        }
    }

    @Override
    public void setRemoved() {
        for (IGridConnection link : links.values())
            if (link.a().getConnections().contains(link))
                link.destroy();
        links.clear();
        super.setRemoved();
    }

    /** The units: only saved (the base class packs the pattern slots on their changes). */
    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == units)
            saveChanges();
        else
            super.onChangeInventory(inv, slot);
    }

    // ── saving and drops ──

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        units.writeToNBT(data, "units", registries);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        units.readFromNBT(data, "units", registries);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int i = 0; i < units.size(); i++)
            if (!units.getStackInSlot(i).isEmpty())
                drops.add(units.getStackInSlot(i));
    }

    @Override
    public void clearContent() {
        super.clearContent();
        units.clear();
    }
}
