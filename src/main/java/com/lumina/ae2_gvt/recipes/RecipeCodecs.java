package com.lumina.ae2_gvt.recipes;

import appeng.api.stacks.GenericStack;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The recipes' JSON (1.20.1's serializers read a JsonObject; the mod writes its recipes as codecs): ingredients, sized
 * ingredients, results, AE2 keys, and a serializer that sends a recipe to the client as its JSON text.
 */
public final class RecipeCodecs {
    public static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.flatXmap(
            d -> {
                try {
                    return DataResult.success(Ingredient.fromJson(d.convert(JsonOps.INSTANCE).getValue(), false));
                } catch (JsonParseException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            ing -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, ing.toJson())));

    public static final Codec<SizedIngredient> SIZED = Codec.PASSTHROUGH.flatXmap(
            d -> {
                try {
                    JsonElement json = d.convert(JsonOps.INSTANCE).getValue();
                    return DataResult.success(SizedIngredient.fromJson(json.getAsJsonObject()));
                } catch (RuntimeException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            s -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, s.toJson())));

    /** A recipe's result. Not ItemStack.CODEC: that one takes at most 99, and silently makes a larger count 1. */
    public static final Codec<ItemStack> RESULT = RecordCodecBuilder.create(i -> i.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("id").forGetter(ItemStack::getItem),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(ItemStack::getCount)
    ).apply(i, ItemStack::new));

    public static final Codec<SizedFluid> SIZED_FLUID = Codec.PASSTHROUGH.flatXmap(
            d -> {
                try {
                    JsonElement json = d.convert(JsonOps.INSTANCE).getValue();
                    return DataResult.success(SizedFluid.fromJson(json.getAsJsonObject()));
                } catch (RuntimeException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            s -> DataResult.success(new Dynamic<>(JsonOps.INSTANCE, s.toJson())));

    /** A fluid result: {@code {"id": "minecraft:water", "amount": 1000}}. */
    public static final Codec<FluidStack> FLUID_RESULT = RecordCodecBuilder.create(i -> i.group(
            ForgeRegistries.FLUIDS.getCodec().fieldOf("id").forGetter(FluidStack::getFluid),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount)
    ).apply(i, FluidStack::new));

    /** AE2 key with an amount, as AE2 writes it: {@code {"#c": "ae2:f", "id": "minecraft:water", "#": 1000}}. */
    public static final Codec<GenericStack> GENERIC_STACK = CompoundTag.CODEC.flatXmap(
            tag -> {
                GenericStack stack = GenericStack.readTag(tag);
                return stack == null ? DataResult.error(() -> "not an AE stack: " + tag) : DataResult.success(stack);
            },
            stack -> DataResult.success(GenericStack.writeTag(stack)));

    private RecipeCodecs() {
    }

    /**
     * A serializer written as one codec per recipe id: it reads the datapack's JSON with it and sends the recipe to the client as the
     * same JSON text.
     */
    public abstract static class JsonSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
        private static final int MAX_JSON = 1 << 21;

        protected abstract Codec<T> codec(ResourceLocation id);

        @Override
        public T fromJson(ResourceLocation id, JsonObject json) {
            return codec(id).parse(JsonOps.INSTANCE, json).getOrThrow(false, msg -> {
                throw new JsonParseException(id + ": " + msg);
            });
        }

        @Override
        public T fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            JsonElement json = com.google.gson.JsonParser.parseString(buf.readUtf(MAX_JSON));
            return codec(id).parse(JsonOps.INSTANCE, json).getOrThrow(false, msg -> {
                throw new JsonParseException(id + ": " + msg);
            });
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, T recipe) {
            JsonElement json = codec(recipe.getId()).encodeStart(JsonOps.INSTANCE, recipe).getOrThrow(false, msg -> {
                throw new JsonParseException(recipe.getId() + ": " + msg);
            });
            buf.writeUtf(json.toString(), MAX_JSON);
        }
    }
}
