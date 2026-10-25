package com.lumina.ae2_gvt.client;

import java.io.IOException;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/**
 * The mod's core shaders (assets/ae2_gvt/shaders/core/*.json + .vsh/.fsh), all on the entity vertex
 * format. Loaded with the resources; the render types read them through suppliers, so a reload
 * swaps them in place.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class ClientShaders {
    static ShaderInstance holeLens, holeBody, fusionPlasma, collectorLens;

    private ClientShaders() {
    }

    @SubscribeEvent
    static void register(RegisterShadersEvent event) throws IOException {
        var provider = event.getResourceProvider();
        event.registerShader(new ShaderInstance(provider, id("hole_lens"), DefaultVertexFormat.NEW_ENTITY), s -> holeLens = s);
        event.registerShader(new ShaderInstance(provider, id("hole_body"), DefaultVertexFormat.NEW_ENTITY), s -> holeBody = s);
        event.registerShader(new ShaderInstance(provider, id("fusion_plasma"), DefaultVertexFormat.NEW_ENTITY), s -> fusionPlasma = s);
        event.registerShader(new ShaderInstance(provider, id("collector_lens"), DefaultVertexFormat.NEW_ENTITY), s -> collectorLens = s);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, name);
    }
}
