package com.lumina.ae2_gvt.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * PROTOTYPE - render type for the twin holes: shader {@code ae2_gvt:prototype_twin_hole}
 * (registered in {@link TwinStarPreviewClient}). Uses the entity vertex format with the pair-frame
 * data packed into UV0 / UV1 (see the .vsh).
 */
public final class TwinHoleRenderTypes {
    public static final RenderType TWIN_HOLE = RenderType.create("ae2_gvt_prototype_twin_hole", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1 << 18, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> TwinStarPreviewClient.twinHole))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private TwinHoleRenderTypes() {
    }
}
