package com.lumina.ae2_gvt.integration.emi;

import java.util.List;

import appeng.api.stacks.GenericStack;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.client.FactoryRecipeLook;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.integration.WorldInteraction;
import com.lumina.ae2_gvt.client.SequencePattern;
import com.lumina.ae2_gvt.integration.WorldScene;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedEmiSlot;
import com.lumina.lmb.compat.RecipeNumbers;
import com.lumina.ae2_gvt.recipes.BorrowedRecipes;
import com.lumina.ae2_gvt.recipes.FusionRecipe;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * The 引力加工工廠's and 核融合發生器's recipes in EMI, laid out as in JEI (GravityFactoryJei, FusionJei): the
 * inputs, an arrow, the results and a line or two about them. The machine is the workstation.
 */
@EmiEntrypoint
public class GravityTechEmi implements EmiPlugin {
    private static final EmiRecipeCategory FACTORY = new EmiRecipeCategory(id("gravity_processing"), EmiStack.of(Blocks.GRAVITY_FACTORY.get()));
    private static final EmiRecipeCategory FUSION = new EmiRecipeCategory(id("fusion"), EmiStack.of(Blocks.FUSION_CONTROLLER.get()));
    private static final EmiRecipeCategory WORLD = new EmiRecipeCategory(id("world_interaction"), EmiStack.of(GravityHole.BLACK_HOLE_ITEM.get()));
    private static final int SLOT = 18;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, path);
    }

    @Override
    public void register(EmiRegistry registry) {
        // the dispatch marker is no real item: nobody looks it up
        registry.removeEmiStacks(EmiStack.of(com.lumina.ae2_gvt.items.Items.DISPATCH_MARKER.get()));
        registry.addCategory(FACTORY);
        registry.addCategory(FUSION);
        registry.addCategory(WORLD);
        registry.addRecipeDecorator(GravityTechEmi::decorate);
        WorldInteraction.all(registry.getRecipeManager()).forEach(w -> registry.addRecipe(new World(w)));
        registry.addWorkstation(FACTORY, EmiStack.of(Blocks.GRAVITY_FACTORY.get()));
        registry.addWorkstation(FACTORY, EmiStack.of(Items.CHAOS_MATTER.get()));
        registry.addWorkstation(FUSION, EmiStack.of(Blocks.FUSION_CONTROLLER.get()));
        List<RecipeHolder<GravityProcessingRecipe>> factory = new java.util.ArrayList<>(
                registry.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get()));
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null)
            factory.addAll(BorrowedRecipes.build(level));
        for (RecipeHolder<GravityProcessingRecipe> r : factory) {
            GravityProcessingRecipe v = r.value();
            List<EmiIngredient> in = new java.util.ArrayList<>(ingredients(v.ingredients()));
            for (var fluid : v.fluidIngredients())
                in.add(EmiIngredient.of(java.util.Arrays.stream(fluid.getFluids())
                        .map(s -> EmiStack.of(s.getFluid(), fluid.amount())).toList(), fluid.amount()));
            registry.addRecipe(new FactoryRecipe(r.id(), v, in));
        }
        for (RecipeHolder<FusionRecipe> r : registry.getRecipeManager().getAllRecipesFor(Recipes.FUSION.get())) {
            FusionRecipe v = r.value();
            List<EmiIngredient> in = new java.util.ArrayList<>(ingredients(v.ingredients()));
            for (GenericStack key : v.keys()) {
                ItemStack shown = Gravitons.shown(key);
                in.add(EmiStack.of(shown, shown.getCount()));
            }
            registry.addRecipe(new Recipe(FUSION, r.id(), 3, in, v.results(), List.of(Component.translatable("gui.ae2_gvt.fusion.jei.info",
                    RecipeNumbers.compact(v.temperature()), String.format("%.1f", v.time() / 20.0), String.format("%.1f", v.power())))));
        }
    }

    private static List<EmiIngredient> ingredients(List<SizedIngredient> sized) {
        return sized.stream().map(s -> EmiIngredient.of(s.ingredient(), s.count())).toList();
    }

    private static final class Recipe extends BasicEmiRecipe {
        private static final int RESULT_COLUMNS = 3;
        private final int columns, rows;
        private final List<Component> lines;

        Recipe(EmiRecipeCategory category, ResourceLocation id, int columns, List<EmiIngredient> in, List<ItemStack> results, List<Component> lines) {
            super(category, id, columns * SLOT + 30 + RESULT_COLUMNS * SLOT, 3 * SLOT + lines.size() * 10 + 2);
            this.columns = columns;
            this.rows = 3;
            this.lines = lines;
            inputs.addAll(in);
            results.forEach(s -> outputs.add(EmiStack.of(s, s.getCount())));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < inputs.size(); i++)
                widgets.add(new CountedEmiSlot(inputs.get(i), (i % columns) * SLOT, (i / columns) * SLOT));
            int arrow = columns * SLOT + 3;
            widgets.addTexture(EmiTexture.EMPTY_ARROW, arrow, (rows * SLOT - 17) / 2);
            for (int i = 0; i < outputs.size(); i++)
                widgets.add(new CountedEmiSlot(outputs.get(i), arrow + 27 + (i % RESULT_COLUMNS) * SLOT, (i / RESULT_COLUMNS) * SLOT)).recipeContext(this);
            for (int i = 0; i < lines.size(); i++) {
                Component line = lines.get(i);
                widgets.addDrawable(0, rows * SLOT + 2 + i * 10, getDisplayWidth(), 10,
                        (g, mouseX, mouseY, delta) -> RecipeNumbers.drawLine(g, line, 0, 0, getDisplayWidth(), 0xFF404040));
            }
        }
    }

    /** A factory recipe drawn as the factory's GUI ({@link FactoryRecipeLook}). */
    private static final class FactoryRecipe extends BasicEmiRecipe {
        private final GravityProcessingRecipe recipe;
        private final List<EmiIngredient> kept;

        FactoryRecipe(ResourceLocation id, GravityProcessingRecipe recipe, List<EmiIngredient> in) {
            super(FACTORY, id, FactoryRecipeLook.WIDTH, FactoryRecipeLook.HEIGHT);
            this.recipe = recipe;
            this.kept = recipe.catalysts().stream().map(c -> EmiIngredient.of(c)).toList();
            inputs.addAll(in);
            recipe.results().forEach(s -> outputs.add(EmiStack.of(s, s.getCount())));
            recipe.fluidResults().forEach(f -> outputs.add(EmiStack.of(f.getFluid(), f.getAmount())));
            catalysts.addAll(kept);
            // paid from the network and shown as text: U on them still finds the recipe
            if (recipe.gravitons() > 0)
                catalysts.add(EmiStack.of(Gravitons.GRAVITON.get()));
            if (recipe.antigravitons() > 0)
                catalysts.add(EmiStack.of(Gravitons.ANTIGRAVITON.get()));
        }

        private static int x(int left, int i) {
            return left + (i % FactoryRecipeLook.COLUMNS) * FactoryRecipeLook.SLOT - 1;
        }

        private static int y(int i) {
            return FactoryRecipeLook.SLOT_Y + (i / FactoryRecipeLook.COLUMNS) * FactoryRecipeLook.SLOT - 1;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, FactoryRecipeLook.WIDTH, FactoryRecipeLook.HEIGHT,
                    (g, mouseX, mouseY, delta) -> FactoryRecipeLook.draw(g, recipe));
            if (!recipe.networkCosts().isEmpty())
                widgets.addTooltipText(FactoryRecipeLook.networkTooltip(recipe), FactoryRecipeLook.INPUT_X,
                        FactoryRecipeLook.LINE_Y - 1, FactoryRecipeLook.WIDTH - 2 * FactoryRecipeLook.INPUT_X, 11);
            // items, then fluids, then what is kept, in the input slots
            int at = 0;
            for (EmiIngredient in : inputs) {
                if (at >= FactoryRecipeLook.SLOTS)
                    break;
                widgets.add(new CountedEmiSlot(in, x(FactoryRecipeLook.INPUT_X, at), y(at))).drawBack(false);
                at++;
            }
            for (EmiIngredient keep : kept) {
                if (at >= FactoryRecipeLook.SLOTS)
                    break;
                int left = x(FactoryRecipeLook.INPUT_X, at) + 1, top = y(at) + 1;
                widgets.add(new CountedEmiSlot(keep, left - 1, top - 1)).drawBack(false)
                        .appendTooltip(Component.translatable("gui.ae2_gvt.factory.jei.kept"));
                widgets.addDrawable(left, top, 16, 16, (g, mouseX, mouseY, delta) -> FactoryRecipeLook.drawKept(g, 0, 0));
                at++;
            }
            for (int i = 0; i < outputs.size() && i < FactoryRecipeLook.SLOTS; i++)
                widgets.add(new CountedEmiSlot(outputs.get(i), x(FactoryRecipeLook.OUTPUT_X, i), y(i)))
                        .drawBack(false).recipeContext(this);
        }
    }

    /**
     * A button in the corner of every recipe that makes a sequence pattern of it ({@link SequencePattern}): every material in the
     * place the recipe has it, one item each, with the GUI slot of that place. Greyed while the pattern terminal is not open.
     */
    private static void decorate(dev.emi.emi.api.recipe.EmiRecipe recipe, WidgetHolder widgets) {
        if (recipe.getInputs().isEmpty() || recipe.getOutputs().isEmpty()) {
            return;
        }
        int x = recipe.getDisplayWidth() - 13, y = recipe.getDisplayHeight() - 13;
        widgets.addButton(x, y, 12, 12, 0, 0, SequencePattern.TEXTURE, () -> SequencePattern.terminal() != null,
                (mouseX, mouseY, button) -> fillSequence(recipe));
        widgets.addTooltipText(List.of(SequencePattern.tooltip()), x, y, 12, 12);
    }

    private static void fillSequence(dev.emi.emi.api.recipe.EmiRecipe recipe) {
        List<GenericStack> inputs = new java.util.ArrayList<>();
        for (dev.emi.emi.api.stack.EmiIngredient in : recipe.getInputs()) {
            inputs.add(in.isEmpty() ? null : stackOf(in.getEmiStacks().get(0)));
        }
        while (!inputs.isEmpty() && inputs.get(inputs.size() - 1) == null) {
            inputs.remove(inputs.size() - 1);
        }
        List<GenericStack> outputs = new java.util.ArrayList<>();
        for (EmiStack out : recipe.getOutputs()) {
            GenericStack s = stackOf(out);
            if (s != null) {
                outputs.add(s);
            }
        }
        ItemStack machine = ItemStack.EMPTY;
        for (dev.emi.emi.api.stack.EmiIngredient station : dev.emi.emi.api.EmiApi.getRecipeManager().getWorkstations(recipe.getCategory())) {
            if (!station.isEmpty() && !station.getEmiStacks().get(0).getItemStack().isEmpty()) {
                machine = station.getEmiStacks().get(0).getItemStack().copyWithCount(1);
                break;
            }
        }
        SequencePattern.open(new SequencePattern.Request(inputs, outputs, machine, recipe.getId()));
    }

    private static @org.jetbrains.annotations.Nullable GenericStack stackOf(EmiStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getKey() instanceof net.minecraft.world.level.material.Fluid fluid) {
            return new GenericStack(appeng.api.stacks.AEFluidKey.of(new net.neoforged.neoforge.fluids.FluidStack(
                    fluid.builtInRegistryHolder(), 1, stack.getComponentChanges())), stack.getAmount());
        }
        return GenericStack.fromItemStack(stack.getItemStack());
    }

    /** What happens in the world ({@link WorldInteraction}), pictured as in JEI (WorldScene). */
    private static final class World extends BasicEmiRecipe {
        private final WorldInteraction w;
        private final WorldScene.Layout layout;

        World(WorldInteraction w) {
            super(WORLD, w.id(), WorldScene.WIDTH, WorldScene.HEIGHT);
            this.w = w;
            this.layout = WorldScene.of(w);
            w.inputs().forEach(s -> inputs.add(EmiStack.of(s, s.getCount())));
            w.conditions().forEach(s -> catalysts.add(EmiStack.of(s)));
            w.outputs().forEach(s -> outputs.add(EmiStack.of(s, s.getCount())));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, WorldScene.WIDTH, WorldScene.HEIGHT, (g, mouseX, mouseY, delta) -> WorldScene.draw(g, layout));
            for (WorldScene.Slot s : layout.slots()) {
                switch (s.part()) {
                    case INPUT -> widgets.add(new CountedEmiSlot(inputs.get(s.index()), s.x(), s.y()));
                    case CONDITION -> widgets.add(new CountedEmiSlot(catalysts.get(s.index()), s.x(), s.y()));
                    case OUTPUT -> widgets.add(new CountedEmiSlot(outputs.get(s.index()), s.x(), s.y())).recipeContext(this);
                }
            }
            for (WorldScene.Icon i : layout.icons())
                if (i.tip() != null)
                    widgets.addTooltipText(List.of(i.tip()), i.x(), i.y(), i.sprite().w, i.sprite().h);
            if (layout.ring() != null)
                widgets.addTooltipText(List.of(layout.ring().tip()), layout.ring().x(), layout.ring().y(), layout.ring().w(), layout.ring().h());
        }
    }
}
