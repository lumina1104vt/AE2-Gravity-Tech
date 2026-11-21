package com.lumina.ae2_gvt.reg;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

/** A block that is not registered yet; also an {@link ItemLike}, as NeoForge's is. */
public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike {
    public DeferredBlock(RegistryObject<T> object) {
        super(object);
    }

    @Override
    public Item asItem() {
        return get().asItem();
    }
}
