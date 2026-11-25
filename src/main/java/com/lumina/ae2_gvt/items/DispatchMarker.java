package com.lumina.ae2_gvt.items;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.lumina.twinstar.algorithm.ae2.VirtualKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The dispatch marker: the product a pattern names when it is done the moment its materials are in the machine (a multiblock
 * whose real product leaves by its own output hatch). It is no real item: hidden from JEI / EMI and the creative tab, never
 * stored, never handed to anyone; the engine counts it in the job's ledger only.
 * <p>
 * A marker made from a product remembers it (a tag on the stack, so its tooltip can say what it stands for); every marker is a
 * marker whatever it remembers.
 */
public final class DispatchMarker {
    private static final String ORIGIN = "origin";

    private DispatchMarker() {
    }

    public static boolean is(AEKey key) {
        return key instanceof AEItemKey item && item.getItem() == Items.DISPATCH_MARKER.get();
    }

    public static AEItemKey key() {
        return AEItemKey.of(Items.DISPATCH_MARKER.get());
    }

    /** The marker that stands for {@code origin}: the product the pattern named before it was turned into a marker. */
    public static AEItemKey key(AEKey origin) {
        if (is(origin))
            return (AEItemKey) origin;                   // a marker of a marker is the marker
        CompoundTag tag = new CompoundTag();
        tag.put(ORIGIN, origin.toTagGeneric());
        return AEItemKey.of(Items.DISPATCH_MARKER.get(), tag);
    }

    /** What a marker stands for, or null (a marker made without one, or not a marker). */
    public static @Nullable AEKey originOf(ItemStack marker) {
        CompoundTag tag = marker.getTag();
        if (tag == null || !tag.contains(ORIGIN, 10))
            return null;
        try {
            return AEKey.fromTagGeneric(tag.getCompound(ORIGIN));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Tells the engine which key is a mark. */
    public static void register() {
        VirtualKeys.register(DispatchMarker::is);
    }
}
