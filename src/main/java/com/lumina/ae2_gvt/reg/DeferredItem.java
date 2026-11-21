package com.lumina.ae2_gvt.reg;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

/** An item that is not registered yet; also an {@link ItemLike}, as NeoForge's is. */
public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike {
    public DeferredItem(RegistryObject<T> object) {
        super(object);
    }

    @Override
    public Item asItem() {
        return get();
    }

    public ItemStack toStack() {
        return new ItemStack(get());
    }

    public ItemStack toStack(int count) {
        return new ItemStack(get(), count);
    }
}
