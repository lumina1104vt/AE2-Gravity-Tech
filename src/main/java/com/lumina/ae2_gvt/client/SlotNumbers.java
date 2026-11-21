package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Ctrl+G (in any GUI; the key can be changed in the controls): every GUI shows, on each of its slots, the number a pattern's "GUI" setting uses for it (counted
 * from 1 over the slots that are not the player's inventory, in the menu's order). For finding out which slot of a
 * machine's GUI a material goes into.
 */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class SlotNumbers {
    private static boolean shown;

    private SlotNumbers() {
    }

    private static final KeyMapping TOGGLE = new KeyMapping("key.ae2_gvt.slot_numbers", KeyConflictContext.GUI, KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.ae2_gvt");







    @SubscribeEvent
    public static void pressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>
                && TOGGLE.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            shown = !shown;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void draw(ContainerScreenEvent.Render.Foreground event) {
        if (!shown || Minecraft.getInstance().player == null)
            return;
        GuiGraphics g = event.getGuiGraphics();
        var player = Minecraft.getInstance().player.getInventory();
        var font = Minecraft.getInstance().font;
        int n = 0;
        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            if (slot.container == player)
                continue;
            n++;
            String text = String.valueOf(n);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(slot.x + 1, slot.y + 1, 400);
            pose.scale(0.6f, 0.6f, 1);
            g.drawString(font, text, 0, 0, 0xFFFF55, true);
            pose.popPose();
        }
    }

    /** The events on the mod bus (1.20.1: a subscriber class takes one bus). */
    @Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
        }
    
    }
}
