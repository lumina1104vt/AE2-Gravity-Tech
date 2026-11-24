package com.lumina.bridge.data;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tree of named values for saving, without the version's own classes: numbers, text, number arrays, other
 * {@code Data} and {@link DataList}s. Reads work the way Minecraft's compound tag does: a name that is missing
 * (or holds another sort of value) gives zero, empty or false. {@link Nbt} turns it into and out of the real thing.
 */
public final class Data {
    final Map<String, Object> values = new LinkedHashMap<>();

    // ---- write ----

    public Data putInt(String name, int value) {
        values.put(name, value);
        return this;
    }

    public Data putLong(String name, long value) {
        values.put(name, value);
        return this;
    }

    public Data putDouble(String name, double value) {
        values.put(name, value);
        return this;
    }

    /** Saved as a byte, 0 or 1, as Minecraft does. */
    public Data putBoolean(String name, boolean value) {
        values.put(name, (byte) (value ? 1 : 0));
        return this;
    }

    public Data putString(String name, String value) {
        values.put(name, value);
        return this;
    }

    public Data putIntArray(String name, int[] value) {
        values.put(name, value.clone());
        return this;
    }

    /** Saved as four ints (most significant first), as Minecraft does. */
    public Data putUUID(String name, java.util.UUID value) {
        long most = value.getMostSignificantBits(), least = value.getLeastSignificantBits();
        values.put(name, new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
        return this;
    }

    public Data put(String name, Data value) {
        values.put(name, value);
        return this;
    }

    public Data put(String name, DataList value) {
        values.put(name, value);
        return this;
    }

    // ---- read ----

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public int getInt(String name) {
        return values.get(name) instanceof Number n ? n.intValue() : 0;
    }

    public long getLong(String name) {
        return values.get(name) instanceof Number n ? n.longValue() : 0L;
    }

    public double getDouble(String name) {
        return values.get(name) instanceof Number n ? n.doubleValue() : 0.0;
    }

    public boolean getBoolean(String name) {
        return values.get(name) instanceof Number n && n.byteValue() != 0;
    }

    public String getString(String name) {
        return values.get(name) instanceof String s ? s : "";
    }

    public int[] getIntArray(String name) {
        return values.get(name) instanceof int[] a ? a.clone() : new int[0];
    }

    public @Nullable java.util.UUID getUUID(String name) {
        return values.get(name) instanceof int[] a && a.length == 4
                ? new java.util.UUID((long) a[0] << 32 | a[1] & 0xFFFFFFFFL, (long) a[2] << 32 | a[3] & 0xFFFFFFFFL) : null;
    }

    /** The sub-tree of that name; an empty one when there is none. */
    public Data getData(String name) {
        return values.get(name) instanceof Data d ? d : new Data();
    }

    /** The list of that name; an empty one when there is none. */
    public DataList getList(String name) {
        return values.get(name) instanceof DataList l ? l : new DataList();
    }

    public int size() {
        return values.size();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Data d) || d.values.size() != values.size())
            return false;
        for (Map.Entry<String, Object> e : values.entrySet())
            if (!same(e.getValue(), d.values.get(e.getKey())))
                return false;
        return true;
    }

    static boolean same(Object a, Object b) {
        return a instanceof int[] x ? b instanceof int[] y && java.util.Arrays.equals(x, y) : java.util.Objects.equals(a, b);
    }

    @Override
    public int hashCode() {
        return values.size();
    }

    /** The stored values, for the version side to turn into its own tags. */
    public Map<String, Object> raw() {
        return java.util.Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
