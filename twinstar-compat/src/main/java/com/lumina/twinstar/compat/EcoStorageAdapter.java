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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Neo ECO's drives and the storage system's shared domain: grid node owners that mount a storage which can say
 * amounts past a long ({@code ExactAmountSource}) and endless ones. Called by name, so Neo ECO is not needed to
 * compile; without the method the adapter answers "nothing" and AE2's view stays. Taking out goes through the
 * storage's own long-sized extract, as often as needed.
 */
public final class EcoStorageAdapter implements StorageAdapter {
    /** listing a storage is not free, and the engine asks key by key */
    private static final long LISTING_NANOS = 50_000_000L;

    private static final ClassValue<Optional<Method>> VISIT = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                Method m = type.getMethod("neoecoae$visitExactAmounts", BiConsumer.class);
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
        private long listedAt;
        /** what is past a long */
        private final Map<AEKey, BigInteger> amounts = new HashMap<>();
        private final Set<AEKey> endless = new HashSet<>();

        private Mounted(MountedView owner) {
            this.owner = owner;
        }

        private void refresh() {
            MEStorage mounted = owner.twinstarCompat$mounted();
            long now = System.nanoTime();
            if (mounted == listedOf && now - listedAt <= LISTING_NANOS)
                return;
            listedOf = mounted;
            listedAt = now;
            amounts.clear();
            endless.clear();
            if (mounted == null)
                return;
            Method visit = VISIT.get(mounted.getClass()).orElse(null);
            if (visit == null)
                return;
            Set<AEKey> saturated = Saturated.keys(mounted);
            if (saturated.isEmpty())
                return;
            BiConsumer<AEKey, Object> take = (key, exact) -> {
                if (!saturated.contains(key))
                    return;
                try {
                    if ((boolean) exact.getClass().getMethod("infinite").invoke(exact)) {
                        endless.add(key);
                    } else {
                        BigInteger value = (BigInteger) exact.getClass().getMethod("value").invoke(exact);
                        if (value.compareTo(Saturated.LONG_MAX) > 0)
                            amounts.put(key, value);
                    }
                } catch (ReflectiveOperationException e) {
                    // not as expected: AE2's view of this key stays
                }
            };
            try {
                visit.invoke(mounted, take);
            } catch (ReflectiveOperationException e) {
                amounts.clear();
                endless.clear();
            }
        }

        @Override
        public boolean isEndless(AEKey key) {
            refresh();
            return endless.contains(key);
        }

        /** The exact amount of a key that is past a long; zero for any other, and for an endless one. */
        @Override
        public BigInteger getAmount(AEKey key) {
            refresh();
            return amounts.getOrDefault(key, BigInteger.ZERO);
        }

        @Override
        public BigInteger extractBig(AEKey key, BigInteger amount, boolean simulate) {
            BigInteger left = amount.min(getAmount(key)).max(BigInteger.ZERO);
            MEStorage mounted = owner.twinstarCompat$mounted();
            if (simulate || mounted == null)
                return left;
            BigInteger done = BigInteger.ZERO;
            // one long per call; far more calls than any real amount needs
            for (int i = 0; i < 100_000 && left.signum() > 0; i++) {
                long chunk = left.min(Saturated.LONG_MAX).longValue();
                long got = mounted.extract(key, chunk, Actionable.MODULATE, IActionSource.empty());
                done = done.add(BigInteger.valueOf(got));
                left = left.subtract(BigInteger.valueOf(got));
                if (got < chunk)
                    break;
            }
            listedAt -= LISTING_NANOS + 1;     // what was taken changes the listing
            return done;
        }

        @Override
        public void forEachStored(BiConsumer<AEKey, BigInteger> action) {
            refresh();
            Map.copyOf(amounts).forEach(action);
        }
    }
}
