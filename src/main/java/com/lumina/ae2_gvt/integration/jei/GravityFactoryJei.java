package com.lumina.ae2_gvt.integration.jei;

import java.util.Arrays;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.IModPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusGroup;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.lumina.twinstar.algorithm.ae2.RecipeHolder;
import com.lumina.ae2_gvt.recipes.SizedIngredient;

/**
 * 引力加工工廠 recipes in JEI: the inputs (4 × 3, as the factory's input slots), an arrow, the results,
 * and the time, energy and gravitons under them. The factory is the catalyst.
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
        if (level != null)
            registration.addRecipes(TYPE, RecipeHolder.allOf(level.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Blocks.GRAVITY_FACTORY.get(), TYPE);
        registration.addRecipeCatalyst(Items.CHAOS_MATTER.get(), TYPE);
    }

    private static final class Category implements IRecipeCategory<RecipeHolder<GravityProcessingRecipe>> {
        private static final int SLOT = 18, COLUMNS = 4, ROWS = 3, ARROW_X = COLUMNS * SLOT + 6, OUT_X = ARROW_X + 30;
        private static final int WIDTH = OUT_X + 3 * SLOT, HEIGHT = ROWS * SLOT + 22;
        private final IDrawable icon;
        private final IDrawableStatic arrow;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemLike(Blocks.GRAVITY_FACTORY.get());
            this.arrow = gui.getRecipeArrow();
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
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GravityProcessingRecipe> holder, IFocusGroup focuses) {
            GravityProcessingRecipe r = holder.value();
            for (int i = 0; i < r.ingredients().size() && i < COLUMNS * ROWS; i++) {
                SizedIngredient in = r.ingredients().get(i);
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * SLOT, 1 + (i / COLUMNS) * SLOT)
                        .setStandardSlotBackground()
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStacks(Arrays.stream(in.ingredient().getItems()).map(s -> s.copyWithCount(in.count())).toList());
            }
            // paid from the network and shown as text: U on them still finds the recipe
            if (r.gravitons() > 0)
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Gravitons.GRAVITON.get()));
            if (r.antigravitons() > 0)
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(Gravitons.ANTIGRAVITON.get()));
            List<ItemStack> results = r.results();
            for (int i = 0; i < results.size() && i < 3 * ROWS; i++)
                builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1 + (i % 3) * SLOT, 1 + (i / 3) * SLOT)
                        .setStandardSlotBackground()
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStacks(List.of(results.get(i)));
        }

        @Override
        public void draw(RecipeHolder<GravityProcessingRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            arrow.draw(g, ARROW_X, (ROWS * SLOT - 17) / 2);
            GravityProcessingRecipe r = holder.value();
            Component info = Component.translatable("gui.ae2_gvt.factory.jei.info",
                    String.format("%.1f", r.time() / 20.0), RecipeNumbers.compact(r.energy()));
            RecipeNumbers.drawLine(g, info, 0, ROWS * SLOT + 3, WIDTH, 0xFF404040);
        // gravitons / anti-gravitons taken from the network when it finishes
        MutableComponent cost = Component.empty();
        if (r.gravitons() > 0)
            cost.append(Component.translatable("item.ae2_gvt.graviton")).append(" " + RecipeNumbers.compact(r.gravitons()));
        if (r.antigravitons() > 0)
            cost.append(r.gravitons() > 0 ? "・" : "").append(Component.translatable("item.ae2_gvt.antigraviton"))
                    .append(" " + RecipeNumbers.compact(r.antigravitons()));
        RecipeNumbers.drawLine(g, cost, 0, ROWS * SLOT + 13, WIDTH, 0xFF404040);
        }
    }
}
