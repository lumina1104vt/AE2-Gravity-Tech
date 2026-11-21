package com.lumina.ae2_gvt.items.armor;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The modules that use the linked network: auto stock (top up the hotbar), recharge (charge what is carried), pick craft (middle click). */
public final class ArmorNetworkEffects {
    private ArmorNetworkEffects() {
    }

    /** Called every 20 ticks for a player. */
    static void tick(ServerPlayer player) {
        boolean stock = ArmorEffects.has(player, Items.AUTO_STOCK_MODULE), recharge = ArmorEffects.has(player, Items.RECHARGE_MODULE);
        if (!stock && !recharge)
            return;
        var grid = ArmorNetwork.grid(player);
        if (grid == null)
            return;
        if (stock)
            autoStock(player, grid);
        if (recharge)
            recharge(player, grid);
    }

    /** A hotbar stack (or the off hand) that has fallen to half is filled up again from the network. */
    private static void autoStock(ServerPlayer player, appeng.api.networking.IGrid grid) {
        var inventory = player.getInventory();
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player);
        for (int i = 0; i < 10; i++) {
            ItemStack stack = i < 9 ? inventory.items.get(i) : inventory.offhand.get(0);
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1 || stack.getCount() > stack.getMaxStackSize() / 2)
                continue;
            var key = AEItemKey.of(stack);
            if (key == null)
                continue;
            int want = stack.getMaxStackSize() - stack.getCount();
            long got = storage.extract(key, want, Actionable.SIMULATE, source);
            if (got <= 0 || !ArmorNetwork.pay(grid, got * Config.STOCK_COST.get()))
                continue;
            got = storage.extract(key, got, Actionable.MODULATE, source);
            stack.grow((int) got);
        }
    }

    /** Anything carried that takes energy is charged, from the network's power. */
    private static void recharge(ServerPlayer player, appeng.api.networking.IGrid grid) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty())
                continue;
            var energy = stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).resolve().orElse(null);
            if (energy == null || !energy.canReceive())
                continue;
            int room = energy.receiveEnergy((int) Math.min(Integer.MAX_VALUE, Config.RECHARGE_FE.get()), true);
            if (room <= 0 || !ArmorNetwork.pay(grid, room / 2.0))
                continue;
            energy.receiveEnergy(room, false);
        }
    }

    /** Not in the network: if it has a pattern, AE2's own screen asks how many, then shows the plan to start. */
    private static void order(ServerPlayer player, appeng.api.networking.IGrid grid, AEItemKey key, ItemStack wanted) {
        if (!grid.getCraftingService().isCraftable(key)) {
            player.displayClientMessage(Component.translatable("gui.ae2_gvt.armor_network.not_stocked", wanted.getHoverName()), true);
            return;
        }
        // the helmet carries the module, and is what AE2's screens are opened from
        var head = net.minecraft.world.entity.EquipmentSlot.HEAD;
        appeng.menu.me.crafting.CraftAmountMenu.open(player, appeng.menu.locator.MenuLocators.forInventorySlot(head.getIndex(36)), key,
                Config.PICK_CRAFT_AMOUNT.get());
    }

    /** Middle click on a block: its item comes out of the network into the hand (the hotbar slot it already is in, if it is carried). */
    public static void pick(ServerPlayer player, ItemStack wanted) {
        if (!ArmorEffects.has(player, Items.PICK_CRAFT_MODULE) || wanted.isEmpty())
            return;
        var grid = ArmorNetwork.grid(player);
        if (grid == null) {
            player.displayClientMessage(Component.translatable("gui.ae2_gvt.armor_network.no_link"), true);
            return;
        }
        var key = AEItemKey.of(wanted);
        if (key == null)
            return;
        var inventory = player.getInventory();
        // already carried: just select it
        for (int i = 0; i < 9; i++) {
            if (ItemStack.isSameItemSameTags(inventory.items.get(i), wanted)) {
                inventory.selected = i;
                return;
            }
        }
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player);
        long got = storage.extract(key, wanted.getMaxStackSize(), Actionable.SIMULATE, source);
        if (got <= 0) {
            order(player, grid, key, wanted);
            return;
        }
        if (!ArmorNetwork.pay(grid, got * Config.STOCK_COST.get()))
            return;
        got = storage.extract(key, got, Actionable.MODULATE, source);
        ItemStack out = key.toStack((int) got);
        // the hand if it is empty, else a free hotbar slot, else the inventory
        int slot = inventory.items.get(inventory.selected).isEmpty() ? inventory.selected : -1;
        for (int i = 0; i < 9 && slot < 0; i++) {
            if (inventory.items.get(i).isEmpty())
                slot = i;
        }
        if (slot >= 0) {
            inventory.items.set(slot, out);
            inventory.selected = slot;
        } else if (!inventory.add(out)) {
            player.drop(out, false);
        }
    }
}
