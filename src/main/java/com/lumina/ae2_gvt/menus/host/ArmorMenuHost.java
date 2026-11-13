package com.lumina.ae2_gvt.menus.host;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import com.lumina.ae2_gvt.items.armor.ArmorNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a worn piece opens: its modules' screen, and (it acts for the network it is linked to) AE2's own screens for ordering a
 * crafting job, which ask the host for a network to work in and for a way back.
 */
public class ArmorMenuHost extends ItemMenuHost<Item> implements ISubMenuHost, IActionHost {
    public ArmorMenuHost(Item item, Player player, ItemMenuHostLocator locator) {
        super(item, player, locator);
    }

    /** The network's access point the wearer reaches: where the crafting job is worked out and started. */
    @Override
    public IGridNode getActionableNode() {
        if (getPlayer() instanceof ServerPlayer player) {
            var point = ArmorNetwork.accessPoint(player);
            if (point != null)
                return point.getActionableNode();
        }
        return null;
    }

    /** Nothing to go back to: the order's screens just close. */
    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        player.closeContainer();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getItemStack();
    }
}
