package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.ArmorEffects;
import com.lumina.ae2_gvt.payload.ArmorActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The antigravity armor's keys (portable workbench, dash) and the flight drift module, which only the player's own client can do. */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class ArmorClient {
    public static final KeyMapping WORKBENCH = new KeyMapping("key.ae2_gvt.armor_workbench", GLFW.GLFW_KEY_B, "key.categories.misc");
    public static final KeyMapping DASH = new KeyMapping("key.ae2_gvt.armor_dash", GLFW.GLFW_KEY_V, "key.categories.misc");

    private ArmorClient() {
    }







    /** Pick block with the armor's pick craft module: the server takes the item out of the network, vanilla does nothing in survival. */
    @SubscribeEvent
    public static void pick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isPickBlock())
            return;
        if (mc.player == null || mc.player.isCreative() || !ArmorEffects.has(mc.player, Items.PICK_CRAFT_MODULE))
            return;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            com.lumina.ae2_gvt.payload.ModNet.toServer(new ArmorActionPayload(ArmorActionPayload.PICK, hit.getBlockPos()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (WORKBENCH.consumeClick()) {
            if (player != null && mc.screen == null)
                com.lumina.ae2_gvt.payload.ModNet.toServer(new ArmorActionPayload(ArmorActionPayload.WORKBENCH));
        }
        while (DASH.consumeClick()) {
            if (player != null && mc.screen == null)
                com.lumina.ae2_gvt.payload.ModNet.toServer(new ArmorActionPayload(ArmorActionPayload.DASH));
        }
        // flight drift: flying with no key held, the player stops instead of gliding on
        if (player != null && player.getAbilities().flying && ArmorEffects.has(player, Items.FLIGHT_DRIFT_MODULE)) {
            var input = player.input;
            if (input.forwardImpulse == 0 && input.leftImpulse == 0 && !input.jumping && !input.shiftKeyDown)
                player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
        }
    }

    /** The events on the mod bus (1.20.1: a subscriber class takes one bus). */
    @Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(WORKBENCH);
            event.register(DASH);
        }
    
    }
}
