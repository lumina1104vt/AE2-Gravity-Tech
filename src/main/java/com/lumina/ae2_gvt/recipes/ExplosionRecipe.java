package com.lumina.ae2_gvt.recipes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Items lying together that are caught in an explosion ({@code ae2_gvt:explosion}): one {@code center} item, and the
 * {@code ingredients} within {@code radius} blocks of it, are used up and make {@code results} (items dropped there)
 * and/or {@code block} (put where the center lay). With {@code repeat} it is made as many times over as everything is
 * there. What else the explosion does is {@code shield}: {@code all} (no item, no block is hurt), {@code items} (items
 * survive) or {@code none}. Recipes of higher {@code priority} are tried first; once one with shield {@code all} has
 * made something, no other is tried.
 * <pre>
 * {"type": "ae2_gvt:explosion", "center": {"item": "ae2_gvt:chaos_matter"},
 *  "ingredients": [{"item": "ae2:singularity", "count": 1}, {"item": "ae2_gvt:gravity_dust", "count": 64}],
 *  "block": "ae2_gvt:black_hole", "radius": 1.5, "shield": "all", "priority": 1}
 * </pre>
 */
public record ExplosionRecipe(ResourceLocation id, Ingredient center, List<SizedIngredient> ingredients, List<ItemStack> results,
                              Optional<ResourceLocation> block, double radius, boolean repeat, Shield shield, int priority)
        implements Recipe<Container> {
    public enum Shield {
        NONE, ITEMS, ALL;

        static final Codec<Shield> CODEC = Codec.STRING.xmap(s -> valueOf(s.toUpperCase(Locale.ROOT)), s -> s.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return false;   // the explosion does the work, not a crafting grid
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        if (!results.isEmpty())
            return results.get(0);
        return block.map(b -> new ItemStack(BuiltInRegistries.BLOCK.get(b))).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(center);
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.EXPLOSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.EXPLOSION.get();
    }

    public static final class Serializer extends RecipeCodecs.JsonSerializer<ExplosionRecipe> {
        @Override
        protected Codec<ExplosionRecipe> codec(ResourceLocation id) {
            return RecordCodecBuilder.create(i -> i.group(
                    RecipeCodecs.INGREDIENT.fieldOf("center").forGetter(ExplosionRecipe::center),
                    RecipeCodecs.SIZED.listOf().optionalFieldOf("ingredients", List.of()).forGetter(ExplosionRecipe::ingredients),
                    RecipeCodecs.RESULT.listOf().optionalFieldOf("results", List.of()).forGetter(ExplosionRecipe::results),
                    ResourceLocation.CODEC.optionalFieldOf("block").forGetter(ExplosionRecipe::block),
                    Codec.DOUBLE.optionalFieldOf("radius", 1.5).forGetter(ExplosionRecipe::radius),
                    Codec.BOOL.optionalFieldOf("repeat", false).forGetter(ExplosionRecipe::repeat),
                    Shield.CODEC.optionalFieldOf("shield", Shield.NONE).forGetter(ExplosionRecipe::shield),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(ExplosionRecipe::priority)
            ).apply(i, (center, ingredients, results, block, radius, repeat, shield, priority) ->
                    new ExplosionRecipe(id, center, ingredients, results, block, radius, repeat, shield, priority)));
        }
    }
}
