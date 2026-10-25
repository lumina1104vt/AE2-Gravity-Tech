package com.lumina.twinstar.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A button of the twin-star screens: dark fill, violet edge that lights up under the mouse. */
public class DarkButton extends Button {
    public DarkButton(Component message, OnPress onPress) {
        super(0, 0, 0, 0, message, onPress, DEFAULT_NARRATION);
    }

    public DarkButton(Component message, Runnable onPress) {
        this(message, b -> onPress.run());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hot = active && isHovered();
        int edge = !active ? DarkStyle.DISABLED_EDGE : hot ? DarkStyle.HOVER_EDGE : DarkStyle.EDGE;
        int fill = !active ? DarkStyle.DISABLED_FILL : hot ? DarkStyle.HOVER_FILL : DarkStyle.FILL;
        g.fill(x, y, x + w, y + h, edge);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (active)
            g.fill(x + 1, y + 1, x + w - 1, y + 2, DarkStyle.HIGHLIGHT);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, DarkStyle.SHADOW);
        renderContent(g, x, y, w, h);
    }

    /** The label, centred. */
    protected void renderContent(GuiGraphics g, int x, int y, int w, int h) {
        Font font = Minecraft.getInstance().font;
        Component text = getMessage();
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2,
                active ? DarkStyle.TEXT : DarkStyle.TEXT_DISABLED, false);
    }
}
