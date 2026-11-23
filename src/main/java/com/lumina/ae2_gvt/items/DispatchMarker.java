package com.lumina.ae2_gvt.items;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.lumina.twinstar.algorithm.ae2.VirtualKeys;

/**
 * The dispatch marker: the product a pattern names when it is done the moment its materials are in the machine (a multiblock
 * whose real product leaves by its own output hatch). It is no real item: hidden from JEI / EMI and the creative tab, never
 * stored, never handed to anyone; the engine counts it in the job's ledger only.
 */
public final class DispatchMarker {
    private DispatchMarker() {
    }

    public static boolean is(AEKey key) {
        return key instanceof AEItemKey item && item.getItem() == Items.DISPATCH_MARKER.get();
    }

    public static AEItemKey key() {
        return AEItemKey.of(Items.DISPATCH_MARKER.get());
    }

    /** Tells the engine which key is a mark. */
    public static void register() {
        VirtualKeys.register(DispatchMarker::is);
    }
}
