package com.lumina.ae2_gvt.prototype.terminal;

import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import com.lumina.ae2_gvt.AE2GravityTech;

/** AE2's pattern encoding terminal plus the remembered machine (a ghost item, never dropped). */
public class TwinStarPatternTerminalPart extends PatternEncodingTerminalPart {
    private static final ResourceLocation SCREEN_OFF = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "part/twin_star_pattern_terminal_off");
    private static final ResourceLocation SCREEN_ON = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "part/twin_star_pattern_terminal_on");

    // AE2's display frame and status lights around our own screen (registered in AE2GravityTechClient)
    @PartModels
    public static final IPartModel TWIN_STAR_OFF = new PartModel(MODEL_BASE, SCREEN_OFF, MODEL_STATUS_OFF);
    @PartModels
    public static final IPartModel TWIN_STAR_ON = new PartModel(MODEL_BASE, SCREEN_ON, MODEL_STATUS_ON);
    @PartModels
    public static final IPartModel TWIN_STAR_HAS_CHANNEL = new PartModel(MODEL_BASE, SCREEN_ON, MODEL_STATUS_HAS_CHANNEL);

    private final AppEngInternalInventory machineInv = new AppEngInternalInventory(this, 1, 1);

    public TwinStarPatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    /** One ghost slot: the machine the encoded recipe runs in. */
    public InternalInventory getMachineInv() {
        return machineInv;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        machineInv.clear();
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        machineInv.readFromNBT(tag, "machine", registries);
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        machineInv.writeToNBT(tag, "machine", registries);
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(TWIN_STAR_OFF, TWIN_STAR_ON, TWIN_STAR_HAS_CHANNEL);
    }

    @Override
    public MenuType<?> getMenuType(Player p) {
        return TwinStarTerminal.MENU.get();
    }
}
