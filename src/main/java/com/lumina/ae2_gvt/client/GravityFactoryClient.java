package com.lumina.ae2_gvt.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.GravityFactoryScreen;

import appeng.init.client.InitScreens;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GravityFactoryClient {
    private GravityFactoryClient() {
    }



    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        InitScreens.register(Menus.GRAVITY_FACTORY.get(), GravityFactoryScreen::new, "/screens/gravity_processing_factory.json");
    });
    }
}
