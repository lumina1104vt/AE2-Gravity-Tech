package com.lumina.ae2_gvt.recipes;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;

/** The mod's own recipe types. */
public final class Recipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2GravityTech.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GravityProcessingRecipe>> GRAVITY_PROCESSING =
            TYPES.register("gravity_processing", () -> RecipeType.simple(new ResourceLocation(AE2GravityTech.MODID, "gravity_processing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GravityProcessingRecipe>> GRAVITY_PROCESSING_SERIALIZER =
            SERIALIZERS.register("gravity_processing", GravityProcessingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionRecipe>> FUSION =
            TYPES.register("fusion", () -> RecipeType.simple(new ResourceLocation(AE2GravityTech.MODID, "fusion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionRecipe>> FUSION_SERIALIZER =
            SERIALIZERS.register("fusion", FusionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<HoleConversionRecipe>> HOLE_CONVERSION =
            TYPES.register("hole_conversion", () -> RecipeType.simple(new ResourceLocation(AE2GravityTech.MODID, "hole_conversion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HoleConversionRecipe>> HOLE_CONVERSION_SERIALIZER =
            SERIALIZERS.register("hole_conversion", HoleConversionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExplosionRecipe>> EXPLOSION =
            TYPES.register("explosion", () -> RecipeType.simple(new ResourceLocation(AE2GravityTech.MODID, "explosion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExplosionRecipe>> EXPLOSION_SERIALIZER =
            SERIALIZERS.register("explosion", ExplosionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FactoryBorrowRecipe>> FACTORY_BORROW =
            TYPES.register("factory_borrow", () -> RecipeType.simple(new ResourceLocation(AE2GravityTech.MODID, "factory_borrow")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FactoryBorrowRecipe>> FACTORY_BORROW_SERIALIZER =
            SERIALIZERS.register("factory_borrow", FactoryBorrowRecipe.Serializer::new);

    /** The input of a recipe that no crafting grid or machine feeds: what happens in the world. */
    public static final Container NO_INPUT = new SimpleContainer(0);

    private Recipes() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
