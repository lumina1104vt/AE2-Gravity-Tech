package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.QuantumControllerScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Development only ({@code -Dae2_gvt.devQuantum=true}): types a frequency into the quantum controller's screen once it is open. */
public final class DevChecksClient {
    private static int open;

    private DevChecksClient() {
    }

    public static void init() {
        if (!Boolean.getBoolean("ae2_gvt.devQuantum"))
            return;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END)
                return;
            if (Minecraft.getInstance().screen instanceof QuantumControllerScreen screen) {
                open++;
                if (open == 80 || open == 160)
                    screen.getMenu().cycleRole();     // off -> master -> slave
                if (open > 20 && open % 5 == 0 && open < 60) {
                    screen.devType((char) ('a' + (open / 5) % 20));
                    AE2GravityTech.LOGGER.info("dev quantum: typed a character");
                }
            }
        });
    }
}
