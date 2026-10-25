package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.hole.GravityHole;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Hooks the holes up on the client. The entity renderer draws nothing: the holes are drawn after
 * the world (see {@link HoleRenderer}) so their lensing can bend the finished scene. Their shaders
 * are in {@link ClientShaders}.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class HoleClient {
    private HoleClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GravityHole.BLACK_HOLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(GravityHole.WHITE_HOLE.get(), NoopRenderer::new);
    }
}
