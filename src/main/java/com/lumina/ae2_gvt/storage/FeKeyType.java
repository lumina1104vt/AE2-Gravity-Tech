package com.lumina.ae2_gvt.storage;

import com.mojang.serialization.MapCodec;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The ME storage type of {@link FeKey}. */
public final class FeKeyType extends AEKeyType {
    public static final FeKeyType INSTANCE = new FeKeyType();

    private FeKeyType() {
        super(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fe"), FeKey.class, Component.translatable("ae2_gvt.key_type.fe"));
    }

    /** FE kept in a cell: a byte holds 10,000, so a 1k cell holds about 10 million and a 256k cell about 2.6 billion. */
    @Override
    public int getAmountPerByte() {
        return 10_000;
    }

    /** What moves at once, as Applied Flux has it: energy comes in far larger amounts than items. */
    @Override
    public int getAmountPerOperation() {
        return 16384;
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return FeKey.MAP_CODEC;
    }

    @Override
    public AEKey readFromPacket(RegistryFriendlyByteBuf buf) {
        return FeKey.INSTANCE;
    }
}
