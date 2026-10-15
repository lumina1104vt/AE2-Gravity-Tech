package com.lumina.ae2_gvt.prototype.fusion.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.fusion.FusionGenerator;

import appeng.client.InitScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Hooks the fusion plasma's shaders, its renderer and the controller GUI up on the client. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class FusionClient {
    private FusionClient() {
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FusionPlasmaRenderTypes.STAR_PIPELINE);
        event.registerPipeline(FusionPlasmaRenderTypes.CORONA_PIPELINE);
        event.registerPipeline(FusionPlasmaRenderTypes.SHELL_PIPELINE);
        event.registerPipeline(FusionPlasmaRenderTypes.FLARE_PIPELINE);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, FusionGenerator.CONTROLLER_MENU.get(), FusionControllerScreen::new, "/screens/fusion_controller.json");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FusionGenerator.CONTROLLER_ENTITY.get(), context -> new FusionPlasmaRenderer());
    }
}
