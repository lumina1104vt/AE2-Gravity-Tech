package com.lumina.ae2_gvt.client.screen;

import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;
import com.lumina.ae2_gvt.terminal.TwinStarTerminal;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import com.lumina.twinstar.algorithm.ae2.PatternInputGuiSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The sub-machines of the pattern being encoded: nine ghost slots of the terminal's menu, the kinds of
 * block near the machine (an altar's pedestals) that materials set to go to sub-machines go into, left
 * to right. Items go in from the inventory below (or
 * JEI), an empty hand takes one out. Positions from assets/ae2/screens/ae2_gvt_sub_machines.json.
 */
public class SubMachineScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    public SubMachineScreen(TwinStarPatternTermScreen parent) {
        super(parent, "/screens/ae2_gvt_sub_machines.json");
        ItemStack icon = getMenu().getHost().getMainMenuIcon();
        widgets.add("back", new TabButton(Icon.BACK, icon.getHoverName(), btn -> returnToParent()));
        var tip = net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ae2_gvt.sequence_start.hint"));
        var less = widgets.addButton("startLess", Component.literal("-"), () -> moveStart(-1));
        var more = widgets.addButton("startMore", Component.literal("+"), () -> moveStart(1));
        less.setTooltip(tip);
        more.setTooltip(tip);
    }

    /** The first GUI slot of the machine in the machine slot that a sequence pattern fills. */
    private void moveStart(int by) {
        ItemStack machine = getMenu().getMachineSlot().getItem();
        int start = Math.max(1, Math.min(PatternInputGuiSlots.MAX, getMenu().getSequenceStart(machine) + by));
        getMenu().setSequenceStart(machine, start);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
        setSlotsHidden(TwinStarTerminal.SUB_MACHINE_SLOTS, false);
        setSlotsHidden(TwinStarTerminal.TOOL_SLOT, true);
    }

    /** The place of each slot, small in its corner: the order they are tried in. */
    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        Component label = Component.translatable("ae2_gvt.sequence_start", getMenu().getSequenceStart(getMenu().getMachineSlot().getItem()));
        guiGraphics.drawString(font, label, 8, 100, 0x404040, false);
        var slots = getMenu().getSubMachineSlots();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.hasItem()) {
                continue;
            }
            String n = String.valueOf(i + 1);
            guiGraphics.drawString(font, n, slot.x + (16 - font.width(n)) / 2, slot.y + 4, 0x808080, false);
        }
    }
}
