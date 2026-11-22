package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "Ctrl is down": the server cannot see keys, so the client says so right before the right click on a machine that it sends
 * next (the client runs the item first, then sends the click, on the same connection, so this always arrives first). The next
 * click of the 機器連結器 then links every machine of the same kind, see {@link MachineLinkTool#ctrlDown}.
 */
public class LinkAllPayload {
    public static final LinkAllPayload INSTANCE = new LinkAllPayload();

    public void write(FriendlyByteBuf buf) {
    }

    public static LinkAllPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(LinkAllPayload payload, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null)
                MachineLinkTool.ctrlDown(player);
        });
        ctx.setPacketHandled(true);
    }
}
