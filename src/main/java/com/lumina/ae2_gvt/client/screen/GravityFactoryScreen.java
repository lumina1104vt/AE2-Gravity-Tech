package com.lumina.ae2_gvt.client.screen;

import java.util.List;

import appeng.client.gui.AEBaseScreen;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import com.lumina.ae2_gvt.blocks.Blocks;
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
        // AE2's upgrade panel on the right, as on AE2's own machines
        widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE),
                () -> Upgrades.getTooltipLinesForMachine(Blocks.GRAVITY_FACTORY.get())));
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawArrows(g, offsetX + WELL_X, offsetY + WELL_Y, menu.progress / 1000f);
    }

    /** The eight arrows round the well at x, y, lit from the outside in as far as progress (0 … 1). */
    public static void drawArrows(GuiGraphics g, int x, int y, float progress) {
        if (progress <= 0)
            return;
        float edge = ARROW_MAX_R - (ARROW_MAX_R - ARROW_MIN_R) * progress;
        for (int i = 0; i < ARROWS.length; i += 2)
            if (Math.hypot(ARROWS[i], ARROWS[i + 1]) >= edge - 1e-3)
                g.fill(x + ARROWS[i], y + ARROWS[i + 1], x + ARROWS[i] + 1, y + ARROWS[i + 1] + 1, ARROW_LIT);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        int mx = x - leftPos, my = y - topPos;
        if (hoveredSlot == null && Math.abs(mx - WELL_X) <= 13 && Math.abs(my - WELL_Y) <= 13) {
            drawTooltip(g, x, y, List.of(Component.translatable("gui.ae2_gvt.factory.progress", menu.progress / 10),
                    Component.translatable("gui.ae2_gvt.factory.parallel", java.text.NumberFormat.getInstance().format(menu.parallel)),
                    Component.translatable("gui.ae2_gvt.factory.energy", java.text.NumberFormat.getInstance().format(menu.energy),
                            java.text.NumberFormat.getInstance().format(menu.energyCapacity))));
        } else {
            super.renderTooltip(g, x, y);
        }
    }
}
