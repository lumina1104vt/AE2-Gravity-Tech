package com.lumina.twinstar.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** An on/off switch with a label: a 22x12 track whose knob slides right (violet) when on. */
public class DarkToggle extends AbstractButton {
    private boolean selected;
    private final Runnable onChange;

    public DarkToggle(Component label, boolean selected, Runnable onChange) {
        super(0, 0, 0, 0, label);
        this.selected = selected;
        this.onChange = onChange;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void onPress() {
        selected = !selected;
        onChange.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        boolean hot = isHovered();
        int edge = selected ? DarkStyle.TOGGLE_ON_EDGE : DarkStyle.EDGE;
        g.fill(x, y, x + 22, y + 12, hot ? DarkStyle.HOVER_EDGE : edge);
        g.fill(x + 1, y + 1, x + 21, y + 11, selected ? DarkStyle.TOGGLE_ON_FILL : DarkStyle.TOGGLE_OFF_FILL);
        int knob = selected ? x + 12 : x + 2;
        g.fill(knob, y + 2, knob + 8, y + 10, selected ? DarkStyle.TOGGLE_ON_KNOB : DarkStyle.TOGGLE_OFF_KNOB);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), x + 26, y + 2, DarkStyle.TEXT, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
