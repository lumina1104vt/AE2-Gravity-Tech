package com.lumina.ae2_gvt.graviton;

import java.util.List;

import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.items.storage.BasicStorageCell;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredItem;
import com.lumina.ae2_gvt.reg.DeferredRegister;

import com.lumina.ae2_gvt.AE2GravityTech;

/**
 * Registration of the graviton storage type and its cells (textures: tools/graviton_textures.py,
 * the cells by gen_textures.py's cell_anim).
 * <p>
 * The cells are AE2's {@link BasicStorageCell} with AE2's item cell numbers, except that they only
 * know two types (graviton / anti-graviton). The housing + a graviton cell component makes a cell.
 */
public final class Gravitons {

    /** Display-only items: how the two keys look in terminals (not in the creative tab). */
    public static final DeferredItem<Item> GRAVITON = AE2GravityTech.ITEMS.registerSimpleItem("graviton");
    public static final DeferredItem<Item> ANTIGRAVITON = AE2GravityTech.ITEMS.registerSimpleItem("antigraviton");

    public static final DeferredItem<Item> HOUSING = AE2GravityTech.ITEMS.registerSimpleItem("graviton_cell_housing");

    /** Tiers as {name, idle drain, kilobytes, bytes per type} - AE2's item cells. */
    public enum Tier {
        T1K("1k", 0.5, 1, 8),
        T4K("4k", 1.0, 4, 32),
        T16K("16k", 1.5, 16, 128),
        T64K("64k", 2.0, 64, 512),
        T256K("256k", 2.5, 256, 2048);

        public final String size;
        final double idleDrain;
        final int kilobytes, bytesPerType;
        DeferredItem<BasicStorageCell> cell;
        DeferredItem<Item> component;

        Tier(String size, double idleDrain, int kilobytes, int bytesPerType) {
            this.size = size;
            this.idleDrain = idleDrain;
            this.kilobytes = kilobytes;
            this.bytesPerType = bytesPerType;
        }

        public DeferredItem<BasicStorageCell> cell() {
            return cell;
        }

        public DeferredItem<Item> component() {
            return component;
        }
    }

    /** No recipe: see {@link InfiniteGravitonCell}. */
    public static final DeferredItem<InfiniteGravitonCell> INFINITE_CELL =
            AE2GravityTech.ITEMS.registerItem("infinite_graviton_storage_cell", InfiniteGravitonCell::new);

    static {
        for (Tier t : Tier.values()) {
            t.component = AE2GravityTech.ITEMS.registerSimpleItem("graviton_cell_component_" + t.size);
            t.cell = AE2GravityTech.ITEMS.registerItem("graviton_storage_cell_" + t.size,
                    p -> new BasicStorageCell(p.stacksTo(1), t.component.get(), HOUSING.get(), t.idleDrain, t.kilobytes, t.bytesPerType, 2, GravitonKeyType.INSTANCE));
        }
    }

    private Gravitons() {
    }

    public static void init() {
    }

    public static void register(IEventBus modEventBus) {
        // AE2 keeps its key types in a registry of its own, open only while the registries are filled
        modEventBus.addListener((net.minecraftforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(appeng.api.stacks.AEKeyTypesInternal.getRegistry().getRegistryKey()))
                appeng.api.stacks.AEKeyTypes.register(GravitonKeyType.INSTANCE);
        });
    }

    /** A key as a recipe page shows it: gravitons as their own item, which AE2's wrapped stack does not draw. */
    public static ItemStack shown(GenericStack key) {
        if (key.what() instanceof GravitonKey graviton)
            return graviton.displayStack().copyWithCount((int) Math.min(key.amount(), Integer.MAX_VALUE));
        return GenericStack.wrapInItemStack(key);
    }

    /** What goes in the creative tab: the components, the housing, then the cells. */
    public static List<DeferredItem<? extends Item>> all() {
        List<DeferredItem<? extends Item>> items = new java.util.ArrayList<>();
        for (Tier t : Tier.values()) {
            items.add(t.component);
        }
        items.add(HOUSING);
        for (Tier t : Tier.values()) {
            items.add(t.cell);
        }
        items.add(INFINITE_CELL);
        return items;
    }
}
