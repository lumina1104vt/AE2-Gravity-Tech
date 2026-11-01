package com.lumina.ae2_gvt.blocks;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.budding.BuddingParadox;
import com.lumina.ae2_gvt.blocks.cpu.DummyTwinStarCPU;
import com.lumina.ae2_gvt.annihilation.AnnihilationTntBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2GravityTech.MODID);

    public static final DeferredBlock<GravityConstrainer> GRAVITY_CONSTRAINER = BLOCKS.registerBlock(
            "gravity_constrainer", GravityConstrainer::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<GravitonCollector> GRAVITON_COLLECTOR = BLOCKS.registerBlock(
            "graviton_collector", GravitonCollector::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<AccelerationDevice> ACCELERATION_DEVICE = BLOCKS.registerBlock(
            "acceleration_device", AccelerationDevice::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<AnnihilationTntBlock> ANNIHILATION_TNT = BLOCKS.registerBlock("annihilation_tnt", AnnihilationTntBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instabreak().sound(SoundType.GRASS).ignitedByLava());

    public static final DeferredBlock<Block> MACHINE_FRAME = BLOCKS.registerSimpleBlock("machine_frame",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
    public static final DeferredBlock<StructureFrameBlock> STRUCTURE_FRAME = BLOCKS.registerBlock(
            "structure_frame", StructureFrameBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.2f, 11f).sound(SoundType.METAL));

    public static final DeferredBlock<BuddingParadox> ALMOST_FLAWLESS_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "almost_flawless_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> FLAWED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "flawed_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> CHIPPED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "chipped_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> DAMAGED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "damaged_budding_paradox", BuddingParadox::new);

    public static final DeferredBlock<GravityFactory> GRAVITY_FACTORY = Blocks.BLOCKS.registerBlock(
            "gravity_processing_factory", GravityFactory::new);
    public static final DeferredBlock<FusionController> FUSION_CONTROLLER = BLOCKS.registerBlock("fusion_controller",
            FusionController::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
    public static final DeferredBlock<ContainmentGlassBlock> CONTAINMENT_GLASS = BLOCKS.registerBlock("containment_glass",
            ContainmentGlassBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1.5f, 11f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false));

    public static final DeferredBlock<Block> ENERGY_PORT = BLOCKS.registerSimpleBlock("energy_port",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));

    public static final DeferredBlock<com.lumina.ae2_gvt.storage.WhiteHoleEnergyCellBlock> WHITE_HOLE_ENERGY_CELL = BLOCKS.registerBlock(
            "white_hole_energy_cell", com.lumina.ae2_gvt.storage.WhiteHoleEnergyCellBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));

    public static final DeferredBlock<DummyTwinStarCPU> TWINSTAR_CPU = Blocks.BLOCKS.registerBlock(
            "dummy_twinstar_cpu", DummyTwinStarCPU::new);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
