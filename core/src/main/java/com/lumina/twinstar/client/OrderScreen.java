package com.lumina.twinstar.client;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import com.lumina.twinstar.client.widget.DarkAmountEntry;
import com.lumina.twinstar.client.widget.DarkBackButton;
import com.lumina.twinstar.client.widget.DarkButton;
import com.lumina.twinstar.client.widget.DarkStyle;
import com.lumina.twinstar.client.widget.DarkToggle;
import com.lumina.twinstar.order.OrderMenu;
import com.lumina.twinstar.util.BigAmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * The twin-star order screen (layout A, style screens/twinstar_order.json): the item and what the
 * network has of it, the amount (any size up to the order limit), the order options, and "plan →"
 * to the twin-star plan screen; "original screen" to AE2's amount screen for the other planners.
 */
public class OrderScreen extends AEBaseScreen<OrderMenu> {
    private static final int TEXT = 0xDEDEEA;
    private static final int DIM = 0x8C8CA2;
    private static final int GOOD = 0x78E296;
    private static final int BUSY = 0xF0B060;
    /** the header is this much taller than AE2's, for the twin star beside the title */
    private static final int HEADER = 14;
    /** the item icon in the card's 18x18 frame */
    private static final int ICON_X = 12, ICON_Y = 22 + HEADER;
    /** info lines, right of the icon in the item card */
    private static final int INFO_X = 34;
    private static final int INFO_W = 133;

    // the options stay as the player left them (this session)
    private static boolean lastCraftMissing;
    private static boolean lastBestEffort;
    private static boolean lastAutoStart;

    private final DarkAmountEntry amountToCraft;
    private final DarkToggle craftMissing;
    private final DarkToggle bestEffort;
    private final DarkToggle autoStart;
    private final Button next;
    private @Nullable String appliedInitialAmount;

    public OrderScreen(OrderMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        widgets.add("back", new DarkBackButton(menu));
        amountToCraft = new DarkAmountEntry(this::confirm);
        widgets.add("amountToCraft", amountToCraft);

        craftMissing = option("craftMissing", lastCraftMissing, () -> lastCraftMissing = !lastCraftMissing);
        bestEffort = option("bestEffort", lastBestEffort, () -> lastBestEffort = !lastBestEffort);
        autoStart = option("autoStart", lastAutoStart, () -> lastAutoStart = !lastAutoStart);

        Button original = new DarkButton(Component.translatable("gui.twinstar.order.original"), this::useOriginal);
        original.setTooltip(Tooltip.create(Component.translatable("gui.twinstar.order.original.tooltip")));
        widgets.add("original", original);
        next = new DarkButton(Component.translatable("gui.twinstar.order.plan"), this::confirm);
        widgets.add("next", next);
    }

    private DarkToggle option(String id, boolean selected, Runnable onChange) {
        DarkToggle box = new DarkToggle(Component.translatable("gui.twinstar.order." + id), selected, onChange);
        box.setTooltip(Tooltip.create(Component.translatable("gui.twinstar.order." + id + ".tooltip")));
        widgets.add(id, box);
        return box;
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(amountToCraft.field());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        DarkStyle.hideGuideButton(children());
        // the item and the starting amount may arrive in either order: follow the amount once it changes
        GenericStack what = menu.getWhatToCraft();
        if (what != null && !menu.initialAmount.equals(appliedInitialAmount)) {
            NumberEntryType type = NumberEntryType.of(what.what());
            amountToCraft.setAmountPerUnit(type.amountPerUnit());
            amountToCraft.setText(fieldText(menu.initialAmount, type.amountPerUnit()));
            appliedInitialAmount = menu.initialAmount;
        }
        next.active = amountToCraft.isValid();
    }

    /**
     * An amount (base units, decimal text) as the field shows it: plain for a readable number, else
     * as mantissa and exponent ({@code 1e10000}), which the field reads back exactly.
     */
    static String fieldText(String baseUnits, int perUnit) {
        BigDecimal v;
        try {
            v = new BigDecimal(new BigInteger(baseUnits)).divide(BigDecimal.valueOf(Math.max(1, perUnit)));
        } catch (ArithmeticException | NumberFormatException e) {
            return "1";
        }
        v = v.stripTrailingZeros();
        int exponent = -v.scale();
        if (exponent < 6 || v.precision() > 20)
            return v.toPlainString();
        return v.unscaledValue() + "e" + exponent;
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, offsetX, offsetY, mouseX, mouseY, partialTicks);
        TwinStarBadge.draw(g, offsetX + 3, offsetY + 1);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        GenericStack what = menu.getWhatToCraft();
        if (what == null)
            return;
        AEKey key = what.what();
        AEKeyRendering.drawInGui(minecraft, g, ICON_X, ICON_Y, key);

        line(g, AEKeyRendering.getDisplayName(key), 22 + HEADER, TEXT);
        Component stock = menu.infinite
                ? Component.translatable("gui.twinstar.order.infinite")
                : Component.translatable("gui.twinstar.order.stock", shortAmount(menu.stock, key));
        line(g, stock, 32 + HEADER, menu.infinite ? GOOD : TEXT);
        line(g, menu.routes > 0
                ? Component.translatable("gui.twinstar.order.routes", menu.routes)
                : Component.translatable("gui.twinstar.order.no_routes"), 42 + HEADER, menu.routes > 0 ? TEXT : DIM);
        line(g, Component.translatable(menu.upToDate ? "gui.twinstar.order.up_to_date" : "gui.twinstar.order.updating"),
                52 + HEADER, menu.upToDate ? GOOD : BUSY);
    }

    private void line(GuiGraphics g, Component text, int y, int color) {
        var cut = font.substrByWidth(text, INFO_W);
        g.drawString(font, Language.getInstance().getVisualOrder(cut), INFO_X, y, color, false);
    }

    private static String shortAmount(String baseUnits, AEKey key) {
        try {
            return BigAmountFormat.shortForm(new BigInteger(baseUnits), key.getAmountPerUnit(), key.getUnitSymbol() != null ? key.getUnitSymbol() : "");
        } catch (NumberFormatException e) {
            return baseUnits;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        GenericStack what = menu.getWhatToCraft();
        if (what == null)
            return;
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (x >= ICON_X && x < ICON_X + 16 && y >= ICON_Y && y < ICON_Y + 16) {
            drawTooltip(g, mouseX, mouseY, AEKeyRendering.getTooltip(what.what()));
        } else if (x >= INFO_X && x < INFO_X + INFO_W && y >= 31 + HEADER && y < 41 + HEADER && !menu.infinite) {
            AEKey key = what.what();
            String full;
            try {
                full = BigAmountFormat.fullForm(new BigInteger(menu.stock), key.getAmountPerUnit(),
                        key.getUnitSymbol() != null ? key.getUnitSymbol() : "");
            } catch (NumberFormatException e) {
                full = menu.stock;
            }
            drawTooltip(g, mouseX, mouseY, List.of(Component.literal(full)));
        } else if (x >= INFO_X && x < INFO_X + INFO_W && y >= 51 + HEADER && y < 61 + HEADER) {
            drawTooltip(g, mouseX, mouseY, List.of(Component.translatable(menu.upToDate
                    ? "gui.twinstar.order.up_to_date.tooltip" : "gui.twinstar.order.updating.tooltip")));
        }
    }

    private void confirm() {
        BigInteger v = amountToCraft.exactValue();
        if (amountToCraft.isValid())
            menu.confirm(new OrderMenu.Request(v.toString(), craftMissing.isSelected(), bestEffort.isSelected(),
                    autoStart.isSelected()));
    }

    private void useOriginal() {
        BigInteger v = amountToCraft.exactValue();
        menu.useOriginal(v != null ? v.toString() : "1");
    }
}
