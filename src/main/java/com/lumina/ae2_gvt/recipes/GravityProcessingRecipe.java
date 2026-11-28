package com.lumina.ae2_gvt.recipes;

import java.util.List;

import java.util.ArrayList;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
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
 * slots (shapeless), items out (both may also be fluids, in {@code fluid_ingredients} /
 * {@code fluid_results}); {@code time} ticks and {@code energy} FE in all, and on finishing
 * {@code gravitons} / {@code antigravitons} taken from the ME network the factory is cabled to.
 * {@code catalysts} must be in the input slots but are not used up (one is enough for any number of runs at
 * once); {@code network_costs} are other things of the network (AE2's generic stacks) taken like gravitons.
 * <pre>
 * {"type": "ae2_gvt:gravity_processing",
 *  "ingredients": [{"item": "ae2_gvt:gravity_alloy_dust", "count": 1}],
 *  "results": [{"id": "ae2_gvt:gravity_alloy_ingot", "count": 1}],
 *  "time": 100, "energy": 2000, "gravitons": 8, "antigravitons": 0}
 * </pre>
 */
public record GravityProcessingRecipe(List<SizedIngredient> ingredients, List<ItemStack> results, int time, int energy,
                                      long gravitons, long antigravitons,
                                      List<SizedFluidIngredient> fluidIngredients, List<FluidStack> fluidResults,
                                      List<Ingredient> catalysts, List<GenericStack> networkCosts)
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
        // one of each catalyst is set aside first: it has to be there, and is not taken
        for (Ingredient catalyst : catalysts) {
            int at = 0;
            while (at < left.length && (left[at] <= 0 || !catalyst.test(input.getItem(at))))
                at++;
            if (at == left.length)
                return null;
            left[at]--;
        }
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
        for (SizedFluidIngredient ingredient : fluidIngredients) {
            long need = ingredient.amount() * runs;
            for (int i = 0; i < left.length && need > 0; i++) {
                if (left[i] <= 0 || !(input.slots().getKey(i) instanceof AEFluidKey fluid)
                        || !ingredient.ingredient().test(fluid.toStack(1)))
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

    /** Everything one run makes: the items, then the fluids. */
    public List<GenericStack> outputs() {
        List<GenericStack> all = new ArrayList<>(results.size() + fluidResults.size());
        results.forEach(s -> all.add(new GenericStack(AEItemKey.of(s), s.getCount())));
        fluidResults.forEach(f -> all.add(new GenericStack(AEFluidKey.of(f), f.getAmount())));
        return all;
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
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("ingredients", List.of())
                        .forGetter(GravityProcessingRecipe::ingredients),
                Recipes.RESULT.listOf().optionalFieldOf("results", List.of()).forGetter(GravityProcessingRecipe::results),
                Codec.INT.optionalFieldOf("time", 100).forGetter(GravityProcessingRecipe::time),
                Codec.INT.optionalFieldOf("energy", 2000).forGetter(GravityProcessingRecipe::energy),
                Codec.LONG.optionalFieldOf("gravitons", 0L).forGetter(GravityProcessingRecipe::gravitons),
                Codec.LONG.optionalFieldOf("antigravitons", 0L).forGetter(GravityProcessingRecipe::antigravitons),
                SizedFluidIngredient.FLAT_CODEC.listOf().optionalFieldOf("fluid_ingredients", List.of())
                        .forGetter(GravityProcessingRecipe::fluidIngredients),
                FluidStack.CODEC.listOf().optionalFieldOf("fluid_results", List.of())
                        .forGetter(GravityProcessingRecipe::fluidResults),
                Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("catalysts", List.of())
                        .forGetter(GravityProcessingRecipe::catalysts),
                GenericStack.CODEC.listOf().optionalFieldOf("network_costs", List.of())
                        .forGetter(GravityProcessingRecipe::networkCosts)
        ).apply(i, GravityProcessingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GravityProcessingRecipe> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

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
