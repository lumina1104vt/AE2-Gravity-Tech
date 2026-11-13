package com.lumina.ae2_gvt.recipes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Items lying together that are caught in an explosion ({@code ae2_gvt:explosion}): one {@code center} item, and the
 * {@code ingredients} within {@code radius} blocks of it, are used up and make {@code results} (items dropped there)
 * and/or {@code block} (put where the center lay). With {@code repeat} it is made as many times over as everything is
 * there. What else the explosion does is {@code shield}: {@code all} (no item, no block is hurt), {@code items} (items
 * survive) or {@code none}. Recipes of higher {@code priority} are tried first; once one with shield {@code all} has
 * made something, no other is tried.
 * <pre>
 * {"type": "ae2_gvt:explosion", "center": {"item": "ae2_gvt:chaos_matter"},
 *  "ingredients": [{"item": "ae2:singularity", "count": 1}, {"item": "ae2_gvt:gravity_dust", "count": 64}],
 *  "block": "ae2_gvt:black_hole", "radius": 1.5, "shield": "all", "priority": 1}
 * </pre>
 */
public record ExplosionRecipe(Ingredient center, List<SizedIngredient> ingredients, List<ItemStack> results, Optional<ResourceLocation> block,
                              double radius, boolean repeat, Shield shield, int priority) implements Recipe<Recipes.NoInput> {
    public enum Shield {
        NONE, ITEMS, ALL;

        static final Codec<Shield> CODEC = Codec.STRING.xmap(s -> valueOf(s.toUpperCase(Locale.ROOT)), s -> s.name().toLowerCase(Locale.ROOT));
        static final StreamCodec<io.netty.buffer.ByteBuf, Shield> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Shield::ordinal);
    }

    @Override
    public boolean matches(Recipes.NoInput input, Level level) {
        return false;   // the explosion does the work, not a crafting grid
    }

    @Override
    public ItemStack assemble(Recipes.NoInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (!results.isEmpty())
            return results.get(0);
        return block.map(b -> new ItemStack(BuiltInRegistries.BLOCK.get(b))).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(center);
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.EXPLOSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Recipes.EXPLOSION.get();
    }

    public static final class Serializer implements RecipeSerializer<ExplosionRecipe> {
        private static final MapCodec<ExplosionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("center").forGetter(ExplosionRecipe::center),
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(ExplosionRecipe::ingredients),
                Recipes.RESULT.listOf().optionalFieldOf("results", List.of()).forGetter(ExplosionRecipe::results),
                ResourceLocation.CODEC.optionalFieldOf("block").forGetter(ExplosionRecipe::block),
                Codec.DOUBLE.optionalFieldOf("radius", 1.5).forGetter(ExplosionRecipe::radius),
                Codec.BOOL.optionalFieldOf("repeat", false).forGetter(ExplosionRecipe::repeat),
                Shield.CODEC.optionalFieldOf("shield", Shield.NONE).forGetter(ExplosionRecipe::shield),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(ExplosionRecipe::priority)
        ).apply(i, ExplosionRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ExplosionRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.center);
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.ingredients);
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.results);
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.block);
                    buf.writeDouble(r.radius);
                    buf.writeBoolean(r.repeat);
                    Shield.STREAM_CODEC.encode(buf, r.shield);
                    buf.writeVarInt(r.priority);
                },
                buf -> new ExplosionRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                        buf.readDouble(), buf.readBoolean(), Shield.STREAM_CODEC.decode(buf), buf.readVarInt()));

        @Override
        public MapCodec<ExplosionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExplosionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
