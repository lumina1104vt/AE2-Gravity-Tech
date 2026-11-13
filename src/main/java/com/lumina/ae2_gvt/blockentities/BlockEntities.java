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
            "graviton_collector", () -> {
                var type = BlockEntityType.Builder.of(GravitonCollector::new, Blocks.GRAVITON_COLLECTOR.get()).build(null);
                Blocks.GRAVITON_COLLECTOR.get().setBlockEntity(GravitonCollector.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.GRAVITON_COLLECTOR.get().asItem());
                return type;
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityFactoryBlockEntity>> GRAVITY_FACTORY = BLOCK_ENTITIES.register(
            "gravity_processing_factory", () -> {
                var type = BlockEntityType.Builder.of(GravityFactoryBlockEntity::new, Blocks.GRAVITY_FACTORY.get()).build(null);
                Blocks.GRAVITY_FACTORY.get().setBlockEntity(GravityFactoryBlockEntity.class, type, null,
                        GravityFactoryBlockEntity::serverTick);
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.GRAVITY_FACTORY.get().asItem());
                return type;
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineAccessBlockEntity>> MACHINE_ACCESS_BLOCK = BLOCK_ENTITIES.register(
            "machine_access_block", () -> {
                var type = BlockEntityType.Builder.of(MachineAccessBlockEntity::new, Blocks.MACHINE_ACCESS_BLOCK.get()).build(null);
                Blocks.MACHINE_ACCESS_BLOCK.get().setBlockEntity(MachineAccessBlockEntity.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.MACHINE_ACCESS_BLOCK.get().asItem());
                return type;
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionControllerBlockEntity>> FUSION_CONTROLLER = BLOCK_ENTITIES.register(
            "fusion_controller", () -> {
                var type = BlockEntityType.Builder.of(FusionControllerBlockEntity::new, Blocks.FUSION_CONTROLLER.get()).build(null);
                Blocks.FUSION_CONTROLLER.get().setBlockEntity(FusionControllerBlockEntity.class, type,
                        FusionControllerBlockEntity::tick, FusionControllerBlockEntity::tick);
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.FUSION_CONTROLLER.get().asItem());
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DummyTwinStarCPUBlockEntity>> TWINSTAR_CPU = BLOCK_ENTITIES.register(
            "dummy_twinstar_cpu", () -> {
                var type = BlockEntityType.Builder.of(DummyTwinStarCPUBlockEntity::new, Blocks.TWINSTAR_CPU.get()).build(null);
                Blocks.TWINSTAR_CPU.get().setBlockEntity(DummyTwinStarCPUBlockEntity.class, type, null, null);
                // The node's look in AE2's network status; without it the node shows as air and is left out
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.TWINSTAR_CPU.get().asItem());
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.lumina.ae2_gvt.storage.WhiteHoleEnergyCellBlockEntity>> WHITE_HOLE_ENERGY_CELL =
            BLOCK_ENTITIES.register("white_hole_energy_cell", () -> {
                var type = BlockEntityType.Builder.of(com.lumina.ae2_gvt.storage.WhiteHoleEnergyCellBlockEntity::new, Blocks.WHITE_HOLE_ENERGY_CELL.get()).build(null);
                Blocks.WHITE_HOLE_ENERGY_CELL.get().setBlockEntity(com.lumina.ae2_gvt.storage.WhiteHoleEnergyCellBlockEntity.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(type, Blocks.WHITE_HOLE_ENERGY_CELL.get().asItem());
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGeneratorBlockEntity>> CRYSTAL_GENERATOR =
            generator("crystal_generator", Blocks.CRYSTAL_GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGeneratorBlockEntity>> GRAVITON_GENERATOR =
            generator("graviton_generator", Blocks.GRAVITON_GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGeneratorBlockEntity>> ACCRETION_GENERATOR =
            generator("accretion_generator", Blocks.ACCRETION_GENERATOR);

    /** One type per generator (AE2 knows a block entity's item by its type), all of the same class. */
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGeneratorBlockEntity>> generator(
            String name, net.neoforged.neoforge.registries.DeferredBlock<com.lumina.ae2_gvt.blocks.GeneratorBlock> block) {
        return BLOCK_ENTITIES.register(name, () -> {
            var holder = new BlockEntityType<?>[1];
            @SuppressWarnings("unchecked")
            BlockEntityType<PowerGeneratorBlockEntity> type = BlockEntityType.Builder.<PowerGeneratorBlockEntity>of(
                    (pos, state) -> new PowerGeneratorBlockEntity(holder[0], pos, state), block.get()).build(null);
            holder[0] = type;
            block.get().setBlockEntity(PowerGeneratorBlockEntity.class, type, null,
                    (l, p, s, be) -> PowerGeneratorBlockEntity.serverTick(be));
            AEBaseBlockEntity.registerBlockEntityItem(type, block.get().asItem());
            return type;
        });
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyPortBlockEntity>> ENERGY_PORT = BLOCK_ENTITIES.register(
            "energy_port", () -> BlockEntityType.Builder.of(EnergyPortBlockEntity::new, Blocks.ENERGY_PORT.get()).build(null));

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
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, WHITE_HOLE_ENERGY_CELL.get(), (be, ctx) -> be);
        // ME cables: the collector puts gravitons in, the factory takes them out
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, GRAVITON_COLLECTOR.get(), (be, ctx) -> be);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, GRAVITY_FACTORY.get(), (be, ctx) -> be);
        // 機器接入方塊: on the ME network; what a machine pushes out into it, as with the cable part
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, MACHINE_ACCESS_BLOCK.get(), (be, ctx) -> be);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, MACHINE_ACCESS_BLOCK.get(),
                (be, ctx) -> be.core().getReceivedItems());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, MACHINE_ACCESS_BLOCK.get(),
                (be, ctx) -> be.core().getReceivedFluids());
        // 引力加工工廠: pipes put into its inputs and take from its outputs; FE cables power it (any side)
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GRAVITY_FACTORY.get(),
                (be, ctx) -> be.getPipeHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, GRAVITY_FACTORY.get(),
                (be, ctx) -> be.getEnergy());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GRAVITY_FACTORY.get(),
                (be, ctx) -> be.getFluidHandler());
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV, GRAVITY_FACTORY.get(), (be, ctx) -> be.getInputs());
        // the fusion controller: on the ME network; its inputs take anything, as an ME interface's storage
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, FUSION_CONTROLLER.get(), (be, ctx) -> be);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, FUSION_CONTROLLER.get(),
                (be, ctx) -> be.getPipeHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, FUSION_CONTROLLER.get(),
                (be, ctx) -> be.getFluidHandler());
        event.registerBlockEntity(AECapabilities.ME_STORAGE, FUSION_CONTROLLER.get(), (be, ctx) -> be.getInputs());
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV, FUSION_CONTROLLER.get(), (be, ctx) -> be.getInputs());
        // FE cables on an energy port feed the controller of the chamber it is built into
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ENERGY_PORT.get(),
                (be, ctx) -> FusionControllerBlockEntity.energyInput(be.getLevel(), be.getBlockPos()));
        // generators: on the ME network; the crystal one takes dust from hoppers
        for (var type : java.util.List.of(CRYSTAL_GENERATOR, GRAVITON_GENERATOR, ACCRETION_GENERATOR))
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type.get(), (be, ctx) -> be);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, CRYSTAL_GENERATOR.get(),
                (be, ctx) -> be.getFuel().toItemHandler());
        // hoppers and pipes put encoded patterns in
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, TWINSTAR_CPU.get(),
                (be, ctx) -> be.getInsertOnlyHandler());
    }
}
