package com.lumina.ae2_gvt.blocks;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.budding.BuddingParadox;
import com.lumina.ae2_gvt.blocks.cpu.DummyWhiteHoleCPU;
import com.lumina.ae2_gvt.prototype.AnnihilationTntBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2GravityTech.MODID);

    public static final DeferredBlock<GravityConstrainer> GRAVITY_CONSTRAINER = BLOCKS.registerBlock(
            "gravity_constrainer", GravityConstrainer::new,
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<GravitonCollector> GRAVITON_COLLECTOR = BLOCKS.registerBlock(
            "graviton_collector", GravitonCollector::new,
            p -> p.mapColor(MapColor.METAL).strength(4f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<AccelerationDevice> ACCELERATION_DEVICE = BLOCKS.registerBlock(
            "acceleration_device", AccelerationDevice::new,
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<AnnihilationTntBlock> ANNIHILATION_TNT = BLOCKS.registerBlock("annihilation_tnt", AnnihilationTntBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).instabreak().sound(SoundType.GRASS).ignitedByLava());

    public static final DeferredBlock<Block> MACHINE_FRAME = BLOCKS.registerSimpleBlock("machine_frame",
            p -> p.mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
    public static final DeferredBlock<com.lumina.ae2_gvt.prototype.frame.StructureFrameBlock> STRUCTURE_FRAME = BLOCKS.registerBlock(
            "structure_frame", com.lumina.ae2_gvt.prototype.frame.StructureFrameBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.2f, 11f).sound(SoundType.METAL));

    public static final DeferredBlock<BuddingParadox> ALMOST_FLAWLESS_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "almost_flawless_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> FLAWED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "flawed_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> CHIPPED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "chipped_budding_paradox", BuddingParadox::new);
    public static final DeferredBlock<BuddingParadox> DAMAGED_BUDDING_PARADOX = Blocks.BLOCKS.registerBlock(
            "damaged_budding_paradox", BuddingParadox::new);

    public static final DeferredBlock<DummyWhiteHoleCPU> WHILE_HOLE_CPU = Blocks.BLOCKS.registerBlock(
            "dummy_white_hole_cpu", DummyWhiteHoleCPU::new);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
