package com.lumina.ae2_gvt.items.armor;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.lumina.ae2_gvt.menus.host.ArmorMenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IArmorItem extends IMenuItem {
    ArmorItem.Type armorType();

    @Override
    default ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new ArmorMenuHost(player, inventorySlot, stack);
    }
}
