package com.lumina.ae2_gvt.storage;

import java.util.ArrayList;
import java.util.List;

import appeng.items.storage.BasicStorageCell;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import com.lumina.ae2_gvt.reg.DeferredItem;

/**
 * Somewhere in an ME network to keep FE: AE2's own cell housing with a cell component of the tier and an energy acceptor makes
 * a cell that holds only FE. Applied Flux has cells of its own for the same key, so with it installed these are left out of the
 * creative tab and have no recipe (see {@link #enabled}).
 */
public final class FeCells {
    /** Tiers as {name, idle drain, kilobytes}: AE2's item cells. */
    public enum Tier {
        T1K("1k", 0.5, 1),
        T4K("4k", 1.0, 4),
        T16K("16k", 1.5, 16),
        T64K("64k", 2.0, 64),
        T256K("256k", 2.5, 256);

        public final String size;
        private final double idleDrain;
        private final int kilobytes;
        private DeferredItem<BasicStorageCell> cell;

        Tier(String size, double idleDrain, int kilobytes) {
            this.size = size;
            this.idleDrain = idleDrain;
            this.kilobytes = kilobytes;
        }

        public net.minecraft.world.level.ItemLike component() {
            return switch (this) {
                case T1K -> appeng.core.definitions.AEItems.CELL_COMPONENT_1K;
                case T4K -> appeng.core.definitions.AEItems.CELL_COMPONENT_4K;
                case T16K -> appeng.core.definitions.AEItems.CELL_COMPONENT_16K;
                case T64K -> appeng.core.definitions.AEItems.CELL_COMPONENT_64K;
                case T256K -> appeng.core.definitions.AEItems.CELL_COMPONENT_256K;
            };
        }

        public DeferredItem<BasicStorageCell> cell() {
            return cell;
        }
    }

    static {
        for (Tier t : Tier.values()) {
            // one type (FE); the bytes per type do not matter with only one
            t.cell = AE2GravityTech.ITEMS.registerItem("fe_storage_cell_" + t.size,
                    p -> new BasicStorageCell(p.stacksTo(1), t.component(), appeng.core.definitions.AEItems.ITEM_CELL_HOUSING, t.idleDrain, t.kilobytes, 8, 1, FeKeyType.INSTANCE));
        }
    }

    private FeCells() {
    }

    public static void init() {
    }

    /** False when Applied Flux, whose cells hold this key as well, is installed. */
    public static boolean enabled() {
        return !ModList.get().isLoaded("appflux");
    }

    public static List<DeferredItem<? extends Item>> all() {
        List<DeferredItem<? extends Item>> items = new ArrayList<>();
        for (Tier t : Tier.values())
            items.add(t.cell);
        return items;
    }
}
