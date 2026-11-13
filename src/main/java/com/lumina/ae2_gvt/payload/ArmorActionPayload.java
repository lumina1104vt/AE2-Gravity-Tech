package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.ArmorEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A key of the antigravity armor: the portable workbench, the dash. The server checks the module is installed and switched on. */
public record ArmorActionPayload(int action, BlockPos pos) implements CustomPacketPayload {
    public static final int WORKBENCH = 0, DASH = 1, PICK = 2;
    public static final Type<ArmorActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "armor_action"));
    public static final StreamCodec<ByteBuf, ArmorActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorActionPayload::action, BlockPos.STREAM_CODEC, ArmorActionPayload::pos, ArmorActionPayload::new);

    public ArmorActionPayload(int action) {
        this(action, BlockPos.ZERO);
    }
    private static final String DASH_READY = "ae2_gvt_dash_ready";

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArmorActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
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
