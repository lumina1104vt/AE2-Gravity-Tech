package com.lumina.ae2_gvt.prototype.hole.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.hole.GravityHole;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Hooks the holes up on the client. The entity renderer draws nothing: the holes are drawn after
 * the world (see {@link HoleRenderer}) so their lensing can bend the finished scene.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class HoleClient {
    private HoleClient() {
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(HoleRenderTypes.LENS_PIPELINE);
        event.registerPipeline(HoleRenderTypes.BODY_PIPELINE);
        event.registerPipeline(HoleRenderTypes.DISC_PIPELINE);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GravityHole.BLACK_HOLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(GravityHole.WHITE_HOLE.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    static void afterWorld(RenderLevelStageEvent.AfterTranslucentParticles event) {
        HoleRenderer.render();
    }
}
