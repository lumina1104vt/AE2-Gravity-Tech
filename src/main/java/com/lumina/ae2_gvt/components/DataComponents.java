package com.lumina.ae2_gvt.components;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.armor.data.InstalledModule;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AE2GravityTech.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<InstalledModule>>> INSTALLED_MODULES = COMPONENTS
            .registerComponentType("armor_modules", b -> b.persistent(InstalledModule.LIST_CODEC)
                    .networkSynchronized(InstalledModule.LIST_STREAM_CODEC));

    /** On a black hole disk: which of the world's disks it is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BLACK_HOLE_DISK = COMPONENTS
            .registerComponentType("black_hole_disk", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
