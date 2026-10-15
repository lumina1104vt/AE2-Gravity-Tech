package com.lumina.ae2_gvt.prototype.factory;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 引力加工工廠 (gravity processing factory): a single-block general-purpose processing machine -
 * 3x3 item inputs, fluid and gas in, three outputs, fluid and gas out, and one catalyst slot whose
 * stack raises the yield without being used up. Powered by AE energy or FE.
 * <p>
 * For now: the block, its inventories and its GUI. Recipes, processing, tanks and energy come next.
 */
public final class GravityFactory {
    public static final String ID = "gravity_processing_factory";

    /** The catalyst slot. Registered here so it exists before AE2 validates the screen style. */
    public static final SlotSemantic CATALYST = SlotSemantics.register("GRAVITY_CATALYST", false);
    /**
     * The 12 inputs / outputs as three rows of four each: AE2's slot grids only break after 2, 3 or 9
     * columns, so each row is its own horizontal group in the screen style.
     */
    public static final SlotSemantic[] INPUT_ROWS = rows("GRAVITY_FACTORY_INPUT_ROW_", false);
    public static final SlotSemantic[] OUTPUT_ROWS = rows("GRAVITY_FACTORY_OUTPUT_ROW_", false);
    public static final int ROW_SLOTS = 4;

    private static SlotSemantic[] rows(String prefix, boolean playerSide) {
        SlotSemantic[] rows = new SlotSemantic[3];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = SlotSemantics.register(prefix + i, playerSide);
        }
        return rows;
    }

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredBlock<GravityFactoryBlock> BLOCK = AE2GravityTech.BLOCKS.registerBlock(ID, GravityFactoryBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem(ID, BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityFactoryBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register(ID,
            () -> new BlockEntityType<>(GravityFactoryBlockEntity::new, BLOCK.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<GravityFactoryMenu>> MENU = MENUS.register(ID,
            () -> MenuTypeBuilder.<GravityFactoryMenu, GravityFactoryBlockEntity>create(GravityFactoryMenu::new, GravityFactoryBlockEntity.class)
                    .buildUnregistered(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, ID)));

    private GravityFactory() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
