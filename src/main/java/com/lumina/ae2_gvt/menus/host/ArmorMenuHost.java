package com.lumina.ae2_gvt.menus.host;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.locator.ItemMenuHostLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class ArmorMenuHost extends ItemMenuHost<Item> {
    public ArmorMenuHost(Item item, Player player, ItemMenuHostLocator locator) {
        super(item, player, locator);
    }
}
