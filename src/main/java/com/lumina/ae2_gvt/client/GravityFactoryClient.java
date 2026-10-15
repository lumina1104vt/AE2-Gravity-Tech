package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.GravityFactoryScreen;
import com.lumina.ae2_gvt.prototype.factory.GravityFactory;

import appeng.client.InitScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class GravityFactoryClient {
    private GravityFactoryClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, GravityFactory.MENU.get(), GravityFactoryScreen::new, "/screens/gravity_processing_factory.json");
    }
}
