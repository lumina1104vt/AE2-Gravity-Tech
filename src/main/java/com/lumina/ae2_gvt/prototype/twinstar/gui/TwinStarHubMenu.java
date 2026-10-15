/*
 * PROTOTYPE - Twin-Star Hub GUI, hosted by the prototype controller. Delete with the prototype package.
 */
package com.lumina.ae2_gvt.prototype.twinstar.gui;

import java.util.List;

import appeng.api.inventories.InternalInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/**
 * The hub's GUI: its stored patterns (AE2 encoded-pattern slots, which the client shows as the
 * pattern's output) and the player inventory. The status numbers are placeholders until the hub
 * has real crafting / computing units.
 */
public class TwinStarHubMenu extends AEBaseMenu {
    private final TwinStarHubHost host;

    public TwinStarHubMenu(MenuType<?> menuType, int id, Inventory playerInventory, TwinStarHubHost host) {
        super(menuType, id, playerInventory, host);
        this.host = host;
        InternalInventory patterns = host.getHubPatterns();
        for (int i = 0; i < patterns.size(); i++) {
            RestrictedInputSlot slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, patterns, i);
            slot.setStackLimit(1);
            this.addSlot(slot, SlotSemantics.ENCODED_PATTERN);
        }
        this.createPlayerInventorySlots(playerInventory);
    }

    public List<Slot> getPatternSlots() {
        return this.getSlots(SlotSemantics.ENCODED_PATTERN);
    }

    @Override
    public void broadcastChanges() {
        // the GUI belongs to a formed hub; close it if the structure breaks
        if (this.isServerSide() && !host.isHubFormed()) {
            this.setValidMenu(false);
        }
        super.broadcastChanges();
    }
}
