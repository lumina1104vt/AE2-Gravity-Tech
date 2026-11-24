package com.lumina.bridge.key;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Wrapping and unwrapping of {@link Key}, the 1.21.1 + AE2 side. */
public final class Keys {
    // one Key per AE2 key; the set of distinct keys in a world stays small, so it is never trimmed
    private static final Map<AEKey, Key> CACHE = new ConcurrentHashMap<>();

    private Keys() {
    }

    /** AE2 → neutral; the same key always gives the same instance. */
    public static Key of(AEKey key) {
        Key k = CACHE.get(key);
        return k != null ? k : CACHE.computeIfAbsent(key, Keys::wrap);
    }

    /** Neutral → AE2; version layer only. Null when the key did not come from AE2. */
    public static @Nullable AEKey toAe(Key key) {
        return key.handle(AEKey.class);
    }

    private static Key wrap(AEKey key) {
        Key.Kind kind = key.getType() == appeng.api.stacks.AEKeyType.items() ? Key.Kind.ITEM
                : key.getType() == appeng.api.stacks.AEKeyType.fluids() ? Key.Kind.FLUID : Key.Kind.OTHER;
        int limit = key instanceof AEItemKey item ? item.getMaxStackSize() : 1;
        return new Key(kind, key.getId().toString(), key.getUnitSymbol(), key.getAmountPerUnit(), limit, key);
    }
}
