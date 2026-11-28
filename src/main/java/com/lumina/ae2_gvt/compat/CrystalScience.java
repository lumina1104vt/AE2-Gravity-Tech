package com.lumina.ae2_gvt.compat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.lumina.ae2_gvt.items.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Our crystals the AE2 Crystal Science way: a seed that its Crystal Growth Chamber grows into a pure crystal.
 * With that mod the seeds are its own seed item, made by name (the mod is optional and not compiled against);
 * without it they are plain items nothing makes. The seeds and pure crystals are listed in its creative tab.
 */
public final class CrystalScience {
    public static final String MODID = "ae2cs";
    private static final String SEED_CLASS = "io.github.lounode.ae2cs.common.item.CrystalSeedItem";

    private static final List<DeferredItem<Item>> SEEDS = new ArrayList<>();
    private static final List<DeferredItem<Item>> ALL = new ArrayList<>();

    private CrystalScience() {
    }

    public static boolean loaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** Registers the items; before the item register is put on the bus is early enough. */
    public static void init(IEventBus bus) {
        for (String crystal : new String[] {"gravity", "antigravity", "paradox"}) {
            DeferredItem<Item> pure = Items.ITEMS.registerSimpleItem("pure_" + crystal + "_crystal");
            DeferredItem<Item> seed = Items.ITEMS.registerItem(crystal + "_growth_seed", properties -> seed(properties, pure));
            SEEDS.add(seed);
            ALL.add(seed);
            ALL.add(pure);
        }
        bus.addListener(CrystalScience::fillTab);
    }

    /** The seeds and pure crystals, each seed before what it grows into. */
    public static List<DeferredItem<Item>> items() {
        return ALL;
    }

    public static List<DeferredItem<Item>> seeds() {
        return SEEDS;
    }

    private static Item seed(Item.Properties properties, Supplier<Item> grown) {
        if (!loaded())
            return new Item(properties);
        try {
            return (Item) Class.forName(SEED_CLASS).getConstructor(Item.Properties.class, Supplier.class)
                    .newInstance(properties, grown);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new IllegalStateException("AE2 Crystal Science's seed item has changed", e);
        }
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.getNamespace().equals(MODID))
            ALL.forEach(item -> event.accept(item.get()));
    }
}
