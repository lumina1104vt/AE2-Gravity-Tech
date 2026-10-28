package com.lumina.lmb;

import com.lumina.lmb.client.ClientStructures;
import com.lumina.lmb.client.FormedModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Client side: formed structures change how their blocks are drawn. */
@Mod(value = LuminaMultiBlock.MODID, dist = Dist.CLIENT)
public class LuminaMultiBlockClient {
    public LuminaMultiBlockClient(IEventBus modEventBus) {
        modEventBus.addListener(FormedModel::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener(ClientStructures::onLoggingOut);
    }
}
