package com.lumina.ae2_gvt.prototype.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTermMenu;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarTerminal;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI's [+] for the Twin-Star Pattern Terminal: fills the recipe's inputs and outputs into the
 * pattern, and the machine slot with the recipe category's work block (its first crafting station), so
 * the player never has to say which machine a pattern is for.
 */
@JeiPlugin
public class TwinStarJeiPlugin implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new PatternTransfer(registration.getTransferHelper()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private record PatternTransfer(IRecipeTransferHandlerHelper helper) implements IUniversalRecipeTransferHandler<TwinStarPatternTermMenu> {
        @Override
        public Class<? extends TwinStarPatternTermMenu> getContainerClass() {
            return TwinStarPatternTermMenu.class;
        }

        @Override
        public Optional<MenuType<TwinStarPatternTermMenu>> getMenuType() {
            return Optional.of(TwinStarTerminal.MENU.get());
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(TwinStarPatternTermMenu menu, Object recipe, IRecipeSlotsView recipeSlots,
                                                             Player player, boolean maxTransfer, boolean doTransfer) {
            // every alternative of each input, so AE2 can pick the one the network has
            List<List<GenericStack>> inputs = new ArrayList<>();
            for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                List<GenericStack> alternatives = slot.getAllIngredients().map(PatternTransfer::toGenericStack).filter(s -> s != null).toList();
                if (!alternatives.isEmpty()) {
                    inputs.add(alternatives);
                }
            }
            List<GenericStack> outputs = new ArrayList<>();
            for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
                slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst())
                        .map(PatternTransfer::toGenericStack).ifPresent(outputs::add);
            }
            if (inputs.isEmpty() || outputs.isEmpty()) {
                return helper.createUserErrorWithTooltip(Component.translatable("ae2_gvt.jei.pattern_needs_input_and_output"));
            }
            if (!doTransfer) {
                return null;
            }

            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
            menu.getMachineSlot().setFilterTo(findMachine(recipe));   // an empty stack clears the slot
            return null;
        }

        private static @Nullable GenericStack toGenericStack(ITypedIngredient<?> ingredient) {
            Optional<ItemStack> item = ingredient.getItemStack();
            if (item.isPresent()) {
                return GenericStack.fromItemStack(item.get());
            }
            return ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).map(GenericStack::fromFluidStack).orElse(null);
        }

        /** The work block of the category this recipe is shown in: its first crafting station. */
        private static ItemStack findMachine(Object recipe) {
            IJeiRuntime jei = runtime;
            if (jei == null) {
                return ItemStack.EMPTY;
            }
            IRecipeManager recipes = jei.getRecipeManager();
            List<IRecipeCategory<?>> candidates = recipes.createRecipeCategoryLookup().includeHidden().get()
                    .filter(category -> category.getRecipeType().getRecipeClass().isInstance(recipe)).toList();
            IRecipeCategory<?> match = null;
            if (recipe instanceof RecipeHolder<?> holder) {
                // every vanilla-style category shares the RecipeHolder class; JEI names their type
                // after the recipe type, so match on that
                Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
                match = candidates.stream().filter(category -> category.getRecipeType().getUid().equals(typeId)).findFirst().orElse(null);
            }
            if (match == null) {
                // otherwise take the only candidate, or ask which category actually lists the recipe
                match = candidates.size() == 1 ? candidates.get(0)
                        : candidates.stream().filter(category -> lists(recipes, category.getRecipeType(), recipe)).findFirst().orElse(null);
            }
            if (match == null) {
                return ItemStack.EMPTY;
            }
            return recipes.createCraftingStationLookup(match.getRecipeType()).getItemStack().findFirst()
                    .map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
        }

        private static boolean lists(IRecipeManager recipes, IRecipeType<?> type, Object recipe) {
            return recipes.createRecipeLookup(type).includeHidden().get().anyMatch(r -> r == recipe);
        }
    }
}
