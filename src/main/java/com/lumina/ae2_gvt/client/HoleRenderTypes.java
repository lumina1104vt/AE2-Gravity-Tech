package com.lumina.ae2_gvt.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for the holes (shaders in {@link ClientShaders}):
 * <ul>
 *   <li>LENS - a camera-facing panel round the hole that redraws the world behind it bent
 *       (samples {@link SceneSnapshot}); depth-tested, so things in front of the hole stay put</li>
 *   <li>BODY - the hole itself: starfield sphere (inverted for the white hole)</li>
 *   <li>DISC - the accretion disc, translucent</li>
 * </ul>
 */
public final class HoleRenderTypes {
    static final RenderType LENS = RenderType.create("ae2_gvt_hole_lens", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeLens))
                    .setTextureState(LensPass.INPUT)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** The lowest detail: the same panel, but no copy of the frame and no bending - only the rings, laid over the scene. */
    static final RenderType LENS_FLAT = RenderType.create("ae2_gvt_hole_lens_flat", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeLens))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    public static final RenderType BODY = RenderType.create("ae2_gvt_hole_body", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeBody))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));
    static final RenderType DISC = RenderType.create("ae2_gvt_hole_disc", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 2048,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeBody))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private HoleRenderTypes() {
    }
}
