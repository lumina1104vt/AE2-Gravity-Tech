package com.lumina.ae2_gvt.blockentities;

import com.lumina.bridge.Face;
import com.lumina.bridge.Pos;
import com.lumina.bridge.Positions;
import com.lumina.twinstar.port.World;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.items.parts.MachineAccessCore;
import com.lumina.ae2_gvt.items.parts.MachineAccessHost;
import com.lumina.twinstar.api.IMachineAccess;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import com.lumina.bridge.key.Key;
import com.lumina.bridge.key.Keys;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 機器接入方塊: the machine access interface as a block of its own. It sits on no machine; every machine
 * is bound to it from afar with the 機器連結器 (up to 64 blocks, same dimension). Everything else is
 * {@link MachineAccessCore}, shared with the cable part.
 */
public class MachineAccessBlockEntity extends AENetworkedBlockEntity implements IMachineAccess, MachineAccessHost, MachineAccessCore.Host,
        appeng.api.upgrades.IUpgradeableObject {
    private final MachineAccessCore core = new MachineAccessCore(this);

    public MachineAccessBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.MACHINE_ACCESS_BLOCK.get(), pos, state);
        getMainNode().setVisualRepresentation(Blocks.MACHINE_ACCESS_BLOCK.get());
        core.setup(getMainNode());
    }

    @Override
    public MachineAccessCore core() {
        return core;
    }

    // ── MachineAccessCore.Host ──

    @Override
    public @Nullable Level level() {
        return getLevel();
    }

    @Override
    public @Nullable BlockPos pos() {
        return getBlockPos();
    }

    @Override
    public @Nullable Direction side() {
        return null;
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component customName() {
        return hasCustomName() ? getCustomName() : null;
    }

    @Override
    public IManagedGridNode node() {
        return getMainNode();
    }

    @Override
    public IActionHost actionHost() {
        return this;
    }

    @Override
    public net.minecraft.world.level.ItemLike machineItem() {
        return Blocks.MACHINE_ACCESS_BLOCK.get();
    }

    /** the crafting card slot: AE2's upgrade system, the same panel as on AE2's own blocks */
    @Override
    public appeng.api.upgrades.IUpgradeInventory getUpgrades() {
        return core.getUpgrades();
    }

    @Override
    public void save() {
        saveChanges();
    }

    @Override
    public void update() {
        markForUpdate();
    }

    @Override
    public void notifyNeighbors() {
        if (level != null)
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    // ── IMachineAccess ──

    @Override public @Nullable Key getMachine() { return core.getMachine(); }
    @Override public boolean isBlocking() { return core.isBlocking(); }
    @Override public BlockMode getBlockMode() { return core.getBlockMode(); }
    @Override public InsertMode getInsertMode() { return core.getInsertMode(); }
    @Override public @Nullable ServerLevel getMachineLevel() { return core.getMachineLevel(); }
    @Override public @Nullable World getMachineWorld() { return core.getMachineWorld(); }
    @Override public Pos getMachinePos() { return core.getMachinePos(); }
    @Override public Face getMachineFace() { return core.getMachineFace(); }
    @Override public int getGroup() { return core.getGroup(); }
    @Override public int getSubRange() { return core.getSubRange(); }
    @Override public void onRunFed() { core.onRunFed(); }
    @Override public Set<Key> getStockedKeys() { return core.getStockedKeys(); }
    @Override public void takeReceived(BiConsumer<Key, Long> taken) { core.takeReceived(taken); }
    @Override public boolean isAccessActive() { return core.isAccessActive(); }
    @Override public List<IMachineAccess> accessPoints() { return core.accessPoints(); }
    @Override public @Nullable com.lumina.bridge.Place interfacePlace() { return core.interfacePlace(); }
    @Override public int interfaceSide() { return core.interfaceSide(); }
    @Override public @Nullable net.minecraft.network.chat.Component interfaceName() { return core.interfaceName(); }
    @Override public List<com.lumina.bridge.Place> machinePlaces() { return core.machinePlaces(); }

    // ── saving, the client ──

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        core.write(tag, registries);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        core.read(tag, registries);
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        core.writeStream(data);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        core.readStream(data);
        return changed || true;
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        core.drops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        core.clear();
    }
}
