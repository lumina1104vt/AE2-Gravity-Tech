package com.lumina.ae2_gvt.prototype.fusion;

import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 核融合發生器: a 7x7x7 cubic chamber - structure frame on the edges, containment glass on the faces
 * with one gravity constrainer in each face centre pointing in, a plasma (a small star) in the
 * middle. The controller and any number of ports take the place of face glass anywhere.
 * Placeholder: the blocks and their looks only; no structure check, recipes or power yet.
 */
public final class FusionGenerator {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredBlock<FusionControllerBlock> CONTROLLER = AE2GravityTech.BLOCKS.registerBlock("fusion_controller",
            FusionControllerBlock::new, p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
    public static final DeferredBlock<ContainmentGlassBlock> GLASS = AE2GravityTech.BLOCKS.registerBlock("containment_glass",
            ContainmentGlassBlock::new, p -> p.mapColor(MapColor.NONE).strength(1.5f, 11f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, pos, e) -> false).isRedstoneConductor((s, l, pos) -> false)
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    public static final DeferredBlock<Block> MATTER_PORT = AE2GravityTech.BLOCKS.registerSimpleBlock("matter_port",
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
    public static final DeferredBlock<Block> ENERGY_PORT = AE2GravityTech.BLOCKS.registerSimpleBlock("energy_port",
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));

    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("fusion_controller", CONTROLLER);
    public static final DeferredItem<BlockItem> GLASS_ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("containment_glass", GLASS);
    public static final DeferredItem<BlockItem> MATTER_PORT_ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("matter_port", MATTER_PORT);
    public static final DeferredItem<BlockItem> ENERGY_PORT_ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("energy_port", ENERGY_PORT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionControllerBlockEntity>> CONTROLLER_ENTITY =
            BLOCK_ENTITIES.register("fusion_controller", () -> new BlockEntityType<>(FusionControllerBlockEntity::new, CONTROLLER.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<FusionControllerMenu>> CONTROLLER_MENU = MENUS.register("fusion_controller",
            () -> MenuTypeBuilder.<FusionControllerMenu, FusionControllerBlockEntity>create(FusionControllerMenu::new, FusionControllerBlockEntity.class)
                    .buildUnregistered(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion_controller")));

    private FusionGenerator() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    /** Creative tab order. */
    public static List<DeferredItem<BlockItem>> all() {
        return List.of(CONTROLLER_ITEM, GLASS_ITEM, MATTER_PORT_ITEM, ENERGY_PORT_ITEM);
    }

    /** Called from the mod class so the blocks register with BLOCKS / ITEMS. */
    public static void init() {
    }
}
