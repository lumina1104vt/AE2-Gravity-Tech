package com.lumina.ae2_gvt.payload;

import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** The mod's packets (1.20.1: one SimpleChannel; each packet writes and reads itself). */
public final class ModNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AE2GravityTech.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNet() {
    }

    public static void toServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void toPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** To every player within {@code radius} blocks of the point. */
    public static void near(ServerLevel level, double x, double y, double z, double radius, Object packet) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())), packet);
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenArmorModulesPayload.class, OpenArmorModulesPayload::write, OpenArmorModulesPayload::read,
                OpenArmorModulesPayload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ArmorActionPayload.class, ArmorActionPayload::write, ArmorActionPayload::read,
                ArmorActionPayload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HoleBurstPayload.class, HoleBurstPayload::write, HoleBurstPayload::read,
                HoleBurstPayload::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
