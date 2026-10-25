package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import com.lumina.ae2_gvt.client.renderer.FusionPlasmaRenderer;
import com.lumina.ae2_gvt.client.screen.FusionControllerScreen;

import appeng.init.client.InitScreens;
import com.lumina.ae2_gvt.menus.Menus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Hooks the fusion plasma's shaders, its renderer and the controller GUI up on the client. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class FusionClient {
    private FusionClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, Menus.FUSION_CONTROLLER.get(), FusionControllerScreen::new, "/screens/fusion_controller.json");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntities.FUSION_CONTROLLER.get(), context -> new FusionPlasmaRenderer());
    }
}
