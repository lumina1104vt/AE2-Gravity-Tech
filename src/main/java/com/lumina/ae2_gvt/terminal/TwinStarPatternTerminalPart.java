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
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * AE2's pattern encoding terminal plus the remembered machine (a ghost item, never dropped) and the
 * probability of each output that does not always come (by key, like the pattern keeps it), the
 * faces each material goes in through, and the recipe JEI last filled it from.
 */
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
    /** outputs that come only with a probability; an output not listed always comes */
    private final Map<AEKey, Double> outputChances = new LinkedHashMap<>();
    /** per material the machine faces it goes in through (bits by Direction.get3DDataValue); not listed = not set */
    private final Map<AEKey, Integer> inputFaces = new LinkedHashMap<>();
    /** the recipe JEI's [+] last filled the slots from: the amount dialog's [JEI] reads its probabilities */
    private @Nullable ResourceLocation recipe;

    public TwinStarPatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    /** One ghost slot: the machine the encoded recipe runs in. */
    public InternalInventory getMachineInv() {
        return machineInv;
    }

    /** Probability of output {@code key} (1 when it always comes). */
    public double getOutputChance(AEKey key) {
        return outputChances.getOrDefault(key, 1.0);
    }

    /** {@code chance} of 1 (or anything not in (0, 1)) means it always comes. */
    public void setOutputChance(AEKey key, double chance) {
        if (chance > 0 && chance < 1)
            outputChances.put(key, chance);
        else
            outputChances.remove(key);
        markForSave();
    }

    public void clearOutputChances() {
        if (!outputChances.isEmpty()) {
            outputChances.clear();
            markForSave();
        }
    }

    public @Nullable ResourceLocation getRecipe() {
        return recipe;
    }

    public void setRecipe(@Nullable ResourceLocation recipe) {
        if (!java.util.Objects.equals(this.recipe, recipe)) {
            this.recipe = recipe;
            markForSave();
        }
    }

    /** The faces material {@code key} goes in through (0 = not set). */
    public int getInputFaces(AEKey key) {
        return inputFaces.getOrDefault(key, 0);
    }

    public void setInputFaces(AEKey key, int faces) {
        if ((faces & 0x3F) != 0)
            inputFaces.put(key, faces & 0x3F);
        else
            inputFaces.remove(key);
        markForSave();
    }

    public void clearInputFaces() {
        if (!inputFaces.isEmpty()) {
            inputFaces.clear();
            markForSave();
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        machineInv.clear();
        outputChances.clear();
        inputFaces.clear();
        recipe = null;
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        machineInv.readFromNBT(tag, "machine", registries);
        outputChances.clear();
        if (tag.contains("outputChances"))
            PatternChances.CODEC.parse(new Dynamic<>(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("outputChances")))
                    .result().ifPresent(c -> c.entries().forEach(e -> outputChances.put(e.key(), e.chance())));
        inputFaces.clear();
        if (tag.contains("inputFaces"))
            PatternInputFaces.CODEC.parse(new Dynamic<>(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("inputFaces")))
                    .result().ifPresent(f -> f.entries().forEach(e -> inputFaces.put(e.key(), e.faces())));
        recipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        machineInv.writeToNBT(tag, "machine", registries);
        if (!outputChances.isEmpty()) {
            List<PatternChances.Entry> entries = new ArrayList<>();
            outputChances.forEach((k, c) -> entries.add(new PatternChances.Entry(k, c)));
            PatternChances.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), new PatternChances(entries))
                    .result().ifPresent(t -> tag.put("outputChances", t));
        }
        if (!inputFaces.isEmpty()) {
            List<PatternInputFaces.Entry> entries = new ArrayList<>();
            inputFaces.forEach((k, f) -> entries.add(new PatternInputFaces.Entry(k, f)));
            PatternInputFaces.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), new PatternInputFaces(entries))
                    .result().ifPresent(t -> tag.put("inputFaces", t));
        }
        if (recipe != null)
            tag.putString("recipe", recipe.toString());
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
