package com.lumina.ae2_gvt.recipes;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
public record HoleConversionRecipe(Hole hole, Ingredient ingredient, ItemStack result) implements Recipe<Recipes.NoInput> {
    public enum Hole {
        BLACK, WHITE, ANY;

        /** this recipe is for a hole of that colour */
        public boolean covers(boolean white) {
            return this == ANY || (this == WHITE) == white;
        }

        static final Codec<Hole> CODEC = Codec.STRING.xmap(s -> valueOf(s.toUpperCase(Locale.ROOT)), h -> h.name().toLowerCase(Locale.ROOT));
        static final StreamCodec<io.netty.buffer.ByteBuf, Hole> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Hole::ordinal);
    }

    @Override
    public boolean matches(Recipes.NoInput input, Level level) {
        return false;   // the hole does the work, not a crafting grid
    }

    @Override
    public ItemStack assemble(Recipes.NoInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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

    public static final class Serializer implements RecipeSerializer<HoleConversionRecipe> {
        private static final MapCodec<HoleConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Hole.CODEC.optionalFieldOf("hole", Hole.ANY).forGetter(HoleConversionRecipe::hole),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(HoleConversionRecipe::ingredient),
                Recipes.RESULT.fieldOf("result").forGetter(HoleConversionRecipe::result)
        ).apply(i, HoleConversionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, HoleConversionRecipe> STREAM_CODEC = StreamCodec.composite(
                Hole.STREAM_CODEC, HoleConversionRecipe::hole,
                Ingredient.CONTENTS_STREAM_CODEC, HoleConversionRecipe::ingredient,
                ItemStack.STREAM_CODEC, HoleConversionRecipe::result,
                HoleConversionRecipe::new);

        @Override
        public MapCodec<HoleConversionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HoleConversionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
