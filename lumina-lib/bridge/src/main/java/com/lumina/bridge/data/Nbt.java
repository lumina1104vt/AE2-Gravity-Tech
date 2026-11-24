package com.lumina.bridge.data;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

/** The 1.21.1 side of {@link Data}: Minecraft's NBT in and out, tag types kept as they are. */
public final class Nbt {
    private Nbt() {
    }

    public static CompoundTag toNbt(Data data) {
        CompoundTag out = new CompoundTag();
        for (Map.Entry<String, Object> e : data.raw().entrySet())
            out.put(e.getKey(), tagOf(e.getValue()));
        return out;
    }

    public static Data fromNbt(CompoundTag tag) {
        Data out = new Data();
        for (String key : tag.getAllKeys())
            out.values.put(key, valueOf(tag.get(key)));
        return out;
    }

    private static Tag tagOf(Object v) {
        if (v instanceof Integer i)
            return IntTag.valueOf(i);
        if (v instanceof Long l)
            return LongTag.valueOf(l);
        if (v instanceof Double d)
            return DoubleTag.valueOf(d);
        if (v instanceof Float f)
            return FloatTag.valueOf(f);
        if (v instanceof Short s)
            return ShortTag.valueOf(s);
        if (v instanceof Byte b)
            return ByteTag.valueOf(b);
        if (v instanceof String s)
            return StringTag.valueOf(s);
        if (v instanceof int[] a)
            return new IntArrayTag(a.clone());
        if (v instanceof Data d)
            return toNbt(d);
        if (v instanceof DataList l) {
            ListTag out = new ListTag();
            for (Object o : l.raw())
                out.add(tagOf(o));
            return out;
        }
        throw new IllegalArgumentException("not saveable: " + v);
    }

    private static Object valueOf(Tag t) {
        if (t instanceof NumericTag n) {
            if (t instanceof ByteTag)
                return n.getAsByte();
            if (t instanceof ShortTag)
                return n.getAsShort();
            if (t instanceof IntTag)
                return n.getAsInt();
            if (t instanceof LongTag)
                return n.getAsLong();
            if (t instanceof FloatTag)
                return n.getAsFloat();
            return n.getAsDouble();
        }
        if (t instanceof StringTag s)
            return s.getAsString();
        if (t instanceof IntArrayTag a)
            return a.getAsIntArray().clone();
        if (t instanceof CompoundTag c)
            return fromNbt(c);
        if (t instanceof ListTag l) {
            DataList out = new DataList();
            for (Tag e : l)
                out.items.add(valueOf(e));
            return out;
        }
        throw new IllegalArgumentException("not readable: " + t);
    }
}
