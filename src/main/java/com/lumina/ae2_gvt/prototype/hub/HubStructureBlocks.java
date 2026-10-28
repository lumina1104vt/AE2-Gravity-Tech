package com.lumina.ae2_gvt.prototype.hub;

import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Building blocks of the new hexagram hub (design 2026-10-27): the black and white strands of the
 * double helix round the sphere, the six hollow cylinders on the star's tips, and the three floating
 * 3-wide rings, plus the hub's controller. Textures from tools/hub_structure_textures.py. The structure
 * itself is {@link HexagramHub}.
 */
public final class HubStructureBlocks {
    public static final DeferredItem<BlockItem> HELIX_BLACK = item("hub_helix_black",
            AE2GravityTech.BLOCKS.registerBlock("hub_helix_black", HubLookBlock::new, props(MapColor.COLOR_BLACK, SoundType.STONE, 12)));
    public static final DeferredItem<BlockItem> HELIX_WHITE = item("hub_helix_white",
            AE2GravityTech.BLOCKS.registerBlock("hub_helix_white", HubLookBlock::new, props(MapColor.QUARTZ, SoundType.STONE, 12)));
    public static final DeferredItem<BlockItem> PILLAR = item("hub_pillar",
            AE2GravityTech.BLOCKS.registerBlock("hub_pillar", HubPillarBlock::new, props(MapColor.COLOR_GRAY, SoundType.STONE, 10)));
    public static final DeferredItem<BlockItem> RING = item("hub_ring",
            AE2GravityTech.BLOCKS.registerBlock("hub_ring", HubRingBlock::new, props(MapColor.COLOR_BLACK, SoundType.STONE, 15)));
    /** Controller of the hexagram hub, in the middle of its floor. */
    public static final DeferredBlock<TwinStarHubBlock> CONTROLLER_BLOCK = AE2GravityTech.BLOCKS.registerBlock(
            "twin_star_hub", TwinStarHubBlock::new, props(MapColor.METAL, SoundType.METAL, 15));
    public static final DeferredItem<BlockItem> CONTROLLER = item("twin_star_hub", CONTROLLER_BLOCK);

    /**
     * Never placed and never an item: what the crystal blocks are drawn as while the hub is formed - the
     * same material joined to its neighbours (client JoinedLookModel).
     */
    public static final DeferredBlock<Block> GRAVITY_JOINED = look("gravity_block_joined");
    public static final DeferredBlock<Block> ANTIGRAVITY_JOINED = look("antigravity_block_joined");
    public static final DeferredBlock<Block> PARADOX_JOINED = look("paradox_block_joined");
    /** Likewise for the floor's and ceiling's machine frames: joined into one surface. */
    public static final DeferredBlock<Block> FRAME_JOINED = look("machine_frame_joined");

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);
    /** Only there so the client can draw the formed hub's sphere (see client HexagramHubRenderer). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwinStarHubBlockEntity>> CONTROLLER_ENTITY =
            BLOCK_ENTITY_TYPES.register("twin_star_hub", () -> BlockEntityType.Builder.of(TwinStarHubBlockEntity::new, CONTROLLER_BLOCK.get()).build(null));

    private HubStructureBlocks() {
    }

    /**
     * {@code light}: the hub is 60 tall and roofed, so the blocks round the sphere light its inside
     * (ring 15, helix 12, pillars 10) and the controller its dome top (15), which the crystals are too far
     * from; the crystal floor and ceiling light their own levels.
     */
    private static BlockBehaviour.Properties props(MapColor color, SoundType sound, int light) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.2f, 11f).sound(sound).lightLevel(s -> light);
    }

    private static DeferredBlock<Block> look(String name) {
        return AE2GravityTech.BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of().noLootTable());
    }

    private static DeferredItem<BlockItem> item(String name, DeferredBlock<?> block) {
        return AE2GravityTech.ITEMS.registerSimpleBlockItem(name, block);
    }

    /** Creative tab order. */
    public static List<DeferredItem<BlockItem>> all() {
        return List.of(CONTROLLER, HELIX_BLACK, HELIX_WHITE, PILLAR, RING);
    }

    /** Forces the static registrations to run. */
    public static void init() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
