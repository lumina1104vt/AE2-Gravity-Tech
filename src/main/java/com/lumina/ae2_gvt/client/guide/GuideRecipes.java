package com.lumina.ae2_gvt.client.guide;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.recipes.ExplosionRecipe;
import com.lumina.ae2_gvt.recipes.FusionRecipe;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import guideme.compiler.tags.RecipeTypeMappingSupplier;
import guideme.document.block.LytBlock;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Our recipe types for GuideME's recipe tags ({@code <RecipeFor id="ae2_gvt:gravity_dust" />} and the like): the
 * materials in a row, the results in another, under the machine's name. Added to the guides by
 * {@code mixins.GuideDefaultExtensionsMixin}, since the guide the pages are in (AE2's) is built by AE2.
 */
public final class GuideRecipes implements RecipeTypeMappingSupplier {
    @Override
    public void collect(RecipeTypeMappings mappings) {
        mappings.add(Recipes.GRAVITY_PROCESSING.get(), GuideRecipes::gravityProcessing);
        mappings.add(Recipes.FUSION.get(), GuideRecipes::fusion);
        mappings.add(Recipes.EXPLOSION.get(), GuideRecipes::explosion);
    }

    private static LytBlock gravityProcessing(RecipeHolder<GravityProcessingRecipe> holder) {
        GravityProcessingRecipe r = holder.value();
        return LytStandardRecipeBox.builder()
                .icon(Blocks.GRAVITY_FACTORY.get())
                .title(Blocks.GRAVITY_FACTORY.get().getName().getString())
                .input(LytSlotGrid.rowFromStacks(stacks(r.ingredients()), false))
                .output(LytSlotGrid.rowFromStacks(r.results(), false))
                .build(holder);
    }

    private static LytBlock fusion(RecipeHolder<FusionRecipe> holder) {
        FusionRecipe r = holder.value();
        return LytStandardRecipeBox.builder()
                .icon(Blocks.FUSION_CONTROLLER.get())
                .title(Blocks.FUSION_CONTROLLER.get().getName().getString())
                .input(LytSlotGrid.rowFromStacks(stacks(r.ingredients()), false))
                .output(LytSlotGrid.rowFromStacks(r.results(), false))
                .build(holder);
    }

    /** What lies together at the explosion (the centre item first), and what it makes: items, or the block put where the centre lay. */
    private static LytBlock explosion(RecipeHolder<ExplosionRecipe> holder) {
        ExplosionRecipe r = holder.value();
        List<ItemStack> in = new ArrayList<>();
        ItemStack[] centre = r.center().getItems();
        if (centre.length > 0)
            in.add(centre[0]);
        in.addAll(stacks(r.ingredients()));
        List<ItemStack> out = new ArrayList<>(r.results());
        r.block().ifPresent(b -> out.add(new ItemStack(BuiltInRegistries.BLOCK.get(b))));
        return LytStandardRecipeBox.builder()
                .icon(net.minecraft.world.item.Items.TNT)
                .title(Component.translatable("gui.ae2_gvt.world.tip.explosion").getString())
                .input(LytSlotGrid.rowFromStacks(in, false))
                .output(LytSlotGrid.rowFromStacks(out, false))
                .build(holder);
    }

    /** The first item of each ingredient, in its amount. */
    private static List<ItemStack> stacks(List<SizedIngredient> ingredients) {
        List<ItemStack> out = new ArrayList<>();
        for (SizedIngredient in : ingredients) {
            ItemStack[] items = in.getItems();
            if (items.length > 0) {
                out.add(items[0].copyWithCount(in.count()));
            }
        }
        return out;
    }
}
