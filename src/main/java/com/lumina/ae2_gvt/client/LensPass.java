package com.lumina.ae2_gvt.client;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * Bending the frame without copying it. The frame being drawn is already a texture, so a shader can read it where it stands; it only
 * cannot write into it at the same time. So the bent picture is drawn into a second target (shaders read the frame and its depth through
 * {@link #INPUT}) and then laid over the frame. Both steps cover only a rectangle of the screen - the part the effect reaches - so what
 * it costs follows how much of the screen it covers, not the size of the screen. If the framebuffer drawn into has no colour or depth
 * texture (another mod's), {@link #begin} says no and the effect is not drawn.
 * <p>
 * Between {@link #begin} and {@link #end} draw with a RenderType using {@link #INPUT}, no depth test (the target has none: the shader
 * leaves what is nearer than the effect alone, by the frame's depth) and an opaque result (alpha 1 where it drew, 0 elsewhere).
 */
public final class LensPass extends ShardBase {
    private static TextureTarget target;
    private static int mainColor, mainDepth, bound;
    private static final int[] VIEWPORT = new int[4];
    private static int x0, y0, x1, y1;
    private static boolean active;

    /** Sampler0 is the colour of the frame, Sampler1 its depth. */
    public static final RenderStateShard.EmptyTextureStateShard INPUT = new RenderStateShard.EmptyTextureStateShard(
            () -> {
                RenderSystem.setShaderTexture(0, mainColor);
                RenderSystem.setShaderTexture(1, mainDepth);
            }, () -> {
            });

    private LensPass() {
    }

    /** The size of the viewport now (x = width, y = height). */
    static int[] viewport() {
        int[] v = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, v);
        return new int[] {v[2], v[3]};
    }

    /** Starts a pass over the rectangle (pixels); false if it cannot be done (draw nothing then, and do not call {@link #end}). */
    static boolean begin(RenderTarget main, int[] rect) {
        if (active || rect == null) {
            return false;
        }
        bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (bound == 0) {
            return false;
        }
        mainColor = attachment(GL30.GL_COLOR_ATTACHMENT0);
        mainDepth = attachment(GL30.GL_DEPTH_ATTACHMENT);
        if (mainColor == 0 || mainDepth == 0) {
            return false;
        }
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);
        int w = VIEWPORT[2], h = VIEWPORT[3];
        if (w <= 0 || h <= 0) {
            return false;
        }
        if (target == null || target.width != w || target.height != h) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        }
        x0 = rect[0];
        y0 = rect[1];
        x1 = rect[2];
        y1 = rect[3];
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
        RenderSystem.viewport(VIEWPORT[0], VIEWPORT[1], w, h);
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(x0, y0, x1 - x0, y1 - y0);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        active = true;
        return true;
    }

    /** Ends the pass: back to the frame, with what was drawn laid over it. */
    static void end() {
        if (!active) {
            return;
        }
        active = false;
        GlStateManager._disableScissorTest();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
        RenderSystem.viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);

        float w = VIEWPORT[2], h = VIEWPORT[3];
        PoseStack view = RenderSystem.getModelViewStack();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        view.pushPose();
        view.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        float nx0 = x0 / w * 2 - 1, nx1 = x1 / w * 2 - 1, ny0 = y0 / h * 2 - 1, ny1 = y1 / h * 2 - 1;
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(nx0, ny0, 0f).uv(x0 / w, y0 / h).endVertex();
        builder.vertex(nx1, ny0, 0f).uv(x1 / w, y0 / h).endVertex();
        builder.vertex(nx1, ny1, 0f).uv(x1 / w, y1 / h).endVertex();
        builder.vertex(nx0, ny1, 0f).uv(x0 / w, y1 / h).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        view.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, sorting);
    }

    /** The texture attached to the framebuffer drawn into, 0 if there is none or it is not a texture. */
    private static int attachment(int point) {
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // not ours
        }
        int type = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER, point, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        int name = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER, point, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        return GL11.glGetError() == GL11.GL_NO_ERROR && type == GL11.GL_TEXTURE ? name : 0;
    }
}
