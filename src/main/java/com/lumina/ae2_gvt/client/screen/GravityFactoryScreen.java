package com.lumina.ae2_gvt.client.screen;

import java.util.List;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.lumina.ae2_gvt.menus.GravityFactoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 引力加工工廠 GUI. Background: assets/ae2_gvt/textures/gui/gravity_processing_factory.png (made by
 * tools/factory_gui_texture.py, which also prints ARROWS); positions:
 * assets/ae2/screens/gravity_processing_factory.json.
 * <p>
 * Slots only (user): 12 inputs and 12 outputs (4 x 3 each, so the panel is 208 wide), the gravity
 * well and the catalyst in between. Progress: the eight arrows round the well fill from the outside in, all together, like
 * AE2's progress arrows filling in the direction of flow.
 */
public class GravityFactoryScreen extends AEBaseScreen<GravityFactoryMenu> {
    private static final int WELL_X = 104, WELL_Y = 34;
    private static final int ARROW_LIT = 0xFF5F647E;

    /** Arrow pixels, (dx, dy) from the well centre - from tools/factory_gui_texture.py. */
    private static final int[] ARROWS = {
            -12, -3, -12, 3, -11, -6, -11, -3, -11, -2, -11, 2, -11, 3, -11, 6,
            -10, -6, -10, -5, -10, -3, -10, -2, -10, -1, -10, 0, -10, 1, -10, 2,
            -10, 3, -10, 5, -10, 6, -9, -6, -9, -5, -9, -3, -9, -2, -9, -1,
            -9, 0, -9, 1, -9, 2, -9, 3, -9, 5, -9, 6, -8, -6, -8, -5,
            -8, -1, -8, 0, -8, 1, -8, 5, -8, 6, -7, -7, -7, -6, -7, -5,
            -7, 5, -7, 6, -7, 7, -6, -11, -6, -10, -6, -9, -6, -8, -6, -7,
            -6, -6, -6, -5, -6, 5, -6, 6, -6, 7, -6, 8, -6, 9, -6, 10,
            -6, 11, -5, -10, -5, -9, -5, -8, -5, -7, -5, -6, -5, 6, -5, 7,
            -5, 8, -5, 9, -5, 10, -3, -12, -3, -11, -3, -10, -3, -9, -3, 9,
            -3, 10, -3, 11, -3, 12, -2, -11, -2, -10, -2, -9, -2, 9, -2, 10,
            -2, 11, -1, -10, -1, -9, -1, -8, -1, 8, -1, 9, -1, 10, 0, -10,
            0, -9, 0, -8, 0, 8, 0, 9, 0, 10, 1, -10, 1, -9, 1, -8,
            1, 8, 1, 9, 1, 10, 2, -11, 2, -10, 2, -9, 2, 9, 2, 10,
            2, 11, 3, -12, 3, -11, 3, -10, 3, -9, 3, 9, 3, 10, 3, 11,
            3, 12, 5, -10, 5, -9, 5, -8, 5, -7, 5, -6, 5, 6, 5, 7,
            5, 8, 5, 9, 5, 10, 6, -11, 6, -10, 6, -9, 6, -8, 6, -7,
            6, -6, 6, -5, 6, 5, 6, 6, 6, 7, 6, 8, 6, 9, 6, 10,
            6, 11, 7, -7, 7, -6, 7, -5, 7, 5, 7, 6, 7, 7, 8, -6,
            8, -5, 8, -1, 8, 0, 8, 1, 8, 5, 8, 6, 9, -6, 9, -5,
            9, -3, 9, -2, 9, -1, 9, 0, 9, 1, 9, 2, 9, 3, 9, 5,
            9, 6, 10, -6, 10, -5, 10, -3, 10, -2, 10, -1, 10, 0, 10, 1,
            10, 2, 10, 3, 10, 5, 10, 6, 11, -6, 11, -3, 11, -2, 11, 2,
            11, 3, 11, 6, 12, -3, 12, 3,
    };
    private static final float ARROW_MIN_R, ARROW_MAX_R;

    static {
        float min = Float.MAX_VALUE, max = 0;
        for (int i = 0; i < ARROWS.length; i += 2) {
            float r = (float) Math.hypot(ARROWS[i], ARROWS[i + 1]);
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        ARROW_MIN_R = min;
        ARROW_MAX_R = max;
    }

    public GravityFactoryScreen(GravityFactoryMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        // arrows: lit from the outside in
        float progress = menu.progress / 1000f;
        if (progress > 0) {
            float edge = ARROW_MAX_R - (ARROW_MAX_R - ARROW_MIN_R) * progress;
            for (int i = 0; i < ARROWS.length; i += 2) {
                if (Math.hypot(ARROWS[i], ARROWS[i + 1]) >= edge - 1e-3) {
                    int x = offsetX + WELL_X + ARROWS[i], y = offsetY + WELL_Y + ARROWS[i + 1];
                    g.fill(x, y, x + 1, y + 1, ARROW_LIT);
                }
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        int mx = x - leftPos, my = y - topPos;
        if (hoveredSlot == null && Math.abs(mx - WELL_X) <= 13 && Math.abs(my - WELL_Y) <= 13) {
            drawTooltip(g, x, y, List.of(Component.translatable("gui.ae2_gvt.factory.progress", menu.progress / 10)));
        } else {
            super.renderTooltip(g, x, y);
        }
    }
}
