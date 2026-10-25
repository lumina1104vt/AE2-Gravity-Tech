package com.lumina.ae2_gvt.client;

import java.io.IOException;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.renderer.TwinStarPreviewRenderer;
import com.lumina.ae2_gvt.prototype.twinstar.TwinStarPreview;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** PROTOTYPE - hooks the Twin-Star Hub preview renderer and its twin-hole shader up on the client. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class TwinStarPreviewClient {
    static ShaderInstance twinHole;

    private TwinStarPreviewClient() {
    }

    @SubscribeEvent
    static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "prototype_twin_hole"), DefaultVertexFormat.NEW_ENTITY),
                s -> twinHole = s);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TwinStarPreview.BLOCK_ENTITY.get(), context -> new TwinStarPreviewRenderer());
    }
}
