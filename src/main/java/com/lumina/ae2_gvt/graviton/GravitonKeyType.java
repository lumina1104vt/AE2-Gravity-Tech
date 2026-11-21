package com.lumina.ae2_gvt.graviton;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.lumina.ae2_gvt.AE2GravityTech;

/** The ME storage type of {@link GravitonKey}; counted one by one, 8 per byte like items. */
public final class GravitonKeyType extends AEKeyType {
    public static final ResourceLocation ID = new ResourceLocation(AE2GravityTech.MODID, "graviton");
    public static final GravitonKeyType INSTANCE = new GravitonKeyType();

    private GravitonKeyType() {
        super(ID, GravitonKey.class, Component.translatable("ae2_gvt.key_type.graviton"));
    }

    @Override
    public AEKey loadKeyFromTag(net.minecraft.nbt.CompoundTag tag) {
        for (GravitonKey.Kind kind : GravitonKey.Kind.values())
            if (kind.getSerializedName().equals(tag.getString("id")))
                return GravitonKey.of(kind);
        return null;
    }

    @Override
    public AEKey readFromPacket(FriendlyByteBuf buf) {
        return GravitonKey.fromPacket(buf);
    }
}
