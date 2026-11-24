package com.lumina.ae2_gvt.items.parts;

import com.lumina.bridge.Face;
import com.lumina.bridge.Pos;
import com.lumina.bridge.Positions;
import com.lumina.twinstar.port.World;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import com.lumina.ae2_gvt.menus.Menus;
import com.lumina.twinstar.api.IMachineAccess;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import com.lumina.bridge.key.Key;
import com.lumina.bridge.key.Keys;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Machine access interface: put on a machine, it lets the hub run that machine's synthesis patterns
 * on it. All of its settings and work are {@link MachineAccessCore}'s (shared with the wireless block);
 * this is the cable part around it: its look, its box, its screen, and the grid node.
 */
public class MachineAccessInterface extends AEBasePart implements IMachineAccess, MachineAccessHost, MachineAccessCore.Host,
        appeng.api.upgrades.IUpgradeableObject {
    private final MachineAccessCore core = new MachineAccessCore(this);

    public MachineAccessInterface(IPartItem<?> partItem) {
        super(partItem);
        core.setup(getMainNode());
    }

    @Override
    public MachineAccessCore core() {
        return core;
    }

    private static final ResourceLocation MODEL_BASE =
            new ResourceLocation(AE2GravityTech.MODID, "part/machine_access_interface_base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, new ResourceLocation("ae2", "part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, new ResourceLocation("ae2", "part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, new ResourceLocation("ae2", "part/interface_has_channel"));

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    public void getBoxes(IPartCollisionHelper iPartCollisionHelper) {
        iPartCollisionHelper.addBox(2, 2, 14, 14, 14, 16);
        iPartCollisionHelper.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 4;
    }

    // ── the screen ──

    @Override
    public boolean onPartActivate(Player player, net.minecraft.world.InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(Menus.MACHINE_ACCESS.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    /** The 機器連結器, sneaking: pick this interface as the one the linker sets up (again: let go of it). */
    @Override
    public boolean onPartShiftActivate(Player player, net.minecraft.world.InteractionHand hand, Vec3 pos) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MachineLinkTool))
            return false;
        if (isClientSide())
            return true;
        var host = getBlockEntity();
        if (host == null || getLevel() == null)
            return true;
        MachineLinkTool.pick(stack, player, new MachineLinkTool.Link(getLevel().dimension(), host.getBlockPos(), getSide()));
        return true;
    }

    // ── MachineAccessCore.Host ──

    @Override
    public @Nullable Level level() {
        return getLevel();
    }

    @Override
    public @Nullable BlockPos pos() {
        var host = getBlockEntity();
        return host == null ? null : host.getBlockPos();
    }

    @Override
    public @Nullable Direction side() {
        return getSide();
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
        return getPartItem().asItem();
    }

    /** the crafting card slot: AE2's upgrade system, the same panel as on AE2's own parts */
    @Override
    public appeng.api.upgrades.IUpgradeInventory getUpgrades() {
        return core.getUpgrades();
    }

    @Override
    public void save() {
        if (getHost() != null)
            getHost().markForSave();
    }

    @Override
    public void update() {
        if (getHost() != null)
            getHost().markForUpdate();
    }

    @Override
    public void notifyNeighbors() {
        getHost().notifyNeighbors();
    }

    // ── IMachineAccess: the node's owner is this part; the core does it all ──

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

    @Override
    public boolean canConnectRedstone() {
        return core.canConnectRedstone();
    }

    @Override
    public int isProvidingStrongPower() {
        return core.isProvidingStrongPower();
    }

    @Override
    public int isProvidingWeakPower() {
        return core.isProvidingWeakPower();
    }

    // ── to the client, saving ──

    @Override
    public void writeToStream(net.minecraft.network.FriendlyByteBuf data) {
        super.writeToStream(data);
        core.writeStream(data);
    }

    @Override
    public boolean readFromStream(net.minecraft.network.FriendlyByteBuf data) {
        super.readFromStream(data);
        core.readStream(data);
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        core.read(tag);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        core.write(tag);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        core.drops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        core.clear();
    }

    /** What a machine pushes out into the interface goes in as into an ME interface. */
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
            return net.minecraftforge.common.util.LazyOptional.of(() -> core().getReceivedItems()).cast();
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)
            return net.minecraftforge.common.util.LazyOptional.of(() -> core().getReceivedFluids()).cast();
        return super.getCapability(cap);
    }
}
