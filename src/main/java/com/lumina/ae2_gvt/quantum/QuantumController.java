package com.lumina.ae2_gvt.quantum;

import net.minecraft.world.level.block.state.BlockBehaviour;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.ControllerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredBlock;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredItem;
import com.lumina.ae2_gvt.reg.DeferredRegister;

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
    }
}
