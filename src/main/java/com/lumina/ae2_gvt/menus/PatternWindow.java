package com.lumina.ae2_gvt.menus;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The pattern slots a hub's GUI shows: a window of {@code size} slots over the hub's pattern
 * inventory (50,000 slots: a menu with all of them would sync them all every tick). Server: a view of
 * the block entity's patterns from {@code offset}. Client: its own slots, holding exactly what the
 * server sent for each menu slot (the client's block entity has no patterns, and reading it at a moved
 * offset would show stale items).
 */
public final class PatternWindow implements InternalInventory {
    private final @Nullable InternalInventory patterns;
    private final AppEngInternalInventory client;
    private final int size;
    private int offset;

    public PatternWindow(@Nullable InternalInventory patterns, int size) {
        this.patterns = patterns;
        this.size = size;
        this.client = new AppEngInternalInventory(size);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (patterns == null)
            return client.getStackInSlot(slot);
        int i = offset + slot;
        return i < patterns.size() ? patterns.getStackInSlot(i) : ItemStack.EMPTY;
    }

    @Override
    public void setItemDirect(int slot, ItemStack stack) {
        if (patterns == null) {
            client.setItemDirect(slot, stack);
            return;
        }
        int i = offset + slot;
        if (i < patterns.size())
            patterns.setItemDirect(i, stack);
    }
}
