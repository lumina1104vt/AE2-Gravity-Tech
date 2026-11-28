package com.lumina.ae2_gvt.recipes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code ae2_gvt:factory_borrow}: lets the 引力加工工廠 run every recipe of another mod's kind. It is not
 * crafted itself; it says which recipes ({@code from}, the {@code type} written in their JSON), where in that
 * JSON the materials and results are, and what the factory charges. A datapack or script can add, change
 * or remove one. A field is named by its path ({@code "ingredients.top"}).
 * <pre>
 * {"type": "ae2_gvt:factory_borrow", "from": "expatternprovider:circuit_cutter",
 *  "items": ["item_input"], "fluids": ["fluid_input"], "outputs": ["output"],
 *  "energy": 2000, "time": 100}
 * </pre>
 * In 1.20.1 a recipe's JSON can only be had back from serializers with a {@code toJson(JsonObject, Recipe)}
 * (ExtendedAE's have one), and AE2's inscriber and charger are written out by {@link BorrowedRecipes}; other
 * kinds are not borrowed.
 *
 * @param energy    FE of one run when the recipe has no {@code energyField}
 * @param gravitons     taken for each run; when not given, one for every {@code fePerGraviton} FE (or part)
 * @param fePerGraviton 0 for no gravitons
 * @param rest          what only some kinds need, see {@link Rest}
 */
public record FactoryBorrowRecipe(ResourceLocation id, ResourceLocation from, List<String> items, List<String> fluids,
                                  List<String> outputs, List<String> fluidOutputs, Optional<String> energyField,
                                  double energyScale, int energy, int time, Optional<Long> gravitons,
                                  long antigravitons, long fePerGraviton, Rest rest)
        implements Recipe<Container> {

    /**
     * @param when         only recipes whose field has this value
     * @param has          only recipes that have all of these fields
     * @param unless       not recipes that have any of these fields
     * @param keep         materials that must be there but are not used up (an inscriber press)
     * @param needs        items that must be in the inputs too, not used up; tells apart machines that take
     *                     the same material
     * @param alternatives each field of {@code items} / {@code keep} is one ingredient (a list in it is a
     *                     choice), not a list of materials
     */
    public record Rest(Map<String, String> when, List<String> has, List<String> unless, List<String> keep,
                       List<ResourceLocation> needs, boolean alternatives) {
        private static final MapCodec<Rest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("when", Map.of()).forGetter(Rest::when),
                Codec.STRING.listOf().optionalFieldOf("has", List.of()).forGetter(Rest::has),
                Codec.STRING.listOf().optionalFieldOf("unless", List.of()).forGetter(Rest::unless),
                Codec.STRING.listOf().optionalFieldOf("keep", List.of()).forGetter(Rest::keep),
                ResourceLocation.CODEC.listOf().optionalFieldOf("needs", List.of()).forGetter(Rest::needs),
                Codec.BOOL.optionalFieldOf("alternatives", false).forGetter(Rest::alternatives)
        ).apply(i, Rest::new));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
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

    public static final class Serializer extends RecipeCodecs.JsonSerializer<FactoryBorrowRecipe> {
        private static final Codec<List<String>> NAMES = Codec.STRING.listOf();

        @Override
        protected Codec<FactoryBorrowRecipe> codec(ResourceLocation id) {
            return RecordCodecBuilder.create(i -> i.group(
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
            ).apply(i, (from, items, fluids, outputs, fluidOutputs, energyField, energyScale, energy, time, gravitons,
                        antigravitons, fePerGraviton, rest) -> new FactoryBorrowRecipe(id, from, items, fluids, outputs,
                    fluidOutputs, energyField, energyScale, energy, time, gravitons, antigravitons, fePerGraviton,
                    rest)));
        }
    }
}
