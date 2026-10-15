package com.lumina.ae2_gvt.prototype.quantum;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.api.AECapabilities;
import appeng.blockentity.networking.ControllerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration of the quantum controller (textures: tools/quantum_controller_textures.py). */
public final class QuantumController {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,
            AE2GravityTech.MODID);

    public static final DeferredBlock<QuantumControllerBlock> BLOCK = AE2GravityTech.BLOCKS.registerBlock("quantum_controller",
            QuantumControllerBlock::new, p -> p.mapColor(MapColor.METAL).strength(6f).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("quantum_controller", BLOCK);

    /** AE2's own controller block entity, under our block (that is what makes it a real controller). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControllerBlockEntity>> ENTITY = BLOCK_ENTITIES.register(
            "quantum_controller", () -> {
                BlockEntityType<ControllerBlockEntity>[] type = new BlockEntityType[1];
                type[0] = new BlockEntityType<>((pos, state) -> new ControllerBlockEntity(type[0], pos, state), BLOCK.get());
                BLOCK.get().setBlockEntity(ControllerBlockEntity.class, type[0], null, null);
                return type[0];
            });

    private QuantumController() {
    }

    public static void init() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(QuantumController::registerCapabilities);
    }

    /**
     * AE2 only hands these out to its own block entity types, so ours needs them registered: the grid
     * node host (cables connect to it) and FE input (powered straight from a generator, like AE2's).
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ENTITY.get(), (be, ctx) -> be);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ENTITY.get(), ControllerBlockEntity::getEnergyStorage);
    }
}
