package com.lumina.ae2_gvt.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.renderer.HexagramHubRenderer;
import com.lumina.ae2_gvt.hub.HubStructureBlocks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;

/** Hooks up the hexagram hub's sphere renderer and its cosmos shader on the client. */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HexagramHubClient extends ShardBase {
    static ShaderInstance cosmos;

    /** Shader {@code ae2_gvt:hub_cosmos}; back faces culled, so an inward-wound sphere shows only its far wall. */
    public static final RenderType COSMOS = RenderType.create("ae2_gvt_hub_cosmos", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 16, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmos))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));

    private HexagramHubClient() {
    }





    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(AE2GravityTech.MODID, "hub_cosmos"), DefaultVertexFormat.NEW_ENTITY), s -> cosmos = s);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HubStructureBlocks.CONTROLLER_ENTITY.get(), context -> new HexagramHubRenderer());
    }
}
