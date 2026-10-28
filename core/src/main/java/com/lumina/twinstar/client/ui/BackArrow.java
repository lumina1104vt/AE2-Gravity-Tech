package com.lumina.twinstar.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

/** The ↩ of the back button, drawn in pixels (the font's glyph is too small at this size). */
public class BackArrow extends UIElement {
    private static final int COLOR = 0xFFDEDEEA;

    public BackArrow() {
        layout(layout -> layout.width(9).height(8).flexShrink(0));
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        int x = (int) getPositionX(), y = (int) getPositionY();
        var g = context.graphics;
        // a hook from the top right, down, then left to the arrow head
        g.fill(x + 7, y, x + 9, y + 6, COLOR);
        g.fill(x + 2, y + 4, x + 9, y + 6, COLOR);
        g.fill(x + 1, y + 3, x + 2, y + 7, COLOR);
        g.fill(x, y + 4, x + 1, y + 6, COLOR);
        g.fill(x + 2, y + 2, x + 3, y + 8, COLOR);
    }
}
