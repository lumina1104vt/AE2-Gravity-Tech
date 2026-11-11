package com.lumina.ae2_gvt.client.screen;

import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.AECheckbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** The "ignore NBT" checkbox of the amount dialogs' advanced part: sent at once, like the faces. */
final class IgnoreNbt {
    private IgnoreNbt() {
    }

    static void add(WidgetContainer widgets, TwinStarPatternTermMenu menu, int index, boolean output) {
        AECheckbox[] box = new AECheckbox[1];
        box[0] = widgets.addCheckbox("ignoreNbt", Component.translatable("ae2_gvt.pattern.ignore_nbt"),
                () -> menu.setIgnoreNbt(index, output, box[0].isSelected()));
        box[0].setSelected(menu.ignoresNbt(index, output));
        box[0].setTooltip(Tooltip.create(Component.translatable(output ? "ae2_gvt.pattern.ignore_nbt.output"
                : "ae2_gvt.pattern.ignore_nbt.input")));
    }
}
