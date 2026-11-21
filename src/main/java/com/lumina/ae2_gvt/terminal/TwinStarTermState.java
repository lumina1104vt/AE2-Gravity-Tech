package com.lumina.ae2_gvt.terminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.lumina.twinstar.algorithm.ae2.PatternChances;
import com.lumina.twinstar.algorithm.ae2.PatternIgnoreNbt;
import com.lumina.twinstar.algorithm.ae2.PatternInputFaces;
import com.lumina.twinstar.algorithm.ae2.PatternInputRoles;
import com.lumina.twinstar.algorithm.ae2.PatternInputTargets;
import com.lumina.twinstar.algorithm.ae2.PatternOutputGuiSlots;
import com.mojang.serialization.Dynamic;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything the twin-star pattern terminal remembers besides AE2's own pattern logic: the machine, its sub-machines and
 * tool (ghost items, never dropped), the probability of each output that does not always come, the faces, targets, roles and
 * GUI slots of the materials, the products' GUI slots, the NBT that does not matter, and the recipe JEI last filled it from.
 * The terminal part and the wireless terminal each keep one, and save it their own way.
 */
public class TwinStarTermState implements InternalInventoryHost {
    private final Runnable save;
    private final BooleanSupplier clientSide;

    private final AppEngInternalInventory machineInv = new AppEngInternalInventory(this, 1, 1);
    private final AppEngInternalInventory toolInv = new AppEngInternalInventory(this, 1, 1);
    private final AppEngInternalInventory subMachineInv = new AppEngInternalInventory(this, TwinStarTerminal.SUB_MACHINES, 1);
    /** outputs that come only with a probability; an output not listed always comes */
    private final Map<AEKey, Double> outputChances = new LinkedHashMap<>();
    /** per material the machine faces it goes in through (bits by Direction.get3DDataValue); not listed = not set */
    private final Map<AEKey, Integer> inputFaces = new LinkedHashMap<>();
    /** per material where it goes (PatternInputTargets); not listed = by default */
    private final Map<AEKey, Integer> inputTargets = new LinkedHashMap<>();
    /** per material what it is besides used up (PatternInputRoles); not listed = used up */
    private final Map<AEKey, Integer> inputRoles = new LinkedHashMap<>();
    /** per processing input slot (its index) the slot of the machine's GUI its material goes into (PatternInputGuiSlots); not listed = the first that takes it */
    private final Map<Integer, Integer> inputGuiSlots = new LinkedHashMap<>();
    /** per product the slot of the machine's GUI it is taken out of (PatternOutputGuiSlots); not listed = any result slot */
    private final Map<AEKey, Integer> outputGuiSlots = new LinkedHashMap<>();
    /** materials and products whose NBT does not matter */
    private final java.util.Set<AEKey> ignoreNbtInputs = new java.util.LinkedHashSet<>(), ignoreNbtOutputs = new java.util.LinkedHashSet<>();
    /** per machine the sub-machines of the last pattern encoded with it: filled in again for the next */
    private final Map<net.minecraft.world.item.Item, List<net.minecraft.world.item.Item>> lastSubMachines = new LinkedHashMap<>();
    /**
     * per machine (its item id; "" for none) the GUI slot of the machine that a sequence pattern's first recipe slot goes
     * into (1 = the first slot of the menu; a table whose output is its first slot starts at 2)
     */
    private final Map<String, Integer> sequenceStarts = new LinkedHashMap<>();

    public Map<String, Integer> getSequenceStarts() {
        return sequenceStarts;
    }

    public void setSequenceStart(String machine, int start) {
        if (start == 1)
            sequenceStarts.remove(machine);
        else
            sequenceStarts.put(machine, start);
        markForSave();
    }
    /** the recipe JEI's [+] last filled the slots from: the amount dialog's [JEI] reads its probabilities */
    private @Nullable ResourceLocation recipe;

    /** One ghost slot: the machine the encoded recipe runs in. */
    public InternalInventory getMachineInv() {
        return machineInv;
    }

    /** Ghost slots: the sub-machines, tried left to right after the machine. */
    public InternalInventory getToolInv() {
        return toolInv;
    }

    public InternalInventory getSubMachineInv() {
        return subMachineInv;
    }

    /** {@code output}: of a product, else of a material. */
    public boolean ignoresNbt(AEKey key, boolean output) {
        return (output ? ignoreNbtOutputs : ignoreNbtInputs).contains(key);
    }

    public void setIgnoreNbt(AEKey key, boolean output, boolean ignore) {
        var set = output ? ignoreNbtOutputs : ignoreNbtInputs;
        if (ignore ? set.add(key) : set.remove(key))
            markForSave();
    }

    public void clearIgnoreNbt() {
        if (!ignoreNbtInputs.isEmpty() || !ignoreNbtOutputs.isEmpty()) {
            ignoreNbtInputs.clear();
            ignoreNbtOutputs.clear();
            markForSave();
        }
    }

    /** The sub-machines last encoded with {@code machine} (empty when none). */
    public List<net.minecraft.world.item.Item> getLastSubMachines(net.minecraft.world.item.Item machine) {
        return lastSubMachines.getOrDefault(machine, List.of());
    }

    public void setLastSubMachines(net.minecraft.world.item.Item machine, List<net.minecraft.world.item.Item> subs) {
        if (subs.isEmpty())
            lastSubMachines.remove(machine);
        else
            lastSubMachines.put(machine, List.copyOf(subs));
        markForSave();
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

    /** Where material {@code key} goes (0 = by default: the sub-machines when set, else the machine). */
    public int getInputTarget(AEKey key) {
        return inputTargets.getOrDefault(key, PatternInputTargets.DEFAULT);
    }

    public void setInputTarget(AEKey key, int target) {
        if (target != PatternInputTargets.DEFAULT)
            inputTargets.put(key, target);
        else
            inputTargets.remove(key);
        markForSave();
    }

    /** What material {@code key} is besides used up ({@link PatternInputRoles}). */
    public int getInputRole(AEKey key) {
        return inputRoles.getOrDefault(key, PatternInputRoles.NORMAL);
    }

    public void setInputRole(AEKey key, int role) {
        if (role != PatternInputRoles.NORMAL)
            inputRoles.put(key, role);
        else
            inputRoles.remove(key);
        markForSave();
    }

    public int getInputGuiSlot(int index) {
        return inputGuiSlots.getOrDefault(index, 0);
    }

    public void setInputGuiSlot(int index, int slot) {
        if (slot > 0)
            inputGuiSlots.put(index, slot);
        else
            inputGuiSlots.remove(index);
        markForSave();
    }

    public int getOutputGuiSlot(AEKey key) {
        return outputGuiSlots.getOrDefault(key, 0);
    }

    public void setOutputGuiSlot(AEKey key, int slot) {
        if (slot > 0)
            outputGuiSlots.put(key, slot);
        else
            outputGuiSlots.remove(key);
        markForSave();
    }

    public void clearOutputGuiSlots() {
        if (!outputGuiSlots.isEmpty()) {
            outputGuiSlots.clear();
            markForSave();
        }
    }

    public void clearInputGuiSlots() {
        if (!inputGuiSlots.isEmpty()) {
            inputGuiSlots.clear();
            markForSave();
        }
    }

    public void clearInputRoles() {
        if (!inputRoles.isEmpty()) {
            inputRoles.clear();
            markForSave();
        }
    }

    public void clearInputTargets() {
        if (!inputTargets.isEmpty()) {
            inputTargets.clear();
            markForSave();
        }
    }

    public void clear() {
        machineInv.clear();
        subMachineInv.clear();
        toolInv.clear();
        outputChances.clear();
        inputFaces.clear();
        inputTargets.clear();
        inputRoles.clear();
        inputGuiSlots.clear();
        outputGuiSlots.clear();
        ignoreNbtInputs.clear();
        ignoreNbtOutputs.clear();
        recipe = null;
    }

    public void read(CompoundTag tag) {
        machineInv.readFromNBT(tag, "machine");
        subMachineInv.readFromNBT(tag, "subMachines");
        toolInv.readFromNBT(tag, "tool");
        outputChances.clear();
        if (tag.contains("outputChances"))
            PatternChances.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("outputChances")))
                    .result().ifPresent(c -> c.entries().forEach(e -> outputChances.put(e.key(), e.chance())));
        inputFaces.clear();
        if (tag.contains("inputFaces"))
            PatternInputFaces.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("inputFaces")))
                    .result().ifPresent(f -> f.entries().forEach(e -> inputFaces.put(e.key(), e.faces())));
        inputTargets.clear();
        if (tag.contains("inputTargets"))
            PatternInputTargets.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("inputTargets")))
                    .result().ifPresent(f -> f.entries().forEach(e -> inputTargets.put(e.key(), e.target())));
        inputRoles.clear();
        if (tag.contains("inputRoles"))
            PatternInputRoles.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("inputRoles")))
                    .result().ifPresent(f -> f.entries().forEach(e -> inputRoles.put(e.key(), e.role())));
        inputGuiSlots.clear();
        int[] guiPairs = tag.getIntArray("inputGuiSlotPairs");
        for (int i = 0; i + 1 < guiPairs.length; i += 2)
            inputGuiSlots.put(guiPairs[i], guiPairs[i + 1]);
        sequenceStarts.clear();
        CompoundTag starts = tag.getCompound("sequenceStarts");
        for (String k : starts.getAllKeys())
            sequenceStarts.put(k, starts.getInt(k));
        outputGuiSlots.clear();
        if (tag.contains("outputGuiSlots"))
            PatternOutputGuiSlots.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("outputGuiSlots")))
                    .result().ifPresent(f -> f.entries().forEach(e -> outputGuiSlots.put(e.key(), e.slot())));
        recipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        ignoreNbtInputs.clear();
        ignoreNbtOutputs.clear();
        if (tag.contains("ignoreNbt"))
            PatternIgnoreNbt.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag.get("ignoreNbt")))
                    .result().ifPresent(n -> {
                        ignoreNbtInputs.addAll(n.inputs());
                        ignoreNbtOutputs.addAll(n.outputs());
                    });
        lastSubMachines.clear();
        CompoundTag last = tag.getCompound("lastSubMachines");
        for (String k : last.getAllKeys()) {
            var machine = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(k));
            if (machine.isEmpty())
                continue;
            List<net.minecraft.world.item.Item> subs = new ArrayList<>();
            for (net.minecraft.nbt.Tag t : last.getList(k, net.minecraft.nbt.Tag.TAG_STRING))
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(t.getAsString())).ifPresent(subs::add);
            if (!subs.isEmpty())
                lastSubMachines.put(machine.get(), subs);
        }
    }

    public void write(CompoundTag tag) {
        machineInv.writeToNBT(tag, "machine");
        subMachineInv.writeToNBT(tag, "subMachines");
        toolInv.writeToNBT(tag, "tool");
        if (!outputChances.isEmpty()) {
            List<PatternChances.Entry> entries = new ArrayList<>();
            outputChances.forEach((k, c) -> entries.add(new PatternChances.Entry(k, c)));
            PatternChances.CODEC.encodeStart(NbtOps.INSTANCE, new PatternChances(entries))
                    .result().ifPresent(t -> tag.put("outputChances", t));
        }
        if (!inputFaces.isEmpty()) {
            List<PatternInputFaces.Entry> entries = new ArrayList<>();
            inputFaces.forEach((k, f) -> entries.add(new PatternInputFaces.Entry(k, f)));
            PatternInputFaces.CODEC.encodeStart(NbtOps.INSTANCE, new PatternInputFaces(entries))
                    .result().ifPresent(t -> tag.put("inputFaces", t));
        }
        if (!inputTargets.isEmpty()) {
            List<PatternInputTargets.Entry> entries = new ArrayList<>();
            inputTargets.forEach((k, t) -> entries.add(new PatternInputTargets.Entry(k, t)));
            PatternInputTargets.CODEC.encodeStart(NbtOps.INSTANCE, new PatternInputTargets(entries))
                    .result().ifPresent(t -> tag.put("inputTargets", t));
        }
        if (!inputRoles.isEmpty()) {
            List<PatternInputRoles.Entry> entries = new ArrayList<>();
            inputRoles.forEach((k, r) -> entries.add(new PatternInputRoles.Entry(k, r)));
            PatternInputRoles.CODEC.encodeStart(NbtOps.INSTANCE, new PatternInputRoles(entries))
                    .result().ifPresent(t -> tag.put("inputRoles", t));
        }
        if (!inputGuiSlots.isEmpty()) {
            int[] pairs = new int[inputGuiSlots.size() * 2];
            int n = 0;
            for (Map.Entry<Integer, Integer> e : inputGuiSlots.entrySet()) {
                pairs[n++] = e.getKey();
                pairs[n++] = e.getValue();
            }
            tag.putIntArray("inputGuiSlotPairs", pairs);
        }
        if (!sequenceStarts.isEmpty()) {
            CompoundTag starts = new CompoundTag();
            sequenceStarts.forEach(starts::putInt);
            tag.put("sequenceStarts", starts);
        }
        if (!outputGuiSlots.isEmpty()) {
            List<PatternOutputGuiSlots.Entry> entries = new ArrayList<>();
            outputGuiSlots.forEach((k, g) -> entries.add(new PatternOutputGuiSlots.Entry(k, g)));
            PatternOutputGuiSlots.CODEC.encodeStart(NbtOps.INSTANCE, new PatternOutputGuiSlots(entries))
                    .result().ifPresent(t -> tag.put("outputGuiSlots", t));
        }
        if (recipe != null)
            tag.putString("recipe", recipe.toString());
        if (!ignoreNbtInputs.isEmpty() || !ignoreNbtOutputs.isEmpty())
            PatternIgnoreNbt.CODEC.encodeStart(NbtOps.INSTANCE,
                    new PatternIgnoreNbt(List.copyOf(ignoreNbtInputs), List.copyOf(ignoreNbtOutputs))).result().ifPresent(t -> tag.put("ignoreNbt", t));
        if (!lastSubMachines.isEmpty()) {
            CompoundTag last = new CompoundTag();
            lastSubMachines.forEach((machine, subs) -> {
                net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
                subs.forEach(sub -> list.add(net.minecraft.nbt.StringTag.valueOf(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sub).toString())));
                last.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(machine).toString(), list);
            });
            tag.put("lastSubMachines", last);
        }
    }

    private void markForSave() {
        save.run();
    }

    /** {@code save}: called when anything changed; {@code clientSide}: whether this is the client's copy. */
    public TwinStarTermState(Runnable save, BooleanSupplier clientSide) {
        this.save = save;
        this.clientSide = clientSide;
    }

    @Override
    public void saveChanges() {
        markForSave();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
    }

    @Override
    public boolean isClientSide() {
        return clientSide.getAsBoolean();
    }
}
