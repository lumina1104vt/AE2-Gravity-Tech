package com.lumina.ae2_gvt.prototype.frame.client;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class FrameClient {
    private FrameClient() {
    }

    @SubscribeEvent
    static void registerModels(RegisterBlockStateModels event) {
        event.registerModel(StructureFrameModel.ID, StructureFrameModel.Unbaked.MAP_CODEC);
        event.registerModel(com.lumina.ae2_gvt.prototype.fusion.client.ContainmentGlassModel.ID,
                com.lumina.ae2_gvt.prototype.fusion.client.ContainmentGlassModel.Unbaked.MAP_CODEC);
    }
}
