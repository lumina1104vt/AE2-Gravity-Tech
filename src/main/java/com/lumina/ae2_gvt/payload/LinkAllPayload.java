package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Ctrl is down": the server cannot see keys, so the client says so right before the right click on a machine that it sends
 * next (the client runs the item first, then sends the click, on the same connection, so this always arrives first). The next
 * click of the 機器連結器 then links every machine of the same kind, see {@link MachineLinkTool#ctrlDown}.
 */
public record LinkAllPayload() implements CustomPacketPayload {
    public static final LinkAllPayload INSTANCE = new LinkAllPayload();
    public static final Type<LinkAllPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "link_all"));
    public static final StreamCodec<ByteBuf, LinkAllPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LinkAllPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player)
            MachineLinkTool.ctrlDown(player);
    }
}
