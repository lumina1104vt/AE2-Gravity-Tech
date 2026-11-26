package com.lumina.twinstar.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.lumina.twinstar.api.IBigStorage;
import com.lumina.twinstar.api.StorageAdapter;
import com.lumina.twinstar.api.StorageAdapters;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.function.BiConsumer;

/**
 * GTLCore's disks keep finite contents past a long and list them as Long.MAX_VALUE. Their interface
 * {@code PreciseStorageAmount} says the exact amount; it is called by name, so GTLCore is not needed to compile.
 */
public final class GtlStorageAdapter implements StorageAdapter {
    private static final String PRECISE = "org.gtlcore.gtlcore.integration.ae2.storage.PreciseStorageAmount";

    private final Class<?> precise;
    private final Method exact;

    private GtlStorageAdapter(Class<?> precise) throws NoSuchMethodException {
        this.precise = precise;
        this.exact = precise.getMethod("getExactStoredAmount", AEKey.class);
    }

    static void register() {
        try {
            StorageAdapters.register(new GtlStorageAdapter(
                    Class.forName(PRECISE, false, GtlStorageAdapter.class.getClassLoader())));
        } catch (ReflectiveOperationException e) {
            LogUtils.getLogger().warn("GTLCore is here but not as expected; its disks are counted as AE2 lists them", e);
        }
    }

    @Override
    public @Nullable IBigStorage adapt(Object storage) {
        return precise.isInstance(storage) && storage instanceof MEStorage cell ? new Disk(cell) : null;
    }

    private final class Disk extends ExactStorage {
        private final MEStorage cell;

        private Disk(MEStorage cell) {
            this.cell = cell;
        }

        /** The exact amount of a key that is past a long; zero for any other. */
        @Override
        public BigInteger getAmount(AEKey key) {
            try {
                BigInteger all = (BigInteger) exact.invoke(cell, key);
                return all != null && all.compareTo(Saturated.LONG_MAX) > 0 ? all : BigInteger.ZERO;
            } catch (ReflectiveOperationException e) {
                return BigInteger.ZERO;
            }
        }

        @Override
        public BigInteger extractBig(AEKey key, BigInteger amount, boolean simulate) {
            BigInteger left = amount.min(getAmount(key)).max(BigInteger.ZERO);
            if (simulate)
                return left;
            BigInteger done = BigInteger.ZERO;
            // one long per call; far more calls than any real amount needs
            for (int i = 0; i < 100_000 && left.signum() > 0; i++) {
                long chunk = left.min(Saturated.LONG_MAX).longValue();
                long got = cell.extract(key, chunk, Actionable.MODULATE, IActionSource.empty());
                done = done.add(BigInteger.valueOf(got));
                left = left.subtract(BigInteger.valueOf(got));
                if (got < chunk)
                    break;
            }
            return done;
        }

        @Override
        public void forEachStored(BiConsumer<AEKey, BigInteger> action) {
            for (AEKey key : Saturated.keys(cell)) {
                BigInteger amount = getAmount(key);
                if (amount.signum() > 0)
                    action.accept(key, amount);
            }
        }
    }
}
