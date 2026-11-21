package com.lumina.ae2_gvt.reg;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * A registered thing that is not there yet (NeoForge's name and shape; 1.20.1's Forge has RegistryObject, which cannot be
 * extended, so this wraps it).
 */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    protected final RegistryObject<T> object;

    public DeferredHolder(RegistryObject<T> object) {
        this.object = object;
    }

    @Override
    public T get() {
        return object.get();
    }

    public T value() {
        return object.get();
    }

    public ResourceLocation getId() {
        return object.getId();
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<R> getKey() {
        return (ResourceKey<R>) object.getKey();
    }

    public boolean isPresent() {
        return object.isPresent();
    }

    public boolean isBound() {
        return object.isPresent();
    }

    @Override
    public String toString() {
        return object.getId().toString();
    }
}
