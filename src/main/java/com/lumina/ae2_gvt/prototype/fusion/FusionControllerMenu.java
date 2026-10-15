package com.lumina.ae2_gvt.prototype.fusion;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.guisync.GuiSync;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** The fusion controller's GUI: temperature, status and energy to show, the energy-input cap to set. */
public class FusionControllerMenu extends AEBaseMenu {
    private static final ClientActionKey<Long> SET_CAP = new ClientActionKey<>("setEnergyCap");

    private final FusionControllerBlockEntity controller;

    @GuiSync(10)
    public int temperature;
    @GuiSync(11)
    public int equilibrium;
    @GuiSync(12)
    public int need;
    @GuiSync(13)
    public long energyCap;
    @GuiSync(14)
    public long energyIn;
    @GuiSync(15)
    public long energyOut;
    @GuiSync(16)
    public boolean formed;

    public FusionControllerMenu(MenuType<?> menuType, int id, Inventory playerInventory, FusionControllerBlockEntity controller) {
        super(menuType, id, playerInventory, controller);
        this.controller = controller;
        registerClientAction(SET_CAP, ByteBufCodecs.VAR_LONG, this::setEnergyCap);
        energyCap = controller.energyCap();
    }

    public void setEnergyCap(long cap) {
        if (isClientSide()) {
            energyCap = cap;
            sendClientAction(SET_CAP, cap);
        } else {
            controller.setEnergyCap(cap);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            temperature = (int) Math.round(controller.temperature());
            equilibrium = (int) Math.round(controller.equilibrium());
            need = FusionControllerBlockEntity.PLACEHOLDER_NEED;
            energyCap = controller.energyCap();
            energyIn = controller.energyIn();
            energyOut = controller.energyOut();
            formed = controller.formed();
        }
        super.broadcastChanges();
    }

    /** 0 idle (not formed), 1 heating (will reach the recipe), 2 running, 3 too cold (the cap can't get there). */
    public int status() {
        if (!formed) {
            return 0;
        }
        if (temperature >= need) {
            return 2;
        }
        return equilibrium >= need ? 1 : 3;
    }
}
