package com.lumina.ae2_gvt.prototype.twinstar.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.twinstar.TwinStarPreview;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** PROTOTYPE - hooks the Twin-Star Hub preview renderer up on the client. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class TwinStarPreviewClient {
    private TwinStarPreviewClient() {
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(TwinHoleRenderTypes.PIPELINE);   // twin-hole shader
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TwinStarPreview.BLOCK_ENTITY.get(), context -> new TwinStarPreviewRenderer());
    }
}
