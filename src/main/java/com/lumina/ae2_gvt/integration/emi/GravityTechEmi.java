package com.lumina.ae2_gvt.integration.emi;

import java.util.List;

import appeng.api.stacks.GenericStack;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.integration.WorldInteraction;
import com.lumina.ae2_gvt.client.SequencePattern;
import com.lumina.ae2_gvt.integration.WorldScene;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.lmb.compat.CountedEmiSlot;
import com.lumina.lmb.compat.RecipeNumbers;
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
        registry.addCategory(FACTORY);
        registry.addCategory(FUSION);
        registry.addCategory(WORLD);
        registry.addRecipeDecorator(GravityTechEmi::decorate);
        WorldInteraction.all(registry.getRecipeManager()).forEach(w -> registry.addRecipe(new World(w)));
        registry.addWorkstation(FACTORY, EmiStack.of(Blocks.GRAVITY_FACTORY.get()));
        registry.addWorkstation(FACTORY, EmiStack.of(Items.CHAOS_MATTER.get()));
        registry.addWorkstation(FUSION, EmiStack.of(Blocks.FUSION_CONTROLLER.get()));
        for (RecipeHolder<GravityProcessingRecipe> r : registry.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get())) {
            MutableComponent cost = Component.empty();
            GravityProcessingRecipe v = r.value();
            if (v.gravitons() > 0)
                cost.append(Component.translatable("item.ae2_gvt.graviton")).append(" " + RecipeNumbers.compact(v.gravitons()));
            if (v.antigravitons() > 0)
                cost.append(v.gravitons() > 0 ? "・" : "").append(Component.translatable("item.ae2_gvt.antigraviton")).append(" " + RecipeNumbers.compact(v.antigravitons()));
            Recipe recipe = new Recipe(FACTORY, r.id(), 4, ingredients(v.ingredients()), v.results(), List.of(
                    Component.translatable("gui.ae2_gvt.factory.jei.info", String.format("%.1f", v.time() / 20.0), RecipeNumbers.compact(v.energy())), cost));
            // paid from the network and shown as text: U on them still finds the recipe
            if (v.gravitons() > 0)
                recipe.addCatalyst(EmiStack.of(Gravitons.GRAVITON.get()));
            if (v.antigravitons() > 0)
                recipe.addCatalyst(EmiStack.of(Gravitons.ANTIGRAVITON.get()));
            registry.addRecipe(recipe);
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

        void addCatalyst(EmiStack stack) {
            catalysts.add(stack);
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
        inputs = SequencePattern.withoutGaps(inputs, recipe.getCategory().getId());
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
