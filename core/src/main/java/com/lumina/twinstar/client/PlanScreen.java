package com.lumina.twinstar.client;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import com.lumina.twinstar.client.widget.DarkBackButton;
import com.lumina.twinstar.client.widget.DarkButton;
import com.lumina.twinstar.client.widget.DarkStyle;
import com.lumina.twinstar.order.PlanMenu;
import com.lumina.twinstar.order.PlanView;
import com.lumina.twinstar.util.BigAmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The twin-star plan screen (layout B, style screens/twinstar_plan.json): the crafting tree on the
 * left (each item with how much the plan uses and where it comes from), the summary and the missing
 * materials on the right; back to the order screen, plan again, or start on the hub.
 */
public class PlanScreen extends AEBaseScreen<PlanMenu> {
    private static final int TEXT = 0xDEDEEA;
    private static final int DIM = 0x8C8CA2;
    private static final int CRAFT = 0x96ACFF;
    private static final int GOOD = 0x78E296;
    private static final int BAD = 0xFF7474;
    private static final int LINE = 0xFF484468;
    private static final int MISSING_ROW = 0xFF30141C;
    /** the inset colour, over the missing list's red when nothing is missing */
    private static final int PANEL_FILL = 0xFF12121B;
    /** the header is this much taller than AE2's, for the twin star beside the title */
    private static final int HEADER = 14;
    private static final float SMALL = 0.75f;

    // tree area (inside the inset, left of the scrollbar)
    private static final int TREE_X = 8, TREE_Y = 20 + HEADER, TREE_W = 138, ROW_H = 21, ROWS = 7;
    private static final int MAX_INDENT_DEPTH = 6;
    // right column
    private static final int SIDE_X = 168, SIDE_W = 84;
    private static final int MISSING_Y = 95 + HEADER, MISSING_ROW_H = 18, MISSING_ROWS = 4;

    private final Scrollbar scrollbar;
    private final Button start;
    private final Button replan;

    public PlanScreen(PlanMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        widgets.add("close", new DarkBackButton(menu));
        scrollbar = widgets.addScrollBar("scrollbar", DarkStyle.SCROLLBAR);
        widgets.add("back", new DarkButton(Component.translatable("gui.twinstar.plan.back"), menu::back));
        replan = new DarkButton(Component.translatable("gui.twinstar.plan.replan"), menu::replan);
        widgets.add("replan", replan);
        start = new DarkButton(Component.translatable("gui.twinstar.plan.start"), menu::start);
        widgets.add("start", start);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        DarkStyle.hideGuideButton(children());
        PlanView view = view();
        int rows = view != null ? view.rows().size() + (view.truncated() ? 1 : 0) : 0;
        scrollbar.setRange(0, Math.max(0, rows - ROWS), ROWS);
        // too big for the black hole: starting would only fail
        start.active = view != null && view.complete() && view.fits();
        replan.active = !menu.planning;
    }

    /** The view of the current plan; null while planning. */
    private @Nullable PlanView view() {
        return menu.planning ? null : menu.getView();
    }

    // ── drawing ──

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        TwinStarBadge.draw(g, offsetX + 3, offsetY + 1);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        PlanView view = view();
        if (view == null) {
            Component msg = menu.planning ? Component.translatable("gui.twinstar.plan.planning")
                    : !menu.error.isEmpty() ? errorText(menu.error) : Component.translatable("gui.twinstar.plan.planning");
            g.drawCenteredString(font, msg, TREE_X + TREE_W / 2, TREE_Y + 66, menu.error.isEmpty() ? TEXT : BAD);
            return;
        }
        drawTree(g, view);
        drawSummary(g, view);
        drawMissing(g, view);
    }

    private void drawTree(GuiGraphics g, PlanView view) {
        int first = scrollbar.getCurrentScroll();
        List<PlanView.Row> rows = view.rows();
        for (int i = 0; i < ROWS; i++) {
            int index = first + i;
            int y = TREE_Y + i * ROW_H;
            if (index == rows.size() && view.truncated()) {
                small(g, Component.translatable("gui.twinstar.plan.truncated"), TREE_X + 4, y + 6, DIM, TREE_W - 8);
                break;
            }
            if (index >= rows.size())
                break;
            PlanView.Row row = rows.get(index);
            int x = TREE_X + 2 + Math.min(row.depth(), MAX_INDENT_DEPTH) * 10;
            if (!row.missing().isEmpty() || row.missingMachine() != null)
                g.fill(TREE_X, y, TREE_X + TREE_W, y + ROW_H - 1, MISSING_ROW);
            if (row.depth() > 0) {
                g.vLine(x - 6, y - 2, y + 9, LINE);
                g.hLine(x - 6, x - 2, y + 9, LINE);
            }
            AEKeyRendering.drawInGui(minecraft, g, x, y + 2, row.key());
            int textX = x + 18;
            int width = TREE_X + TREE_W - textX - 1;
            small(g, AEKeyRendering.getDisplayName(row.key()).copy().append(" ×" + shortText(row.amount(), row.key())),
                    textX, y + 2, TEXT, width);
            small(g, sources(row), textX, y + 11, TEXT, width);
        }
    }

    /** Where a row's amount comes from: crafted / stock / infinite source / missing, coloured. */
    private static Component sources(PlanView.Row row) {
        List<MutableComponent> parts = new ArrayList<>();
        if (row.shared())
            parts.add(Component.translatable("gui.twinstar.plan.shared").withColor(DIM));
        if (!row.runs().isEmpty())
            parts.add(Component.translatable("gui.twinstar.plan.runs", shortText(row.runs(), null)).withColor(CRAFT));
        if (!row.stock().isEmpty())
            parts.add(Component.translatable("gui.twinstar.plan.stock", shortText(row.stock(), row.key())).withColor(TEXT));
        if (row.infinite())
            parts.add(Component.translatable("gui.twinstar.plan.infinite").withColor(GOOD));
        if (!row.missing().isEmpty())
            parts.add(Component.translatable("gui.twinstar.plan.missing", shortText(row.missing(), row.key())).withColor(BAD));
        if (row.missingMachine() != null)
            parts.add(Component.translatable("gui.twinstar.plan.missing_machine", AEKeyRendering.getDisplayName(row.missingMachine()))
                    .withColor(BAD));
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                out.append(Component.literal("・").withColor(DIM));
            out.append(parts.get(i));
        }
        return out;
    }

    private void drawSummary(GuiGraphics g, PlanView view) {
        AEKeyRendering.drawInGui(minecraft, g, SIDE_X, 21 + HEADER, view.target());
        g.drawString(font, "×" + shortText(view.amount(), view.target()), SIDE_X + 18, 25 + HEADER, TEXT, false);
        int y = 39 + HEADER;
        small(g, Component.translatable("gui.twinstar.plan.patterns", view.patterns()), SIDE_X, y, DIM, SIDE_W);
        small(g, Component.translatable("gui.twinstar.plan.time", millis(view.planNanos())), SIDE_X, y + 7, DIM, SIDE_W);
        small(g, Component.translatable("gui.twinstar.plan.capacity", shortText(view.usage(), null),
                limit(view.freeCapacity())), SIDE_X, y + 14, view.fits() ? TEXT : BAD, SIDE_W);
        small(g, Component.translatable("gui.twinstar.plan.parallel", limit(view.parallel())), SIDE_X, y + 21, TEXT, SIDE_W);
        if (!menu.error.isEmpty())      // a start that failed
            small(g, errorText(menu.error), SIDE_X, y + 29, BAD, SIDE_W);
        else if (!view.fits())          // said before the start is pressed
            small(g, errorText("cpu_too_small"), SIDE_X, y + 29, BAD, SIDE_W);
    }

    private void drawMissing(GuiGraphics g, PlanView view) {
        List<PlanView.Missing> missing = view.missing();
        if (missing.isEmpty()) {
            g.fill(166, 81 + HEADER, 254, 168 + HEADER, PANEL_FILL);
            small(g, Component.translatable(view.complete() ? "gui.twinstar.plan.complete" : "gui.twinstar.plan.less"),
                    SIDE_X, 84 + HEADER, GOOD, SIDE_W);
            return;
        }
        long machines = missing.stream().filter(PlanView.Missing::machine).count();
        long materials = missing.size() - machines;
        Component title = machines == 0 ? Component.translatable("gui.twinstar.plan.missing_title", materials)
                : materials == 0 ? Component.translatable("gui.twinstar.plan.missing_machines_title", machines)
                : Component.translatable("gui.twinstar.plan.missing_both_title", materials, machines);
        g.drawString(font, title, SIDE_X, 83 + HEADER, BAD, false);
        int shown = missing.size() > MISSING_ROWS ? MISSING_ROWS - 1 : missing.size();
        for (int i = 0; i < shown; i++) {
            PlanView.Missing m = missing.get(i);
            int y = MISSING_Y + i * MISSING_ROW_H;
            AEKeyRendering.drawInGui(minecraft, g, SIDE_X, y, m.key());
            small(g, AEKeyRendering.getDisplayName(m.key()), SIDE_X + 18, y + 1, BAD, SIDE_W - 18);
            small(g, m.machine() ? Component.translatable("gui.twinstar.plan.machine")
                    : Component.literal("×" + shortText(m.amount(), m.key())), SIDE_X + 18, y + 9, BAD, SIDE_W - 18);
        }
        if (shown < missing.size())
            small(g, Component.translatable("gui.twinstar.plan.more_missing", missing.size() - shown), SIDE_X,
                    MISSING_Y + shown * MISSING_ROW_H + 4, BAD, SIDE_W);
    }

    /** Text at 3/4 size, cut to {@code width} (in screen pixels). */
    private void small(GuiGraphics g, Component text, int x, int y, int color, int width) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(SMALL, SMALL, 1);
        var cut = font.substrByWidth(text, (int) (width / SMALL));
        g.drawString(font, Language.getInstance().getVisualOrder(cut), 0, 0, color, false);
        g.pose().popPose();
    }

    // ── tooltips ──

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        PlanView view = view();
        if (view == null)
            return;
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (x >= TREE_X && x < TREE_X + TREE_W && y >= TREE_Y && y < TREE_Y + ROWS * ROW_H) {
            int index = scrollbar.getCurrentScroll() + (y - TREE_Y) / ROW_H;
            if (index < view.rows().size())
                drawTooltip(g, mouseX, mouseY, rowTooltip(view.rows().get(index)));
        } else if (x >= SIDE_X && x < SIDE_X + SIDE_W && y >= MISSING_Y && y < MISSING_Y + MISSING_ROWS * MISSING_ROW_H) {
            int index = (y - MISSING_Y) / MISSING_ROW_H;
            List<PlanView.Missing> missing = view.missing();
            int shown = missing.size() > MISSING_ROWS ? MISSING_ROWS - 1 : missing.size();
            if (index < shown) {
                PlanView.Missing m = missing.get(index);
                drawTooltip(g, mouseX, mouseY, List.of(AEKeyRendering.getDisplayName(m.key()), m.machine()
                        ? Component.translatable("gui.twinstar.plan.machine.tooltip").withColor(BAD)
                        : Component.translatable("gui.twinstar.plan.missing", fullText(m.amount(), m.key())).withColor(BAD)));
            }
        } else if (x >= SIDE_X && x < SIDE_X + SIDE_W && y >= 18 + HEADER && y < 76 + HEADER) {
            drawTooltip(g, mouseX, mouseY, List.of(AEKeyRendering.getDisplayName(view.target()),
                    Component.literal("×" + fullText(view.amount(), view.target())),
                    Component.translatable("gui.twinstar.plan.capacity.tooltip", fullText(view.usage(), null),
                            view.freeCapacity().isEmpty() ? "∞" : fullText(view.freeCapacity(), null))));
        }
    }

    private static List<Component> rowTooltip(PlanView.Row row) {
        List<Component> lines = new ArrayList<>();
        lines.add(AEKeyRendering.getDisplayName(row.key()));
        lines.add(Component.translatable("gui.twinstar.plan.uses", fullText(row.amount(), row.key())));
        if (!row.runs().isEmpty())
            lines.add(Component.translatable("gui.twinstar.plan.runs", fullText(row.runs(), null)).withColor(CRAFT));
        if (!row.stock().isEmpty())
            lines.add(Component.translatable("gui.twinstar.plan.stock", fullText(row.stock(), row.key())));
        if (row.infinite())
            lines.add(Component.translatable("gui.twinstar.plan.infinite").withColor(GOOD));
        if (!row.missing().isEmpty())
            lines.add(Component.translatable("gui.twinstar.plan.missing", fullText(row.missing(), row.key())).withColor(BAD));
        if (row.missingMachine() != null)
            lines.add(Component.translatable("gui.twinstar.plan.missing_machine", AEKeyRendering.getDisplayName(row.missingMachine()))
                    .withColor(BAD));
        if (row.shared())
            lines.add(Component.translatable("gui.twinstar.plan.shared.tooltip").withColor(DIM));
        return lines;
    }

    // ── amounts (sent as digits when they fit a long, else already in unit form) ──

    private static @Nullable BigInteger digits(String s) {
        if (s.isEmpty() || s.length() > 19)
            return null;
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i)))
                return null;
        return new BigInteger(s);
    }

    private static String shortText(String s, @Nullable AEKey key) {
        BigInteger v = digits(s);
        if (v == null)
            return s;
        if (key != null && key.getAmountPerUnit() > 1)
            return BigAmountFormat.shortForm(v, key.getAmountPerUnit(), unit(key));
        return BigAmountFormat.unitForm(v);
    }

    private static String fullText(String s, @Nullable AEKey key) {
        BigInteger v = digits(s);
        if (v == null)
            return s;
        if (key != null && key.getAmountPerUnit() > 1)
            return BigAmountFormat.fullForm(v, key.getAmountPerUnit(), unit(key));
        return NumberFormat.getInstance().format(v);
    }

    private static String unit(AEKey key) {
        String u = key.getUnitSymbol();
        return u != null ? u : "";
    }

    private static String limit(String s) {
        return s.isEmpty() ? "∞" : shortText(s, null);
    }

    private static String millis(long nanos) {
        double ms = nanos / 1e6;
        return ms < 10 ? String.format("%.1f", ms) : String.valueOf(Math.round(ms));
    }

    private static Component errorText(String code) {
        return Component.translatableWithFallback("gui.twinstar.plan.error." + code.toLowerCase(Locale.ROOT), code);
    }
}
