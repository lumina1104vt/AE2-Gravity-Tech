package com.lumina.twinstar.compat.mixin;

import appeng.api.storage.IStorageMounts;
import appeng.api.storage.MEStorage;
import com.lumina.twinstar.compat.MountedView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only remembers what a Neo ECO block mounts: AE2 asks it to mount and keeps the answer to itself, so there is no
 * other way to reach it from outside.
 */
@Pseudo
@Mixin(targets = {"cn.dancingsnow.neoecoae.blocks.entity.storage.ECODriveBlockEntity",
        "cn.dancingsnow.neoecoae.blocks.entity.storage.ECOStorageSystemBlockEntity"}, remap = false)
public abstract class EcoMountMixin implements MountedView {
    @Unique
    private MEStorage twinstarCompat$storage;

    @Inject(method = "mountInventories", at = @At("HEAD"), remap = false)
    private void twinstarCompat$unmounted(IStorageMounts mounts, CallbackInfo ci) {
        twinstarCompat$storage = null;
    }

    @ModifyArg(method = "mountInventories", index = 0, remap = false,
            at = @At(value = "INVOKE", target = "Lappeng/api/storage/IStorageMounts;mount(Lappeng/api/storage/MEStorage;I)V"))
    private MEStorage twinstarCompat$remember(MEStorage storage) {
        twinstarCompat$storage = storage;
        return storage;
    }

    @Override
    public @Nullable MEStorage twinstarCompat$mounted() {
        return twinstarCompat$storage;
    }
}
