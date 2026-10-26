package com.lumina.ae2_gvt.client.screen;

import java.math.BigDecimal;
import java.util.Locale;

import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AE2's amount dialog of a processing output slot (same layout and behaviour as AE2's
 * {@code SetProcessingPatternAmountScreen}) with a probability field in the empty strip under the
 * title: in percent, empty or 100 = always comes.
 */
public class SetOutputAmountScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    private static final int CHANCE_X = 48, CHANCE_Y = 17, CHANCE_W = 50, CHANCE_H = 11;

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;
    private final int outputIndex;
    private EditBox chance;
    private String chanceText;

    public SetOutputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int outputIndex) {
        super(parent, "/screens/set_processing_pattern_amount.json");
        this.currentStack = currentStack;
        this.slot = slot;
        this.outputIndex = outputIndex;
        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        ItemStack icon = getMenu().getHost().getMainMenuIcon();
        widgets.add("back", new TabButton(Icon.BACK, icon.getHoverName(), btn -> returnToParent()));
        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        amount.setLongValue(currentStack.amount());
        amount.setMaxValue(getMaxAmount());
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);
        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
        double c = getMenu().getOutputChance(outputIndex);
        chanceText = c < 1 ? percent(c) : "";
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
        chance = new EditBox(font, leftPos + CHANCE_X, topPos + CHANCE_Y, CHANCE_W, CHANCE_H,
                Component.translatable("ae2_gvt.pattern.chance"));
        chance.setMaxLength(12);
        chance.setFilter(s -> s.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.'));
        chance.setValue(chanceText);
        chance.setResponder(s -> chanceText = s);
        addRenderableWidget(chance);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable("ae2_gvt.pattern.chance"), 8, CHANCE_Y + 2, 0x404040, false);
        guiGraphics.drawString(font, "%", CHANCE_X + CHANCE_W + 3, CHANCE_Y + 2, 0x404040, false);
    }

    private void confirm() {
        amount.getLongValue().ifPresent(value -> {
            value = Longs.constrainToRange(value, 0, getMaxAmount());
            GenericStack stack = value <= 0 ? null : new GenericStack(currentStack.what(), value);
            PacketDistributor.sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index,
                    GenericStack.wrapInItemStack(stack)));
            if (stack != null) {
                getMenu().setOutputChance(outputIndex, parseChance(chanceText));
            }
            returnToParent();
        });
    }

    /** Percent text → probability; empty, unreadable or out of (0, 100) = always. */
    private static double parseChance(String text) {
        try {
            double p = Double.parseDouble(text.trim()) / 100;
            return p > 0 && p < 1 ? p : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String percent(double chance) {
        return new BigDecimal(String.format(Locale.ROOT, "%.6f", chance * 100)).stripTrailingZeros().toPlainString();
    }

    private long getMaxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }
}
