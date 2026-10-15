package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.ArmorModuleScreen;
import com.lumina.ae2_gvt.client.screen.TwinStarPatternTermScreen;
import com.lumina.ae2_gvt.menus.Menus;
import com.lumina.ae2_gvt.payload.OpenArmorModulesPayload;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarTerminal;

import appeng.client.InitScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class TwinStarTerminalClient {
    private TwinStarTerminalClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, TwinStarTerminal.MENU.get(), TwinStarPatternTermScreen::new,
                "/screens/terminals/twin_star_pattern_terminal.json");
    }

    /** Encoded patterns made in the terminal say which machine they are for. */
    @SubscribeEvent
    static void patternTooltip(ItemTooltipEvent event) {
        Holder<Item> machine = event.getItemStack().get(TwinStarTerminal.PATTERN_MACHINE.get());
        if (machine != null) {
            event.getToolTip().add(Component.translatable("ae2_gvt.pattern.machine", machine.value().getName(new ItemStack(machine)))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** The armor module screen and the key that opens it (K by default) while a piece is worn. */
    @EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
    public static final class ArmorModuleClient {
        public static final KeyMapping OPEN = new KeyMapping("key.ae2_gvt.armor_modules", GLFW.GLFW_KEY_K, KeyMapping.Category.MISC);

        private ArmorModuleClient() {
        }

        @SubscribeEvent
        static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }

        @SubscribeEvent
        static void registerScreens(RegisterMenuScreensEvent event) {
            InitScreens.register(event, Menus.ARMOR_MODULE_MENU.get(), ArmorModuleScreen::new, "/screens/armor_modules.json");
        }

        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            while (OPEN.consumeClick()) {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                    ClientPacketDistributor.sendToServer(OpenArmorModulesPayload.INSTANCE);
                }
            }
        }
    }
}
