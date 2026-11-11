package com.lumina.ae2_gvt.menus;

import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** The quantum controller's screen: its role (off, master, slave), its frequency and what the link is doing. */
public class QuantumControllerMenu extends AEBaseMenu {
    private static final String CYCLE_ROLE = "cycleRole";
    private static final String SET_FREQUENCY = "setFrequency";
    private static final String OPEN_MAIN = "openMain";

    private final QuantumControllerBlockEntity host;

    @GuiSync(1)
    public int role;
    @GuiSync(2)
    public String frequency = "";
    @GuiSync(3)
    public int linkState;
    @GuiSync(4)
    public int slaves;
    /** the frequencies in use: QuantumLinks.summary() */
    @GuiSync(5)
    public String known = "";

    public QuantumControllerMenu(MenuType<?> menuType, int id, Inventory playerInventory, QuantumControllerBlockEntity host) {
        super(menuType, id, playerInventory, host);
        this.host = host;
        registerClientAction(CYCLE_ROLE, this::cycleRole);
        registerClientAction(SET_FREQUENCY, String.class, this::setFrequency);
        registerClientAction(OPEN_MAIN, this::openMain);
    }

    public void cycleRole() {
        if (isClientSide()) {
            sendClientAction(CYCLE_ROLE);
            return;
        }
        host.setRole(host.getRole() + 1);
    }

    /** Back to the main screen: AE2's network status. */
    public void openMain() {
        if (isClientSide()) {
            sendClientAction(OPEN_MAIN);
            return;
        }
        appeng.menu.MenuOpener.open(appeng.menu.me.networktool.NetworkStatusMenu.CONTROLLER_TYPE, getPlayer(),
                appeng.menu.locator.MenuLocators.forBlockEntity(host));
    }

    public void setFrequency(String name) {
        if (isClientSide()) {
            sendClientAction(SET_FREQUENCY, name);
            return;
        }
        host.setFrequency(name);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            role = host.getRole();
            frequency = host.getFrequency();
            linkState = host.getLinkState();
            slaves = host.getSlaves();
            known = com.lumina.ae2_gvt.quantum.QuantumLinks.summary();
        }
        super.broadcastChanges();
    }
}
