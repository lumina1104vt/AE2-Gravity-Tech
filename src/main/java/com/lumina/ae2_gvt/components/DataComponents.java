package com.lumina.ae2_gvt.components;

import com.lumina.ae2_gvt.items.armor.data.InstalledModule;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

/** The data the mod keeps on its items (NBT on the stack: see {@link ItemData}). */
public class DataComponents {
    public static final ItemData<List<InstalledModule>> INSTALLED_MODULES = new ItemData<>("armor_modules", InstalledModule.LIST_CODEC);

    /** On a black hole disk: which of the world's disks it is. */
    public static final ItemData<UUID> BLACK_HOLE_DISK = new ItemData<>("black_hole_disk", UUIDUtil.CODEC);
}
