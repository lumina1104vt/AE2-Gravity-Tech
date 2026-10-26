package com.lumina.ae2_gvt.items.parts;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.menus.Menus;
import com.lumina.twinstar.api.IMachineAccess;

import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Machine access interface: put on a machine, it lets the hub run that machine's synthesis patterns
 * on it ({@link IMachineAccess}; the hub does the work through the machine's capabilities). Set in
 * its screen:
 * <ul>
 *   <li>a machine it also stands for, besides the block it is on (a ghost slot, empty by default:
 *   for a multiblock's port or another tier of the same machine);</li>
 *   <li>blocking: another pattern's materials wait until the previous one's are gone (default off);</li>
 *   <li>how materials go in: through its own face, the faces the pattern gives, or no face.</li>
 * </ul>
 */
public class MachineAccessInterface extends AEBasePart implements IMachineAccess, InternalInventoryHost {
    private final AppEngInternalInventory machineInv = new AppEngInternalInventory(this, 1, 1);
    private boolean blocking;
    private InsertMode insertMode = InsertMode.DEFAULT;

    public MachineAccessInterface(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setFlags().setIdlePowerUsage(10);
    }

    private static final ResourceLocation MODEL_BASE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "part/machine_access_interface_base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_has_channel"));

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
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(Menus.MACHINE_ACCESS.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    public InternalInventory getMachineInv() {
        return machineInv;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
        getHost().markForSave();
    }

    public void setInsertMode(InsertMode insertMode) {
        this.insertMode = insertMode;
        getHost().markForSave();
    }

    // ── IMachineAccess ──

    @Override
    public @Nullable Item getMachine() {
        ItemStack s = machineInv.getStackInSlot(0);
        return s.isEmpty() ? null : s.getItem();
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public InsertMode getInsertMode() {
        return insertMode;
    }

    @Override
    public @Nullable ServerLevel getMachineLevel() {
        return getLevel() instanceof ServerLevel level ? level : null;
    }

    @Override
    public BlockPos getMachinePos() {
        var host = getBlockEntity();
        return host == null ? BlockPos.ZERO : host.getBlockPos().relative(getSide());
    }

    @Override
    public Direction getMachineFace() {
        return getSide().getOpposite();
    }

    @Override
    public boolean isAccessActive() {
        return getMainNode().isActive();
    }

    // ── saving ──

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        getHost().markForSave();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        machineInv.readFromNBT(tag, "machine", registries);
        blocking = tag.getBoolean("blocking");
        try {
            insertMode = InsertMode.valueOf(tag.getString("insertMode"));
        } catch (IllegalArgumentException e) {
            insertMode = InsertMode.DEFAULT;
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        machineInv.writeToNBT(tag, "machine", registries);
        tag.putBoolean("blocking", blocking);
        tag.putString("insertMode", insertMode.name());
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        // the machine slot is a ghost slot: nothing to drop
    }
}
