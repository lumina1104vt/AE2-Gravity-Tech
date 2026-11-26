package com.lumina.twinstar.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.lumina.twinstar.api.IBigStorage;
import com.lumina.twinstar.api.StorageAdapter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Neo ECO's drives and the storage system's shared domain: grid node owners that mount a storage taking
 * BigInteger amounts. Called by name, so Neo ECO is not needed to compile; without the method the adapter answers
 * "nothing" and AE2's view stays.
 */
public final class EcoStorageAdapter implements StorageAdapter {
    /** more than any storage holds: one that gives all of it never runs out */
    private static final BigInteger PROBE = BigInteger.ONE.shiftLeft(4096);
    /** listing a storage is not free, and the engine asks key by key */
    private static final long LISTING_NANOS = 50_000_000L;

    private static final ClassValue<Optional<Method>> EXTRACT = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                Method m = type.getMethod("extractBigInteger", AEKey.class, BigInteger.class, Actionable.class,
                        IActionSource.class);
                m.trySetAccessible();
                return Optional.of(m);
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        }
    };

    @Override
    public @Nullable IBigStorage adapt(Object storage) {
        return storage instanceof MountedView view ? new Mounted(view) : null;
    }

    private static final class Mounted extends ExactStorage {
        private final MountedView owner;
        private @Nullable MEStorage listedOf;
        private Set<AEKey> saturated = Set.of();
        private long listedAt;

        private Mounted(MountedView owner) {
            this.owner = owner;
        }

        private Set<AEKey> saturated() {
            MEStorage mounted = owner.twinstarCompat$mounted();
            if (mounted == null || EXTRACT.get(mounted.getClass()).isEmpty())
                return Set.of();
            long now = System.nanoTime();
            if (mounted != listedOf || now - listedAt > LISTING_NANOS) {
                saturated = Saturated.keys(mounted);
                listedOf = mounted;
                listedAt = now;
            }
            return saturated;
        }

        private BigInteger take(AEKey key, BigInteger amount, Actionable mode) {
            MEStorage mounted = owner.twinstarCompat$mounted();
            if (mounted == null)
                return BigInteger.ZERO;
            try {
                Method m = EXTRACT.get(mounted.getClass()).orElse(null);
                return m == null ? BigInteger.ZERO
                        : (BigInteger) m.invoke(mounted, key, amount, mode, IActionSource.empty());
            } catch (ReflectiveOperationException e) {
                return BigInteger.ZERO;
            }
        }

        @Override
        public boolean isEndless(AEKey key) {
            return saturated().contains(key) && take(key, PROBE, Actionable.SIMULATE).equals(PROBE);
        }

        /** The exact amount of a key that is past a long; zero for any other, and for an endless one. */
        @Override
        public BigInteger getAmount(AEKey key) {
            if (!saturated().contains(key))
                return BigInteger.ZERO;
            BigInteger all = take(key, PROBE, Actionable.SIMULATE);
            return all.equals(PROBE) ? BigInteger.ZERO : all;
        }

        @Override
        public BigInteger extractBig(AEKey key, BigInteger amount, boolean simulate) {
            if (amount.signum() <= 0 || getAmount(key).signum() == 0)
                return BigInteger.ZERO;
            return take(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        }

        @Override
        public void forEachStored(BiConsumer<AEKey, BigInteger> action) {
            for (AEKey key : saturated()) {
                BigInteger amount = getAmount(key);
                if (amount.signum() > 0)
                    action.accept(key, amount);
            }
        }
    }
}
