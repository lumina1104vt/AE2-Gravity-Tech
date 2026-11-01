package com.lumina.ae2_gvt.integration.jei;

import java.util.Arrays;
import java.util.List;

import appeng.api.stacks.GenericStack;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.recipes.FusionRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.IModPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusGroup;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * 核融合發生器 reactions in JEI: the inputs (anything AE2 holds that isn't an item as its wrapped stack),
 * an arrow, the results, and the temperature, time and energy returned under them. The controller is
 * the catalyst.
 */
@JeiPlugin
public class FusionJei implements IModPlugin {
    public static final RecipeType<RecipeHolder<FusionRecipe>> TYPE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion"));

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null)
            registration.addRecipes(TYPE, level.getRecipeManager().getAllRecipesFor(Recipes.FUSION.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Blocks.FUSION_CONTROLLER.get(), TYPE);
    }

    private static final class Category implements IRecipeCategory<RecipeHolder<FusionRecipe>> {
        private static final int SLOT = 18, COLUMNS = 3, ROWS = 3, ARROW_X = COLUMNS * SLOT + 6, OUT_X = ARROW_X + 30;
        private static final int WIDTH = OUT_X + 3 * SLOT, HEIGHT = ROWS * SLOT + 12;
        private final IDrawable icon;
        private final IDrawableStatic arrow;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(Blocks.FUSION_CONTROLLER.get());
            this.arrow = gui.getRecipeArrow();
        }

        @Override
        public RecipeType<RecipeHolder<FusionRecipe>> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("block.ae2_gvt.fusion_controller");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FusionRecipe> holder, IFocusGroup focuses) {
            FusionRecipe r = holder.value();
            int slot = 0;
            for (SizedIngredient in : r.ingredients())
                input(builder, slot++).addItemStacks(Arrays.stream(in.ingredient().getItems()).map(s -> s.copyWithCount(in.count())).toList());
            for (GenericStack key : r.keys())
                input(builder, slot++).addItemStacks(List.of(Gravitons.shown(key)));
            List<ItemStack> results = r.results();
            for (int i = 0; i < results.size() && i < 3 * ROWS; i++)
                builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1 + (i % 3) * SLOT, 1 + (i / 3) * SLOT)
                        .setOutputSlotBackground()
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStacks(List.of(results.get(i)));
        }

        private static mezz.jei.api.gui.builder.IRecipeSlotBuilder input(IRecipeLayoutBuilder builder, int i) {
            return builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * SLOT, 1 + (i / COLUMNS) * SLOT).setStandardSlotBackground()
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE);
        }

        @Override
        public void draw(RecipeHolder<FusionRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            arrow.draw(g, ARROW_X, (ROWS * SLOT - 17) / 2);
            FusionRecipe r = holder.value();
            Component info = Component.translatable("gui.ae2_gvt.fusion.jei.info", RecipeNumbers.compact(r.temperature()),
                    String.format("%.1f", r.time() / 20.0), String.format("%.1f", r.power()));
            RecipeNumbers.drawLine(g, info, 0, ROWS * SLOT + 3, WIDTH, 0xFF404040);
        }
    }
}
