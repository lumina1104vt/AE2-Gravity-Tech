package com.lumina.ae2_gvt.blockentities;

import appeng.blockentity.AEBaseBlockEntity;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredRegister;

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
            String name, com.lumina.ae2_gvt.reg.DeferredBlock<com.lumina.ae2_gvt.blocks.GeneratorBlock> block) {
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
    }
}
