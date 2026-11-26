package com.lumina.twinstar.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.lumina.twinstar.api.PushBatcher;
import com.lumina.twinstar.api.PushBatchers;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

/**
 * GTLCore's own crafting engine pushes many runs at once into its pattern buffers, and into providers set to
 * "auto expand". Its rule is asked as it stands (by name, so GTLCore is not needed to compile), so a twin-star job
 * batches exactly where a job of that engine would, with the same settings.
 */
public final class GtlPushBatcher implements PushBatcher {
    private static final String POLICY = "org.gtlcore.gtlcore.integration.ae2.graph.GtlDispatchPolicy";

    private final Method expanded;
    private final Method capacity;

    private GtlPushBatcher(Class<?> policy) throws NoSuchMethodException {
        expanded = policy.getDeclaredMethod("expanded", IPatternDetails.class, ICraftingProvider.class);
        capacity = policy.getDeclaredMethod("capacity", IPatternDetails.class, ICraftingProvider.class, long.class,
                boolean.class);
        expanded.setAccessible(true);
        capacity.setAccessible(true);
    }

    static void register() {
        try {
            PushBatchers.register(new GtlPushBatcher(
                    Class.forName(POLICY, false, GtlPushBatcher.class.getClassLoader())));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("GTLCore is here but not as expected; patterns are pushed one run at a time", e);
        }
    }

    @Override
    public long maxRuns(ICraftingProvider provider, IPatternDetails pattern, long wanted) {
        try {
            if (!(boolean) expanded.invoke(null, pattern, provider))
                return 1;
            return (long) capacity.invoke(null, pattern, provider, wanted, true);
        } catch (ReflectiveOperationException e) {
            return 1;
        }
    }
}
