package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.ArmorEffects;
import com.lumina.ae2_gvt.payload.ArmorActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The antigravity armor's keys (portable workbench, dash) and the flight drift module, which only the player's own client can do. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class ArmorClient {
    public static final KeyMapping WORKBENCH = new KeyMapping("key.ae2_gvt.armor_workbench", GLFW.GLFW_KEY_B, "key.categories.misc");
    public static final KeyMapping DASH = new KeyMapping("key.ae2_gvt.armor_dash", GLFW.GLFW_KEY_V, "key.categories.misc");

    private ArmorClient() {
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WORKBENCH);
        event.register(DASH);
    }

    /** Pick block with the armor's pick craft module: the server takes the item out of the network, vanilla does nothing in survival. */
    @SubscribeEvent
    static void pick(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isPickBlock())
            return;
        if (mc.player == null || mc.player.isCreative() || !ArmorEffects.has(mc.player, Items.PICK_CRAFT_MODULE))
            return;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            PacketDistributor.sendToServer(new ArmorActionPayload(ArmorActionPayload.PICK, hit.getBlockPos()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (WORKBENCH.consumeClick()) {
            if (player != null && mc.screen == null)
                PacketDistributor.sendToServer(new ArmorActionPayload(ArmorActionPayload.WORKBENCH));
        }
        while (DASH.consumeClick()) {
            if (player != null && mc.screen == null)
                PacketDistributor.sendToServer(new ArmorActionPayload(ArmorActionPayload.DASH));
        }
        // flight drift: flying with no key held, the player stops instead of gliding on
        if (player != null && player.getAbilities().flying && ArmorEffects.has(player, Items.FLIGHT_DRIFT_MODULE)) {
            var input = player.input;
            if (input.forwardImpulse == 0 && input.leftImpulse == 0 && !input.jumping && !input.shiftKeyDown)
                player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
        }
    }
}
