package com.lumina.twinstar.compat;

import appeng.api.stacks.AEKey;
import com.lumina.twinstar.api.IBigStorage;

import java.math.BigInteger;

/**
 * What the adapters share: they only read and take out. Putting in stays AE2's way, with its priorities and
 * partitions.
 */
abstract class ExactStorage implements IBigStorage {
    @Override
    public BigInteger insertBig(AEKey key, BigInteger amount, boolean simulate) {
        return BigInteger.ZERO;
    }
}
