package com.lumina.ae2_gvt.recipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.twinstar.algorithm.ae2.RecipeHolder;
import com.mojang.logging.LogUtils;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Other mods' recipes as factory recipes, as the {@link FactoryBorrowRecipe}s say. Each recipe is written
 * back to its JSON by its serializer's {@code toJson} and read from there, so no other mod's class is named
 * and changes made by datapacks and scripts are followed. A recipe with a part that can't be read is left out
 * whole, never run with a material missing. Results of this mod are left out: those have their own recipes.
 */
public final class BorrowedRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** the server's lists; a reload makes a new recipe manager */
    private static final Map<RecipeManager, List<RecipeHolder<GravityProcessingRecipe>>> CACHE = new WeakHashMap<>();

    /** A hundred ticks (the usual run) of a full buffer. */
    private static final long MAX_ENERGY = 1_000_000_000L;

    /** More kinds of material first, then more of them: a recipe is not taken for one that needs only part of it. */
    private static final Comparator<RecipeHolder<GravityProcessingRecipe>> ORDER =
            Comparator.<RecipeHolder<GravityProcessingRecipe>>comparingInt(h -> -(h.value().ingredients().size()
                        + h.value().fluidIngredients().size() + h.value().catalysts().size()))
                .thenComparingLong(h -> -h.value().ingredients().stream().mapToLong(SizedIngredient::count).sum())
                .thenComparingLong(h -> -h.value().fluidIngredients().stream().mapToLong(SizedFluid::amount).sum())
                .thenComparing(h -> h.id().toString());
    private static final Map<RecipeManager, List<RecipeHolder<GravityProcessingRecipe>>> OWN = new WeakHashMap<>();

    private BorrowedRecipes() {
    }

    /** The factory's own recipes in the order it tries them (server side). */
    public static synchronized List<RecipeHolder<GravityProcessingRecipe>> own(Level level) {
        return OWN.computeIfAbsent(level.getRecipeManager(), m -> {
            List<RecipeHolder<GravityProcessingRecipe>> all = new ArrayList<>(RecipeHolder.allOf(level.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get())));
            all.sort(ORDER);
            return all;
        });
    }

    /** The borrowed recipes, built once per recipe manager (server side). */
    public static synchronized List<RecipeHolder<GravityProcessingRecipe>> of(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(), m -> build(level));
    }

    /**
     * The borrowed recipes as the level's recipes are now; those with more materials first, then those that
     * use more of them, so a big batch is not run as small ones of another recipe.
     */
    public static List<RecipeHolder<GravityProcessingRecipe>> build(Level level) {
        Map<ResourceLocation, List<FactoryBorrowRecipe>> kinds = new HashMap<>();
        level.getRecipeManager().getAllRecipesFor(Recipes.FACTORY_BORROW.get()).stream()
                .sorted(Comparator.comparing((FactoryBorrowRecipe k) -> k.getId().toString()))
                .forEach(k -> kinds.computeIfAbsent(k.from(), f -> new ArrayList<>()).add(k));
        List<RecipeHolder<GravityProcessingRecipe>> all = new ArrayList<>();
        if (kinds.isEmpty())
            return all;
        for (Recipe<?> source : level.getRecipeManager().getRecipes()) {
            ResourceLocation from = BuiltInRegistries.RECIPE_SERIALIZER.getKey(source.getSerializer());
            List<FactoryBorrowRecipe> of = from == null ? null : kinds.get(from);
            if (of == null)
                continue;
            try {
                // "/…": no such recipe in the data, as recipe viewers expect of made-up ids
                ResourceLocation id = new ResourceLocation(AE2GravityTech.MODID,
                        "/borrowed/" + source.getId().getNamespace() + "/" + source.getId().getPath());
                JsonObject json = json(source);
                for (FactoryBorrowRecipe kind : of) {
                    if (!applies(kind.rest(), json))
                        continue;
                    GravityProcessingRecipe recipe = convert(id, kind, json);
                    if (recipe != null)
                        all.add(new RecipeHolder<>(id, recipe));
                    break;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.warn("Gravity factory can't run recipe {}: {}", source.getId(), e.toString());
            }
        }
        all.sort(ORDER);
        LOGGER.info("Gravity factory borrows {} recipes of other mods", all.size());
        return all;
    }

    private static JsonObject json(Recipe<?> recipe) throws ReflectiveOperationException {
        // AE2's serializers can't write; these two are written out here
        if (recipe instanceof InscriberRecipe inscriber)
            return json(inscriber);
        if (recipe instanceof ChargerRecipe charger) {
            JsonObject json = new JsonObject();
            json.add("ingredient", charger.getIngredient().toJson());
            json.add("result", written(new ItemStack(charger.result)));
            return json;
        }
        RecipeSerializer<?> serializer = recipe.getSerializer();
        JsonObject json = new JsonObject();
        serializer.getClass().getMethod("toJson", JsonObject.class, Recipe.class).invoke(serializer, json, recipe);
        return json;
    }

    private static JsonObject json(InscriberRecipe recipe) {
        JsonObject ingredients = new JsonObject();
        ingredients.add("middle", recipe.getMiddleInput().toJson());
        if (!recipe.getTopOptional().isEmpty())
            ingredients.add("top", recipe.getTopOptional().toJson());
        if (!recipe.getBottomOptional().isEmpty())
            ingredients.add("bottom", recipe.getBottomOptional().toJson());
        JsonObject json = new JsonObject();
        json.addProperty("mode", recipe.getProcessType() == InscriberProcessType.PRESS ? "press" : "inscribe");
        json.add("ingredients", ingredients);
        json.add("result", written(recipe.getResultItem()));
        return json;
    }

    private static JsonObject written(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        json.addProperty("count", stack.getCount());
        return json;
    }

    private static boolean applies(FactoryBorrowRecipe.Rest rest, JsonObject json) {
        for (var wanted : rest.when().entrySet())
            if (find(json, wanted.getKey()).stream()
                    .noneMatch(e -> e.isJsonPrimitive() && e.getAsString().equals(wanted.getValue())))
                return false;
        return rest.has().stream().noneMatch(path -> find(json, path).isEmpty())
                && rest.unless().stream().allMatch(path -> find(json, path).isEmpty());
    }

    /** What is at that path; a list on the way is searched in every element. */
    private static List<JsonElement> find(JsonElement from, String path) {
        List<JsonElement> found = List.of(from);
        for (String name : path.split("\\.")) {
            List<JsonElement> next = new ArrayList<>();
            for (JsonElement at : found) {
                Iterable<JsonElement> each = at.isJsonArray() ? at.getAsJsonArray() : List.of(at);
                for (JsonElement e : each)
                    if (e.isJsonObject() && e.getAsJsonObject().has(name) && !e.getAsJsonObject().get(name).isJsonNull())
                        next.add(e.getAsJsonObject().get(name));
            }
            found = next;
        }
        return found;
    }

    private static GravityProcessingRecipe convert(ResourceLocation id, FactoryBorrowRecipe kind, JsonObject json) {
        List<SizedIngredient> items = new ArrayList<>();
        FactoryBorrowRecipe.Rest rest = kind.rest();
        List<Ingredient> catalysts = new ArrayList<>();
        if (rest.alternatives()) {
            for (String field : kind.items())
                for (JsonElement e : find(json, field))
                    items.add(new SizedIngredient(Ingredient.fromJson(e, false), 1));
            for (String field : rest.keep())
                for (JsonElement e : find(json, field))
                    catalysts.add(Ingredient.fromJson(e, false));
        } else {
            for (JsonObject entry : entries(json, kind.items()))
                items.add(new SizedIngredient(Ingredient.fromJson(body(entry), false), (int) amount(entry)));
            for (JsonObject entry : entries(json, rest.keep()))
                catalysts.add(Ingredient.fromJson(body(entry), false));
        }
        for (ResourceLocation needed : rest.needs())
            catalysts.add(Ingredient.of(BuiltInRegistries.ITEM.getOptional(needed)
                    .orElseThrow(() -> new IllegalArgumentException("no item " + needed))));
        List<SizedFluid> fluids = new ArrayList<>();
        for (JsonObject entry : entries(json, kind.fluids()))
            fluids.add(SizedFluid.of(body(entry), (int) amount(entry)));
        List<ItemStack> results = new ArrayList<>();
        List<FluidStack> fluidResults = new ArrayList<>();
        for (JsonObject entry : entries(json, kind.outputs())) {
            if (entry.has("fluid"))
                fluidResults.add(fluid(entry));
            else
                results.add(item(entry));
        }
        for (JsonObject entry : entries(json, kind.fluidOutputs()))
            fluidResults.add(fluid(entry));
        if (items.isEmpty() && fluids.isEmpty() || results.isEmpty() && fluidResults.isEmpty())
            return null;
        for (ItemStack result : results)
            if (BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(AE2GravityTech.MODID))
                return null;
        int energy = kind.energy();
        if (kind.energyField().isPresent() && !find(json, kind.energyField().get()).isEmpty()) {
            double needed = find(json, kind.energyField().get()).get(0).getAsDouble() * kind.energyScale();
            // more than the factory's buffer can feed in a run: left out, not run for less
            if (needed > MAX_ENERGY)
                return null;
            energy = (int) Math.round(needed);
        }
        long perGraviton = kind.fePerGraviton();
        long gravitons = kind.gravitons().orElse(perGraviton <= 0 ? 0 : (energy + perGraviton - 1) / perGraviton);
        return new GravityProcessingRecipe(id, items, results, kind.time(), energy, gravitons, kind.antigravitons(),
                fluids, fluidResults, catalysts);
    }

    /** The entries under those fields (each one entry or a list of them); empty ones are no entry. */
    private static List<JsonObject> entries(JsonObject json, List<String> fields) {
        List<JsonObject> entries = new ArrayList<>();
        for (String field : fields) {
            for (JsonElement value : find(json, field)) {
                JsonArray array;
                if (value.isJsonArray()) {
                    array = value.getAsJsonArray();
                } else {
                    array = new JsonArray();
                    array.add(value);
                }
                for (JsonElement e : array)
                    if (e.getAsJsonObject().size() > 0)
                        entries.add(e.getAsJsonObject());
            }
        }
        return entries;
    }

    private static long amount(JsonObject entry) {
        for (String key : new String[] {"amount", "count"})
            if (entry.has(key))
                return entry.get(key).getAsLong();
        return 1;
    }

    /** The ingredient of an entry: its "ingredient", or the entry itself without the amount. */
    private static JsonElement body(JsonObject entry) {
        if (entry.has("ingredient"))
            return entry.get("ingredient");
        JsonObject body = entry.deepCopy();
        body.remove("amount");
        body.remove("count");
        return body;
    }

    private static ItemStack item(JsonObject entry) {
        JsonObject stack = entry.deepCopy();
        if (!stack.has("item"))
            stack.add("item", stack.remove("id"));
        return CraftingHelper.getItemStack(stack, true);
    }

    private static FluidStack fluid(JsonObject entry) {
        String name = (entry.has("fluid") ? entry.get("fluid") : entry.get("id")).getAsString();
        var fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(name));
        FluidStack stack = new FluidStack(fluid, (int) amount(entry));
        if (stack.isEmpty())
            throw new IllegalArgumentException("unknown fluid " + name);
        return stack;
    }
}
