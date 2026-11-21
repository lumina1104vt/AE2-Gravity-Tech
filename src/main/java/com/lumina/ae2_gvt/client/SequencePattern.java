package com.lumina.ae2_gvt.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * A sequence pattern: the pattern of a recipe whose materials must go into their places (an ordered crafting table).
 * It is no pattern of its own: the usual synthesis pattern, written with every material slot holding one item and
 * its own GUI slot number, the place of that slot in the recipe, so what the terminal's dialogs say about the slots is
 * there from the start. Made from a recipe by the button JEI and EMI show beside it.
 */
public final class SequencePattern {
    public static final ResourceLocation TEXTURE = new ResourceLocation(AE2GravityTech.MODID, "textures/gui/pattern_buttons.png");

    private SequencePattern() {
    }

    /** The twin-star pattern terminal the player has open, or null. */
    public static @Nullable TwinStarPatternTermMenu terminal() {
        var player = Minecraft.getInstance().player;
        return player != null && player.containerMenu instanceof TwinStarPatternTermMenu menu ? menu : null;
    }

    public static Component tooltip() {
        return Component.translatable(terminal() != null ? "ae2_gvt.jei.sequence_pattern" : "ae2_gvt.jei.sequence_pattern.no_terminal");
    }

    /**
     * A recipe to make a sequence pattern of: one entry per slot of the recipe in order, null for an empty one; what it
     * makes; the machine it is for (empty: none); the recipe, for the probabilities it gives (null: none).
     */
    public record Request(List<@Nullable GenericStack> inputs, List<GenericStack> outputs, ItemStack machine, @Nullable ResourceLocation recipeId) {
    }

    /** The recipe fits the open terminal's slots. */
    private static boolean fits(TwinStarPatternTermMenu menu, Request r) {
        long filled = r.inputs().stream().filter(s -> s != null).count();
        return filled > 0 && filled <= menu.getProcessingInputSlots().length
                && !r.outputs().isEmpty() && r.outputs().size() <= menu.getProcessingOutputSlots().length;
    }

    /**
     * What the button does: asks which GUI slot of the machine the first slot of the recipe goes into (a table whose
     * output is its first slot starts at 2; remembered per machine), then {@link #fill}. False when there is no
     * terminal open or the recipe does not fit.
     */
    public static boolean open(Request request) {
        TwinStarPatternTermMenu menu = terminal();
        if (menu == null || !fits(menu, request)) {
            return false;
        }
        var mc = Minecraft.getInstance();
        mc.setScreen(new com.lumina.ae2_gvt.client.screen.SequenceStartScreen(mc.screen, request, menu.getSequenceStart(request.machine())));
        return true;
    }

    /**
     * Writes the pattern into the open terminal: the filled recipe slots go one after the other into the terminal's material
     * slots (no gaps: the place of a slot is its GUI slot number, {@code start} + the recipe slot's position), each holding
     * one item, or the amount of a fluid. The start is kept for that machine. The recipe screen is closed, back to the terminal.
     */
    public static boolean fill(Request r, int start) {
        TwinStarPatternTermMenu menu = terminal();
        if (menu == null || !fits(menu, r)) {
            return false;
        }
        FakeSlot[] ins = menu.getProcessingInputSlots();
        FakeSlot[] outs = menu.getProcessingOutputSlots();
        menu.setSequenceStart(r.machine(), start);
        menu.setMode(EncodingMode.PROCESSING);
        menu.clear();
        int next = 0;
        for (int i = 0; i < r.inputs().size(); i++) {
            GenericStack stack = r.inputs().get(i);
            if (stack == null) {
                continue;
            }
            // one item in each place; a fluid keeps its amount
            GenericStack one = stack.what() instanceof AEItemKey ? new GenericStack(stack.what(), 1) : stack;
            set(ins[next], one);
            menu.setInputGuiSlot(next, start + i);
            next++;
        }
        for (int i = 0; i < r.outputs().size(); i++) {
            set(outs[i], r.outputs().get(i));
        }
        menu.getMachineSlot().setFilterTo(r.machine());   // an empty stack clears the slot
        menu.setRecipeChances(r.recipeId());
        // back to the terminal, as JEI's [+] does: this dialog and the recipe screen (JEI's, EMI's) are no container screens
        var mc = Minecraft.getInstance();
        for (int guard = 0; guard < 4 && mc.screen != null
                && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>); guard++) {
            mc.screen.onClose();
        }
        return true;
    }

    private static void set(FakeSlot slot, GenericStack stack) {
        appeng.core.sync.network.NetworkHandler.instance().sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index, GenericStack.wrapInItemStack(stack)));
    }
}
