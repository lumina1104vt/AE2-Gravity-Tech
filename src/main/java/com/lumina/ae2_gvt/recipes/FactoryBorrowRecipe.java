package com.lumina.ae2_gvt.recipes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code ae2_gvt:factory_borrow}: lets the 引力加工工廠 run every recipe of another mod's kind. It is not
 * crafted itself; it says which recipes ({@code from}, the {@code type} written in their JSON), where in that
 * JSON the materials and results are, and what the factory charges. A datapack or script can add, change
 * or remove one. A field is named by its path ({@code "inputs.items"}).
 * <pre>
 * {"type": "ae2_gvt:factory_borrow", "from": "advanced_ae:reaction",
 *  "items": ["input_items"], "fluids": ["input_fluid"], "outputs": ["output"],
 *  "energy_field": "input_energy", "energy_scale": 2, "time": 100}
 * </pre>
 *
 * @param energy        FE of one run when the recipe has no {@code energyField}
 * @param gravitons     taken for each run; when not given, one for every {@code fePerGraviton} FE (or part)
 * @param fePerGraviton 0 for no gravitons
 * @param rest          what only some kinds need, see {@link Rest}
 */
public record FactoryBorrowRecipe(ResourceLocation from, List<String> items, List<String> fluids, List<String> outputs,
                                  List<String> fluidOutputs, Optional<String> energyField, double energyScale,
                                  int energy, int time, Optional<Long> gravitons, long antigravitons,
                                  long fePerGraviton, Rest rest)
        implements Recipe<Recipes.NoInput> {

    /**
     * @param when         only recipes whose field has this value
     * @param has          only recipes that have all of these fields
     * @param unless       not recipes that have any of these fields (parts the factory can't do)
     * @param keep         materials that must be there but are not used up (an inscriber press)
     * @param needs        items that must be in the inputs too, not used up; tells apart machines that take
     *                     the same material
     * @param alternatives each field of {@code items} / {@code keep} is one ingredient (a list in it is a
     *                     choice), not a list of materials
     * @param extraFluids  fluids the other machine takes without them being written as materials
     * @param network      what is taken from the ME network besides gravitons (lightning, …)
     */
    public record Rest(Map<String, String> when, List<String> has, List<String> unless, List<String> keep, List<ResourceLocation> needs,
                       boolean alternatives, List<ExtraFluid> extraFluids, List<NetworkCost> network) {
        private static final MapCodec<Rest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("when", Map.of()).forGetter(Rest::when),
                Codec.STRING.listOf().optionalFieldOf("has", List.of()).forGetter(Rest::has),
                Codec.STRING.listOf().optionalFieldOf("unless", List.of()).forGetter(Rest::unless),
                Codec.STRING.listOf().optionalFieldOf("keep", List.of()).forGetter(Rest::keep),
                ResourceLocation.CODEC.listOf().optionalFieldOf("needs", List.of()).forGetter(Rest::needs),
                Codec.BOOL.optionalFieldOf("alternatives", false).forGetter(Rest::alternatives),
                ExtraFluid.CODEC.listOf().optionalFieldOf("extra_fluids", List.of()).forGetter(Rest::extraFluids),
                NetworkCost.CODEC.listOf().optionalFieldOf("network", List.of()).forGetter(Rest::network)
        ).apply(i, Rest::new));
    }

    /** That much of a fluid, or as much as the recipe's field says; not when it has the field {@code without}. */
    public record ExtraFluid(ResourceLocation fluid, int amount, Optional<String> amountField, Optional<String> without) {
        static final Codec<ExtraFluid> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(ExtraFluid::fluid),
                Codec.INT.optionalFieldOf("amount", 0).forGetter(ExtraFluid::amount),
                Codec.STRING.optionalFieldOf("amount_field").forGetter(ExtraFluid::amountField),
                Codec.STRING.optionalFieldOf("without").forGetter(ExtraFluid::without)
        ).apply(i, ExtraFluid::new));
    }

    /**
     * An AE2 key ({@code key}: its JSON, where {@code "{path}"} is that field of the recipe, and
     * {@code "{path:value}"} falls back to the value) and how much of it: the {@code amountField}, or
     * {@code amount} when the recipe has none. A recipe without a field of the key that has no fallback costs
     * none of it. Recipes are read as their mod writes them back, which leaves default values out: give those
     * here.
     */
    public record NetworkCost(Map<String, String> key, long amount, Optional<String> amountField) {
        static final Codec<NetworkCost> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("key").forGetter(NetworkCost::key),
                Codec.LONG.optionalFieldOf("amount", 1L).forGetter(NetworkCost::amount),
                Codec.STRING.optionalFieldOf("amount_field").forGetter(NetworkCost::amountField)
        ).apply(i, NetworkCost::new));
    }

    @Override
    public boolean matches(Recipes.NoInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Recipes.NoInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.FACTORY_BORROW_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.FACTORY_BORROW.get();
    }

    public static final class Serializer implements RecipeSerializer<FactoryBorrowRecipe> {
        private static final Codec<List<String>> NAMES = Codec.STRING.listOf();
        private static final MapCodec<FactoryBorrowRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("from").forGetter(FactoryBorrowRecipe::from),
                NAMES.optionalFieldOf("items", List.of()).forGetter(FactoryBorrowRecipe::items),
                NAMES.optionalFieldOf("fluids", List.of()).forGetter(FactoryBorrowRecipe::fluids),
                NAMES.optionalFieldOf("outputs", List.of()).forGetter(FactoryBorrowRecipe::outputs),
                NAMES.optionalFieldOf("fluid_outputs", List.of()).forGetter(FactoryBorrowRecipe::fluidOutputs),
                Codec.STRING.optionalFieldOf("energy_field").forGetter(FactoryBorrowRecipe::energyField),
                Codec.DOUBLE.optionalFieldOf("energy_scale", 1.0).forGetter(FactoryBorrowRecipe::energyScale),
                Codec.INT.optionalFieldOf("energy", 2000).forGetter(FactoryBorrowRecipe::energy),
                Codec.INT.optionalFieldOf("time", 100).forGetter(FactoryBorrowRecipe::time),
                Codec.LONG.optionalFieldOf("gravitons").forGetter(FactoryBorrowRecipe::gravitons),
                Codec.LONG.optionalFieldOf("antigravitons", 0L).forGetter(FactoryBorrowRecipe::antigravitons),
                Codec.LONG.optionalFieldOf("fe_per_graviton", 1000L).forGetter(FactoryBorrowRecipe::fePerGraviton),
                Rest.CODEC.forGetter(FactoryBorrowRecipe::rest)
        ).apply(i, FactoryBorrowRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, FactoryBorrowRecipe> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override
        public MapCodec<FactoryBorrowRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FactoryBorrowRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
