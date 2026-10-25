package com.lumina.ae2_gvt.menus;

import appeng.menu.AEBaseMenu;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.items.armor.IArmorItem;
import com.lumina.ae2_gvt.items.armor.data.InstalledModule;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ArmorModuleMenu extends AEBaseMenu {
    private final Player player;
    private final SimpleContainer install = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final Slot installSlot;
    private int selected;

    public ArmorModuleMenu(MenuType<?> menuType, int id, Inventory inventory, Object host) {
        super(menuType, id, inventory, host);
        this.player = inventory.player;

        List<ArmorItem.Type> worn = worn();
        this.selected = ArmorModules.PIECES.indexOf(worn.getFirst());
        this.installSlot = addSlot(new Slot(install, 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canInstall(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }, ArmorModules.INSTALL_SLOT);

        createPlayerInventorySlots(inventory);
        for (ArmorItem.Type piece : ArmorModules.PIECES) {
            addSlot(new Slot(inventory, piece.getSlot().getIndex(36), -10000, -10000) {
                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
    }

    public List<ArmorItem.Type> worn() {
        List<ArmorItem.Type> worn = new ArrayList<>();
        for (ArmorItem.Type type : ArmorModules.PIECES)
            if (ArmorModules.isPiece(player.getItemBySlot(type.getSlot()), type))
                worn.add(type);

        return worn;
    }

    public ArmorItem.Type selected() {
        return ArmorModules.PIECES.get(selected);
    }

    public ItemStack selectedStack() {
        ItemStack stack = player.getItemBySlot(selected().getSlot());
        return ArmorModules.isPiece(stack, selected()) ? stack : ItemStack.EMPTY;
    }

    public Set<InstalledModule> installed() {
        return ArmorModules.installed(selectedStack());
    }

    private boolean canInstall(ItemStack module) {
        ItemStack armor = selectedStack();
        return ArmorModules.fits(module.getItem(), selected(), (Class<? extends IArmorItem>) armor.getItem().getClass()) &&
                installed().stream().noneMatch(m -> m.module().value() == module.getItem());
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        ItemStack module = install.getItem(0);
        if (!player.level().isClientSide() && !module.isEmpty() && canInstall(module)) {
            Set<InstalledModule> set = new TreeSet<>(installed());
            set.add(new InstalledModule(module.getItemHolder(), true));
            ArmorModules.setInstalled(selectedStack(), set);
            install.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Action action = Action.values()[id / 100];
        int index = id % 100;

        if (action == Action.SELECT) {
            if (index < ArmorModules.PIECES.size() && worn().contains(ArmorModules.PIECES.get(index))) {
                selected = index;
                return true;
            }
            return false;
        }

        List<InstalledModule> list = new ArrayList<>(installed());
        if (index >= list.size())
            return false;

        if (action == Action.TOGGLE)
            list.set(index, list.get(index).toggled());
        else if (action == Action.REMOVE) {
            ItemStack module = new ItemStack(list.remove(index).module());
            if (!player.level().isClientSide())
                player.getInventory().placeItemBackInInventory(module);
        }
        else return false;

        if (!player.level().isClientSide())
            ArmorModules.setInstalled(selectedStack(), new TreeSet<>(list));

        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot slot = slots.get(idx);
        if (slot == installSlot || slot.container != player.getInventory() || !slot.isActive() || !slot.hasItem() || !canInstall(slot.getItem()))
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        install.setItem(0, stack.split(1));
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, install);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && !worn().isEmpty();
    }

    public enum Action {
        SELECT,
        TOGGLE,
        REMOVE;
    }
}
