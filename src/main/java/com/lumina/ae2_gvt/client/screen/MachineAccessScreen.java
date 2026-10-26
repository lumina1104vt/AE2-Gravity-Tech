package com.lumina.ae2_gvt.client.screen;

import java.util.Locale;

import com.lumina.ae2_gvt.menus.MachineAccessMenu;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AE2Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Machine access interface screen; positions from assets/ae2/screens/machine_access_interface.json
 * (AE2's pattern provider background for now).
 */
public class MachineAccessScreen extends AEBaseScreen<MachineAccessMenu> {
    private static final int TEXT = 0x404040;

    private final AE2Button blocking;
    private final AE2Button insertMode;

    public MachineAccessScreen(MachineAccessMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        blocking = widgets.addButton("blocking", Component.empty(), menu::toggleBlocking);
        insertMode = widgets.addButton("insertMode", Component.empty(), menu::cycleInsertMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        blocking.setMessage(Component.translatable(menu.blocking ? "gui.ae2_gvt.machine_access.blocking_on"
                : "gui.ae2_gvt.machine_access.blocking_off"));
        insertMode.setMessage(Component.translatable("gui.ae2_gvt.machine_access.insert."
                + menu.getInsertMode().name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        Component status;
        String[] f = menu.feeding.split("\t");
        if (menu.feeding.isEmpty() || f.length != 3) {
            status = Component.translatable("gui.ae2_gvt.machine_access.idle");
        } else {
            status = Component.translatable("gui.ae2_gvt.machine_access.feeding", f[0], f[1], f[2]);
        }
        guiGraphics.drawString(font, font.plainSubstrByWidth(status.getString(), imageWidth - 16), 8, 86, TEXT, false);
    }
}
