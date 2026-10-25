package com.lumina.twinstar;

import appeng.api.networking.GridServices;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.order.PlanViewPacket;
import com.lumina.twinstar.service.TwinStarService;
import com.lumina.twinstar.storage.TwinstarContent;
import com.lumina.twinstar.tools.EngineCommands;
import com.lumina.twinstar.tools.NetworkDump;
import com.lumina.twinstar.tools.PatternGen;
import com.lumina.twinstar.tools.RecipeDump;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Twinstar.MODID)
public class Twinstar {
    public static final String MODID = "twinstar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Twinstar(IEventBus modEventBus) {
        GridServices.register(ITwinStarService.class, TwinStarService.class);
        modEventBus.addListener(Twinstar::registerPayloads);
        TwinstarContent.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RecipeDump::registerCommands);
        NeoForge.EVENT_BUS.addListener(PatternGen::registerCommands);
        NeoForge.EVENT_BUS.addListener(EngineCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(NetworkDump::registerCommands);
        NeoForge.EVENT_BUS.addListener(PatternGen::onServerTick);
        NeoForge.EVENT_BUS.addListener(PatternGen::onServerStopping);
        NeoForge.EVENT_BUS.addListener(RecipeDump::onServerStarted);
        NeoForge.EVENT_BUS.addListener(RecipeDump::onServerTick);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(PlanViewPacket.TYPE, PlanViewPacket.STREAM_CODEC, PlanViewPacket::handle);
    }
}
