package com.lumina.ae2_gvt.hole;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.items.Items;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredBlock;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredItem;
import com.lumina.ae2_gvt.reg.DeferredRegister;

/**
 * Black and white holes: blocks, so crystal buds can grow on them (black: gravity crystal, white:
 * anti-gravity crystal, as from AE2's budding certus, but they never decay). Only black holes will
 * spawn naturally (not yet); both can be made artificially ({@link HoleCollapse}). They can't be
 * mined: only an explosion destroys one, leaving chaos matter.
 */
public final class GravityHole {
    /** Hole radius in blocks: what is drawn, and what an item must touch to be swallowed. */
    public static final float RADIUS = 0.35f;

    public static final DeferredBlock<GravityHoleBlock> BLACK_HOLE = Blocks.BLOCKS.registerBlock("black_hole",
            p -> new GravityHoleBlock(false, p), properties(MapColor.COLOR_BLACK));
    public static final DeferredBlock<GravityHoleBlock> WHITE_HOLE = Blocks.BLOCKS.registerBlock("white_hole",
            p -> new GravityHoleBlock(true, p), properties(MapColor.SNOW));
    /** Creative only: in survival a hole is made, never carried. */
    public static final DeferredItem<BlockItem> BLACK_HOLE_ITEM = Items.ITEMS.registerSimpleBlockItem(BLACK_HOLE);
    public static final DeferredItem<BlockItem> WHITE_HOLE_ITEM = Items.ITEMS.registerSimpleBlockItem(WHITE_HOLE);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityHoleBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("gravity_hole",
                    () -> BlockEntityType.Builder.of(GravityHoleBlockEntity::new, BLACK_HOLE.get(), WHITE_HOLE.get()).build(null));

    private GravityHole() {
    }

    /**
     * Unbreakable by hand, but no resistance to explosions; no pushing by pistons. Forced solid: its
     * shape is too small to count as solid by itself, and water and lava wash non-solid blocks away.
     */
    private static BlockBehaviour.Properties properties(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0f, 0.0f).noOcclusion()
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false)
                .pushReaction(PushReaction.BLOCK).randomTicks().forceSolidOn();
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
