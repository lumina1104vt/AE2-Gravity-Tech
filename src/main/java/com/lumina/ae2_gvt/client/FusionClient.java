package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import com.lumina.ae2_gvt.client.renderer.FusionPlasmaRenderer;
import com.lumina.ae2_gvt.client.screen.FusionControllerScreen;

import appeng.init.client.InitScreens;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraftforge.api.distmarker.Dist;

/** Hooks the fusion plasma's shaders, its renderer and the controller GUI up on the client. */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FusionClient {
    private FusionClient() {
    }





    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        InitScreens.register(Menus.FUSION_CONTROLLER.get(), FusionControllerScreen::new, "/screens/fusion_controller.json");
    });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntities.FUSION_CONTROLLER.get(), context -> new FusionPlasmaRenderer());
        // a hole where the world's drawing does not run (a guide's scene)
        event.registerBlockEntityRenderer(com.lumina.ae2_gvt.hole.GravityHole.BLOCK_ENTITY.get(),
                context -> new com.lumina.ae2_gvt.client.renderer.SimpleHoleRenderer());
    }
}
