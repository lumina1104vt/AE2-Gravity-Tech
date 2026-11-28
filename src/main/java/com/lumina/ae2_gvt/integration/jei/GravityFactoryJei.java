package com.lumina.ae2_gvt.integration.jei;

import java.util.Arrays;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.client.FactoryRecipeLook;
import com.lumina.ae2_gvt.recipes.BorrowedRecipes;
import com.lumina.ae2_gvt.recipes.SizedFluid;
import mezz.jei.api.forge.ForgeTypes;
import net.minecraftforge.fluids.FluidStack;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.IModPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusGroup;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.lumina.twinstar.algorithm.ae2.RecipeHolder;
import com.lumina.ae2_gvt.recipes.SizedIngredient;

/**
 * 引力加工工廠 recipes in JEI, drawn as the factory's GUI ({@link FactoryRecipeLook}). The factory is the
 * catalyst.
 */
@JeiPlugin
public class GravityFactoryJei implements IModPlugin {
    public static final RecipeType<RecipeHolder<GravityProcessingRecipe>> TYPE =
            new RecipeType<>(new ResourceLocation(AE2GravityTech.MODID, "gravity_processing"), (Class<RecipeHolder<GravityProcessingRecipe>>) (Class<?>) RecipeHolder.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AE2GravityTech.MODID, "gravity_processing");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(TYPE, RecipeHolder.allOf(level.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get())));
            registration.addRecipes(TYPE, BorrowedRecipes.build(level));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Blocks.GRAVITY_FACTORY.get(), TYPE);
        registration.addRecipeCatalyst(Items.CHAOS_MATTER.get(), TYPE);
    }

    private static final class Category implements IRecipeCategory<RecipeHolder<GravityProcessingRecipe>> {
        private final IDrawable icon;
        private final IDrawable kept = new IDrawable() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public int getHeight() {
                return 16;
            }

            @Override
            public void draw(GuiGraphics g, int x, int y) {
                FactoryRecipeLook.drawKept(g, x, y);
            }
        };

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(Blocks.GRAVITY_FACTORY.get());
        }

        @Override
        public RecipeType<RecipeHolder<GravityProcessingRecipe>> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.ae2_gvt.gravity_processing_factory");
        }

        @Override
        public int getWidth() {
            return FactoryRecipeLook.WIDTH;
        }

        @Override
        public int getHeight() {
            return FactoryRecipeLook.HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        // the panel is the factory's own
        @Override
        public boolean needsRecipeBorder() {
            return false;
        }

        private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int left, int i) {
            return builder.addSlot(role, left + (i % FactoryRecipeLook.COLUMNS) * FactoryRecipeLook.SLOT,
                    FactoryRecipeLook.SLOT_Y + (i / FactoryRecipeLook.COLUMNS) * FactoryRecipeLook.SLOT);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GravityProcessingRecipe> holder, IFocusGroup focuses) {
            GravityProcessingRecipe r = holder.value();
            // items, then fluids, then what is kept, in the input slots
            int at = 0;
            for (SizedIngredient in : r.ingredients()) {
                if (at >= FactoryRecipeLook.SLOTS)
                    break;
                slot(builder, RecipeIngredientRole.INPUT, FactoryRecipeLook.INPUT_X, at++)
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStacks(Arrays.stream(in.ingredient().getItems()).map(s -> s.copyWithCount(in.count())).toList());
            }
            for (SizedFluid in : r.fluidIngredients()) {
                if (at >= FactoryRecipeLook.SLOTS)
                    break;
                slot(builder, RecipeIngredientRole.INPUT, FactoryRecipeLook.INPUT_X, at++)
                        .setFluidRenderer(in.amount(), false, 16, 16)
                        .addIngredients(ForgeTypes.FLUID_STACK, in.stacks());
            }
            for (var catalyst : r.catalysts()) {
                if (at >= FactoryRecipeLook.SLOTS)
                    break;
                slot(builder, RecipeIngredientRole.CATALYST, FactoryRecipeLook.INPUT_X, at++)
                        .setOverlay(kept, 0, 0)
                        .addItemStacks(Arrays.asList(catalyst.getItems()))
                        .addRichTooltipCallback((view, tooltip) ->
                                tooltip.add(Component.translatable("gui.ae2_gvt.factory.jei.kept")));
            }
            // paid from the network and shown as text: U on them still finds the recipe
            if (r.gravitons() > 0)
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Gravitons.GRAVITON.get()));
            if (r.antigravitons() > 0)
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Gravitons.ANTIGRAVITON.get()));
            int out = 0;
            for (ItemStack result : r.results()) {
                if (out >= FactoryRecipeLook.SLOTS)
                    break;
                slot(builder, RecipeIngredientRole.OUTPUT, FactoryRecipeLook.OUTPUT_X, out++)
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStacks(List.of(result));
            }
            for (FluidStack result : r.fluidResults()) {
                if (out >= FactoryRecipeLook.SLOTS)
                    break;
                slot(builder, RecipeIngredientRole.OUTPUT, FactoryRecipeLook.OUTPUT_X, out++)
                        .setFluidRenderer(result.getAmount(), false, 16, 16)
                        .addIngredient(ForgeTypes.FLUID_STACK, result);
            }
        }

        @Override
        public void draw(RecipeHolder<GravityProcessingRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            FactoryRecipeLook.draw(g, holder.value());
        }
    }
}
