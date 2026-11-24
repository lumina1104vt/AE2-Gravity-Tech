package com.lumina.bridge.key;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * One kind of thing the engine moves around, version-neutral. Wraps whatever the platform uses (on 1.21.1 an AE2
 * key); everything but the version layer talks to this type only. No amount: amounts are separate.
 * <p>
 * Equality is the wrapped key's own, so item components stay distinct without the engine knowing they exist.
 * {@link Keys} hands out one instance per wrapped key.
 */
public final class Key {
    public enum Kind {ITEM, FLUID, OTHER}

    private final Kind kind;
    private final String id;
    private final String unit;
    private final int amountPerUnit;
    private final int stackLimit;
    private final Object handle;
    private final Supplier<String> displayName;
    private final Supplier<String> typeName;
    private final BiPredicate<Key, Key> sameIgnoringData;

    Key(Kind kind, String id, String unit, int amountPerUnit, int stackLimit, Object handle,
        Supplier<String> displayName, Supplier<String> typeName,
        BiPredicate<Key, Key> sameIgnoringData) {
        this.kind = Objects.requireNonNull(kind);
        this.id = Objects.requireNonNull(id);
        this.unit = unit == null ? "" : unit;
        this.amountPerUnit = amountPerUnit;
        this.stackLimit = stackLimit;
        this.handle = Objects.requireNonNull(handle);
        this.displayName = displayName;
        this.typeName = typeName;
        this.sameIgnoringData = sameIgnoringData;
    }

    public Kind kind() {
        return kind;
    }

    /** Stable name, e.g. {@code minecraft:iron_ingot}; for logs, sorting and tests. Not unique across components. */
    public String id() {
        return id;
    }

    /** Unit symbol: empty for items, "B" for fluids. */
    public String unit() {
        return unit;
    }

    /** How many raw amounts make one unit (1 for items, 1000 for fluids). */
    public int amountPerUnit() {
        return amountPerUnit;
    }

    /** The most that goes into one slot at a time. */
    public int stackLimit() {
        return stackLimit;
    }

    /** The name a player sees (an item's name, a fluid's name); for messages and logs, not for logic. */
    public String displayName() {
        return displayName.get();
    }

    /** What sort of thing it is, as a player would call it ("Items", "Fluids"); for messages. */
    public String typeName() {
        return typeName.get();
    }

    /**
     * The same thing as {@code other} apart from extra data on it: an item of the same kind with the same wear,
     * whatever else is on it (a tool in another state of repair keeps its kind; one with a name on it is the same).
     */
    public boolean sameIgnoringData(Key other) {
        return equals(other) || sameIgnoringData.test(this, other);
    }

    /** The wrapped platform key, or null when it is not a {@code type}. Version layer only. */
    public <T> @Nullable T handle(Class<T> type) {
        return type.isInstance(handle) ? type.cast(handle) : null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Key k && handle.equals(k.handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return kind + ":" + id;
    }
}
