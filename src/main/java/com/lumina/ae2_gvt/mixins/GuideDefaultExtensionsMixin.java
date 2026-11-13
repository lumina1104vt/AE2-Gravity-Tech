package com.lumina.ae2_gvt.mixins;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.client.guide.GuideRecipes;

import guideme.compiler.tags.RecipeTypeMappingSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GuideME's recipe tags only know the recipe types the guide was built with, and the guide our pages are in is AE2's.
 * GuideME gives every guide it builds a list of recipe types of its own; ours are put in that list. Without GuideME
 * (or with a GuideME this does not fit) nothing happens and the recipe tags simply do not show our recipes.
 */
@Pseudo
@Mixin(targets = "guideme.internal.extensions.DefaultExtensions", remap = false)
public abstract class GuideDefaultExtensionsMixin {
    @Inject(method = "vanillaRecipeTypes", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ae2_gvt$recipeTypes(CallbackInfoReturnable<List<RecipeTypeMappingSupplier>> cir) {
        List<RecipeTypeMappingSupplier> all = new ArrayList<>(cir.getReturnValue());
        all.add(new GuideRecipes());
        cir.setReturnValue(all);
    }
}
