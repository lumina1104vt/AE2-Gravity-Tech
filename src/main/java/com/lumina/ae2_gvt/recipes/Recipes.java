package com.lumina.ae2_gvt.recipes;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own recipe types. */
public final class Recipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2GravityTech.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GravityProcessingRecipe>> GRAVITY_PROCESSING =
            TYPES.register("gravity_processing", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "gravity_processing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GravityProcessingRecipe>> GRAVITY_PROCESSING_SERIALIZER =
            SERIALIZERS.register("gravity_processing", GravityProcessingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionRecipe>> FUSION =
            TYPES.register("fusion", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionRecipe>> FUSION_SERIALIZER =
            SERIALIZERS.register("fusion", FusionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<HoleConversionRecipe>> HOLE_CONVERSION =
            TYPES.register("hole_conversion", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "hole_conversion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HoleConversionRecipe>> HOLE_CONVERSION_SERIALIZER =
            SERIALIZERS.register("hole_conversion", HoleConversionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExplosionRecipe>> EXPLOSION =
            TYPES.register("explosion", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "explosion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExplosionRecipe>> EXPLOSION_SERIALIZER =
            SERIALIZERS.register("explosion", ExplosionRecipe.Serializer::new);

    /** The input of a recipe that no crafting grid or machine feeds: what happens in the world. */
    public enum NoInput implements net.minecraft.world.item.crafting.RecipeInput {
        INSTANCE;

        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /** A recipe's result. Not ItemStack.CODEC: that one takes at most 99, and silently makes a larger count 1. */
    public static final Codec<ItemStack> RESULT = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(ItemStack::getCount)
    ).apply(i, ItemStack::new));

    private Recipes() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
