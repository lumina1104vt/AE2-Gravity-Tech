package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.menus.ArmorModuleMenu;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.items.armor.data.InstalledModule;

import com.lumina.twinstar.client.hover.KeyHit;
import com.lumina.twinstar.client.hover.KeyHover;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;

/**
 * The armor module screen, on AE2's framework and made of AE2's own GUI art
 * (assets/ae2_gvt/textures/gui/armor_modules.png from tools/armor_gui_texture.py, positions in
 * assets/ae2/screens/armor_modules.json): the worn pieces as AE2 tab buttons in a column on the
 * left of the panel (only the pieces worn; the selected one framed black / white), the selected
 * piece's modules in a
 * crafting-CPU-style list (card, name, AE2's switch recoloured black / white, cog, clear), the
 * install slot in its gravity well, the inventory below. No text besides the title and the names.
 */
public class ArmorModuleScreen extends AEBaseScreen<ArmorModuleMenu> implements KeyHover {
    private static final int TAB_X = 5, TAB_Y = 22, TAB_STEP = 22, TAB = 20;
    private static final int ROW_X = 32, ROW_Y = 41, ROW_STEP = 23, ROW_W = 144, ROW_H = 22, ROWS = 4;
    private static final int SWITCH_X = ROW_X + 86, COG_X = ROW_X + 108, CLEAR_X = ROW_X + 125;
    private static final int TEXT = 0xFF404040, FRAME_OUT = 0xFF14141A, FRAME_IN = 0xFFF4F4FA;

    private static final Blitter SHEET = Blitter.texture(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/armor_modules.png"));
    private static final Blitter ROW = SHEET.copy().src(0, 234, ROW_W, ROW_H);
    private static final Blitter SWITCH_ON = SHEET.copy().src(204, 0, 22, 12);
    private static final Blitter SWITCH_OFF = SHEET.copy().src(204, 12, 22, 12);
    private static final Blitter TAB_BG = Icon.TAB_BUTTON_BACKGROUND.getBlitter();
    private static final Blitter TAB_BG_SELECTED = Icon.TAB_BUTTON_BACKGROUND_FOCUS.getBlitter();
    private static final Blitter COG = Icon.COG.getBlitter();
    private static final Blitter CLEAR = Icon.CLEAR.getBlitter();

    private final Scrollbar scrollbar;

    public ArmorModuleScreen(ArmorModuleMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ItemStack armor = menu.selectedStack();
        if (!armor.isEmpty()) {
            setTextContent(TEXT_ID_DIALOG_TITLE, armor.getHoverName());
        }
        scrollbar.setRange(0, Math.max(0, menu.installed().size() - ROWS), 1);
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        List<ArmorItem.Type> worn = menu.worn();
        for (int i = 0; i < worn.size(); i++) {
            int x = offsetX + TAB_X, y = offsetY + TAB_Y + i * TAB_STEP;
            boolean selected = worn.get(i) == menu.selected();
            if (selected) {                                    // our black / white frame round the selected piece
                g.fill(x - 2, y - 2, x + TAB + 2, y + TAB + 2, FRAME_OUT);
                g.fill(x - 1, y - 1, x + TAB + 1, y + TAB + 1, FRAME_IN);
            }
            (selected ? TAB_BG_SELECTED : TAB_BG).copy().dest(x, y).blit(g);
            g.renderItem(minecraft.player.getItemBySlot(worn.get(i).getSlot()), x + 2, y + 2);
        }
        List<InstalledModule> list = new ArrayList<>(menu.installed());
        int first = scrollbar.getCurrentScroll();
        for (int r = 0; r < ROWS && first + r < list.size(); r++) {
            InstalledModule m = list.get(first + r);
            int y = offsetY + ROW_Y + r * ROW_STEP;
            ROW.copy().dest(offsetX + ROW_X, y).blit(g);
            g.renderItem(new ItemStack(m.module()), offsetX + ROW_X + 3, y + 3);
            (m.enabled() ? SWITCH_ON : SWITCH_OFF).copy().dest(offsetX + SWITCH_X, y + 5).blit(g);
            COG.copy().dest(offsetX + COG_X, y + 3).blit(g);
            CLEAR.copy().dest(offsetX + CLEAR_X, y + 3).blit(g);
        }
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(g, offsetX, offsetY, mouseX, mouseY);
        List<InstalledModule> list = new ArrayList<>(menu.installed());
        int first = scrollbar.getCurrentScroll();
        for (int r = 0; r < ROWS && first + r < list.size(); r++) {
            g.drawString(font, font.plainSubstrByWidth(shortName(list.get(first + r)), SWITCH_X - ROW_X - 24), ROW_X + 22,
                    ROW_Y + r * ROW_STEP + 7, TEXT, false);
        }
        List<ArmorItem.Type> worn = menu.worn();
        for (int i = 0; i < worn.size(); i++) {
            int x = leftPos + TAB_X, y = topPos + TAB_Y + i * TAB_STEP;
            if (mouseX >= x && mouseX < x + TAB && mouseY >= y && mouseY < y + TAB) {
                setTooltipForNextRenderPass(minecraft.player.getItemBySlot(worn.get(i).getSlot()).getHoverName());
            }
        }
        int row = row(mouseY);
        if (row >= 0) {
            InstalledModule m = list.get(first + row);
            String key = switch (part(mouseX, mouseY, row)) {
                case 1 -> m.enabled() ? "on" : "off";
                case 2 -> "settings";
                case 3 -> "remove";
                default -> null;
            };
            if (key != null) {
                setTooltipForNextRenderPass(Component.translatable("gui.ae2_gvt.armor_modules." + key));
            } else {
                setTooltipForNextRenderPass(getTooltipFromItem(minecraft, new ItemStack(m.module())).stream().map(Component::getVisualOrderText).toList());
            }
        }
    }

    /** The module's name without the "Module" word every card carries (the tooltip has the full name). */
    private static String shortName(InstalledModule m) {
        String name = new ItemStack(m.module()).getHoverName().getString();
        String suffix = Component.translatable("gui.ae2_gvt.armor_modules.suffix").getString();
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()).trim() : name;
    }

    /**
     * What the mouse is on for JEI and EMI (their A key bookmarks it): a worn piece's tab, or a module's card
     * (its icon and name; not the switch, cog and clear buttons).
     */
    @Override
    public @Nullable KeyHit keyAt(double mouseX, double mouseY) {
        List<ArmorItem.Type> worn = menu.worn();
        for (int i = 0; i < worn.size(); i++) {
            int x = leftPos + TAB_X, y = topPos + TAB_Y + i * TAB_STEP;
            if (mouseX >= x && mouseX < x + TAB && mouseY >= y && mouseY < y + TAB) {
                AEItemKey key = AEItemKey.of(minecraft.player.getItemBySlot(worn.get(i).getSlot()));
                return key == null ? null : new KeyHit(key, x, y, TAB, TAB);
            }
        }
        int row = row(mouseY);
        if (row >= 0 && part(mouseX, mouseY, row) == 0) {
            List<InstalledModule> list = new ArrayList<>(menu.installed());
            AEItemKey key = AEItemKey.of(new ItemStack(list.get(scrollbar.getCurrentScroll() + row).module()));
            return key == null ? null : new KeyHit(key, leftPos + ROW_X, topPos + ROW_Y + row * ROW_STEP, ROW_W, ROW_H);
        }
        return null;
    }

    /** The visible row under the mouse, or -1. */
    private int row(double my) {
        int r = (int) Math.floor((my - topPos - ROW_Y) / ROW_STEP);
        double in = my - topPos - ROW_Y - r * ROW_STEP;
        return r >= 0 && r < ROWS && in < ROW_H && scrollbar.getCurrentScroll() + r < menu.installed().size() ? r : -1;
    }

    /** What in the row is under the mouse: 1 switch, 2 cog, 3 clear, 0 elsewhere (or outside the row). */
    private int part(double mx, double my, int row) {
        double x = mx - leftPos, y = my - topPos - ROW_Y - row * ROW_STEP;
        if (x < ROW_X || x >= ROW_X + ROW_W) {
            return -1;
        }
        if (y >= 5 && y < 17 && x >= SWITCH_X && x < SWITCH_X + 22) {
            return 1;
        }
        if (y >= 3 && y < 19 && x >= COG_X && x < COG_X + 16) {
            return 2;
        }
        return y >= 3 && y < 19 && x >= CLEAR_X && x < CLEAR_X + 16 ? 3 : 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ArmorItem.Type> worn = menu.worn();
            for (int i = 0; i < worn.size(); i++) {
                double x = mouseX - leftPos - TAB_X, y = mouseY - topPos - TAB_Y - i * TAB_STEP;
                if (x >= 0 && x < TAB && y >= 0 && y < TAB) {
                    button(ArmorModuleMenu.Action.SELECT.ordinal() * 100 + ArmorModules.PIECES.indexOf(worn.get(i)), true);
                    return true;
                }
            }
            int row = row(mouseY);
            if (row >= 0) {
                int part = part(mouseX, mouseY, row);
                int index = scrollbar.getCurrentScroll() + row;
                if (part == 1) {
                    button(ArmorModuleMenu.Action.TOGGLE.ordinal() * 100 + index, false);
                    return true;
                }
                if (part == 3) {
                    button(ArmorModuleMenu.Action.REMOVE.ordinal() * 100 + index, false);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void button(int id, boolean predict) {
        if (predict) {
            menu.clickMenuButton(minecraft.player, id);
            scrollbar.setCurrentScroll(0);
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}
