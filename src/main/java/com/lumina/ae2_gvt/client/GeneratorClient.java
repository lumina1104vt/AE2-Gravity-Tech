package com.lumina.ae2_gvt.client;

import appeng.init.client.InitScreens;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.GeneratorScreen;
import com.lumina.ae2_gvt.menus.Menus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The generators' screens: the crystal one has a dust slot, the others only show. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class GeneratorClient {
    private GeneratorClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, Menus.CRYSTAL_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_crystal.json");
        InitScreens.register(event, Menus.GRAVITON_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_plain.json");
        InitScreens.register(event, Menus.ACCRETION_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_plain.json");
    }
}
