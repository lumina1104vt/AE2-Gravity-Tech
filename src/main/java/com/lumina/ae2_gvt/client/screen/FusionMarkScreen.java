package com.lumina.ae2_gvt.client.screen;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.menus.FusionControllerMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;

/**
 * How much of what is marked a slot of the fusion controller is kept stocked with, as AE2's ME interface
 * asks it: middle-click a marked slot to get here. Positions: assets/ae2/screens/fusion_mark.json.
 */
public class FusionMarkScreen extends AESubScreen<FusionControllerMenu, FusionControllerScreen> {
    private final NumberEntryWidget amount;

    public FusionMarkScreen(FusionControllerScreen parent, int slot, GenericStack mark) {
        super(parent, "/screens/fusion_mark.json");
        widgets.add("back", new TabButton(Icon.ARROW_LEFT, Blocks.FUSION_CONTROLLER.get().getName(), btn -> returnToParent()));
        amount = widgets.addNumberEntryWidget("amount", NumberEntryType.of(mark.what()));
        amount.setTextFieldStyle(style.getWidget("amountInput"));
        amount.setMinValue(1);
        amount.setMaxValue(Long.MAX_VALUE / 4);
        amount.setLongValue(mark.amount());
        amount.setOnChange(() -> amount.getLongValue().ifPresent(v -> getMenu().setMarkAmount(slot, v)));
        amount.setOnConfirm(this::returnToParent);
    }
}
