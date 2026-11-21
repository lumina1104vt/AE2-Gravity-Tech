package com.lumina.ae2_gvt.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import appeng.init.client.InitScreens;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.GeneratorScreen;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraftforge.api.distmarker.Dist;

/** The generators' screens: the crystal one has a dust slot, the others only show. */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GeneratorClient {
    private GeneratorClient() {
    }



    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        InitScreens.register(Menus.CRYSTAL_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_crystal.json");
        InitScreens.register(Menus.GRAVITON_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_plain.json");
        InitScreens.register(Menus.ACCRETION_GENERATOR.get(), GeneratorScreen::new, "/screens/generator_plain.json");
    });
    }
}
