package com.lumina.ae2_gvt.graviton;

import com.mojang.serialization.MapCodec;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.lumina.ae2_gvt.AE2GravityTech;

/** The ME storage type of {@link GravitonKey}; counted one by one, 8 per byte like items. */
public final class GravitonKeyType extends AEKeyType {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "graviton");
    public static final GravitonKeyType INSTANCE = new GravitonKeyType();

    private GravitonKeyType() {
        super(ID, GravitonKey.class, Component.translatable("ae2_gvt.key_type.graviton"));
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return GravitonKey.MAP_CODEC;
    }

    @Override
    public AEKey readFromPacket(RegistryFriendlyByteBuf buf) {
        return GravitonKey.fromPacket(buf);
    }
}
