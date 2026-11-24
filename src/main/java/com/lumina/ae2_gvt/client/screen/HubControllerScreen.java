package com.lumina.ae2_gvt.client.screen;

import java.math.BigInteger;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.menus.HubControllerMenu;
import com.lumina.ae2_gvt.hub.TwinStarHubBlockEntity;
import com.lumina.twinstar.client.TheoryScreen;
import com.lumina.corelib.util.BigAmountFormat;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The hexagram hub controller's GUI. Background: assets/ae2_gvt/textures/gui/twin_star_hub.png (made
 * by tools/hub_controller_gui_texture.py); positions: assets/ae2/screens/twin_star_hub.json.
 * <ul>
 *   <li>top: the black-hole unit slot with the capacity it gives, the white-hole slot with the
 *       parallel count; the title line tells whether the hub is formed and running, and its ports</li>
 *   <li>patterns: as the test hub's (up to 50,000, the stored ones plus one empty slot, four rows at a time)</li>
 *   <li>left toolbar: the theory viewer</li>
 * </ul>
 */
public class HubControllerScreen extends AEBaseScreen<HubControllerMenu> {
    private static final int COLUMNS = HubControllerMenu.COLUMNS, VISIBLE_ROWS = HubControllerMenu.VISIBLE_ROWS;
    private static final int LABEL = 0xFF404040, VALUE = 0xFF2A2A40, COUNT = 0xFF606070;
    /** running: blue; not running: orange (told apart without red / green, and in words) */
    private static final int ON = 0xFF2F6FB0, OFF = 0xFFB0601C;
    private static final Blitter SLOT_FRAME = Blitter.texture(
            new ResourceLocation(AE2GravityTech.MODID, "textures/gui/twin_star_hub.png"), 256, 256).src(200, 0, 19, 19);

    private final Scrollbar scrollbar;
    private int shown;

    public HubControllerScreen(HubControllerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);
        // the engine's theory layer for this hub's network; comes back to this screen
        addToLeftToolbar(TheoryScreen.button(0, 0, () -> TheoryScreen.open(null, () -> this)));
        // what this network's machines used up lately; comes back to this screen
        addToLeftToolbar(com.lumina.twinstar.client.UsageScreen.button(0, 0, () -> com.lumina.twinstar.client.UsageScreen.open(() -> this)));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        // the stored patterns are packed at the front: show them and the first empty slot
        shown = Math.min(TwinStarHubBlockEntity.MAX_PATTERNS, menu.stored + 1);
        int rows = (shown + COLUMNS - 1) / COLUMNS;
        scrollbar.setRange(0, Math.max(0, rows - VISIBLE_ROWS), 1);
        int scroll = scrollbar.getCurrentScroll();
        menu.setScrollRow(scroll);
        repositionSlots(SlotSemantics.ENCODED_PATTERN);
        List<Slot> slots = menu.getPatternSlots();
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i) instanceof AppEngSlot slot)
                slot.setActive(scroll * COLUMNS + i < shown);
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        for (Slot slot : menu.getPatternSlots())
            if (slot.isActive())
                SLOT_FRAME.dest(offsetX + slot.x - 1, offsetY + slot.y - 1).blit(g);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        // units: what each side gives
        g.drawString(font, Component.translatable("gui.ae2_gvt.twin_star_hub.black"), 31, 22, LABEL, false);
        g.drawString(font, value("capacity", menu.capacity), 31, 33, VALUE, false);
        g.drawString(font, Component.translatable("gui.ae2_gvt.twin_star_hub.white"), 118, 22, LABEL, false);
        g.drawString(font, value("parallel", menu.parallel), 118, 33, VALUE, false);
        // state, right of the title
        Component state = Component.translatable(menu.active ? "gui.ae2_gvt.twin_star_hub.state.running"
                : !menu.formed ? "gui.ae2_gvt.twin_star_hub.state.not_formed"
                : menu.ports == 0 ? "gui.ae2_gvt.twin_star_hub.state.no_port"
                : menu.capacity.isEmpty() || menu.parallel.isEmpty() ? "gui.ae2_gvt.twin_star_hub.state.no_units"
                : "gui.ae2_gvt.twin_star_hub.state.no_power");
        g.drawString(font, state, 187 - font.width(state), 6, menu.active ? ON : OFF, false);
        Component count = Component.literal(String.valueOf(Math.max(0, shown - 1)));
        g.drawString(font, count, 187 - font.width(count), 51, COUNT, false);
    }

    /** "容量 4.3G", "容量 ∞", or "沒有單元". */
    private static Component value(String what, String amount) {
        if (amount.isEmpty())
            return Component.translatable("gui.ae2_gvt.twin_star_hub.no_unit");
        String shortText = amount.equals("∞") ? amount : BigAmountFormat.unitForm(new BigInteger(amount));
        return Component.translatable("gui.ae2_gvt.twin_star_hub." + what + ".value", shortText);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        // the exact numbers over the short ones
        int lx = x - leftPos, ly = y - topPos;
        String exact = ly >= 31 && ly < 42 ? lx >= 31 && lx < 94 ? menu.capacity : lx >= 118 && lx < 188 ? menu.parallel : null : null;
        if (exact != null && !exact.isEmpty() && !exact.equals("∞"))
            g.renderTooltip(font, Component.literal(java.text.NumberFormat.getInstance().format(new BigInteger(exact))), x, y);
    }
}
