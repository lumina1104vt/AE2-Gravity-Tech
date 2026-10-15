package com.lumina.ae2_gvt.items.armor.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record InstalledModule(Holder<Item> module, boolean enabled) implements Comparable<InstalledModule> {
    public static final Codec<InstalledModule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("module").forGetter(InstalledModule::module),
            Codec.BOOL.fieldOf("enabled").forGetter(InstalledModule::enabled)).apply(inst, InstalledModule::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, InstalledModule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM), InstalledModule::module,
            ByteBufCodecs.BOOL, InstalledModule::enabled,
            InstalledModule::new);

    public static final Codec<List<InstalledModule>> LIST_CODEC = CODEC.listOf();
    public static final StreamCodec<RegistryFriendlyByteBuf, List<InstalledModule>> LIST_STREAM_CODEC = STREAM_CODEC
            .apply(ByteBufCodecs.list());

    public InstalledModule toggled() {
        return new InstalledModule(module(), !enabled());
    }

    @Override
    public int compareTo(@NotNull InstalledModule o) {
        return Objects.requireNonNull(module.value().builtInRegistryHolder().getKey())
                .compareTo(Objects.requireNonNull(o.module.value().builtInRegistryHolder().getKey()));
    }
}
