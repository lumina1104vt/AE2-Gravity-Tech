package com.lumina.ae2_gvt.hole;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.core.definitions.AEItems;

import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Artificial holes: chaos matter + a singularity + a large amount of crystal dust, dropped together
 * and caught in an explosion, collapse into a hole - an AE2 singularity makes a black hole, an
 * anti-singularity a white hole. The hole is the same as a natural one. An explosion that makes a
 * hole destroys no items, no other holes nearby and no blocks.
 * <p>
 * Without chaos matter, a singularity + an anti-singularity + paradox crystal dust caught in an
 * explosion make two 悖論量子糾纏態奇點 ({@link #entangle}).
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID)
public final class HoleCollapse {
    /**
     * Gravity crystal dust needed. Only gravity dust: anti-gravity (and so paradox) crystal needs a
     * white hole first, and this is where white holes come from.
     */
    static final int DUST = 64;
    /** How close the ingredients must lie to the chaos matter, in blocks. */
    private static final double REACH = 1.5;

    private HoleCollapse() {
    }

    @SubscribeEvent
    static void onDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<ItemEntity> items = new ArrayList<>();
        event.getAffectedEntities().forEach(e -> {
            if (e instanceof ItemEntity item) {
                items.add(item);
            }
        });
        boolean collapsed = false;
        for (ItemEntity core : items) {
            if (core.isAlive() && core.getItem().is(Items.CHAOS_MATTER.get())) {
                collapsed |= collapse(level, core, items);
            }
        }
        if (collapsed) {
            // nothing else is hurt: no items, no blocks (so no other hole either)
            event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
            event.getAffectedBlocks().clear();
            return;
        }
        boolean entangled = false;
        for (ItemEntity anti : items) {
            if (anti.isAlive() && anti.getItem().is(Items.ANTI_SINGULARITY.get())) {
                entangled |= entangle(level, anti, items);
            }
        }
        if (entangled) {
            // the items lying there (and the new singularities) survive; blocks are hurt as usual
            event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
        }
    }

    /**
     * Singularity + anti-singularity + paradox crystal dust around {@code anti}, one of each, make two
     * 悖論量子糾纏態奇點 (as AE2's singularity + ender dust make two quantum entangled singularities);
     * as many times over as all three are there.
     */
    private static boolean entangle(ServerLevel level, ItemEntity anti, List<ItemEntity> items) {
        List<ItemEntity> near = items.stream()
                .filter(i -> i.isAlive() && i.distanceToSqr(anti) <= REACH * REACH).toList();
        Predicate<ItemStack> antiSingularity = s -> s.is(Items.ANTI_SINGULARITY.get());
        Predicate<ItemStack> singularity = s -> s.is(AEItems.SINGULARITY.asItem());
        Predicate<ItemStack> dust = s -> s.is(AE2GravityTech.PARADOX.dust().get());
        int n = Math.min(count(near, antiSingularity), Math.min(count(near, singularity), count(near, dust)));
        if (n <= 0) {
            return false;
        }
        take(near, antiSingularity, n);
        take(near, singularity, n);
        take(near, dust, n);
        ItemStack made = new ItemStack(Items.PARADOX_QUANTUM_ENTANGLED_SINGULARITY.get(), n * 2);
        while (!made.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, anti.getX(), anti.getY(), anti.getZ(), made.split(made.getMaxStackSize())));
        }
        return true;
    }

    /** Collapses the ingredients around {@code core} into a hole, if they are all there. */
    private static boolean collapse(ServerLevel level, ItemEntity core, List<ItemEntity> items) {
        List<ItemEntity> near = items.stream()
                .filter(i -> i.isAlive() && i.distanceToSqr(core) <= REACH * REACH).toList();
        Predicate<ItemStack> dust = s -> s.is(AE2GravityTech.GRAVITY.dust().get());
        Predicate<ItemStack> singularity = s -> s.is(AEItems.SINGULARITY.asItem());
        Predicate<ItemStack> anti = s -> s.is(Items.ANTI_SINGULARITY.get());
        boolean white = count(near, singularity) == 0;
        if (count(near, dust) < DUST || count(near, white ? anti : singularity) < 1) {
            return false;
        }
        take(near, s -> s.is(Items.CHAOS_MATTER.get()), 1);
        take(near, white ? anti : singularity, 1);
        take(near, dust, DUST);
        // in the block the chaos matter lies in (over whatever loose thing is there: grass, water)
        BlockPos at = core.blockPosition();
        if (level.getBlockState(at).canBeReplaced())
            level.setBlockAndUpdate(at, (white ? GravityHole.WHITE_HOLE : GravityHole.BLACK_HOLE).get().defaultBlockState());
        return true;
    }

    private static int count(List<ItemEntity> items, Predicate<ItemStack> what) {
        return items.stream().filter(i -> what.test(i.getItem())).mapToInt(i -> i.getItem().getCount()).sum();
    }

    private static void take(List<ItemEntity> items, Predicate<ItemStack> what, int amount) {
        for (ItemEntity i : items) {
            ItemStack stack = i.getItem();
            if (amount > 0 && i.isAlive() && what.test(stack)) {
                int n = Math.min(amount, stack.getCount());
                amount -= n;
                if (n == stack.getCount()) {
                    i.discard();
                } else {
                    i.setItem(stack.copyWithCount(stack.getCount() - n));
                }
            }
        }
    }
}
