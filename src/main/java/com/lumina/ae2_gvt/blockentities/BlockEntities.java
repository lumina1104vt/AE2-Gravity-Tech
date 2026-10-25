package com.lumina.ae2_gvt.blockentities;

import appeng.api.AECapabilities;
import appeng.blockentity.AEBaseBlockEntity;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravitonCollector>> GRAVITON_COLLECTOR = BLOCK_ENTITIES.register(
            "graviton_collector", () -> BlockEntityType.Builder.of(GravitonCollector::new, Blocks.GRAVITON_COLLECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityFactoryBlockEntity>> GRAVITY_FACTORY = BLOCK_ENTITIES.register(
            "gravity_processing_factory", () -> BlockEntityType.Builder.of(GravityFactoryBlockEntity::new, Blocks.GRAVITY_FACTORY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionControllerBlockEntity>> FUSION_CONTROLLER = BLOCK_ENTITIES.register(
            "fusion_controller", () -> BlockEntityType.Builder.of(FusionControllerBlockEntity::new, Blocks.FUSION_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DummyTwinStarCPUBlockEntity>> TWINSTAR_CPU = BLOCK_ENTITIES.register(
            "dummy_twinstar_cpu", () -> {
                var type = BlockEntityType.Builder.of(DummyTwinStarCPUBlockEntity::new, Blocks.TWINSTAR_CPU.get()).build(null);
                Blocks.TWINSTAR_CPU.get().setBlockEntity(DummyTwinStarCPUBlockEntity.class, type, null, null);
                // The node's look in AE2's network status; without it the node shows as air and is left out
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.TWINSTAR_CPU.get().asItem());
                return type;
            });

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BlockEntities::registerCapabilities);
    }

    /**
     * AE2 finds a neighbour's grid node through this capability and only registers it for its own
     * block entity types. Without it a cable can't see our node: it only links when our node is the one
     * created last (placing the block), and drops after a reload.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, TWINSTAR_CPU.get(), (be, ctx) -> be);
        // hoppers and pipes put encoded patterns in
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, TWINSTAR_CPU.get(),
                (be, ctx) -> be.getInsertOnlyHandler());
    }
}
