package com.lumina.ae2_gvt.client.screen;

import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.menus.FusionControllerMenu;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;

/**
 * The fusion controller's energy-input cap, on a page of its own as AE2 has its priority: back tab,
 * AE2's number entry. Positions: assets/ae2/screens/fusion_cap.json.
 */
public class FusionCapScreen extends AESubScreen<FusionControllerMenu, FusionControllerScreen> {
    private final NumberEntryWidget cap;

    public FusionCapScreen(FusionControllerScreen parent) {
        super(parent, "/screens/fusion_cap.json");
        widgets.add("back", new TabButton(Icon.ARROW_LEFT, Blocks.FUSION_CONTROLLER.get().getName(), btn -> returnToParent()));
        cap = widgets.addNumberEntryWidget("cap", NumberEntryType.UNITLESS);
        cap.setTextFieldStyle(style.getWidget("capInput"));
        cap.setMinValue(0);
        cap.setMaxValue(FusionControllerBlockEntity.MAX_CAP);
        cap.setLongValue(getMenu().energyCap);
        cap.setOnChange(() -> cap.getLongValue().ifPresent(getMenu()::setEnergyCap));
        cap.setOnConfirm(this::returnToParent);
    }
}
