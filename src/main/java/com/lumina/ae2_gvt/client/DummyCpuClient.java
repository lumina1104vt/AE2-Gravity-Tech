package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.DummyCpuScreen;
import com.lumina.ae2_gvt.menus.Menus;

import appeng.init.client.InitScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The twin-star hosts' GUIs: the test hub's and the hexagram hub controller's (and the machine access interface's). */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class DummyCpuClient {
    private DummyCpuClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, Menus.DUMMY_CPU.get(), DummyCpuScreen::new, "/screens/dummy_twinstar_cpu.json");
        InitScreens.register(event, Menus.TWIN_STAR_HUB.get(), com.lumina.ae2_gvt.client.screen.HubControllerScreen::new,
                "/screens/twin_star_hub.json");
        InitScreens.register(event, Menus.MACHINE_ACCESS.get(), com.lumina.ae2_gvt.client.screen.MachineAccessScreen::new,
                "/screens/machine_access_interface.json");
    }
}
