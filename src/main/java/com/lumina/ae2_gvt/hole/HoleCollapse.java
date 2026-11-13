package com.lumina.ae2_gvt.hole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.recipes.ExplosionRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * What an explosion makes of the items lying together in it: the {@code ae2_gvt:explosion} recipes. The ones that
 * ship with the mod: chaos matter + a singularity + a large amount of gravity crystal dust collapse into a black hole
 * (with an anti-singularity instead, a white hole), and a singularity + an anti-singularity + paradox crystal dust
 * make two 悖論量子糾纏態奇點. See {@link ExplosionRecipe} for what a recipe can say.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID)
public final class HoleCollapse {
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
        if (items.isEmpty()) {
            return;
        }
        List<ExplosionRecipe> recipes = level.getRecipeManager().getAllRecipesFor(Recipes.EXPLOSION.get()).stream()
                .map(RecipeHolder::value)
                .sorted(Comparator.comparingInt(ExplosionRecipe::priority).reversed())
                .toList();
        boolean keepItems = false;
        for (ExplosionRecipe recipe : recipes) {
            if (!apply(level, recipe, items)) {
                continue;
            }
            if (recipe.shield() == ExplosionRecipe.Shield.ALL) {
                // nothing else is hurt: no items, no blocks (so no other hole either), and no other recipe is tried
                event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
                event.getAffectedBlocks().clear();
                return;
            }
            keepItems |= recipe.shield() == ExplosionRecipe.Shield.ITEMS;
        }
        if (keepItems) {
            // the items lying there (and what was made) survive; blocks are hurt as usual
            event.getAffectedEntities().removeIf(e -> e instanceof ItemEntity);
        }
    }

    /** Makes {@code recipe} of the items around each of its center items, as many times as it is asked to; true if it made something. */
    private static boolean apply(ServerLevel level, ExplosionRecipe recipe, List<ItemEntity> items) {
        boolean made = false;
        for (ItemEntity core : items) {
            if (core.isAlive() && recipe.center().test(core.getItem())) {
                made |= gather(level, recipe, core, items);
            }
        }
        return made;
    }

    private static boolean gather(ServerLevel level, ExplosionRecipe recipe, ItemEntity core, List<ItemEntity> items) {
        double reach = recipe.radius() * recipe.radius();
        List<ItemEntity> near = items.stream().filter(i -> i.isAlive() && i.distanceToSqr(core) <= reach).toList();
        Predicate<ItemStack> center = recipe.center();
        int times = count(near, center);
        for (SizedIngredient need : recipe.ingredients()) {
            times = Math.min(times, count(near, need.ingredient()) / Math.max(1, need.count()));
        }
        if (times <= 0) {
            return false;
        }
        if (!recipe.repeat()) {
            times = 1;
        }
        take(near, center, times);
        for (SizedIngredient need : recipe.ingredients()) {
            take(near, need.ingredient(), need.count() * times);
        }
        for (ItemStack result : recipe.results()) {
            ItemStack made = result.copyWithCount(result.getCount() * times);
            while (!made.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, core.getX(), core.getY(), core.getZ(), made.split(made.getMaxStackSize())));
            }
        }
        recipe.block().ifPresent(id -> {
            // in the block the center lies in (over whatever loose thing is there: grass, water). Put there a tick later,
            // when the explosion is over: AE2's tiny TNT tells the event first and only then picks the blocks it breaks,
            // and would break the new hole with them
            Block block = BuiltInRegistries.BLOCK.get(id);
            BlockPos at = core.blockPosition();
            level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
                if (level.getBlockState(at).canBeReplaced()) {
                    level.setBlockAndUpdate(at, block.defaultBlockState());
                }
            }));
        });
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
