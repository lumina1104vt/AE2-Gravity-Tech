package com.lumina.ae2_gvt.terminal;

import java.util.function.DoubleSupplier;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The twin-star pattern terminal to carry: AE2's wireless terminal (its battery, its range and wireless access points, its
 * linking to a network, its upgrade slots) whose screen is the pattern terminal. What the terminal remembers (the machine, the
 * sub-machines, the settings of the materials) and its patterns are kept in the item.
 */
public class WirelessTwinStarTerminalItem extends WirelessTerminalItem {
    public WirelessTwinStarTerminalItem(DoubleSupplier powerCapacity, Properties props) {
        super(powerCapacity, props);
    }

    /**
     * AE2's own wireless terminals open whether or not they are connected and only show "not connected", which is of no matter to
     * a storage terminal. Encoding patterns needs no network, so without this one out of power, not linked to a network or out of
     * reach of its access points would work all the same, which the pattern terminal on a cable does not (it needs the network).
     * So it opens only when it is connected, and says why when it is not.
     */
    @Override
    protected boolean openFromInventory(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu) {
        if (!player.level().isClientSide()) {
            ItemStack stack = locator.locateItem(player);
            if (!stack.isEmpty()) {
                // a creative player needs no power (as with AE2's own terminals); the battery of anyone else is looked at itself
                if (!player.isCreative() && getAECurrentPower(stack) <= 0) {
                    player.displayClientMessage(Component.translatable("ae2_gvt.wireless_terminal.no_power"), true);
                    return false;
                }
                var host = getMenuHost(player, locator, null);
                host.refreshLink();
                if (!host.getLinkStatus().connected()) {
                    player.displayClientMessage(host.getLinkStatus().statusDescription(), true);
                    return false;
                }
            }
        }
        return super.openFromInventory(player, locator, returningFromSubmenu);
    }

    @Override
    public MenuType<?> getMenuType() {
        return TwinStarTerminal.WIRELESS_MENU.get();
    }

    @Override
    public WirelessTwinStarTerminalMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator locator, @Nullable BlockHitResult hitResult) {
        return new WirelessTwinStarTerminalMenuHost<>(this, player, locator, (p, subMenu) -> openFromInventory(p, locator, true));
    }
}
