package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.AE2GravityTech;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: a big ripple runs out from {@code pos}, {@code reach} blocks far (a hole blown up, an 湮滅 TNT). */
public record HoleBurstPayload(BlockPos pos, float reach) implements CustomPacketPayload {
    public static final Type<HoleBurstPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "hole_burst"));
    public static final StreamCodec<ByteBuf, HoleBurstPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HoleBurstPayload::pos, ByteBufCodecs.FLOAT, HoleBurstPayload::reach, HoleBurstPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HoleBurstPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.lumina.ae2_gvt.client.HoleRipples.burst(payload.pos(), payload.reach()));
    }
}
