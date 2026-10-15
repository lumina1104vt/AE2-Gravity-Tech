package com.lumina.ae2_gvt.blockentities;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.cpu.DummyWhiteHoleCPUBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravitonCollector>> GRAVITON_COLLECTOR = BLOCK_ENTITIES.register(
            "graviton_collector", () -> new BlockEntityType<>(GravitonCollector::new, Blocks.GRAVITON_COLLECTOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DummyWhiteHoleCPUBlockEntity>> WHITE_HOLE_CPU = BLOCK_ENTITIES.register(
            "dummy_white_hole_cpu", () -> new BlockEntityType<>(DummyWhiteHoleCPUBlockEntity::new, Blocks.WHILE_HOLE_CPU.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
