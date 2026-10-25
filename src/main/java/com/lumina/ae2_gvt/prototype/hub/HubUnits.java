package com.lumina.ae2_gvt.prototype.hub;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Twin-Star Hub's internal units, which make up its sphere: black-hole crafting units and
 * white-hole computing units in four tiers (singular, imaginary, paradox, planck), one blank unit
 * shared by both sides, and the black hole core and white hole core (one of each per hub). Blocks only for now - what each tier adds comes with the real hub.
 */
public final class HubUnits {
    public static final List<String> TIERS = List.of("singular", "imaginary", "paradox", "planck");

    public static final DeferredItem<BlockItem> BLANK = unit("blank_unit");
    /** One of each per hub, anywhere among the units. */
    public static final DeferredItem<BlockItem> BLACK_HOLE_CORE = unit("black_hole_core");
    public static final DeferredItem<BlockItem> WHITE_HOLE_CORE = unit("white_hole_core");
    public static final List<DeferredItem<BlockItem>> CRAFTING = new ArrayList<>();
    public static final List<DeferredItem<BlockItem>> COMPUTING = new ArrayList<>();

    static {
        for (String tier : TIERS) {
            CRAFTING.add(unit(tier + "_crafting_unit"));
            COMPUTING.add(unit(tier + "_computing_unit"));
        }
    }

    private HubUnits() {
    }

    private static DeferredItem<BlockItem> unit(String name) {
        DeferredBlock<Block> block = AE2GravityTech.BLOCKS.registerSimpleBlock(name,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.2f, 11f).sound(SoundType.METAL));
        return AE2GravityTech.ITEMS.registerSimpleBlockItem(name, block);
    }

    private static java.util.Set<Block> unitBlocks;

    /** Any unit, or one of the two cores: what fills the hub's sphere. */
    public static boolean isUnit(Block block) {
        if (unitBlocks == null) {   // registries are frozen by the time anything asks
            unitBlocks = java.util.Set.copyOf(all().stream().map(item -> item.get().getBlock()).toList());
        }
        return unitBlocks.contains(block);
    }

    /** Creative tab order: the cores, blank, then each tier's crafting and computing unit. */
    public static List<DeferredItem<BlockItem>> all() {
        List<DeferredItem<BlockItem>> all = new ArrayList<>(List.of(BLACK_HOLE_CORE, WHITE_HOLE_CORE, BLANK));
        for (int i = 0; i < TIERS.size(); i++) {
            all.add(CRAFTING.get(i));
            all.add(COMPUTING.get(i));
        }
        return all;
    }

    /** Forces the static registrations to run. */
    public static void init() {
    }
}
