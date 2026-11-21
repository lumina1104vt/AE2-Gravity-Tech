package com.lumina.ae2_gvt.recipes;

import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.Ingredient;

/** An ingredient and how many of it (NeoForge has one; 1.20.1 does not). In JSON: the ingredient's own fields plus {@code count}. */
public record SizedIngredient(Ingredient ingredient, int count) {
    public static SizedIngredient fromJson(JsonObject json) {
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        return new SizedIngredient(Ingredient.fromJson(json, false), Math.max(1, count));
    }

    public JsonObject toJson() {
        JsonObject json = ingredient.toJson().getAsJsonObject();
        json.addProperty("count", count);
        return json;
    }
}
