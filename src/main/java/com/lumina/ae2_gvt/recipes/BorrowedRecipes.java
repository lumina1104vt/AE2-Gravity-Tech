package com.lumina.ae2_gvt.recipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import appeng.api.stacks.GenericStack;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.slf4j.Logger;

/**
 * Other mods' recipes as factory recipes, as the {@link FactoryBorrowRecipe}s say. Each recipe is written
 * back to its JSON by its own codec and read from there, so no other mod's class is touched and changes made
 * by datapacks and scripts are followed. A recipe with a part that can't be read is left out whole, never
 * run with a material missing. Results of this mod are left out: those have their own recipes.
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
                .thenComparingLong(h -> -h.value().fluidIngredients().stream().mapToLong(SizedFluidIngredient::amount).sum())
                .thenComparing(h -> h.id().toString());
    private static final Map<RecipeManager, List<RecipeHolder<GravityProcessingRecipe>>> OWN = new WeakHashMap<>();

    private BorrowedRecipes() {
    }

    /** The factory's own recipes in the order it tries them (server side). */
    public static synchronized List<RecipeHolder<GravityProcessingRecipe>> own(Level level) {
        return OWN.computeIfAbsent(level.getRecipeManager(), m -> {
            List<RecipeHolder<GravityProcessingRecipe>> all = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(Recipes.GRAVITY_PROCESSING.get()));
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
                .sorted(Comparator.comparing(h -> h.id().toString()))
                .forEach(h -> kinds.computeIfAbsent(h.value().from(), k -> new ArrayList<>()).add(h.value()));
        List<RecipeHolder<GravityProcessingRecipe>> all = new ArrayList<>();
        if (kinds.isEmpty())
            return all;
        DynamicOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            ResourceLocation from = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer());
            List<FactoryBorrowRecipe> of = from == null ? null : kinds.get(from);
            if (of == null)
                continue;
            try {
                JsonObject json = Recipe.CODEC.encodeStart(ops, holder.value()).getOrThrow().getAsJsonObject();
                for (FactoryBorrowRecipe kind : of) {
                    if (!applies(kind.rest(), json))
                        continue;
                    GravityProcessingRecipe recipe = convert(kind, json, ops);
                    if (recipe != null) {
                        // "/…": no such recipe in the data, as recipe viewers expect of made-up ids
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID,
                                "/borrowed/" + holder.id().getNamespace() + "/" + holder.id().getPath());
                        all.add(new RecipeHolder<>(id, recipe));
                    }
                    break;
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Gravity factory can't run recipe {}: {}", holder.id(), e.toString());
            }
        }
        all.sort(ORDER);
        LOGGER.info("Gravity factory borrows {} recipes of other mods", all.size());
        return all;
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
            for (JsonElement at : found)
                for (JsonElement e : at.isJsonArray() ? at.getAsJsonArray().asList() : List.of(at))
                    if (e.isJsonObject() && e.getAsJsonObject().has(name) && !e.getAsJsonObject().get(name).isJsonNull())
                        next.add(e.getAsJsonObject().get(name));
            found = next;
        }
        return found;
    }

    private static GravityProcessingRecipe convert(FactoryBorrowRecipe kind, JsonObject json, DynamicOps<JsonElement> ops) {
        FactoryBorrowRecipe.Rest rest = kind.rest();
        List<SizedIngredient> items = new ArrayList<>();
        List<Ingredient> catalysts = new ArrayList<>();
        if (rest.alternatives()) {
            for (String field : kind.items())
                for (JsonElement e : find(json, field))
                    items.add(new SizedIngredient(Ingredient.CODEC_NONEMPTY.parse(ops, e).getOrThrow(), 1));
            for (String field : rest.keep())
                for (JsonElement e : find(json, field))
                    catalysts.add(Ingredient.CODEC_NONEMPTY.parse(ops, e).getOrThrow());
        } else {
            for (JsonObject entry : entries(json, kind.items()))
                items.add(new SizedIngredient(Ingredient.CODEC_NONEMPTY.parse(ops, body(entry)).getOrThrow(),
                        (int) amount(entry)));
            for (JsonObject entry : entries(json, rest.keep()))
                catalysts.add(Ingredient.CODEC_NONEMPTY.parse(ops, body(entry)).getOrThrow());
        }
        for (ResourceLocation needed : rest.needs())
            catalysts.add(Ingredient.of(BuiltInRegistries.ITEM.getOptional(needed)
                    .orElseThrow(() -> new IllegalArgumentException("no item " + needed))));
        List<SizedFluidIngredient> fluids = new ArrayList<>();
        for (JsonObject entry : entries(json, kind.fluids())) {
            JsonElement body = body(entry);
            if (body.isJsonObject() && body.getAsJsonObject().has("id")) {    // written as a fluid stack
                JsonObject named = new JsonObject();
                named.add("fluid", body.getAsJsonObject().get("id"));
                body = named;
            }
            FluidIngredient ingredient = FluidIngredient.CODEC_NON_EMPTY.parse(ops, body).getOrThrow();
            fluids.add(new SizedFluidIngredient(ingredient, (int) amount(entry)));
        }
        for (FactoryBorrowRecipe.ExtraFluid extra : rest.extraFluids()) {
            if (extra.without().isPresent() && !find(json, extra.without().get()).isEmpty())
                continue;
            long amount = extra.amountField().map(field -> number(json, field, extra.amount())).orElse((long) extra.amount());
            if (amount > 0)
                fluids.add(SizedFluidIngredient.of(BuiltInRegistries.FLUID.getOptional(extra.fluid())
                        .orElseThrow(() -> new IllegalArgumentException("no fluid " + extra.fluid())), (int) amount));
        }
        List<ItemStack> results = new ArrayList<>();
        List<FluidStack> fluidResults = new ArrayList<>();
        for (JsonObject entry : entries(json, kind.outputs())) {
            if (entry.has("#t") && entry.get("#t").getAsString().equals("ae2:f"))
                fluidResults.add(fluid(entry, ops));
            else
                results.add(item(entry, ops));
        }
        for (JsonObject entry : entries(json, kind.fluidOutputs()))
            fluidResults.add(fluid(entry, ops));
        if (items.isEmpty() && fluids.isEmpty() || results.isEmpty() && fluidResults.isEmpty())
            return null;
        for (ItemStack result : results)
            if (BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(AE2GravityTech.MODID))
                return null;
        int energy = kind.energy();
        if (kind.energyField().isPresent() && !find(json, kind.energyField().get()).isEmpty()) {
            double needed = find(json, kind.energyField().get()).getFirst().getAsDouble() * kind.energyScale();
            // more than the factory's buffer can feed in a run: left out, not run for less
            if (needed > MAX_ENERGY)
                return null;
            energy = (int) Math.round(needed);
        }
        long perGraviton = kind.fePerGraviton();
        long gravitons = kind.gravitons().orElse(perGraviton <= 0 ? 0 : (energy + perGraviton - 1) / perGraviton);
        return new GravityProcessingRecipe(items, results, kind.time(), energy, gravitons, kind.antigravitons(),
                fluids, fluidResults, catalysts, network(rest, json, ops));
    }

    private static List<GenericStack> network(FactoryBorrowRecipe.Rest rest, JsonObject json, DynamicOps<JsonElement> ops) {
        List<GenericStack> costs = new ArrayList<>();
        next:
        for (FactoryBorrowRecipe.NetworkCost cost : rest.network()) {
            JsonObject stack = new JsonObject();
            for (var part : cost.key().entrySet()) {
                String value = part.getValue();
                if (value.startsWith("{") && value.endsWith("}")) {
                    String[] field = value.substring(1, value.length() - 1).split(":", 2);
                    List<JsonElement> found = find(json, field[0]);
                    if (found.isEmpty() && field.length < 2)
                        continue next;
                    value = found.isEmpty() ? field[1] : found.getFirst().getAsString();
                }
                stack.addProperty(part.getKey(), value);
            }
            long amount = cost.amountField().map(field -> number(json, field, cost.amount())).orElse(cost.amount());
            if (amount <= 0)
                continue;
            stack.addProperty("#", amount);
            costs.add(GenericStack.CODEC.parse(ops, stack).getOrThrow());
        }
        return costs;
    }

    private static long number(JsonObject json, String path, long without) {
        List<JsonElement> found = find(json, path);
        return found.isEmpty() ? without : found.getFirst().getAsLong();
    }

    /** The entries under those fields (each one entry or a list of them); empty ones are no entry. */
    private static List<JsonObject> entries(JsonObject json, List<String> fields) {
        List<JsonObject> entries = new ArrayList<>();
        for (String field : fields) {
            for (JsonElement value : find(json, field)) {
                for (JsonElement e : value.isJsonArray() ? value.getAsJsonArray().asList() : List.of(value))
                    if (!e.getAsJsonObject().isEmpty())
                        entries.add(e.getAsJsonObject());
            }
        }
        return entries;
    }

    private static long amount(JsonObject entry) {
        for (String key : new String[] {"#", "amount", "count"})
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

    private static JsonObject stack(JsonObject entry) {
        JsonObject stack = new JsonObject();
        stack.add("id", entry.has("id") ? entry.get("id") : entry.get("fluid"));
        if (entry.has("components"))
            stack.add("components", entry.get("components"));
        return stack;
    }

    private static ItemStack item(JsonObject entry, DynamicOps<JsonElement> ops) {
        // a result written as a tag: its first item by id, so every look gives the same one
        if (!entry.has("id") && entry.has("tag")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.get("tag").getAsString()));
            Item first = BuiltInRegistries.ITEM.getTag(tag).stream().flatMap(named -> named.stream().map(Holder::value))
                    .min(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                    .orElseThrow(() -> new IllegalArgumentException("no item in #" + tag.location()));
            return new ItemStack(first, (int) amount(entry));
        }
        ItemStack result = ItemStack.STRICT_SINGLE_ITEM_CODEC.parse(ops, stack(entry)).getOrThrow();
        return result.copyWithCount((int) amount(entry));
    }

    private static FluidStack fluid(JsonObject entry, DynamicOps<JsonElement> ops) {
        JsonObject stack = stack(entry);
        stack.addProperty("amount", (int) amount(entry));
        return FluidStack.CODEC.parse(ops, stack).getOrThrow();
    }
}
