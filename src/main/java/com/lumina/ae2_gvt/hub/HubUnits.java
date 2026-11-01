package com.lumina.ae2_gvt.hub;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * The Twin-Star Hub's units: black-hole crafting units (capacity) and white-hole computing units
 * (parallel) in four tiers (singular, imaginary, paradox, planck). They go into the controller, one
 * stack of one tier per side, up to 64; the hub gets each unit's value times the count. The blank
 * unit and the black / white hole cores are ingredients of the controller.
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

    /**
     * Per unit, by tier (singular, imaginary, paradox, planck): black-hole capacity (items the jobs may
     * hold at once) and white-hole parallel count (operations per tick per job); null = unlimited.
     */
    // capacity: 2^21 (about 2.1 million) a singular unit, ×2^18 a tier; 64 paradox units = 2^63, a long's
    // limit (user, 2026-10-28). Beyond that: planck, unlimited
    private static final BigInteger[] CAPACITY = {
            BigInteger.ONE.shiftLeft(15).subtract(BigInteger.ONE),
            BigInteger.ONE.shiftLeft(31).subtract(BigInteger.ONE),
            BigInteger.ONE.shiftLeft(47).subtract(BigInteger.ONE),
            null};
    // parallel: 2^7 = 128 a singular unit, ×2^9 a tier; 64 paradox units = 2^31, an int's limit (user, 2026-10-28)
    private static final BigInteger[] PARALLEL = {
            BigInteger.ONE.shiftLeft(7).subtract(BigInteger.ONE),
            BigInteger.ONE.shiftLeft(15).subtract(BigInteger.ONE),
            BigInteger.ONE.shiftLeft(23).subtract(BigInteger.ONE),
            null};

    private HubUnits() {
    }

    /** Tier of a black-hole crafting unit (0 = singular … 3 = planck), −1 for anything else. */
    public static int craftingTier(ItemStack stack) {
        for (int i = 0; i < CRAFTING.size(); i++)
            if (stack.is(CRAFTING.get(i).get()))
                return i;
        return -1;
    }

    /** Tier of a white-hole computing unit, −1 for anything else. */
    public static int computingTier(ItemStack stack) {
        for (int i = 0; i < COMPUTING.size(); i++)
            if (stack.is(COMPUTING.get(i).get()))
                return i;
        return -1;
    }

    /** Capacity of {@code count} units of a tier; null = unlimited. */
    public static @Nullable BigInteger capacity(int tier, int count) {
        return CAPACITY[tier] == null ? null : CAPACITY[tier].multiply(BigInteger.valueOf(count));
    }

    /** Parallel count of {@code count} units of a tier; null = unlimited. */
    public static @Nullable BigInteger parallel(int tier, int count) {
        return PARALLEL[tier] == null ? null : PARALLEL[tier].multiply(BigInteger.valueOf(count));
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
