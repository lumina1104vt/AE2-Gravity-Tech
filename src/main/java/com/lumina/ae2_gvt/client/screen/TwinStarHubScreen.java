/*
 * PROTOTYPE - Twin-Star Hub GUI, hosted by the prototype controller. Delete with the prototype package.
 */
package com.lumina.ae2_gvt.client.screen;

import java.util.List;

import com.lumina.ae2_gvt.prototype.twinstar.gui.TwinStarHubMenu;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Twin-Star Hub GUI. Background: assets/ae2_gvt/textures/gui/prototype_twin_star_hub.png (made by
 * tools/prototype_hub_gui_texture.py); positions: assets/ae2/screens/prototype_twin_star_hub.json.
 * The numbers other than the pattern count are placeholders ("-") until the hub has real units.
 */
public class TwinStarHubScreen extends AEBaseScreen<TwinStarHubMenu> {
    private static final int COLUMNS = 9, VISIBLE_ROWS = 4;
    private static final int ON_BLACK = 0xFFECECF0, ON_BLACK_DIM = 0xFFA0A2B0;
    private static final int ON_WHITE = 0xFF1E1E24, ON_WHITE_DIM = 0xFF6E7080;
    private static final int ON_TABLE = 0xFF28282F, ON_TABLE_DIM = 0xFF787A8A;
    private static final int STATUS_OK = 0xFF2E7D46;
    private static final Component NONE = Component.literal("-");
    private static final String[] TIERS = {"singular", "imaginary", "paradox", "planck"};

    private final Scrollbar scrollbar;

    public TwinStarHubScreen(TwinStarHubMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);
        int rows = (menu.getPatternSlots().size() + COLUMNS - 1) / COLUMNS;
        this.scrollbar.setRange(0, Math.max(0, rows - VISIBLE_ROWS), 1);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        // scroll the pattern grid: every slot gets its grid position, then the window is shifted
        this.repositionSlots(SlotSemantics.ENCODED_PATTERN);
        List<Slot> slots = this.menu.getPatternSlots();
        int scroll = this.scrollbar.getCurrentScroll();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) instanceof AppEngSlot slot) {
                int row = i / COLUMNS - scroll;
                slot.setActive(row >= 0 && row < VISIBLE_ROWS);
                slot.y -= scroll * 18;
            }
        }
    }

    @Override
    public void drawBG(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawBridgePulse(guiGraphics, offsetX, offsetY);
    }

    // same idea as the controller block: a white pulse runs black -> white, then a black one runs back
    private static void drawBridgePulse(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY) {
        double phase = (System.currentTimeMillis() % 4800L) / 4800.0;
        boolean whiteRun = phase < 0.5;
        double along = (phase * 2) % 1;
        double head = 92 + (whiteRun ? along : 1 - along) * 11;
        int target = whiteRun ? 255 : 16;
        for (int x = 92; x <= 103; x++) {
            double glow = Math.exp(-Math.pow((x - head) / 2.2, 2));
            int v = (int) Math.round(110 + (target - 110) * glow);
            guiGraphics.fill(offsetX + x, offsetY + 46, offsetX + x + 1, offsetY + 49, 0xFF000000 | v << 16 | v << 8 | v);
        }
    }

    @Override
    public void drawFG(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        right(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.status.running"), 187, 6, STATUS_OK);

        // black hole = crafting side
        text(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.black"), 34, 23, ON_BLACK);
        int stored = 0;
        for (Slot slot : this.menu.getPatternSlots()) {
            if (slot.hasItem()) {
                stored++;
            }
        }
        row(guiGraphics, "patterns", Component.literal(stored + " / " + this.menu.getPatternSlots().size()), 13, 89, 43, ON_BLACK_DIM, ON_BLACK);
        row(guiGraphics, "jobs", NONE, 13, 89, 53, ON_BLACK_DIM, ON_BLACK);
        row(guiGraphics, "cpus", NONE, 13, 89, 63, ON_BLACK_DIM, ON_BLACK);

        // white hole = computing side
        text(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.white"), 107, 23, ON_WHITE);
        row(guiGraphics, "parallel", NONE, 107, 183, 43, ON_WHITE_DIM, ON_WHITE);
        row(guiGraphics, "capacity", NONE, 107, 183, 53, ON_WHITE_DIM, ON_WHITE);
        row(guiGraphics, "usage", NONE, 107, 183, 63, ON_WHITE_DIM, ON_WHITE);

        // units by tier
        for (int i = 0; i < TIERS.length; i++) {
            int x = 76 + i * 32;
            centred(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.tier." + TIERS[i]), x, 85, ON_TABLE);
            centred(guiGraphics, Component.literal("0"), x, 94, ON_TABLE_DIM);
            centred(guiGraphics, Component.literal("0"), x, 102, ON_TABLE_DIM);
        }
        text(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.units.black"), 11, 94, ON_TABLE);
        text(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub.units.white"), 11, 102, ON_TABLE);
    }

    private void row(GuiGraphicsExtractor guiGraphics, String key, Component value, int left, int right, int y, int labelColour, int valueColour) {
        text(guiGraphics, Component.translatable("gui.ae2_gvt.twin_star_hub." + key), left, y, labelColour);
        right(guiGraphics, value, right, y, valueColour);
    }

    private void text(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int colour) {
        guiGraphics.text(this.font, text, x, y, colour, false);
    }

    private void right(GuiGraphicsExtractor guiGraphics, Component text, int rightX, int y, int colour) {
        guiGraphics.text(this.font, text, rightX - this.font.width(text), y, colour, false);
    }

    private void centred(GuiGraphicsExtractor guiGraphics, Component text, int centreX, int y, int colour) {
        guiGraphics.text(this.font, text, centreX - this.font.width(text) / 2, y, colour, false);
    }
}
