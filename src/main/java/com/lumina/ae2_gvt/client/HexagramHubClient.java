package com.lumina.ae2_gvt.client;

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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Hooks up the Twin-Star Hub's effects renderer and shader on the client. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class HexagramHubClient {
    static ShaderInstance effects;

    private static final RenderStateShard.ShaderStateShard SHADER = new RenderStateShard.ShaderStateShard(() -> effects);

    /** The opaque parts: the two holes. */
    public static final RenderType SOLID = RenderType.create("ae2_gvt_voxel_core_solid", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 20, false, false, RenderType.CompositeState.builder()
                    .setShaderState(SHADER)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** The light: added onto what is behind, hidden by what is in front, writes no depth. */
    public static final RenderType GLOW = RenderType.create("ae2_gvt_voxel_core_glow", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 22, false, false, RenderType.CompositeState.builder()
                    .setShaderState(SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private HexagramHubClient() {
    }

    @SubscribeEvent
    static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "voxel_core_fx"), DefaultVertexFormat.NEW_ENTITY),
                s -> effects = s);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HubStructureBlocks.CONTROLLER_ENTITY.get(), context -> new HexagramHubRenderer());
    }
}
