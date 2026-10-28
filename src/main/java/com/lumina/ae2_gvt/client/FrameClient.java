package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.model.ContainmentGlassModel;
import com.lumina.ae2_gvt.client.model.JoinedLookModel;
import com.lumina.ae2_gvt.client.model.StructureFrameModel;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** The connected-texture models: geometry loaders named in the block model JSONs ("loader"). */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class FrameClient {
    private FrameClient() {
    }

    @SubscribeEvent
    static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(StructureFrameModel.ID, StructureFrameModel.LOADER);
        event.register(ContainmentGlassModel.ID, ContainmentGlassModel.LOADER);
        event.register(JoinedLookModel.ID, JoinedLookModel.LOADER);
    }
}
