package com.lumina.ae2_gvt.client.screen;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.menus.DummyCpuMenu;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Dummy CPU GUI. Background: assets/ae2_gvt/textures/gui/dummy_twinstar_cpu.png (made by
 * tools/dummy_cpu_gui_texture.py); positions: assets/ae2/screens/dummy_twinstar_cpu.json.
 * <ul>
 *   <li>capacity / parallel: digits only, empty (or 0) = unlimited, shown as the ∞ placeholder</li>
 *   <li>patterns: up to 50,000; the stored ones plus one empty slot are shown, four rows at a time.
 *       The menu has only the visible slots; scrolling moves them over the patterns (server side)</li>
 * </ul>
 */
public class DummyCpuScreen extends AEBaseScreen<DummyCpuMenu> {
    private static final int COLUMNS = DummyCpuMenu.COLUMNS, VISIBLE_ROWS = DummyCpuMenu.VISIBLE_ROWS;
    private static final int LABEL = 0xFF404040, COUNT = 0xFF606070;
    private static final Blitter SLOT_FRAME = Blitter.texture(
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/dummy_twinstar_cpu.png"), 256, 256).src(200, 0, 19, 19);

    private final Scrollbar scrollbar;
    private final AETextField capacity;
    private final AETextField parallel;
    private int shown;

    public DummyCpuScreen(DummyCpuMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);
        this.capacity = numberField("capacity", menu::setCapacity);
        this.parallel = numberField("parallel", menu::setParallel);
    }

    /** Any size: plain digits, or units (K…Q) and scientific notation (3E, 2e18, 5K); empty = unlimited. */
    private AETextField numberField(String id, Consumer<String> onChange) {
        AETextField field = widgets.addTextField(id);
        field.setMaxLength(1024);
        field.setFilter(s -> s.matches("[0-9.eEkKmMgGtTpPzZyYrRqQ_]*"));
        field.setPlaceholder(Component.literal("∞"));
        field.setTooltipMessage(List.of(Component.translatable("gui.ae2_gvt.dummy_cpu." + id),
                Component.translatable("gui.ae2_gvt.dummy_cpu.unlimited_hint")));
        field.setResponder(onChange);
        return field;
    }

    /** Puts the synced value in the field unless the player is typing in it. */
    private static void sync(AETextField field, Supplier<String> value) {
        if (field.isFocused()) {
            return;
        }
        String v = value.get();
        String text = v == null || v.equals("0") ? "" : v;
        if (!field.getValue().equals(text)) {
            field.setValue(text);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        sync(capacity, () -> menu.capacity);
        sync(parallel, () -> menu.parallel);

        // the stored patterns are packed at the front: show them and the first empty slot
        shown = Math.min(DummyTwinStarCPUBlockEntity.MAX_PATTERNS, menu.stored + 1);
        int rows = (shown + COLUMNS - 1) / COLUMNS;
        scrollbar.setRange(0, Math.max(0, rows - VISIBLE_ROWS), 1);
        int scroll = scrollbar.getCurrentScroll();
        menu.setScrollRow(scroll);     // the server moves the window and sends its contents

        repositionSlots(SlotSemantics.ENCODED_PATTERN);
        List<Slot> slots = menu.getPatternSlots();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) instanceof AppEngSlot slot) {
                slot.setActive(scroll * COLUMNS + i < shown);
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        for (Slot slot : menu.getPatternSlots()) {
            if (slot.isActive()) {
                SLOT_FRAME.dest(offsetX + slot.x - 1, offsetY + slot.y - 1).blit(g);
            }
        }
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("gui.ae2_gvt.dummy_cpu.capacity"), 12, 22, LABEL, false);
        g.drawString(font, Component.translatable("gui.ae2_gvt.dummy_cpu.parallel"), 12, 36, LABEL, false);
        Component count = Component.literal(String.valueOf(Math.max(0, shown - 1)));
        g.drawString(font, count, 187 - font.width(count), 51, COUNT, false);
    }
}
