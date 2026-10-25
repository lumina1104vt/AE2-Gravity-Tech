package com.lumina.twinstar.client;

import appeng.init.client.InitScreens;
import com.lumina.twinstar.Twinstar;
import com.lumina.twinstar.storage.TwinstarContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The lib's screens. */
@EventBusSubscriber(modid = Twinstar.MODID, value = Dist.CLIENT)
public final class TwinstarClient {
    private TwinstarClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, TwinstarContent.ORDER_MENU.get(), OrderScreen::new, "/screens/twinstar_order.json");
        InitScreens.register(event, TwinstarContent.PLAN_MENU.get(), PlanScreen::new, "/screens/twinstar_plan.json");
    }
}
