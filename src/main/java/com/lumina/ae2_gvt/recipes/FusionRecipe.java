package com.lumina.ae2_gvt.recipes;

import java.util.List;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * A 核融合發生器 reaction ({@code ae2_gvt:fusion}): base materials and a seed of what it makes, in any
 * of the input slots, make far more of it; it runs {@code time} ticks at {@code temperature} K or
 * hotter, and while it runs the chamber gives back {@code power} times the energy going in. Besides the
 * items of {@code ingredients}, {@code keys} takes anything else AE2 can hold (fluids, gravitons …).
 * <pre>
 * {"type": "ae2_gvt:fusion",
 *  "ingredients": [{"item": "ae2:fluix_crystal", "count": 256}],
 *  "results": [{"id": "ae2_gvt:gravity_dust", "count": 1024}],
 *  "keys": [{"#t": "ae2:f", "id": "minecraft:water", "#": 1000}],
 *  "temperature": 15000, "time": 40, "power": 1.5}
 * </pre>
 */
public record FusionRecipe(List<SizedIngredient> ingredients, List<GenericStack> keys, List<ItemStack> results, int temperature, int time,
                           double power) implements Recipe<FusionRecipe.Input> {

    /** The controller's input slots. */
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

    /** How much each slot gives, or null when the inputs don't cover the reaction. */
    public long[] take(Input input) {
        ConfigInventory slots = input.slots();
        long[] left = new long[slots.size()];
        for (int i = 0; i < left.length; i++)
            left[i] = slots.getAmount(i);
        long[] taken = new long[slots.size()];
        for (SizedIngredient ingredient : ingredients) {
            long need = ingredient.count();
            for (int i = 0; i < left.length && need > 0; i++)
                if (ingredient.ingredient().test(input.getItem(i)))
                    need -= takeFrom(i, need, left, taken);
            if (need > 0)
                return null;
        }
        for (GenericStack key : keys) {
            long need = key.amount();
            for (int i = 0; i < left.length && need > 0; i++)
                if (key.what().equals(slots.getKey(i)))
                    need -= takeFrom(i, need, left, taken);
            if (need > 0)
                return null;
        }
        return taken;
    }

    private static long takeFrom(int slot, long need, long[] left, long[] taken) {
        long n = Math.min(need, left[slot]);
        left[slot] -= n;
        taken[slot] += n;
        return n;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return take(input) != null;
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
        return Recipes.FUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.FUSION.get();
    }

    public static final class Serializer implements RecipeSerializer<FusionRecipe> {
        private static final MapCodec<FusionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(FusionRecipe::ingredients),
                GenericStack.CODEC.listOf().optionalFieldOf("keys", List.of()).forGetter(FusionRecipe::keys),
                Recipes.RESULT.listOf().fieldOf("results").forGetter(FusionRecipe::results),
                Codec.INT.fieldOf("temperature").forGetter(FusionRecipe::temperature),
                Codec.INT.optionalFieldOf("time", 40).forGetter(FusionRecipe::time),
                Codec.DOUBLE.optionalFieldOf("power", 1.0).forGetter(FusionRecipe::power)
        ).apply(i, FusionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), FusionRecipe::ingredients,
                GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()), FusionRecipe::keys,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), FusionRecipe::results,
                ByteBufCodecs.VAR_INT, FusionRecipe::temperature,
                ByteBufCodecs.VAR_INT, FusionRecipe::time,
                ByteBufCodecs.DOUBLE, FusionRecipe::power,
                FusionRecipe::new);

        @Override
        public MapCodec<FusionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
