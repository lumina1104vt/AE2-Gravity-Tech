package com.lumina.ae2_gvt.client.guide;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.recipes.FusionRecipe;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import guideme.compiler.tags.RecipeTypeMappingSupplier;
import guideme.document.block.LytBlock;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import net.minecraft.world.item.ItemStack;
import com.lumina.twinstar.util.RecipeHolder;
import com.lumina.ae2_gvt.recipes.SizedIngredient;

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
    }

    private static LytBlock gravityProcessing(GravityProcessingRecipe r) {
        return LytStandardRecipeBox.builder()
                .icon(Blocks.GRAVITY_FACTORY.get())
                .title(Blocks.GRAVITY_FACTORY.get().getName().getString())
                .input(LytSlotGrid.rowFromStacks(stacks(r.ingredients()), false))
                .output(LytSlotGrid.rowFromStacks(r.results(), false))
                .build(r);
    }

    private static LytBlock fusion(FusionRecipe r) {
        return LytStandardRecipeBox.builder()
                .icon(Blocks.FUSION_CONTROLLER.get())
                .title(Blocks.FUSION_CONTROLLER.get().getName().getString())
                .input(LytSlotGrid.rowFromStacks(stacks(r.ingredients()), false))
                .output(LytSlotGrid.rowFromStacks(r.results(), false))
                .build(r);
    }

    /** The first item of each ingredient, in its amount. */
    private static List<ItemStack> stacks(List<SizedIngredient> ingredients) {
        List<ItemStack> out = new ArrayList<>();
        for (SizedIngredient in : ingredients) {
            ItemStack[] items = in.ingredient().getItems();
            if (items.length > 0) {
                ItemStack one = items[0].copy();
                one.setCount(in.count());
                out.add(one);
            }
        }
        return out;
    }
}
