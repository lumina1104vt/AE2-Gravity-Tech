package com.lumina.twinstar.compat;

import appeng.api.storage.MEStorage;
import org.jetbrains.annotations.Nullable;

/** A Neo ECO block that says what it has mounted on the network (added by this mod's one mixin). */
public interface MountedView {
    @Nullable MEStorage twinstarCompat$mounted();
}
