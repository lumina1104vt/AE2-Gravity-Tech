package com.lumina.ae2_gvt.recipes;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * What a hole does to an item that touches it ({@code ae2_gvt:hole_conversion}): every such item is turned into
 * {@code result} (as many times over as the stack is big) and put back on top of the hole, which does not eat what it
 * made. {@code hole} is {@code black}, {@code white} or {@code any}.
 * <pre>
 * {"type": "ae2_gvt:hole_conversion", "hole": "black",
 *  "ingredient": {"item": "ae2:singularity"}, "result": {"id": "ae2_gvt:anti_singularity", "count": 1}}
 * </pre>
 */
public record HoleConversionRecipe(ResourceLocation id, Hole hole, Ingredient ingredient, ItemStack result) implements Recipe<Container> {
    public enum Hole {
        BLACK, WHITE, ANY;

        /** this recipe is for a hole of that colour */
        public boolean covers(boolean white) {
            return this == ANY || (this == WHITE) == white;
        }

        static final Codec<Hole> CODEC = Codec.STRING.xmap(s -> valueOf(s.toUpperCase(Locale.ROOT)), h -> h.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return false;   // the hole does the work, not a crafting grid
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.HOLE_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.HOLE_CONVERSION.get();
    }

    public static final class Serializer extends RecipeCodecs.JsonSerializer<HoleConversionRecipe> {
        @Override
        protected Codec<HoleConversionRecipe> codec(ResourceLocation id) {
            return RecordCodecBuilder.create(i -> i.group(
                    Hole.CODEC.optionalFieldOf("hole", Hole.ANY).forGetter(HoleConversionRecipe::hole),
                    RecipeCodecs.INGREDIENT.fieldOf("ingredient").forGetter(HoleConversionRecipe::ingredient),
                    RecipeCodecs.RESULT.fieldOf("result").forGetter(HoleConversionRecipe::result)
            ).apply(i, (hole, ingredient, result) -> new HoleConversionRecipe(id, hole, ingredient, result)));
        }
    }
}
