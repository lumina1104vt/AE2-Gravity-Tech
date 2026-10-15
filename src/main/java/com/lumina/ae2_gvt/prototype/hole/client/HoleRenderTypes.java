package com.lumina.ae2_gvt.prototype.hole.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Pipelines for the holes (registered in {@link HoleClient}):
 * <ul>
 *   <li>LENS - a camera-facing panel round the hole that redraws the world behind it bent
 *       (samples {@link SceneSnapshot}); depth-tested, so things in front of the hole stay put</li>
 *   <li>BODY - the hole itself: starfield sphere (inverted for the white hole)</li>
 *   <li>DISC - the accretion disc, translucent</li>
 * </ul>
 */
final class HoleRenderTypes {
    private static RenderPipeline.Builder base(String name, String shader) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "pipeline/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/" + shader))
                .withFragmentShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/" + shader))
                .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS);
    }

    static final RenderPipeline LENS_PIPELINE = base("hole_lens", "hole_lens")
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    static final RenderPipeline BODY_PIPELINE = base("hole_body", "hole_body")
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(true)
            .build();
    static final RenderPipeline DISC_PIPELINE = base("hole_disc", "hole_body")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();

    static final RenderType LENS = RenderType.create("ae2_gvt_hole_lens", RenderSetup.builder(LENS_PIPELINE)
            .withTexture("Sampler0", SceneSnapshot.ID, () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .createRenderSetup());
    static final RenderType BODY = RenderType.create("ae2_gvt_hole_body", RenderSetup.builder(BODY_PIPELINE).createRenderSetup());
    static final RenderType DISC = RenderType.create("ae2_gvt_hole_disc", RenderSetup.builder(DISC_PIPELINE).createRenderSetup());

    private HoleRenderTypes() {
    }
}
