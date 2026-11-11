package com.lumina.ae2_gvt.integration.jei;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.integration.WorldInteraction;
import com.lumina.lmb.compat.CountedItemRenderer;
import com.lumina.lmb.compat.RecipeNumbers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** What happens in the world ({@link WorldInteraction}) in JEI: inputs, what it takes besides, an arrow, what comes of it, a few words. */
@JeiPlugin
public class WorldInteractionJei implements IModPlugin {
    public static final RecipeType<WorldInteraction> TYPE = RecipeType.create(AE2GravityTech.MODID, "world_interaction", WorldInteraction.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "world_interaction");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, WorldInteraction.all());
    }

    private static final class Category implements IRecipeCategory<WorldInteraction> {
        static final int SLOT = 18, STEP = 20, INPUTS = 3, CONDITION_X = INPUTS * STEP + 4, ARROW_X = CONDITION_X + 2 * STEP + 2,
                OUT_X = ARROW_X + 28, OUT_COLUMNS = 3, OUTPUTS = 6, WIDTH = OUT_X + OUT_COLUMNS * STEP,
                // two rows of slots and a line, whatever the interaction: about four to a page
                TEXT_Y = 2 * STEP + 2, HEIGHT = TEXT_Y + 10;
        private final IDrawable icon;
        private final IDrawableStatic arrow;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemStack(new ItemStack(GravityHole.BLACK_HOLE_ITEM.get()));
            this.arrow = gui.getRecipeArrow();
        }

        @Override
        public RecipeType<WorldInteraction> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.ae2_gvt.world.title");
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
        public void setRecipe(IRecipeLayoutBuilder builder, WorldInteraction w, IFocusGroup focuses) {
            for (int i = 0; i < w.inputs().size() && i < INPUTS; i++)
                slot(builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * STEP, 1), w.inputs().get(i)).setStandardSlotBackground();
            for (int i = 0; i < w.conditions().size() && i < 2; i++)
                slot(builder.addSlot(RecipeIngredientRole.CATALYST, CONDITION_X + 1 + i * STEP, 1), w.conditions().get(i)).setStandardSlotBackground();
            for (int i = 0; i < w.outputs().size() && i < OUTPUTS; i++)
                slot(builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1 + i % OUT_COLUMNS * STEP, 1 + i / OUT_COLUMNS * STEP), w.outputs().get(i)).setStandardSlotBackground();
        }

        private static IRecipeSlotBuilder slot(IRecipeSlotBuilder slot, ItemStack stack) {
            return slot.setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE).addItemStack(stack);
        }

        @Override
        public void draw(WorldInteraction w, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            arrow.draw(g, ARROW_X, (2 * STEP - 17) / 2);
                RecipeNumbers.drawLine(g, w.text(), 0, TEXT_Y, WIDTH, 0xFF404040);
        }
    }
}
