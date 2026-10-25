package com.lumina.ae2_gvt.prototype.twinstar.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * PROTOTYPE - custom pipeline for the twin holes: shaders {@code ae2_gvt:core/prototype_twin_hole}.
 * Uses the vanilla ENTITY vertex format with the pair-frame data packed into UV0 / UV1 (see the
 * .vsh), and the Globals block for time. Registered in {@link TwinStarPreviewClient}.
 */
final class TwinHoleRenderTypes {
    static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "pipeline/prototype_twin_hole"))
            .withVertexShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/prototype_twin_hole"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/prototype_twin_hole"))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .build();

    static final RenderType TWIN_HOLE = RenderType.create("ae2_gvt_prototype_twin_hole",
            RenderSetup.builder(PIPELINE).createRenderSetup());

    private TwinHoleRenderTypes() {
    }
}
