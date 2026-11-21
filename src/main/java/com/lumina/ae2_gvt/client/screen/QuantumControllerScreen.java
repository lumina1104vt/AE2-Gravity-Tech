package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.menus.QuantumControllerMenu;
import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;
import com.lumina.ae2_gvt.quantum.QuantumLinks;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

/**
 * The quantum controller, in AE2's style: the role on the left toolbar (a drawn icon for each: nothing, a tower sending, a tower receiving), the frequency as a name typed in a field, a list of the frequencies in use (click one to take it) and what the link is
 * doing under it.
 */
public class QuantumControllerScreen extends AEBaseScreen<QuantumControllerMenu> {
    private static final int TEXT = 0x404040, MUTED = 0x808080, GOOD = 0x2E7D32, BAD = 0xC03030, ACCENT = 0xFF2F86C8;
    private static final int LIST_X = 8, LIST_Y = 38, ROW_H = 20, ROWS = 4, LIST_W = 144, BAR_X = 156, BAR_W = 12, LIST_H = ROWS * ROW_H;

    private static final int SHAPE_NONE = 0, SHAPE_OK = 1, SHAPE_ERROR = 2, SHAPE_WAIT = 3;

    private record Row(String name, int masters, int slaves) {
    }

    private final RoleButton role = new RoleButton();
    private final ClearButton clear = new ClearButton();
    private final Scrollbar scrollbar;
    private final AETextField field;
    private final List<Row> rows = new ArrayList<>();
    private String parsed = null;

    public QuantumControllerScreen(QuantumControllerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        addToLeftToolbar(new BackButton());
        addToLeftToolbar(role);
        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.DEFAULT);
        this.field = widgets.addTextField("frequency");
        field.setMaxLength(QuantumControllerBlockEntity.MAX_FREQUENCY_LENGTH);
        field.setPlaceholder(Component.translatable("gui.ae2_gvt.quantum.frequency_placeholder"));
        field.setTooltipMessage(List.of(Component.translatable("gui.ae2_gvt.quantum.frequency"),
                Component.translatable("gui.ae2_gvt.quantum.frequency_hint")));
        field.setResponder(text -> {
            if (!text.strip().equals(menu.frequency))
                menu.setFrequency(text);
        });
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(clear);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        clear.setX(leftPos + BAR_X + BAR_W - 16);      // at the scrollbar's right edge: AE2's text field is at most 128 wide
        clear.setY(topPos + 16);
        clear.setVisibility(!menu.frequency.isEmpty());
        if (!field.isFocused() && !field.getValue().equals(menu.frequency))
            field.setValue(menu.frequency);
        if (!menu.known.equals(parsed)) {
            parsed = menu.known;
            rows.clear();
            for (String line : parsed.split(String.valueOf(QuantumLinks.LINE))) {
                String[] f = line.split(String.valueOf(QuantumLinks.FIELD));
                if (f.length == 3)
                    rows.add(new Row(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])));
            }
        }
        scrollbar.setRange(0, Math.max(0, rows.size() - ROWS), 1);
    }

    /** The row under the point (relative to the screen's corner), or -1. */
    private int rowAt(double lx, double ly) {
        if (lx < LIST_X || lx >= LIST_X + LIST_W || ly < LIST_Y || ly >= LIST_Y + ROWS * ROW_H)
            return -1;
        int index = scrollbar.getCurrentScroll() + (int) ((ly - LIST_Y) / ROW_H);
        return index < rows.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = button == 0 ? rowAt(mouseX - leftPos, mouseY - topPos) : -1;
        if (row >= 0) {
            field.setValue(rows.get(row).name());
            menu.setFrequency(rows.get(row).name());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        // the list's frame and its recessed rows, as AE2 draws a slot area
        g.fill(offsetX + LIST_X - 1, offsetY + LIST_Y - 1, offsetX + LIST_X + LIST_W + 1, offsetY + LIST_Y + ROWS * ROW_H + 1, 0xFFF2F2F2);
        g.fill(offsetX + LIST_X, offsetY + LIST_Y, offsetX + LIST_X + LIST_W, offsetY + LIST_Y + ROWS * ROW_H, 0xFFADB0C4);
        // the scrollbar's track, which the widget does not draw (only the handle): the same recessed area, beside the list
        g.fill(offsetX + BAR_X - 1, offsetY + LIST_Y - 1, offsetX + BAR_X + BAR_W + 1, offsetY + LIST_Y + LIST_H + 1, 0xFFF2F2F2);
        g.fill(offsetX + BAR_X, offsetY + LIST_Y, offsetX + BAR_X + BAR_W, offsetY + LIST_Y + LIST_H, 0xFFADB0C4);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(g, offsetX, offsetY, mouseX, mouseY);
        int hover = rowAt(mouseX - leftPos, mouseY - topPos);
        int first = scrollbar.getCurrentScroll();
        if (rows.isEmpty())
            g.drawString(font, Component.translatable("gui.ae2_gvt.quantum.none_in_use"), LIST_X + 3, LIST_Y + 3, MUTED, false);
        for (int r = 0; r < ROWS && first + r < rows.size(); r++) {
            Row row = rows.get(first + r);
            int y = LIST_Y + r * ROW_H;
            if (first + r == hover)
                g.fill(LIST_X, y, LIST_X + LIST_W, y + ROW_H, 0x60FFFFFF);
            if (row.name().equals(menu.frequency)) {         // the one this controller is on: a frame, not a colour
                g.fill(LIST_X, y, LIST_X + LIST_W, y + 1, ACCENT);
                g.fill(LIST_X, y + ROW_H - 1, LIST_X + LIST_W, y + ROW_H, ACCENT);
                g.fill(LIST_X, y, LIST_X + 1, y + ROW_H, ACCENT);
                g.fill(LIST_X + LIST_W - 1, y, LIST_X + LIST_W, y + ROW_H, ACCENT);
            }
            g.drawString(font, font.plainSubstrByWidth(row.name(), 62), LIST_X + 4, y + 6, TEXT, false);
            if (row.masters() > 1) {      // two masters on one frequency: a frame round the masters, a shape and not a colour
                g.fill(LIST_X + 68, y + 1, LIST_X + 102, y + 2, 0xFF282C3C);
                g.fill(LIST_X + 68, y + ROW_H - 2, LIST_X + 102, y + ROW_H - 1, 0xFF282C3C);
                g.fill(LIST_X + 68, y + 1, LIST_X + 69, y + ROW_H - 1, 0xFF282C3C);
                g.fill(LIST_X + 101, y + 1, LIST_X + 102, y + ROW_H - 1, 0xFF282C3C);
            }
            count(g, true, LIST_X + 70, y + 2, row.masters());
            count(g, false, LIST_X + 104, y + 2, row.slaves());
        }
        Component state;
        int color = MUTED;
        int shape = SHAPE_NONE;
        switch (menu.linkState) {
            case QuantumLinks.MASTER_OK -> {
                state = Component.translatable("gui.ae2_gvt.quantum.state.master", menu.slaves);
                color = GOOD;
                shape = SHAPE_OK;
            }
            case QuantumLinks.SLAVE_OK -> {
                state = Component.translatable("gui.ae2_gvt.quantum.state.slave");
                color = GOOD;
                shape = SHAPE_OK;
            }
            case QuantumLinks.NO_MASTER -> {
                state = Component.translatable("gui.ae2_gvt.quantum.state.no_master");
                color = BAD;
                shape = SHAPE_ERROR;
            }
            case QuantumLinks.TWO_MASTERS -> {
                state = Component.translatable("gui.ae2_gvt.quantum.state.two_masters");
                color = BAD;
                shape = SHAPE_ERROR;
            }
            case QuantumLinks.FAILED -> {
                state = Component.translatable("gui.ae2_gvt.quantum.state.failed");
                color = BAD;
                shape = SHAPE_WAIT;
            }
            default -> state = Component.translatable(menu.frequency.isEmpty() ? "gui.ae2_gvt.quantum.state.no_frequency"
                    : "gui.ae2_gvt.quantum.state.off");
        }
        statusShape(g, shape, 8, 124);
        int y = 124;      // a line or two, wrapped to the panel
        for (var line : font.split(FormattedText.of(state.getString()), 146)) {
            g.drawString(font, line, 22, y, color, false);
            y += 10;
        }
    }

    /** The state as a shape (8 x 8), so it does not rest on the text's colour: a hollow square, a tick, an exclamation mark, three dots. */
    private static void statusShape(GuiGraphics g, int shape, int x, int y) {
        int c = 0xFF282C3C;
        switch (shape) {
            case SHAPE_OK -> {
                int[][] tick = {{0, 4}, {1, 5}, {2, 6}, {3, 5}, {4, 4}, {5, 3}, {6, 2}, {7, 1}};
                for (int[] p : tick)
                    g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 2, c);
            }
            case SHAPE_ERROR -> {
                g.fill(x + 3, y, x + 5, y + 5, c);
                g.fill(x + 3, y + 6, x + 5, y + 8, c);
            }
            case SHAPE_WAIT -> {
                g.fill(x, y + 5, x + 2, y + 7, c);
                g.fill(x + 3, y + 5, x + 5, y + 7, c);
                g.fill(x + 6, y + 5, x + 8, y + 7, c);
            }
            default -> {
                g.fill(x, y, x + 8, y + 1, c);
                g.fill(x, y + 7, x + 8, y + 8, c);
                g.fill(x, y, x + 1, y + 8, c);
                g.fill(x + 7, y, x + 8, y + 8, c);
            }
        }
    }

    /** A role's icon with a number to its right: how many masters or slaves a frequency has. */
    private void count(GuiGraphics g, boolean master, int x, int y, int count) {
        icon(g, master, x, y);
        g.drawString(font, Integer.toString(count), x + 19, y + 4, TEXT, false);
    }

    /** A tower: the master sends its signal to both sides, the slave takes it in from one. 16 x 16 pixels, X is dark. */
    private static final String[] MASTER_ICON = {
            "XX........XX",
            "X.XX....XX.X",
            "X.X..XX..X.X",
            "X.X..XX..X.X",
            "X.X..XX..X.X",
            "X.XX.XX.XX.X",
            "XX..XXXX..XX",
            ".X..XXXX..X.",
            "....XXXX....",
            "...XX..XX...",
            "...XX..XX...",
            "...X....X..."};
    private static final String[] SLAVE_ICON = {
            "XX..........",
            "X.XX........",
            "X.X..XX.....",
            "X.X..XX.....",
            "X.X..XX.....",
            "X.XX.XX.....",
            "XX..XXXX....",
            ".X..XXXX....",
            "....XXXX....",
            "...XX..XX...",
            "...XX..XX...",
            "...X....X..."};

    /** The roles' own icons (not blocks), in AE2 blue: 12 x 12 pixels, put in the middle of a 16 x 16 button with a margin all round. */
    private static void icon(GuiGraphics g, boolean master, int x, int y) {
        String[] rows = master ? MASTER_ICON : SLAVE_ICON;
        for (int r = 0; r < rows.length; r++) {
            int c = 0;
            while (c < rows[r].length()) {
                if (rows[r].charAt(c) != 'X') {
                    c++;
                    continue;
                }
                int end = c;
                while (end < rows[r].length() && rows[r].charAt(end) == 'X')
                    end++;
                g.fill(x + 2 + c, y + 2 + r, x + 2 + end, y + 3 + r, 0xFF2F86C8);
                c = end;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int row = rowAt(mouseX - leftPos, mouseY - topPos);
        if (row >= 0) {
            Row r = rows.get(row);
            g.renderTooltip(font, List.of(Component.literal(r.name()),
                    Component.translatable("gui.ae2_gvt.quantum.row_masters", r.masters()).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.ae2_gvt.quantum.row_slaves", r.slaves()).withStyle(ChatFormatting.GRAY),
                    Component.translatable(r.masters() > 1 ? "gui.ae2_gvt.quantum.row_conflict"
                            : r.masters() == 0 ? "gui.ae2_gvt.quantum.row_no_master" : "gui.ae2_gvt.quantum.row_hint").withStyle(ChatFormatting.DARK_GRAY)),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    /** AE2's back arrow: to the main screen. */
    private final class BackButton extends IconButton {
        BackButton() {
            super(btn -> menu.openMain());
        }

        @Override
        protected Icon getIcon() {
            return Icon.ARROW_LEFT;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.quantum.back"));
        }
    }

    /** AE2's clear cross: takes the frequency off (the controller is then on none). */
    private final class ClearButton extends IconButton {
        ClearButton() {
            super(btn -> {
                field.setValue("");
                menu.setFrequency("");
            });
        }

        @Override
        protected Icon getIcon() {
            return Icon.CLEAR;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.quantum.clear"));
        }
    }

    /** The role, as the item it stands for: nothing (AE2's clear cross), an AE2 controller, a quantum link chamber. */
    private final class RoleButton extends IconButton {
        RoleButton() {
            super(btn -> menu.cycleRole());
        }

        @Override
        protected Icon getIcon() {
            return menu.role == QuantumControllerBlockEntity.OFF ? Icon.CLEAR : null;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible || menu.role == QuantumControllerBlockEntity.OFF)
                return;
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            icon(g, menu.role == QuantumControllerBlockEntity.MASTER, getX(), getY() + (isHovered() ? 1 : 0));
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            String r = switch (menu.role) {
                case QuantumControllerBlockEntity.MASTER -> "master";
                case QuantumControllerBlockEntity.SLAVE -> "slave";
                default -> "off";
            };
            return List.of(Component.translatable("gui.ae2_gvt.quantum.role"),
                    Component.translatable("gui.ae2_gvt.quantum.role." + r).withStyle(ChatFormatting.WHITE),
                    Component.translatable("gui.ae2_gvt.quantum.role." + r + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
