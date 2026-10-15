package com.lumina.ae2_gvt.prototype.fusion.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Pipelines for the fusion plasma, shaders {@code ae2_gvt:core/fusion_plasma}: the star surface
 * (opaque), its corona and the containment shell (translucent, depth-tested but not written) and
 * the prominences / eruption jets (added light). Registered in {@link FusionClient}.
 */
final class FusionPlasmaRenderTypes {
    private static RenderPipeline.Builder base(String name) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "pipeline/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/fusion_plasma"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/fusion_plasma"))
                .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS);
    }

    static final RenderPipeline STAR_PIPELINE = base("fusion_plasma")
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(true)
            .build();
    static final RenderPipeline CORONA_PIPELINE = base("fusion_corona")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(true)
            .build();

    static final RenderPipeline SHELL_PIPELINE = base("fusion_shell")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();
    static final RenderPipeline FLARE_PIPELINE = base("fusion_flare")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .build();

    static final RenderType STAR = RenderType.create("ae2_gvt_fusion_plasma", RenderSetup.builder(STAR_PIPELINE).createRenderSetup());
    static final RenderType CORONA = RenderType.create("ae2_gvt_fusion_corona", RenderSetup.builder(CORONA_PIPELINE).createRenderSetup());
    static final RenderType SHELL = RenderType.create("ae2_gvt_fusion_shell", RenderSetup.builder(SHELL_PIPELINE).createRenderSetup());
    static final RenderType FLARE = RenderType.create("ae2_gvt_fusion_flare", RenderSetup.builder(FLARE_PIPELINE).createRenderSetup());

    private FusionPlasmaRenderTypes() {
    }
}
