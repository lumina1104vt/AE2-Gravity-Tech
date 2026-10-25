package com.lumina.ae2_gvt.items.armor;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.locator.ItemMenuHostLocator;
import com.lumina.ae2_gvt.menus.host.ArmorMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public interface IArmorItem extends IMenuItem {
    ArmorItem.Type armorType();

    @Override
    default ItemMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator itemMenuHostLocator, @Nullable BlockHitResult blockHitResult) {
        return new ArmorMenuHost((Item) this, player, itemMenuHostLocator);
    }
}
