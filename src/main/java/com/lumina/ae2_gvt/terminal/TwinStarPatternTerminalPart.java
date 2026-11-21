package com.lumina.ae2_gvt.terminal;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
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
import com.lumina.twinstar.algorithm.ae2.PatternChances;
import com.lumina.twinstar.algorithm.ae2.PatternInputFaces;
import com.lumina.twinstar.algorithm.ae2.PatternIgnoreNbt;
import com.lumina.twinstar.algorithm.ae2.PatternOutputGuiSlots;
import com.lumina.twinstar.algorithm.ae2.PatternInputRoles;
import com.lumina.twinstar.algorithm.ae2.PatternInputTargets;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * AE2's pattern encoding terminal plus the remembered machine and sub-machines (ghost items, never dropped) and the
 * probability of each output that does not always come (by key, like the pattern keeps it), the
 * faces each material goes in through, and the recipe JEI last filled it from.
 */
public class TwinStarPatternTerminalPart extends PatternEncodingTerminalPart implements ITwinStarTermHost {
    private static final ResourceLocation SCREEN_OFF = new ResourceLocation(AE2GravityTech.MODID, "part/twin_star_pattern_terminal_off");
    private static final ResourceLocation SCREEN_ON = new ResourceLocation(AE2GravityTech.MODID, "part/twin_star_pattern_terminal_on");

    // AE2's display frame and status lights around our own screen (registered in AE2GravityTechClient)
    @PartModels
    public static final IPartModel TWIN_STAR_OFF = new PartModel(MODEL_BASE, SCREEN_OFF, MODEL_STATUS_OFF);
    @PartModels
    public static final IPartModel TWIN_STAR_ON = new PartModel(MODEL_BASE, SCREEN_ON, MODEL_STATUS_ON);
    @PartModels
    public static final IPartModel TWIN_STAR_HAS_CHANNEL = new PartModel(MODEL_BASE, SCREEN_ON, MODEL_STATUS_HAS_CHANNEL);

    private final TwinStarTermState state = new TwinStarTermState(this::markForSave, () -> getLevel() != null && getLevel().isClientSide());

    public TwinStarPatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public TwinStarTermState state() {
        return state;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        state.clear();
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        state.read(tag);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        state.write(tag);
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
