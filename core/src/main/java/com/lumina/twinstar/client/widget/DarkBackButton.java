package com.lumina.twinstar.client.widget;

import appeng.client.gui.implementations.AESubScreen;
import appeng.menu.ISubMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Back to the terminal the screen was opened from (as AE2's back button), with a drawn ↩ arrow. */
public class DarkBackButton extends DarkButton {
    public DarkBackButton(ISubMenu menu) {
        super(Component.empty(), AESubScreen::goBack);
        setTooltip(Tooltip.create(menu.getHost().getMainMenuIcon().getHoverName()));
    }

    @Override
    protected void renderContent(GuiGraphics g, int x, int y, int w, int h) {
        int c = DarkStyle.TEXT;
        int ox = x + (w - 9) / 2, oy = y + (h - 8) / 2;
        // ↩: a hook from the top right, down, then left to the arrow head
        g.fill(ox + 7, oy, ox + 9, oy + 6, c);
        g.fill(ox + 2, oy + 4, ox + 9, oy + 6, c);
        g.fill(ox + 1, oy + 3, ox + 2, oy + 7, c);
        g.fill(ox, oy + 4, ox + 1, oy + 6, c);
        g.fill(ox + 2, oy + 2, ox + 3, oy + 8, c);
    }
}
