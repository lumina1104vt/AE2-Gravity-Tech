package com.lumina.ae2_gvt.reg;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;
import java.util.function.Supplier;

/** NeoForge's DeferredRegister, in the shape the mod was written against, over Forge's. */
public class DeferredRegister<R> {
    private final net.minecraftforge.registries.DeferredRegister<R> inner;

    protected DeferredRegister(net.minecraftforge.registries.DeferredRegister<R> inner) {
        this.inner = inner;
    }

    public static <R> DeferredRegister<R> create(ResourceKey<? extends Registry<R>> key, String modid) {
        return new DeferredRegister<>(net.minecraftforge.registries.DeferredRegister.create(key, modid));
    }

    public static Blocks createBlocks(String modid) {
        return new Blocks(net.minecraftforge.registries.DeferredRegister.create(ForgeRegistries.BLOCKS, modid));
    }

    public static Items createItems(String modid) {
        return new Items(net.minecraftforge.registries.DeferredRegister.create(ForgeRegistries.ITEMS, modid));
    }

    public <T extends R> DeferredHolder<R, T> register(String name, Supplier<? extends T> supplier) {
        return new DeferredHolder<>(inner.register(name, supplier));
    }

    public void register(IEventBus bus) {
        inner.register(bus);
    }

    net.minecraftforge.registries.DeferredRegister<R> inner() {
        return inner;
    }

    /** Blocks: with the helpers that take a block's properties. */
    public static final class Blocks extends DeferredRegister<Block> {
        Blocks(net.minecraftforge.registries.DeferredRegister<Block> inner) {
            super(inner);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, B> factory,
                                                                BlockBehaviour.Properties properties) {
            return new DeferredBlock<>(inner().register(name, () -> factory.apply(properties)));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, B> factory) {
            return registerBlock(name, factory, BlockBehaviour.Properties.of());
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
            return registerBlock(name, Block::new, properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(String name) {
            return registerSimpleBlock(name, BlockBehaviour.Properties.of());
        }
    }

    /** Items: with the helpers that take an item's properties. */
    public static final class Items extends DeferredRegister<Item> {
        Items(net.minecraftforge.registries.DeferredRegister<Item> inner) {
            super(inner);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
            return new DeferredItem<>(inner().register(name, () -> factory.apply(properties)));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, I> factory) {
            return registerItem(name, factory, new Item.Properties());
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties properties) {
            return registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return registerSimpleItem(name, new Item.Properties());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, DeferredBlock<? extends Block> block, Item.Properties properties) {
            return new DeferredItem<>(inner().register(name, () -> new BlockItem(block.get(), properties)));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, DeferredBlock<? extends Block> block) {
            return registerSimpleBlockItem(name, block, new Item.Properties());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredBlock<? extends Block> block, Item.Properties properties) {
            return registerSimpleBlockItem(block.getId().getPath(), block, properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredBlock<? extends Block> block) {
            return registerSimpleBlockItem(block, new Item.Properties());
        }
    }
}
