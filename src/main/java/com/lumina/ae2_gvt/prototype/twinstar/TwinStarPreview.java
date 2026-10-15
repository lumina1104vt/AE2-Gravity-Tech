package com.lumina.ae2_gvt.prototype.twinstar;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** PROTOTYPE - registrations for the Twin-Star Hub animation preview (see package-info). */
public final class TwinStarPreview {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2GravityTech.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2GravityTech.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);

    private static final String NAME = "prototype_twin_star_preview";

    public static final DeferredBlock<TwinStarPreviewBlock> BLOCK = BLOCKS.registerBlock(NAME, TwinStarPreviewBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(1.0f));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.registerSimpleBlockItem(NAME, BLOCK);
    /** Invisible stand-in for the placeholder frame while formed; never obtainable as an item. */
    public static final DeferredBlock<PrototypeHubFrameBlock> FRAME = BLOCKS.registerBlock("prototype_hub_frame",
            PrototypeHubFrameBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5f).noOcclusion().noLootTable()
                    .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwinStarPreviewBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(NAME, () -> new BlockEntityType<>(TwinStarPreviewBlockEntity::new, BLOCK.get()));

    private TwinStarPreview() {
    }

    public static void register(IEventBus modBus) {
        com.lumina.ae2_gvt.prototype.twinstar.gui.TwinStarHub.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(TwinStarPreview::addToTab);
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AE2GravityTech.EXAMPLE_TAB.getKey()) {
            event.accept(ITEM);
        }
    }
}
