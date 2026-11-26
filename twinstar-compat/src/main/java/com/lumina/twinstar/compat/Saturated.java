package com.lumina.twinstar.compat;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * An adapter only speaks for what AE2's long cannot say: the keys a storage lists as Long.MAX_VALUE. Everything
 * else the engine reaches through AE2 as before.
 */
public final class Saturated {
    public static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private Saturated() {
    }

    public static Set<AEKey> keys(MEStorage storage) {
        KeyCounter listed = new KeyCounter();
        storage.getAvailableStacks(listed);
        Set<AEKey> out = new HashSet<>();
        for (var e : listed)
            if (e.getLongValue() == Long.MAX_VALUE)
                out.add(e.getKey());
        return out;
    }
}
