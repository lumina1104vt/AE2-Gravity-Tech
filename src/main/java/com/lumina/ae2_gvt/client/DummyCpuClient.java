package com.lumina.ae2_gvt.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.DummyCpuScreen;
import com.lumina.ae2_gvt.menus.Menus;

import appeng.init.client.InitScreens;
import net.minecraftforge.api.distmarker.Dist;

/** The twin-star hosts' GUIs: the test hub's and the hexagram hub controller's (and the machine access interface's). */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DummyCpuClient {
    private DummyCpuClient() {
    }



    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        InitScreens.register(Menus.DUMMY_CPU.get(), DummyCpuScreen::new, "/screens/dummy_twinstar_cpu.json");
        InitScreens.register(Menus.TWIN_STAR_HUB.get(), com.lumina.ae2_gvt.client.screen.HubControllerScreen::new,
                "/screens/twin_star_hub.json");
        InitScreens.register(Menus.MACHINE_ACCESS.get(), com.lumina.ae2_gvt.client.screen.MachineAccessScreen::new,
                "/screens/machine_access_interface.json");
        InitScreens.register(Menus.QUANTUM_CONTROLLER.get(), com.lumina.ae2_gvt.client.screen.QuantumControllerScreen::new,
                "/screens/quantum_controller.json");
    });
    }
}
