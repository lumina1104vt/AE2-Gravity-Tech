package com.lumina.ae2_gvt.integration.jei;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.SequencePattern;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;
import com.lumina.ae2_gvt.terminal.TwinStarTerminal;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI's [+] for the Twin-Star Pattern Terminal: fills the recipe's inputs and outputs into the
 * pattern, and the machine slot with the recipe category's work block (its first crafting station), so
 * the player never has to say which machine a pattern is for; the recipe's id goes to the server,
 * which reads the probabilities of its outputs from the recipe. Items and fluids are read directly,
 * other ingredients (Mekanism's chemicals) through AE2 JEI Integration's converters when installed.
 */
@JeiPlugin
public class TwinStarJeiPlugin implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new PatternTransfer(registration.getTransferHelper()));
    }

    /**
     * A button beside every recipe that makes a sequence pattern of it ({@link SequencePattern}): every material in the
     * place the recipe has it, one item each, with the GUI slot of that place. Shown while the pattern terminal is open.
     */
    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        IDrawable icon = registration.getJeiHelpers().getGuiHelper().createDrawable(SequencePattern.TEXTURE, 0, 12, 12, 12);
        registration.addRecipeButtonFactory(new IRecipeButtonControllerFactory() {
            @Override
            public <T> IIconButtonController createButtonController(IRecipeLayoutDrawable<T> layout) {
                return new IIconButtonController() {
                    @Override
                    public boolean onPress(IJeiUserInput input) {
                        if (input.isSimulate()) {
                            return SequencePattern.terminal() != null;
                        }
                        return fillSequence(layout);
                    }

                    @Override
                    public void getTooltips(ITooltipBuilder tooltip) {
                        tooltip.add(SequencePattern.tooltip());
                    }

                    @Override
                    public void initState(IButtonState state) {
                        state.setIcon(icon);
                    }

                    @Override
                    public void updateState(IButtonState state) {
                        boolean open = SequencePattern.terminal() != null;
                        state.setVisible(open);
                        state.setActive(open);
                    }
                };
            }
        });
    }

    /** The recipe as it is laid out, slot by slot, into the open terminal. */
    private static boolean fillSequence(IRecipeLayoutDrawable<?> layout) {
        IRecipeSlotsView view = layout.getRecipeSlotsView();
        List<GenericStack> inputs = new ArrayList<>();
        for (IRecipeSlotView slot : view.getSlotViews(RecipeIngredientRole.INPUT)) {
            inputs.add(slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst())
                    .map(PatternTransfer::toGenericStack).orElse(null));
        }
        while (!inputs.isEmpty() && inputs.get(inputs.size() - 1) == null) {
            inputs.remove(inputs.size() - 1);
        }
        List<GenericStack> outputs = new ArrayList<>();
        for (IRecipeSlotView slot : view.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst())
                    .map(PatternTransfer::toGenericStack).ifPresent(outputs::add);
        }
        IRecipeCategory<?> category = layout.getRecipeCategory();
        return SequencePattern.open(new SequencePattern.Request(inputs, outputs, PatternTransfer.machineOf(category),
                PatternTransfer.idOf(category, layout.getRecipe())));
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
            return Optional.empty();   // the terminal part and the wireless terminal have this menu class, with a menu type each
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
            IRecipeCategory<?> category = findCategory(recipe);
            menu.getMachineSlot().setFilterTo(machineOf(category));   // an empty stack clears the slot
            // also puts in the sub-machines last used with that machine. Not the category's other stations:
            // those are other machines that do the same alone (every modded crafting table), not parts of one
            menu.setRecipeChances(idOf(category, recipe));
            return null;
        }

        @SuppressWarnings("unchecked")
        private static @Nullable ResourceLocation idOf(@Nullable IRecipeCategory<?> category, Object recipe) {
            if (category == null) {
                return null;
            }
            try {
                return ((IRecipeCategory<Object>) category).getRegistryName(recipe);
            } catch (RuntimeException e) {
                return null;
            }
        }

        private static @Nullable GenericStack toGenericStack(ITypedIngredient<?> ingredient) {
            Optional<ItemStack> item = ingredient.getItemStack();
            if (item.isPresent()) {
                return GenericStack.fromItemStack(item.get());
            }
            Optional<GenericStack> fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).map(GenericStack::fromFluidStack);
            return fluid.isPresent() ? fluid.get() : Converters.convert(ingredient);
        }

        /** The category this recipe is shown in. */
        private static @Nullable IRecipeCategory<?> findCategory(Object recipe) {
            IJeiRuntime jei = runtime;
            if (jei == null) {
                return null;
            }
            IRecipeManager recipes = jei.getRecipeManager();
            List<IRecipeCategory<?>> candidates = recipes.createRecipeCategoryLookup().includeHidden().get()
                    .filter(category -> category.getRecipeType().getRecipeClass().isInstance(recipe)).toList();
            IRecipeCategory<?> match = null;
            if (recipe instanceof RecipeHolder<?> holder) {
                // every vanilla-style category shares the RecipeHolder class; JEI names their type
                // after the recipe type, so match on that
                ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
                match = candidates.stream().filter(category -> category.getRecipeType().getUid().equals(typeId)).findFirst().orElse(null);
            }
            if (match == null) {
                // otherwise take the only candidate, or ask which category actually lists the recipe
                match = candidates.size() == 1 ? candidates.get(0)
                        : candidates.stream().filter(category -> lists(recipes, category.getRecipeType(), recipe)).findFirst().orElse(null);
            }
            return match;
        }

        /** The work block of a category: its first crafting station. */
        private static ItemStack machineOf(@Nullable IRecipeCategory<?> category) {
            IJeiRuntime jei = runtime;
            if (jei == null || category == null) {
                return ItemStack.EMPTY;
            }
            return jei.getRecipeManager().createRecipeCatalystLookup(category.getRecipeType()).getItemStack().findFirst()
                    .map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
        }

        private static boolean lists(IRecipeManager recipes, RecipeType<?> type, Object recipe) {
            return recipes.createRecipeLookup(type).includeHidden().get().anyMatch(r -> r == recipe);
        }
    }

    /**
     * Other ingredient types (Mekanism's chemicals): the converters other mods register with AE2 JEI
     * Integration (Applied Mekanistics does for chemicals), found by reflection so that mod stays
     * optional. Without it only items and fluids come over.
     */
    private static final class Converters {
        private static final String CLASS = "tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters";
        private static @Nullable Method getConverter;
        private static @Nullable Method toStack;
        private static boolean looked;

        static @Nullable GenericStack convert(ITypedIngredient<?> ingredient) {
            if (!looked) {
                looked = true;
                try {
                    Class<?> converters = Class.forName(CLASS);
                    getConverter = converters.getMethod("getConverter", IIngredientType.class);
                    toStack = Class.forName("tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter")
                            .getMethod("getStackFromIngredient", Object.class);
                } catch (ReflectiveOperationException | LinkageError e) {
                    getConverter = null;    // AE2 JEI Integration is not installed
                }
            }
            if (getConverter == null || toStack == null) {
                return null;
            }
            try {
                Object converter = getConverter.invoke(null, ingredient.getType());
                if (converter == null) {
                    return null;
                }
                return toStack.invoke(converter, ingredient.getIngredient()) instanceof GenericStack stack ? stack : null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                AE2GravityTech.LOGGER.debug("AE2 JEI Integration could not convert {}", ingredient, e);
                return null;
            }
        }
    }
}
