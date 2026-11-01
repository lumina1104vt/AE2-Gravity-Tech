package com.lumina.ae2_gvt.recipes;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import appeng.api.stacks.AEItemKey;
import appeng.util.ConfigInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A 引力加工工廠 recipe ({@code ae2_gvt:gravity_processing}): items with counts in, in any of the input
 * slots (shapeless), items out; {@code time} ticks and {@code energy} FE in all, and on finishing
 * {@code gravitons} / {@code antigravitons} taken from the ME network the factory is cabled to.
 * <pre>
 * {"type": "ae2_gvt:gravity_processing",
 *  "ingredients": [{"item": "ae2_gvt:gravity_alloy_dust", "count": 1}],
 *  "results": [{"id": "ae2_gvt:gravity_alloy_ingot", "count": 1}],
 *  "time": 100, "energy": 2000, "gravitons": 8, "antigravitons": 0}
 * </pre>
 */
public record GravityProcessingRecipe(List<SizedIngredient> ingredients, List<ItemStack> results, int time, int energy,
                                      long gravitons, long antigravitons)
        implements Recipe<GravityProcessingRecipe.Input> {

    /** The factory's input slots. */
    public record Input(ConfigInventory slots) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return slots.getKey(index) instanceof AEItemKey item ? item.toStack() : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return slots.size();
        }
    }

    /**
     * How much each slot gives to that many runs at once, or null when the inputs don't cover them.
     * Greedy: each ingredient takes from the first slots that hold it.
     */
    public long[] take(Input input, long runs) {
        long[] left = new long[input.size()];
        for (int i = 0; i < left.length; i++)
            left[i] = input.slots().getAmount(i);
        long[] taken = new long[input.size()];
        for (SizedIngredient ingredient : ingredients) {
            long need = ingredient.count() * runs;
            for (int i = 0; i < left.length && need > 0; i++) {
                if (left[i] <= 0 || !ingredient.ingredient().test(input.getItem(i)))
                    continue;
                long n = Math.min(need, left[i]);
                left[i] -= n;
                taken[i] += n;
                need -= n;
            }
            if (need > 0)
                return null;
        }
        return taken;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return take(input, 1) != null;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;    // not in the recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.GRAVITY_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.GRAVITY_PROCESSING.get();
    }

    public static final class Serializer implements RecipeSerializer<GravityProcessingRecipe> {
        private static final MapCodec<GravityProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(GravityProcessingRecipe::ingredients),
                Recipes.RESULT.listOf().fieldOf("results").forGetter(GravityProcessingRecipe::results),
                Codec.INT.optionalFieldOf("time", 100).forGetter(GravityProcessingRecipe::time),
                Codec.INT.optionalFieldOf("energy", 2000).forGetter(GravityProcessingRecipe::energy),
                Codec.LONG.optionalFieldOf("gravitons", 0L).forGetter(GravityProcessingRecipe::gravitons),
                Codec.LONG.optionalFieldOf("antigravitons", 0L).forGetter(GravityProcessingRecipe::antigravitons)
        ).apply(i, GravityProcessingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GravityProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), GravityProcessingRecipe::ingredients,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), GravityProcessingRecipe::results,
                ByteBufCodecs.VAR_INT, GravityProcessingRecipe::time,
                ByteBufCodecs.VAR_INT, GravityProcessingRecipe::energy,
                ByteBufCodecs.VAR_LONG, GravityProcessingRecipe::gravitons,
                ByteBufCodecs.VAR_LONG, GravityProcessingRecipe::antigravitons,
                GravityProcessingRecipe::new);

        @Override
        public MapCodec<GravityProcessingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GravityProcessingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
