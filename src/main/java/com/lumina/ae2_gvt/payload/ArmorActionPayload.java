package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.ArmorEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A key of the antigravity armor: the portable workbench, the dash. The server checks the module is installed and switched on. */
public record ArmorActionPayload(int action, BlockPos pos) {
    public static final int WORKBENCH = 0, DASH = 1, PICK = 2;
    private static final String DASH_READY = "ae2_gvt_dash_ready";

    public ArmorActionPayload(int action) {
        this(action, BlockPos.ZERO);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeBlockPos(pos);
    }

    public static ArmorActionPayload read(FriendlyByteBuf buf) {
        return new ArmorActionPayload(buf.readVarInt(), buf.readBlockPos());
    }

    public static void handle(ArmorActionPayload payload, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;
            switch (payload.action) {
                case WORKBENCH -> {
                    if (ArmorEffects.has(player, Items.WORKBENCH_MODULE))
                        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CraftingMenu(id, inventory, ContainerLevelAccess.NULL),
                                Component.translatable("container.crafting")));
                }
                case DASH -> dash(player);
                case PICK -> pick(player, payload.pos);
                default -> {
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void pick(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64 || !player.level().isLoaded(pos))
            return;
        var state = player.level().getBlockState(pos);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        com.lumina.ae2_gvt.items.armor.ArmorNetworkEffects.pick(player, state.getCloneItemStack(hit, player.level(), pos, player));
    }

    private static void dash(ServerPlayer player) {
        if (!ArmorEffects.has(player, Items.DASH_MODULE) || player.level().getGameTime() < player.getPersistentData().getLong(DASH_READY))
            return;
        player.getPersistentData().putLong(DASH_READY, player.level().getGameTime() + Config.DASH_COOLDOWN.get());
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(Config.DASH_SPEED.get()));
        player.hurtMarked = true;
        player.fallDistance = 0;
    }
}
