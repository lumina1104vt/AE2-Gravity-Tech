package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.model.ContainmentGlassModel;
import com.lumina.ae2_gvt.client.model.JoinedLookModel;
import com.lumina.ae2_gvt.client.model.StructureFrameModel;

import net.minecraftforge.api.distmarker.Dist;

/** The connected-texture models: geometry loaders named in the block model JSONs ("loader"). */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FrameClient {
    private FrameClient() {
    }



    @SubscribeEvent
    public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(StructureFrameModel.ID.getPath(), StructureFrameModel.LOADER);
        event.register(ContainmentGlassModel.ID.getPath(), ContainmentGlassModel.LOADER);
        event.register(JoinedLookModel.ID.getPath(), JoinedLookModel.LOADER);
    }
}
