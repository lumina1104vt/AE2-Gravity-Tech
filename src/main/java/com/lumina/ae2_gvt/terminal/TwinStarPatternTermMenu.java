package com.lumina.ae2_gvt.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.lumina.twinstar.algorithm.ae2.PatternChances;
import com.lumina.twinstar.algorithm.ae2.PatternInputFaces;
import com.lumina.twinstar.algorithm.ae2.PatternIgnoreNbt;
import com.lumina.twinstar.algorithm.ae2.PatternInputGuiSlots;
import com.lumina.twinstar.algorithm.ae2.PatternOutputGuiSlots;
import com.lumina.twinstar.algorithm.ae2.PatternInputRoles;
import com.lumina.twinstar.algorithm.ae2.PatternInputTargets;
import com.lumina.twinstar.storage.TwinstarContent;
import com.lumina.twinstar.tools.RecipeChances;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * AE2's pattern encoding menu, locked to the processing layout, plus the machine ghost slot and the
 * sub-machine ones (blocks near the machine some materials go into; set in their own dialog), the
 * probability of each output (set in the amount dialog of an output slot, or read from the recipe
 * by JEI's [+]) and the machine faces each material goes in through (set in the amount dialog of an
 * input slot; used by interfaces set to "specified") or where else it goes (its sub-machines, in front
 * of the machine). Extending AE2's menu keeps its ME grid,
 * encoding and recipe-transfer handling.
 */
public class TwinStarPatternTermMenu extends PatternEncodingTermMenu {
    private static final String SET_OUTPUT_CHANCE = "setOutputChance";
    private static final String RECIPE_CHANCES = "recipeChances";
    private static final String SET_INPUT_FACES = "setInputFaces";
    private static final String SET_INPUT_TARGET = "setInputTarget";
    private static final String SET_INPUT_ROLE = "setInputRole";
    private static final String SET_INPUT_GUI_SLOT = "setInputGuiSlot";
    private static final String SET_OUTPUT_GUI_SLOT = "setOutputGuiSlot";
    private static final String SET_SEQUENCE_START = "setSequenceStart";
    private static final String SET_IGNORE_NBT = "setIgnoreNbt";

    private final ITwinStarTermHost host;
    private final FakeSlot machineSlot;
    private final List<FakeSlot> subMachineSlots = new ArrayList<>();
    private final FakeSlot toolSlot;
    /** Per processing output slot its probability ("" = always), ';' between slots: what the dialog shows. */
    @GuiSync(200)
    public String outputChances = "";
    /** Per processing input slot the face bits of what is in it ("" = not set), ';' between slots. */
    @GuiSync(201)
    public String inputFaces = "";
    /**
     * Per processing output slot its probability by the recipe JEI's [+] filled the slots from, ';'
     * between slots: what the dialog's [JEI] button puts back ("" = no such recipe, 1 = always).
     */
    @GuiSync(202)
    public String recipeChances = "";
    /** Per processing input slot where what is in it goes ("" = into the machine), ';' between slots. */
    @GuiSync(203)
    public String inputTargets = "";
    /** Per processing input slot what it is besides used up (PatternInputRoles; "" = used up), ';' between slots. */
    @GuiSync(206)
    public String inputRoles = "";
    /** Per processing input slot the GUI slot its material goes into ("" = the first that takes it), ';' between slots. */
    @GuiSync(207)
    public String inputGuiSlots = "";
    /** Per processing output slot the GUI slot its product is taken out of ("" = any result slot), ';' between slots. */
    @GuiSync(208)
    public String outputGuiSlots = "";
    /** per machine (item id) the GUI slot a sequence pattern starts at, "id=n;id=n" (see the part) */
    @GuiSync(209)
    public String sequenceStarts = "";
    /** Per processing input slot, then per output slot: 1 when its NBT does not matter. */
    @GuiSync(204)
    public String ignoreNbtIn = "";
    @GuiSync(205)
    public String ignoreNbtOut = "";
    /** the probabilities of {@link TwinStarTermState#getRecipe()}, read once per recipe */
    private @Nullable ResourceLocation readRecipe;
    private @Nullable Map<String, Double> readChances;
    /** the encoded pattern slot as last seen: one put in loads its probabilities, like AE2 loads the rest */
    private ItemStack lastEncoded = ItemStack.EMPTY;
    /**
     * While AE2 encodes: the pattern it puts in the slot is not one put in by the player, so it must not reset the
     * probabilities and faces; it did, and every pattern came out without them.
     */
    private boolean encoding;

    public TwinStarPatternTermMenu(MenuType<?> menuType, int id, Inventory ip, ITwinStarTermHost host) {
        super(menuType, id, ip, host, true);
        this.host = host;
        this.machineSlot = new FakeSlot(host.state().getMachineInv(), 0);
        this.machineSlot.setHideAmount(true);
        this.addSlot(this.machineSlot, TwinStarTerminal.MACHINE_SLOT);
        this.toolSlot = new FakeSlot(host.state().getToolInv(), 0);
        this.toolSlot.setHideAmount(true);
        this.addSlot(this.toolSlot, TwinStarTerminal.TOOL_SLOT);
        for (int i = 0; i < TwinStarTerminal.SUB_MACHINES; i++) {
            FakeSlot sub = new FakeSlot(host.state().getSubMachineInv(), i);
            sub.setHideAmount(true);
            subMachineSlots.add(sub);
            this.addSlot(sub, TwinStarTerminal.SUB_MACHINE_SLOTS);
        }

        // one combined pattern type: no crafting / smithing / stonecutting modes
        this.mode = EncodingMode.PROCESSING;
        if (this.isServerSide()) {
            host.getLogic().setMode(EncodingMode.PROCESSING);
            lastEncoded = encodedSlot().getItem().copy();
        }
        registerClientAction(SET_OUTPUT_CHANCE, String.class, this::receiveOutputChance);
        registerClientAction(RECIPE_CHANCES, String.class, this::receiveRecipeChances);
        registerClientAction(SET_INPUT_FACES, String.class, this::receiveInputFaces);
        registerClientAction(SET_INPUT_TARGET, String.class, this::receiveInputTarget);
        registerClientAction(SET_INPUT_ROLE, String.class, this::receiveInputRole);
        registerClientAction(SET_INPUT_GUI_SLOT, String.class, this::receiveInputGuiSlot);
        registerClientAction(SET_OUTPUT_GUI_SLOT, String.class, this::receiveOutputGuiSlot);
        registerClientAction(SET_SEQUENCE_START, String.class, this::receiveSequenceStart);
        registerClientAction(SET_IGNORE_NBT, String.class, this::receiveIgnoreNbt);
    }

    private Slot encodedSlot() {
        return this.getSlots(SlotSemantics.ENCODED_PATTERN).get(0);
    }

    /**
     * AE2 encodes a processing pattern; we turn it into a synthesis pattern (same data) and stamp
     * the machine on it (or clear a stale one), and the probabilities of its outputs.
     */
    @Override
    public void encode() {
        if (this.isServerSide()) {
            String refused = refusal();
            if (refused != null) {
                // above the hotbar, not in chat
                getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable(refused)
                        .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                return;
            }
        }
        encoding = true;
        try {
            super.encode();
        } finally {
            encoding = false;
        }
        if (this.isClientSide()) {
            return;
        }
        Slot encoded = encodedSlot();
        ItemStack pattern = encoded.getItem();
        if (!PatternDetailsHelper.isEncodedPattern(pattern)) {
            return;
        }
        if (!pattern.is(TwinStarTerminal.PATTERN.get())) {
            pattern = pattern.transmuteCopy(TwinStarTerminal.PATTERN.get());
        }
        // the machine; with it, the sub-machines left to right (no repeats, not the machine itself)
        ItemStack machine = this.machineSlot.getItem();
        List<Holder<Item>> subs = new ArrayList<>();
        for (FakeSlot slot : subMachineSlots) {
            ItemStack m = slot.getItem();
            if (!m.isEmpty() && m.getItem() != machine.getItem() && subs.stream().noneMatch(h -> h.value() == m.getItem())) {
                subs.add(m.getItemHolder());
            }
        }
        if (machine.isEmpty()) {
            pattern.remove(TwinStarTerminal.PATTERN_MACHINE.get());
        } else {
            pattern.set(TwinStarTerminal.PATTERN_MACHINE.get(), machine.getItemHolder());
        }
        ItemStack tool = toolSlot.getItem();
        if (machine.isEmpty() || tool.isEmpty()) {
            pattern.remove(TwinStarTerminal.PATTERN_TOOL.get());
        } else {
            pattern.set(TwinStarTerminal.PATTERN_TOOL.get(), tool.getItemHolder());
        }
        if (machine.isEmpty() || subs.isEmpty()) {
            pattern.remove(TwinStarTerminal.PATTERN_SUB_MACHINES.get());
        } else {
            pattern.set(TwinStarTerminal.PATTERN_SUB_MACHINES.get(), List.copyOf(subs));
        }
        if (!machine.isEmpty()) {
            // the next pattern for this machine starts with them
            host.state().setLastSubMachines(machine.getItem(), subs.stream().map(Holder::value).toList());
        }
        List<PatternChances.Entry> chances = new ArrayList<>();
        for (FakeSlot slot : getProcessingOutputSlots()) {
            GenericStack out = GenericStack.fromItemStack(slot.getItem());
            if (out == null || chances.stream().anyMatch(e -> e.key().equals(out.what()))) {
                continue;
            }
            double c = host.state().getOutputChance(out.what());
            if (c < 1) {
                chances.add(new PatternChances.Entry(out.what(), c));
            }
        }
        if (chances.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_CHANCES.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_CHANCES.get(), new PatternChances(chances));
        }
        List<PatternInputFaces.Entry> faces = new ArrayList<>();
        for (FakeSlot slot : getProcessingInputSlots()) {
            GenericStack in = GenericStack.fromItemStack(slot.getItem());
            if (in == null || faces.stream().anyMatch(e -> e.key().equals(in.what()))) {
                continue;
            }
            int f = host.state().getInputFaces(in.what());
            if (f != 0) {
                faces.add(new PatternInputFaces.Entry(in.what(), f));
            }
        }
        if (faces.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_INPUT_FACES.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_INPUT_FACES.get(), new PatternInputFaces(faces));
        }
        List<PatternInputTargets.Entry> targets = new ArrayList<>();
        for (FakeSlot slot : getProcessingInputSlots()) {
            GenericStack in = GenericStack.fromItemStack(slot.getItem());
            if (in == null || targets.stream().anyMatch(e -> e.key().equals(in.what()))) {
                continue;
            }
            int t = host.state().getInputTarget(in.what());
            if (t != PatternInputTargets.DEFAULT) {
                targets.add(new PatternInputTargets.Entry(in.what(), t));
            }
        }
        List<PatternInputRoles.Entry> roles = new ArrayList<>();
        for (FakeSlot slot : getProcessingInputSlots()) {
            GenericStack in = GenericStack.fromItemStack(slot.getItem());
            if (in == null || roles.stream().anyMatch(e -> e.key().equals(in.what()))) {
                continue;
            }
            int r = host.state().getInputRole(in.what());
            if (r != PatternInputRoles.NORMAL) {
                roles.add(new PatternInputRoles.Entry(in.what(), r));
            }
        }
        List<PatternOutputGuiSlots.Entry> outGui = new ArrayList<>();
        for (FakeSlot slot : getProcessingOutputSlots()) {
            GenericStack out = GenericStack.fromItemStack(slot.getItem());
            if (out == null || outGui.stream().anyMatch(e -> e.key().equals(out.what()))) {
                continue;
            }
            int g = host.state().getOutputGuiSlot(out.what());
            if (g > 0) {
                outGui.add(new PatternOutputGuiSlots.Entry(out.what(), g));
            }
        }
        if (outGui.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_OUTPUT_GUI_SLOTS.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_OUTPUT_GUI_SLOTS.get(), new PatternOutputGuiSlots(outGui));
        }
        // per material slot, in the order AE2 lists them (the filled ones): the same material can go into many places
        List<Integer> guiSlots = new ArrayList<>();
        FakeSlot[] inSlots = getProcessingInputSlots();
        for (int i = 0; i < inSlots.length; i++) {
            if (GenericStack.fromItemStack(inSlots[i].getItem()) != null) {
                guiSlots.add(host.state().getInputGuiSlot(i));
            }
        }
        if (guiSlots.stream().allMatch(g -> g == 0)) {
            pattern.remove(TwinstarContent.PATTERN_INPUT_GUI_SLOTS.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_INPUT_GUI_SLOTS.get(), new PatternInputGuiSlots(guiSlots));
        }
        if (roles.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_INPUT_ROLES.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_INPUT_ROLES.get(), new PatternInputRoles(roles));
        }
        if (targets.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_INPUT_TARGETS.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_INPUT_TARGETS.get(), new PatternInputTargets(targets));
        }
        List<appeng.api.stacks.AEKey> nbtIn = new ArrayList<>(), nbtOut = new ArrayList<>();
        for (FakeSlot slot : getProcessingInputSlots()) {
            GenericStack in = GenericStack.fromItemStack(slot.getItem());
            if (in != null && !nbtIn.contains(in.what()) && host.state().ignoresNbt(in.what(), false)) {
                nbtIn.add(in.what());
            }
        }
        for (FakeSlot slot : getProcessingOutputSlots()) {
            GenericStack out = GenericStack.fromItemStack(slot.getItem());
            if (out != null && !nbtOut.contains(out.what()) && host.state().ignoresNbt(out.what(), true)) {
                nbtOut.add(out.what());
            }
        }
        if (nbtIn.isEmpty() && nbtOut.isEmpty()) {
            pattern.remove(TwinstarContent.PATTERN_IGNORE_NBT.get());
        } else {
            pattern.set(TwinstarContent.PATTERN_IGNORE_NBT.get(), new PatternIgnoreNbt(nbtIn, nbtOut));
        }
        encoded.set(pattern);
        lastEncoded = pattern.copy();
    }

    /**
     * Why this pattern may not be encoded (a lang key), or null: it needs a machine; one naming a crafting
     * table or the like must be that block's real recipe, since the black hole makes what it says.
     */
    private @Nullable String refusal() {
        ItemStack machine = this.machineSlot.getItem();
        if (machine.isEmpty()) {
            return "ae2_gvt.pattern.refused.no_machine";
        }
        if (!com.lumina.twinstar.algorithm.ae2.WorkstationRecipes.isWorkstation(machine.getItem())) {
            return null;
        }
        List<com.lumina.twinstar.algorithm.ae2.WorkstationRecipes.In> ins = new ArrayList<>();
        for (FakeSlot slot : getProcessingInputSlots()) {
            GenericStack in = GenericStack.fromItemStack(slot.getItem());
            if (in != null) {
                ins.add(new com.lumina.twinstar.algorithm.ae2.WorkstationRecipes.In(List.of(in.what()), in.amount()));
            }
        }
        List<GenericStack> outs = new ArrayList<>();
        for (FakeSlot slot : getProcessingOutputSlots()) {
            GenericStack out = GenericStack.fromItemStack(slot.getItem());
            if (out != null) {
                outs.add(out);
            }
        }
        var level = getPlayer().level();
        return com.lumina.twinstar.algorithm.ae2.WorkstationRecipes.matches(machine.getItem(), ins, outs,
                level.getRecipeManager(), level.registryAccess()) ? null : "ae2_gvt.pattern.refused.not_recipe";
    }

    /** A pattern put in: its machine and sub-machines go back in their slots, like AE2 puts back the rest. */
    private void loadMachines(ItemStack pattern) {
        Holder<Item> machine = pattern.get(TwinStarTerminal.PATTERN_MACHINE.get());
        host.state().getMachineInv().setItemDirect(0, machine == null ? ItemStack.EMPTY : new ItemStack(machine));
        Holder<Item> tool = pattern.get(TwinStarTerminal.PATTERN_TOOL.get());
        host.state().getToolInv().setItemDirect(0, tool == null ? ItemStack.EMPTY : new ItemStack(tool));
        List<Holder<Item>> subs = pattern.getOrDefault(TwinStarTerminal.PATTERN_SUB_MACHINES.get(), List.of());
        for (int i = 0; i < TwinStarTerminal.SUB_MACHINES; i++) {
            host.state().getSubMachineInv().setItemDirect(i, i < subs.size() ? new ItemStack(subs.get(i)) : ItemStack.EMPTY);
        }
    }

    @Override
    public void clear() {
        super.clear();
        if (this.isServerSide()) {
            host.state().clearOutputChances();
            host.state().clearInputFaces();
            host.state().clearInputTargets();
            host.state().clearInputRoles();
            host.state().clearInputGuiSlots();
            host.state().clearOutputGuiSlots();
            host.state().clearIgnoreNbt();
            host.state().setRecipe(null);
            clearSubMachines();
        }
    }

    /**
     * A new pattern begins (cleared, or filled from JEI): the last one's sub-machines go, and those last
     * encoded with the machine now in its slot come in (JEI may put its own after this).
     */
    private void clearSubMachines() {
        fillSubMachines(machineSlot.getItem());
    }

    /** The sub-machine slots: those last encoded with {@code machine}, else empty. */
    private void fillSubMachines(ItemStack machine) {
        host.state().getToolInv().setItemDirect(0, ItemStack.EMPTY);
        List<Item> subs = machine.isEmpty() ? List.of() : host.state().getLastSubMachines(machine.getItem());
        for (int i = 0; i < TwinStarTerminal.SUB_MACHINES; i++) {
            host.state().getSubMachineInv().setItemDirect(i, i < subs.size() ? new ItemStack(subs.get(i)) : ItemStack.EMPTY);
        }
        machineSeen = machine.getItem();
    }

    /** the machine slot as last seen: a machine put in brings its sub-machines when none are set */
    private @Nullable Item machineSeen;

    @Override
    public void broadcastChanges() {
        if (this.isServerSide()) {
            ItemStack machineNow = machineSlot.getItem();
            Item machineItem = machineNow.isEmpty() ? null : machineNow.getItem();
            if (machineItem != machineSeen) {
                machineSeen = machineItem;
                if (machineItem != null && subMachineSlots.stream().noneMatch(Slot::hasItem)) {
                    fillSubMachines(machineNow);
                }
            }
            ItemStack now = encodedSlot().getItem();
            if (!encoding && !ItemStack.matches(now, lastEncoded)) {
                lastEncoded = now.copy();
                if (PatternDetailsHelper.isEncodedPattern(now)) {
                    // a pattern put in: AE2 fills the grid from it, the probabilities come along
                    host.state().clearOutputChances();
                    PatternChances c = now.get(TwinstarContent.PATTERN_CHANCES.get());
                    if (c != null) {
                        c.entries().forEach(e -> host.state().setOutputChance(e.key(), e.chance()));
                    }
                    host.state().clearInputFaces();
                    PatternInputFaces f = now.get(TwinstarContent.PATTERN_INPUT_FACES.get());
                    if (f != null) {
                        f.entries().forEach(e -> host.state().setInputFaces(e.key(), e.faces()));
                    }
                    host.state().clearIgnoreNbt();
                    PatternIgnoreNbt n = now.get(TwinstarContent.PATTERN_IGNORE_NBT.get());
                    if (n != null) {
                        n.inputs().forEach(k -> host.state().setIgnoreNbt(k, false, true));
                        n.outputs().forEach(k -> host.state().setIgnoreNbt(k, true, true));
                    }
                    host.state().clearOutputGuiSlots();
                    PatternOutputGuiSlots outGuiNow = now.get(TwinstarContent.PATTERN_OUTPUT_GUI_SLOTS.get());
                    if (outGuiNow != null) {
                        outGuiNow.entries().forEach(e -> host.state().setOutputGuiSlot(e.key(), e.slot()));
                    }
                    host.state().clearInputGuiSlots();
                    PatternInputGuiSlots guiNow = now.get(TwinstarContent.PATTERN_INPUT_GUI_SLOTS.get());
                    pendingGuiSlots = guiNow == null ? null : guiNow.slots();
                    applyPendingGuiSlots();
                    host.state().clearInputRoles();
                    PatternInputRoles roleNow = now.get(TwinstarContent.PATTERN_INPUT_ROLES.get());
                    if (roleNow != null) {
                        roleNow.entries().forEach(e -> host.state().setInputRole(e.key(), e.role()));
                    }
                    host.state().clearInputTargets();
                    PatternInputTargets t = now.get(TwinstarContent.PATTERN_INPUT_TARGETS.get());
                    if (t != null) {
                        t.entries().forEach(e -> host.state().setInputTarget(e.key(), e.target()));
                    }
                    host.state().setRecipe(null);
                    loadMachines(now);
                    machineSeen = machineSlot.getItem().isEmpty() ? null : machineSlot.getItem().getItem();
                }
            }
            StringBuilder sb = new StringBuilder();
            FakeSlot[] outs = getProcessingOutputSlots();
            for (int i = 0; i < outs.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                GenericStack out = GenericStack.fromItemStack(outs[i].getItem());
                double c = out == null ? 1 : host.state().getOutputChance(out.what());
                if (c < 1) {
                    sb.append(c);
                }
            }
            outputChances = sb.toString();
            StringBuilder ob = new StringBuilder();
            for (int i = 0; i < outs.length; i++) {
                if (i > 0) {
                    ob.append(';');
                }
                GenericStack out = GenericStack.fromItemStack(outs[i].getItem());
                int g = out == null ? 0 : host.state().getOutputGuiSlot(out.what());
                if (g != 0) {
                    ob.append(g);
                }
            }
            outputGuiSlots = ob.toString();
            StringBuilder sb2 = new StringBuilder();
            host.state().getSequenceStarts().forEach((id, n) -> sb2.append(sb2.isEmpty() ? "" : ";").append(id).append('=').append(n));
            sequenceStarts = sb2.toString();
            recipeChances = recipeChancesBySlot(outs);
            StringBuilder fb = new StringBuilder();
            FakeSlot[] ins = getProcessingInputSlots();
            for (int i = 0; i < ins.length; i++) {
                if (i > 0) {
                    fb.append(';');
                }
                GenericStack in = GenericStack.fromItemStack(ins[i].getItem());
                int f = in == null ? 0 : host.state().getInputFaces(in.what());
                if (f != 0) {
                    fb.append(f);
                }
            }
            inputFaces = fb.toString();
            StringBuilder tb = new StringBuilder();
            for (int i = 0; i < ins.length; i++) {
                if (i > 0) {
                    tb.append(';');
                }
                GenericStack in = GenericStack.fromItemStack(ins[i].getItem());
                int t = in == null ? 0 : host.state().getInputTarget(in.what());
                if (t != 0) {
                    tb.append(t);
                }
            }
            inputTargets = tb.toString();
            StringBuilder rb = new StringBuilder();
            for (int i = 0; i < ins.length; i++) {
                if (i > 0) {
                    rb.append(';');
                }
                GenericStack in = GenericStack.fromItemStack(ins[i].getItem());
                int r = in == null ? 0 : host.state().getInputRole(in.what());
                if (r != 0) {
                    rb.append(r);
                }
            }
            inputRoles = rb.toString();
            StringBuilder gb = new StringBuilder();
            for (int i = 0; i < ins.length; i++) {
                if (i > 0) {
                    gb.append(';');
                }
                GenericStack in = GenericStack.fromItemStack(ins[i].getItem());
                int g = in == null ? 0 : host.state().getInputGuiSlot(i);
                if (g != 0) {
                    gb.append(g);
                }
            }
            inputGuiSlots = gb.toString();
            applyPendingGuiSlots();
            ignoreNbtIn = nbtFlags(ins, false);
            ignoreNbtOut = nbtFlags(outs, true);
        }
        super.broadcastChanges();
    }

    // ── probabilities ──

    /** The probability shown for processing output slot {@code index} (1 = always). */
    public double getOutputChance(int index) {
        String[] parts = outputChances.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 1;
        }
        try {
            return Double.parseDouble(parts[index]);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Client: sets the probability of what is in processing output slot {@code index}. */
    public void setOutputChance(int index, double chance) {
        sendClientAction(SET_OUTPUT_CHANCE, index + ";" + String.format(Locale.ROOT, "%.17g", chance));
    }

    private void receiveOutputChance(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            double chance = Double.parseDouble(parts[1]);
            FakeSlot[] outs = getProcessingOutputSlots();
            if (index < 0 || index >= outs.length) {
                return;
            }
            GenericStack out = GenericStack.fromItemStack(outs[index].getItem());
            if (out != null) {
                host.state().setOutputChance(out.what(), chance);
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /**
     * Client, after JEI's [+] filled the slots: the recipe it came from, so the server reads the
     * probabilities of its outputs from the recipe (null: none known, every output always comes).
     */
    public void setRecipeChances(@Nullable ResourceLocation recipe) {
        sendClientAction(RECIPE_CHANCES, recipe == null ? "" : recipe.toString());
    }

    /** The slots JEI filled arrived before this (same connection, in order). */
    private void receiveRecipeChances(String id) {
        host.state().clearOutputChances();
        clearSubMachines();
        ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        host.state().setRecipe(rl);
        Map<String, Double> byId = chancesOf(rl);
        if (byId == null) {
            return;
        }
        for (FakeSlot slot : getProcessingOutputSlots()) {
            GenericStack out = GenericStack.fromItemStack(slot.getItem());
            if (out == null) {
                continue;
            }
            Double c = byId.get(out.what().getId().toString());
            if (c != null) {
                host.state().setOutputChance(out.what(), c);
            }
        }
    }

    /** The probabilities of recipe {@code id}'s outputs by item id; null when there is no such recipe. */
    private @Nullable Map<String, Double> chancesOf(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        if (!id.equals(readRecipe)) {
            var level = getPlayer().level();
            RecipeHolder<?> recipe = level.getRecipeManager().byKey(id).orElse(null);
            readRecipe = id;
            readChances = recipe == null ? null : RecipeChances.of(recipe.value(), level.registryAccess());
        }
        return readChances;
    }

    private String recipeChancesBySlot(FakeSlot[] outs) {
        Map<String, Double> byId = chancesOf(host.state().getRecipe());
        if (byId == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outs.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            GenericStack out = GenericStack.fromItemStack(outs[i].getItem());
            if (out != null) {
                sb.append(byId.getOrDefault(out.what().getId().toString(), 1.0));
            }
        }
        return sb.toString();
    }

    /**
     * The probability of processing output slot {@code index} by the recipe JEI filled the slots
     * from (1 = always); null when the slots did not come from a known recipe.
     */
    public @Nullable Double getRecipeChance(int index) {
        String[] parts = recipeChances.split(";", -1);
        if (recipeChances.isEmpty() || index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(parts[index]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The face bits shown for processing input slot {@code index} (0 = not set). */
    public int getInputFaces(int index) {
        String[] parts = inputFaces.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Client: sets the faces of what is in processing input slot {@code index}. */
    public void setInputFaces(int index, int faces) {
        sendClientAction(SET_INPUT_FACES, index + ";" + faces);
    }

    private void receiveInputFaces(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            int faces = Integer.parseInt(parts[1]);
            FakeSlot[] ins = getProcessingInputSlots();
            if (index < 0 || index >= ins.length) {
                return;
            }
            GenericStack in = GenericStack.fromItemStack(ins[index].getItem());
            if (in != null) {
                host.state().setInputFaces(in.what(), faces);
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    private String nbtFlags(FakeSlot[] slots, boolean output) {
        StringBuilder sb = new StringBuilder();
        for (FakeSlot slot : slots) {
            GenericStack g = GenericStack.fromItemStack(slot.getItem());
            sb.append(g != null && host.state().ignoresNbt(g.what(), output) ? '1' : '0');
        }
        return sb.toString();
    }

    /** The NBT of what is in processing input ({@code output} false) or output slot {@code index} does not matter. */
    public boolean ignoresNbt(int index, boolean output) {
        String flags = output ? ignoreNbtOut : ignoreNbtIn;
        return index >= 0 && index < flags.length() && flags.charAt(index) == '1';
    }

    /** Client: whether the NBT of what is in that slot matters. */
    public void setIgnoreNbt(int index, boolean output, boolean ignore) {
        sendClientAction(SET_IGNORE_NBT, (output ? "o" : "i") + ";" + index + ";" + (ignore ? 1 : 0));
    }

    private void receiveIgnoreNbt(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 3) {
            return;
        }
        try {
            boolean output = parts[0].equals("o");
            int index = Integer.parseInt(parts[1]);
            FakeSlot[] slots = output ? getProcessingOutputSlots() : getProcessingInputSlots();
            if (index < 0 || index >= slots.length) {
                return;
            }
            GenericStack g = GenericStack.fromItemStack(slots[index].getItem());
            if (g != null) {
                host.state().setIgnoreNbt(g.what(), output, parts[2].equals("1"));
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /** Where processing input slot {@code index}'s material goes (0 = into the machine). */
    public int getInputTarget(int index) {
        String[] parts = inputTargets.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String machineId(ItemStack machine) {
        return machine.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(machine.getItem()).toString();
    }

    /** The GUI slot a sequence pattern for {@code machine} starts at (1 = the menu's first slot). */
    public int getSequenceStart(ItemStack machine) {
        String id = machineId(machine);
        for (String part : sequenceStarts.split(";")) {
            int eq = part.lastIndexOf('=');
            if (eq > 0 && part.substring(0, eq).equals(id)) {
                try {
                    return Integer.parseInt(part.substring(eq + 1));
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return 1;
    }

    /** Client: sets the GUI slot a sequence pattern for {@code machine} starts at. */
    public void setSequenceStart(ItemStack machine, int start) {
        sendClientAction(SET_SEQUENCE_START, machineId(machine) + "=" + start);
    }

    private void receiveSequenceStart(String arg) {
        int eq = arg.lastIndexOf('=');
        if (eq < 0) {
            return;
        }
        try {
            int start = Integer.parseInt(arg.substring(eq + 1));
            if (start >= 1 && start <= PatternInputGuiSlots.MAX) {
                host.state().setSequenceStart(arg.substring(0, eq), start);
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /** The GUI slot processing output slot {@code index}'s product is taken out of (0 = any result slot). */
    public int getOutputGuiSlot(int index) {
        String[] parts = outputGuiSlots.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Client: sets the GUI slot the product in processing output slot {@code index} is taken out of. */
    public void setOutputGuiSlot(int index, int slot) {
        sendClientAction(SET_OUTPUT_GUI_SLOT, index + ";" + slot);
    }

    private void receiveOutputGuiSlot(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            int slot = Integer.parseInt(parts[1]);
            FakeSlot[] outs = getProcessingOutputSlots();
            if (index < 0 || index >= outs.length || slot < 0 || slot > PatternOutputGuiSlots.MAX) {
                return;
            }
            GenericStack out = GenericStack.fromItemStack(outs[index].getItem());
            if (out != null) {
                host.state().setOutputGuiSlot(out.what(), slot);
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /** The GUI slot processing input slot {@code index}'s material goes into (0 = the first that takes it). */
    public int getInputGuiSlot(int index) {
        String[] parts = inputGuiSlots.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Client: sets the GUI slot the material in processing input slot {@code index} goes into. */
    public void setInputGuiSlot(int index, int slot) {
        sendClientAction(SET_INPUT_GUI_SLOT, index + ";" + slot);
    }

    private void receiveInputGuiSlot(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            int slot = Integer.parseInt(parts[1]);
            FakeSlot[] ins = getProcessingInputSlots();
            if (index < 0 || index >= ins.length || slot < 0 || slot > PatternInputGuiSlots.MAX) {
                return;
            }
            host.state().setInputGuiSlot(index, slot);
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /** the GUI slots of a pattern that was put in, waiting for the grid to be filled from it (they go to its filled slots in order) */
    private @Nullable List<Integer> pendingGuiSlots;

    private void applyPendingGuiSlots() {
        if (pendingGuiSlots == null) {
            return;
        }
        FakeSlot[] ins = getProcessingInputSlots();
        int filled = 0;
        for (FakeSlot s : ins) {
            if (GenericStack.fromItemStack(s.getItem()) != null) {
                filled++;
            }
        }
        if (filled == 0) {
            return;     // the grid is not filled yet
        }
        int n = 0;
        for (int i = 0; i < ins.length && n < pendingGuiSlots.size(); i++) {
            if (GenericStack.fromItemStack(ins[i].getItem()) != null) {
                host.state().setInputGuiSlot(i, pendingGuiSlots.get(n++));
            }
        }
        pendingGuiSlots = null;
    }

    /** What processing input slot {@code index}'s material is besides used up ({@link PatternInputRoles}; 0 = used up). */
    public int getInputRole(int index) {
        String[] parts = inputRoles.split(";", -1);
        if (index < 0 || index >= parts.length || parts[index].isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Client: sets what the material in processing input slot {@code index} is besides used up. */
    public void setInputRole(int index, int role) {
        sendClientAction(SET_INPUT_ROLE, index + ";" + role);
    }

    private void receiveInputRole(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            int role = Integer.parseInt(parts[1]);
            FakeSlot[] ins = getProcessingInputSlots();
            if (index < 0 || index >= ins.length || role < 0 || role >= PatternInputRoles.KINDS) {
                return;
            }
            GenericStack in = GenericStack.fromItemStack(ins[index].getItem());
            if (in != null) {
                host.state().setInputRole(in.what(), role);
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    /** Client: sets where what is in processing input slot {@code index} goes. */
    public void setInputTarget(int index, int target) {
        sendClientAction(SET_INPUT_TARGET, index + ";" + target);
    }

    private void receiveInputTarget(String arg) {
        String[] parts = arg.split(";");
        if (parts.length != 2) {
            return;
        }
        try {
            int index = Integer.parseInt(parts[0]);
            int target = Integer.parseInt(parts[1]);
            FakeSlot[] ins = getProcessingInputSlots();
            if (index < 0 || index >= ins.length) {
                return;
            }
            GenericStack in = GenericStack.fromItemStack(ins[index].getItem());
            if (in != null) {
                int kind = PatternInputTargets.kind(target);
                host.state().setInputTarget(in.what(), PatternInputTargets.of(kind, PatternInputTargets.distance(target)));
            }
        } catch (NumberFormatException e) {
            // a malformed request: ignore it
        }
    }

    public FakeSlot getMachineSlot() {
        return machineSlot;
    }

    public FakeSlot getToolSlot() {
        return toolSlot;
    }

    public List<FakeSlot> getSubMachineSlots() {
        return subMachineSlots;
    }

    @Override
    public void setMode(EncodingMode mode) {
        // the mode is fixed
    }
}
