package com.lumina.ae2_gvt.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for the fusion plasma, shader {@code ae2_gvt:fusion_plasma} ({@link ClientShaders}):
 * the star surface (opaque), its corona and the containment shell (translucent, depth-tested but
 * not written) and the prominences / eruption jets (added light).
 */
public final class FusionPlasmaRenderTypes {
    public static final RenderType STAR = type("ae2_gvt_fusion_plasma", RenderStateShard.NO_TRANSPARENCY,
            RenderStateShard.COLOR_DEPTH_WRITE, RenderStateShard.CULL);
    public static final RenderType CORONA = type("ae2_gvt_fusion_corona", RenderStateShard.TRANSLUCENT_TRANSPARENCY,
            RenderStateShard.COLOR_WRITE, RenderStateShard.CULL);
    public static final RenderType SHELL = type("ae2_gvt_fusion_shell", RenderStateShard.TRANSLUCENT_TRANSPARENCY,
            RenderStateShard.COLOR_WRITE, RenderStateShard.NO_CULL);
    public static final RenderType FLARE = type("ae2_gvt_fusion_flare", RenderStateShard.LIGHTNING_TRANSPARENCY,
            RenderStateShard.COLOR_WRITE, RenderStateShard.NO_CULL);

    private FusionPlasmaRenderTypes() {
    }

    private static RenderType type(String name, RenderStateShard.TransparencyStateShard transparency,
                                   RenderStateShard.WriteMaskStateShard write, RenderStateShard.CullStateShard cull) {
        return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1 << 16, false,
                transparency != RenderStateShard.NO_TRANSPARENCY, RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.fusionPlasma))
                        .setTransparencyState(transparency)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(write)
                        .setCullState(cull)
                        .createCompositeState(false));
    }
}
