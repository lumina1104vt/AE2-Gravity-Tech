package com.lumina.twinstar.compat;

import com.lumina.twinstar.api.StorageAdapters;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Registers an adapter for each mod it knows that is there. */
@Mod("twinstar_compat")
public final class TwinStarCompat {
    public TwinStarCompat() {
        if (ModList.get().isLoaded("gtlcore"))
            GtlStorageAdapter.register();
        if (ModList.get().isLoaded("neoecoae"))
            StorageAdapters.register(new EcoStorageAdapter());
    }
}
