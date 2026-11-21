package com.lumina.ae2_gvt.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: a big ripple runs out from {@code pos}, {@code reach} blocks far (a hole blown up, an 湮滅 TNT). */
public record HoleBurstPayload(BlockPos pos, float reach) {
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(reach);
    }

    public static HoleBurstPayload read(FriendlyByteBuf buf) {
        return new HoleBurstPayload(buf.readBlockPos(), buf.readFloat());
    }

    public static void handle(HoleBurstPayload payload, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> com.lumina.ae2_gvt.client.HoleRipples.burst(payload.pos(), payload.reach()));
        ctx.setPacketHandled(true);
    }
}
