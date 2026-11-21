package com.lumina.ae2_gvt.client.screen;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;
import com.lumina.twinstar.algorithm.ae2.PatternOutputGuiSlots;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * AE2's amount dialog of a processing output slot (same layout and behaviour as AE2's
 * {@code SetProcessingPatternAmountScreen}) with a probability row under it: the probability in
 * percent (empty or 100 = always comes), a [JEI] button that puts back the probability of the recipe
 * JEI filled the slots from, and a line saying what the probability means (or what is wrong with it;
 * then it cannot be set). That row is the advanced part, shown or hidden by its button. Positions from assets/ae2/screens/ae2_gvt_output_amount.json.
 */
public class SetOutputAmountScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    private static final int HINT_Y = 137;
    private static final int TEXT = 0x404040, ERROR = 0xC03030;

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;
    private final int outputIndex;
    private final boolean advanced;
    private final @Nullable AETextField chance;
    private final net.minecraft.client.gui.components.Button save;
    private final @Nullable net.minecraft.client.gui.components.Button fromRecipe;
    /** the probability as typed, kept while the advanced part is hidden */
    private String chanceText;
    /** the slot of the machine's GUI the product is taken out of (0 = any result slot) */
    private int guiSlot;
    private final @Nullable net.minecraft.client.gui.components.Button guiLess, guiMore;

    public SetOutputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int outputIndex) {
        this(parent, currentStack, slot, outputIndex, chanceTextOf(parent.getMenu().getOutputChance(outputIndex)),
                parent.getMenu().getOutputGuiSlot(outputIndex), false);
    }

    private SetOutputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int outputIndex, String chanceText,
                                  int guiSlot, boolean advanced) {
        super(parent, advanced ? "/screens/ae2_gvt_output_amount_advanced.json" : "/screens/ae2_gvt_output_amount.json");
        this.advanced = advanced;
        this.chanceText = chanceText;
        this.guiSlot = guiSlot;
        this.currentStack = currentStack;
        this.slot = slot;
        this.outputIndex = outputIndex;
        save = widgets.addButton("save", GuiText.Set.text(), this::confirm);
        ItemStack icon = getMenu().getHost().getMainMenuIcon();
        widgets.add("back", new TabButton(Icon.ARROW_LEFT, icon.getHoverName(), btn -> returnToParent()));
        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        amount.setLongValue(currentStack.amount());
        amount.setMaxValue(getMaxAmount());
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);
        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);

        widgets.addButton("advanced", Component.translatable(advanced ? "ae2_gvt.pattern.advanced.hide" : "ae2_gvt.pattern.advanced.show"),
                this::toggleAdvanced);
        if (advanced) {
            chance = widgets.addTextField("chance");
            chance.setMaxLength(12);
            chance.setFilter(s -> s.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.'));
            chance.setPlaceholder(Component.literal("100"));
            chance.setValue(chanceText);
            chance.setResponder(t -> this.chanceText = t);
            fromRecipe = widgets.addButton("recipeChance", Component.literal("JEI"), this::useRecipeChance);
            guiLess = widgets.addButton("guiSlotLess", Component.literal("-"), () -> moveGuiSlot(-1));
            guiMore = widgets.addButton("guiSlotMore", Component.literal("+"), () -> moveGuiSlot(1));
            Tooltip guiHint = Tooltip.create(Component.translatable("ae2_gvt.pattern.gui_slot_out.hint"));
            guiLess.setTooltip(guiHint);
            guiMore.setTooltip(guiHint);
            IgnoreNbt.add(widgets, getMenu(), outputIndex, true);
        } else {
            chance = null;
            fromRecipe = null;
            guiLess = guiMore = null;
        }
    }

    /** The GUI slot up or down; 0 (any result slot) is the bottom. */
    private void moveGuiSlot(int by) {
        guiSlot = Math.max(0, Math.min(PatternOutputGuiSlots.MAX, guiSlot + by));
        getMenu().setOutputGuiSlot(outputIndex, guiSlot);
    }

    private static String chanceTextOf(double c) {
        return c < 1 ? percent(c) : "";
    }

    /** The same dialog in its other layout, with the amount and probability typed so far. */
    private void toggleAdvanced() {
        long value = amount.getLongValue().orElse(currentStack.amount());
        GenericStack stack = new GenericStack(currentStack.what(), Math.max(1, value));
        TwinStarPatternTermScreen parent = getParent();
        returnToParent();
        parent.openDialog(new SetOutputAmountScreen(parent, stack, slot, outputIndex, chanceText, guiSlot, !advanced));
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        save.active = parseChance(chanceText) != null;
        if (chance == null || fromRecipe == null) {
            return;
        }
        Double recipe = getMenu().getRecipeChance(outputIndex);
        fromRecipe.active = recipe != null;
        fromRecipe.setTooltip(Tooltip.create(recipe == null ? Component.translatable("ae2_gvt.pattern.chance.no_recipe")
                : Component.translatable("ae2_gvt.pattern.chance.recipe", recipe < 1 ? percent(recipe) + "%" : "100%")));
        chance.setTextColor(save.active ? 0xFFFFFF : 0xFF6060);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        if (!advanced) {
            return;
        }
        guiGraphics.drawString(font, "GUI", 84, 100, TEXT, false);
        String n = guiSlot == 0 ? "-" : String.valueOf(guiSlot);
        guiGraphics.drawString(font, n, 132 - font.width(n) / 2, 100, TEXT, false);
        Double p = parseChance(chanceText);
        Component hint;
        if (p == null) {
            hint = Component.translatable("ae2_gvt.pattern.chance.invalid");
        } else if (p >= 1) {
            hint = Component.translatable("ae2_gvt.pattern.chance.always");
        } else {
            long each = amount.getLongValue().orElse(currentStack.amount());
            hint = Component.translatable("ae2_gvt.pattern.chance.every", runsPerSuccess(p), amountText(each));
        }
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(hint, imageWidth - 16);
        if (!lines.isEmpty()) {
            guiGraphics.drawString(font, lines.get(0), 8, HINT_Y, p == null ? ERROR : TEXT, false);
        }
    }

    private void useRecipeChance() {
        Double recipe = getMenu().getRecipeChance(outputIndex);
        if (recipe != null && chance != null) {
            chance.setValue(recipe < 1 ? percent(recipe) : "");
        }
    }

    private void confirm() {
        Double p = parseChance(chanceText);
        if (p == null) {
            return;
        }
        amount.getLongValue().ifPresent(value -> {
            value = Longs.constrainToRange(value, 0, getMaxAmount());
            GenericStack stack = value <= 0 ? null : new GenericStack(currentStack.what(), value);
            appeng.core.sync.network.NetworkHandler.instance().sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index,
                    GenericStack.wrapInItemStack(stack)));
            if (stack != null) {
                getMenu().setOutputChance(outputIndex, p);
                getMenu().setOutputGuiSlot(outputIndex, guiSlot);
            }
            returnToParent();
        });
    }

    /** Percent text → probability: empty = always; null when it is not a number in (0, 100]. */
    private static @Nullable Double parseChance(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return 1.0;
        }
        try {
            double p = Double.parseDouble(t) / 100;
            return p > 0 && p <= 1 ? p : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 1 / p, with one decimal when it is not whole. */
    private static String runsPerSuccess(double p) {
        BigDecimal runs = BigDecimal.ONE.divide(BigDecimal.valueOf(p), 1, RoundingMode.HALF_UP).stripTrailingZeros();
        return runs.toPlainString();
    }

    private String amountText(long amount) {
        String n = currentStack.what().formatAmount(amount, AmountFormat.FULL);
        if (currentStack.what() instanceof AEItemKey) {
            return Component.translatable("ae2_gvt.pattern.chance.items", n).getString();
        }
        String unit = currentStack.what().getUnitSymbol();
        return unit == null ? n : n + " " + unit;
    }

    static String percent(double chance) {
        return new BigDecimal(String.format(Locale.ROOT, "%.6f", chance * 100)).stripTrailingZeros().toPlainString();
    }

    private long getMaxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }
}
