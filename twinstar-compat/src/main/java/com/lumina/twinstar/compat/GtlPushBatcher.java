package com.lumina.twinstar.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import com.lumina.twinstar.api.PushBatcher;
import com.lumina.twinstar.api.PushBatchers;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

/**
 * GTLCore's pattern buffers keep everything a push carries, so several runs can go in one push with their inputs
 * added up. Only those buffers: an ordinary AE2 pattern provider may send just one run's amounts of a processing
 * pattern and drop the rest of what it was handed, which would lose materials. How many runs is asked of GTLCore's
 * own rule (by name, so GTLCore is not needed to compile).
 */
public final class GtlPushBatcher implements PushBatcher {
    private static final String POLICY = "org.gtlcore.gtlcore.integration.ae2.graph.GtlDispatchPolicy";
    private static final String BUFFER = "org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine";

    private final Class<?> buffer;

    private final Method expanded;
    private final Method capacity;

    private GtlPushBatcher(Class<?> policy, Class<?> buffer) throws NoSuchMethodException {
        this.buffer = buffer;
        expanded = policy.getDeclaredMethod("expanded", IPatternDetails.class, ICraftingProvider.class);
        capacity = policy.getDeclaredMethod("capacity", IPatternDetails.class, ICraftingProvider.class, long.class,
                boolean.class);
        expanded.setAccessible(true);
        capacity.setAccessible(true);
    }

    static void register() {
        try {
            ClassLoader loader = GtlPushBatcher.class.getClassLoader();
            PushBatchers.register(new GtlPushBatcher(Class.forName(POLICY, false, loader),
                    Class.forName(BUFFER, false, loader)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("GTLCore is here but not as expected; patterns are pushed one run at a time", e);
        }
    }

    @Override
    public long maxRuns(ICraftingProvider provider, IPatternDetails pattern, long wanted) {
        try {
            if (!buffer.isInstance(provider) || !(boolean) expanded.invoke(null, pattern, provider))
                return 1;
            return (long) capacity.invoke(null, pattern, provider, wanted, true);
        } catch (ReflectiveOperationException e) {
            return 1;
        }
    }
}
