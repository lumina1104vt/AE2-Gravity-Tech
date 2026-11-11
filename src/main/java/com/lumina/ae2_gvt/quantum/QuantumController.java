package com.lumina.ae2_gvt.quantum;

import net.minecraft.world.level.block.state.BlockBehaviour;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.api.AECapabilities;
import appeng.blockentity.AEBaseBlockEntity;
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
            QuantumControllerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("quantum_controller", BLOCK);

    /** AE2's own controller block entity, under our block (that is what makes it a real controller). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumControllerBlockEntity>> ENTITY = BLOCK_ENTITIES.register(
            "quantum_controller", () -> {
                BlockEntityType<QuantumControllerBlockEntity>[] type = new BlockEntityType[1];
                type[0] = BlockEntityType.Builder.of((pos, state) -> new QuantumControllerBlockEntity(type[0], pos, state), BLOCK.get()).build(null);
                ((appeng.block.AEBaseEntityBlock) BLOCK.get()).setBlockEntity((Class) QuantumControllerBlockEntity.class, (BlockEntityType) type[0], null, null);   // raw: the block is typed for AE2's own controller class
                // The node's look in AE2's network status; without it the node shows as air and is left out
                AEBaseBlockEntity.registerBlockEntityItem(type[0], BLOCK.get().asItem());
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
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENTITY.get(), ControllerBlockEntity::getEnergyStorage);
    }
}
