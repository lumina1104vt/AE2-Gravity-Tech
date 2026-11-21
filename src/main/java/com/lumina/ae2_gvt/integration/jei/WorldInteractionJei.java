package com.lumina.ae2_gvt.integration.jei;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.integration.WorldInteraction;
import com.lumina.ae2_gvt.integration.WorldScene;
import com.lumina.lmb.compat.CountedItemRenderer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
        return new ResourceLocation(AE2GravityTech.MODID, "world_interaction");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null)
            registration.addRecipes(TYPE, WorldInteraction.all(level.getRecipeManager()));
    }

    private static final class Category implements IRecipeCategory<WorldInteraction> {
        private final IDrawable icon;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemStack(new ItemStack(GravityHole.BLACK_HOLE_ITEM.get()));
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
            return WorldScene.WIDTH;
        }

        @Override
        public int getHeight() {
            return WorldScene.HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, WorldInteraction w, IFocusGroup focuses) {
            WorldScene.Layout layout = WorldScene.of(w);
            for (WorldScene.Slot s : layout.slots()) {
                RecipeIngredientRole role = switch (s.part()) {
                    case INPUT -> RecipeIngredientRole.INPUT;
                    case CONDITION -> RecipeIngredientRole.CATALYST;
                    case OUTPUT -> RecipeIngredientRole.OUTPUT;
                };
                ItemStack stack = switch (s.part()) {
                    case INPUT -> w.inputs().get(s.index());
                    case CONDITION -> w.conditions().get(s.index());
                    case OUTPUT -> w.outputs().get(s.index());
                };
                builder.addSlot(role, s.x() + 1, s.y() + 1).setCustomRenderer(VanillaTypes.ITEM_STACK, CountedItemRenderer.INSTANCE)
                        .addItemStack(stack).setStandardSlotBackground();
            }
            // an explosion is a picture, not a slot, but pressing U on TNT still finds the page
            for (int i : layout.hidden())
                builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(w.conditions().get(i));
        }

        @Override
        public void draw(WorldInteraction w, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
            WorldScene.draw(g, WorldScene.of(w));
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, WorldInteraction w, IRecipeSlotsView slots, double mouseX, double mouseY) {
            WorldScene.tooltip(WorldScene.of(w), mouseX, mouseY).forEach(tooltip::add);
        }
    }
}
