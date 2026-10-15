/*
 * PROTOTYPE - Twin-Star Hub GUI, hosted by the prototype controller. Delete with the prototype package.
 */
package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.TwinStarHubScreen;
import com.lumina.ae2_gvt.prototype.twinstar.gui.TwinStarHub;

import appeng.client.InitScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class TwinStarHubClient {
    private TwinStarHubClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, TwinStarHub.MENU.get(), TwinStarHubScreen::new, "/screens/prototype_twin_star_hub.json");
    }
}
