package com.lumina.twinstar.client;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lumina.twinstar.client.ui.PlanUI;
import com.lumina.twinstar.order.PlanMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * The twin-star plan screen: a plain container screen holding the LDLib2 UI of {@link PlanUI}.
 * The UI is set on the menu here (LDLib2 gives every menu a place for one) and LDLib2 attaches it
 * to this screen when the screen is initialised; this screen only sizes itself to it and keeps it
 * following the menu.
 */
public class PlanScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<PlanMenu> {
    private final PlanUI ui;

    public PlanScreen(PlanMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.ui = new PlanUI(menu, playerInventory.player);
        ((IModularUIHolderMenu) (AbstractContainerMenu) menu).setModularUI(ui.modularUI());
    }

    @Override
    protected void init() {
        imageWidth = PlanUI.WIDTH;
        imageHeight = PlanUI.HEIGHT;
        super.init();
        ui.refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ui.refresh();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // drawn by the LDLib2 UI
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // drawn by the LDLib2 UI
    }
}
