package com.lumina.ae2_gvt.components;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One piece of data kept on an item stack (1.20.1 has no data components: it is NBT on the stack, in the compound
 * {@value #ROOT}, written with a codec; it travels with the stack to clients as the rest of the stack's NBT does).
 */
public final class ItemData<T> {
    public static final String ROOT = AE2GravityTech.MODID;

    private final String id;
    private final Codec<T> codec;

    public ItemData(String id, Codec<T> codec) {
        this.id = id;
        this.codec = codec;
    }

    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND))
            return null;
        Tag value = tag.getCompound(ROOT).get(id);
        return value == null ? null : codec.parse(NbtOps.INSTANCE, value).result().orElse(null);
    }

    /** The same data on an AE2 item key (a pattern's definition). */
    public @Nullable T get(appeng.api.stacks.AEItemKey key) {
        CompoundTag tag = key.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND))
            return null;
        Tag value = tag.getCompound(ROOT).get(id);
        return value == null ? null : codec.parse(NbtOps.INSTANCE, value).result().orElse(null);
    }

    public boolean has(ItemStack stack) {
        return get(stack) != null;
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value == null ? fallback : value;
    }

    public void set(ItemStack stack, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> stack.getOrCreateTagElement(ROOT).put(id, t));
    }

    public void remove(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND))
            return;
        CompoundTag root = tag.getCompound(ROOT);
        root.remove(id);
        if (root.isEmpty())
            tag.remove(ROOT);
        if (tag.isEmpty())
            stack.setTag(null);
    }
}
