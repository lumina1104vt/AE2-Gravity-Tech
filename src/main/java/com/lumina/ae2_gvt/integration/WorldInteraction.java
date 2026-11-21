package com.lumina.ae2_gvt.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import appeng.core.definitions.AEBlocks;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.CrystalLine;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.recipes.ExplosionRecipe;
import com.lumina.ae2_gvt.recipes.HoleConversionRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.lumina.twinstar.util.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import com.lumina.ae2_gvt.recipes.SizedIngredient;

/**
 * What happens in the world rather than in a machine, for JEI and EMI to show beside the recipes: things dropped
 * on a hole or caught together in an explosion (recipes), holes blown up, crystals growing on holes, the paradox seed.
 *
 * @param conditions what has to be there besides the inputs: a hole, an explosion (TNT stands for any)
 */
public record WorldInteraction(String name, Scene scene, List<ItemStack> inputs, List<ItemStack> conditions, List<ItemStack> outputs) {
    /** how it is pictured (WorldScene) */
    public enum Scene {DROP, BLAST, BLOWN, GROWTH, SEED, BUDDING}

    public ResourceLocation id() {
        return new ResourceLocation(AE2GravityTech.MODID, "world/" + name);
    }

    /** The words for what it is: its own line when the language has one, else a general one for how it is pictured. */
    public Component text() {
        String key = "gui.ae2_gvt.world." + name;
        if (net.minecraft.client.resources.language.I18n.exists(key))
            return Component.translatable(key);
        return Component.translatable("gui.ae2_gvt.world.scene." + scene.name().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Everything to show: what the {@code ae2_gvt:hole_conversion} and {@code ae2_gvt:explosion} recipes say, then the
     * things that happen in the world without being a recipe (holes blown up, crystals growing on holes, the paradox
     * seed). Their numbers are those of the code that does them (GravityHoleBlockEntity, GravityHoleBlock,
     * ParadoxSeedItem); change them together.
     */
    public static List<WorldInteraction> all(RecipeManager recipes) {
        List<WorldInteraction> list = new ArrayList<>();
        for (RecipeHolder<HoleConversionRecipe> h : RecipeHolder.allOf(recipes.getAllRecipesFor(Recipes.HOLE_CONVERSION.get())))
            list.add(fromConversion(h));
        RecipeHolder.allOf(recipes.getAllRecipesFor(Recipes.EXPLOSION.get())).stream()
                .sorted(Comparator.comparingInt((RecipeHolder<ExplosionRecipe> h) -> h.value().priority()).reversed())
                .forEach(h -> list.add(fromExplosion(h)));
        list.addAll(mechanics());
        return list;
    }

    private static String nameOf(ResourceLocation id) {
        String path = id.getPath();
        return path.startsWith("world/") ? path.substring("world/".length()) : path;
    }

    private static ItemStack first(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }

    private static WorldInteraction fromConversion(RecipeHolder<HoleConversionRecipe> h) {
        HoleConversionRecipe r = h.value();
        ItemStack hole = stack(r.hole() == HoleConversionRecipe.Hole.WHITE ? GravityHole.WHITE_HOLE_ITEM.get() : GravityHole.BLACK_HOLE_ITEM.get(), 1);
        return new WorldInteraction(nameOf(h.id()), Scene.DROP, List.of(first(r.ingredient())), List.of(hole), List.of(r.result().copy()));
    }

    private static WorldInteraction fromExplosion(RecipeHolder<ExplosionRecipe> h) {
        ExplosionRecipe r = h.value();
        List<ItemStack> in = new ArrayList<>();
        in.add(first(r.center()));
        for (SizedIngredient s : r.ingredients()) {
            ItemStack shown = first(s.ingredient());
            in.add(shown.isEmpty() ? shown : shown.copyWithCount(s.count()));
        }
        List<ItemStack> out = new ArrayList<>();
        r.results().forEach(s -> out.add(s.copy()));
        r.block().ifPresent(b -> out.add(new ItemStack(BuiltInRegistries.BLOCK.get(b))));
        return new WorldInteraction(nameOf(h.id()), Scene.BLAST, in, List.of(stack(net.minecraft.world.item.Items.TNT, 1)), out);
    }

    /** What happens in the world without being a recipe. */
    private static List<WorldInteraction> mechanics() {
        ItemStack explosion = stack(net.minecraft.world.item.Items.TNT, 1);
        ItemStack black = stack(GravityHole.BLACK_HOLE_ITEM.get(), 1), white = stack(GravityHole.WHITE_HOLE_ITEM.get(), 1);
        ItemStack chaos = stack(Items.CHAOS_MATTER.get(), 1);
        ItemStack collector = stack(Items.GRAVITON_COLLECTOR, 1);
        return List.of(
                new WorldInteraction("black_hole_blown", Scene.BLOWN, List.of(black), List.of(explosion, collector),
                        List.of(chaos, GravitonKey.GRAVITON.displayStack().copyWithCount(128))),
                new WorldInteraction("white_hole_blown", Scene.BLOWN, List.of(white), List.of(explosion, collector),
                        List.of(chaos, GravitonKey.ANTIGRAVITON.displayStack().copyWithCount(128))),
                new WorldInteraction("black_hole_growth", Scene.GROWTH, List.of(black), List.of(), growth(AE2GravityTech.GRAVITY, GravitonKey.GRAVITON)),
                new WorldInteraction("white_hole_growth", Scene.GROWTH, List.of(white), List.of(), growth(AE2GravityTech.ANTIGRAVITY, GravitonKey.ANTIGRAVITON)),
                new WorldInteraction("paradox_seed", Scene.SEED, List.of(stack(Items.PARADOX_CRYSTAL_SEED.get(), 1), stack(AEBlocks.FLUIX_BLOCK, 1)),
                        List.of(black, white), List.of(stack(Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX.get(), 1))),
                new WorldInteraction("budding_paradox", Scene.BUDDING, List.of(stack(Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX.get(), 1)), List.of(),
                        List.of(stack(AE2GravityTech.PARADOX.smallBud().get(), 1), stack(AE2GravityTech.PARADOX.mediumBud().get(), 1),
                                stack(AE2GravityTech.PARADOX.largeBud().get(), 1), stack(AE2GravityTech.PARADOX.cluster().get(), 1),
                                stack(AEBlocks.FLUIX_BLOCK, 1))));
    }

    private static List<ItemStack> growth(CrystalLine line, GravitonKey gravitons) {
        return List.of(stack(line.smallBud().get(), 1), stack(line.mediumBud().get(), 1), stack(line.largeBud().get(), 1),
                stack(line.cluster().get(), 1), gravitons.displayStack().copyWithCount(16));
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
