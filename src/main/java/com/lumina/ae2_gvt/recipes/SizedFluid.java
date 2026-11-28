package com.lumina.ae2_gvt.recipes;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A fluid ingredient and its amount in mB (NeoForge has one; 1.20.1 does not). In JSON
 * {@code {"fluid": "minecraft:water", "amount": 1000}} or {@code {"tag": "minecraft:water", "amount": 1000}}.
 */
public record SizedFluid(List<ResourceLocation> fluids, List<ResourceLocation> tags, int amount) {
    public boolean test(Fluid fluid) {
        if (fluids.contains(ForgeRegistries.FLUIDS.getKey(fluid)))
            return true;
        for (ResourceLocation tag : tags)
            if (fluid.builtInRegistryHolder().is(TagKey.create(Registries.FLUID, tag)))
                return true;
        return false;
    }

    /** Every fluid it takes, at its amount (recipe viewers). */
    public List<FluidStack> stacks() {
        List<FluidStack> stacks = new ArrayList<>();
        for (Fluid fluid : ForgeRegistries.FLUIDS)
            if (fluid.isSource(fluid.defaultFluidState()) && test(fluid))
                stacks.add(new FluidStack(fluid, amount));
        return stacks;
    }

    /** One {@code fluid} or {@code tag}, or a list of those under any one of them. */
    public static SizedFluid of(JsonElement body, int amount) {
        List<ResourceLocation> fluids = new ArrayList<>(), tags = new ArrayList<>();
        JsonArray entries = new JsonArray();
        if (body.isJsonArray())
            entries = body.getAsJsonArray();
        else
            entries.add(body);
        for (JsonElement e : entries) {
            JsonObject entry = e.getAsJsonObject();
            if (entry.has("fluid"))
                fluids.add(new ResourceLocation(entry.get("fluid").getAsString()));
            else if (entry.has("tag"))
                tags.add(new ResourceLocation(entry.get("tag").getAsString()));
            else
                throw new JsonParseException("not a fluid ingredient: " + entry);
        }
        return new SizedFluid(fluids, tags, Math.max(1, amount));
    }

    public static SizedFluid fromJson(JsonObject json) {
        return of(json, json.has("amount") ? json.get("amount").getAsInt() : 1000);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (!fluids.isEmpty())
            json.addProperty("fluid", fluids.get(0).toString());
        else if (!tags.isEmpty())
            json.addProperty("tag", tags.get(0).toString());
        json.addProperty("amount", amount);
        return json;
    }
}
