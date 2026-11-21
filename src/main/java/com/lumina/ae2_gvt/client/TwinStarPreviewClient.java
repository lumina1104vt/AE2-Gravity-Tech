package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.renderer.TwinStarPreviewRenderer;
import com.lumina.ae2_gvt.preview.TwinStarPreview;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;

/** PROTOTYPE - hooks the Twin-Star Hub preview renderer and its twin-hole shader up on the client. */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TwinStarPreviewClient {
    static ShaderInstance twinHole;

    private TwinStarPreviewClient() {
    }





    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(AE2GravityTech.MODID, "prototype_twin_hole"), DefaultVertexFormat.NEW_ENTITY),
                s -> twinHole = s);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TwinStarPreview.BLOCK_ENTITY.get(), context -> new TwinStarPreviewRenderer());
    }
}
