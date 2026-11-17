package com.lumina.ae2_gvt.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * A copy of the world as drawn so far this frame, for the holes' gravitational lensing and the
 * graviton collector's space distortion to bend (a shader can't read the target it is drawing
 * into). The colour of the framebuffer being drawn into is copied into an off-screen target, resized
 * with it; {@link #TEXTURE} binds it as Sampler0 for a RenderType.
 * <p>
 * Whatever bends the copy draws through {@link #BUFFERS}, never the shared buffers of RenderBuffers:
 * Accelerated Rendering takes those over, and endBatch then draws nothing until its own flush, after the
 * next copy. The world's effect lands on the frame that follows, and is copied back into itself for good.
 * <p>
 * With Fabulous graphics the translucent layers live in separate targets until the end of the
 * frame, so they are not in the copy.
 */
public final class SceneSnapshot {
    private static TextureTarget target;
    private static boolean warned;

    /** For the effects: drawn at endBatch, whatever other mods do to the shared buffers. */
    public static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 18));

    /** Binds the snapshot as Sampler0. */
    public static final RenderStateShard.EmptyTextureStateShard TEXTURE = new RenderStateShard.EmptyTextureStateShard(
            () -> {
                RenderSystem.setShaderTexture(0, target == null ? 0 : target.getColorTextureId());
                RenderSystem.setShaderTexture(1, target == null ? 0 : target.getDepthTextureId());
            }, () -> {
            });

    private static TextureTarget half;
    /** The depth texture the half-size copy is used with: the one of the framebuffer drawn into (or a copy of it). */
    private static int halfDepth, halfColor;

    /**
     * Sampler0 is the half-size copy ({@link #captureHalf}), Sampler1 the depth of the frame itself, so a shader gets the full size
     * of the frame from {@code textureSize(Sampler1, 0)}.
     */
    public static final RenderStateShard.EmptyTextureStateShard TEXTURE_HALF = new RenderStateShard.EmptyTextureStateShard(
            () -> {
                RenderSystem.setShaderTexture(0, halfColor);
                RenderSystem.setShaderTexture(1, halfDepth);
            }, () -> {
            });

    private SceneSnapshot() {
    }

    /**
     * A copy of the colour at half the width and height (a quarter of the pixels to move; a lens bends and blurs it anyway) for
     * {@link #TEXTURE_HALF}. The depth is not copied at all: it is read from the framebuffer itself, which a pass that does not write
     * depth may do. Only when that framebuffer's depth is not a texture (another mod's) is the full copy with depth made instead.
     */
    public static void captureHalf(RenderTarget main) {
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int w = viewport[2] > 0 ? viewport[2] : main.width, h = viewport[3] > 0 ? viewport[3] : main.height;
        int hw = Math.max(1, (w + 1) / 2), hh = Math.max(1, (h + 1) / 2);
        int depth = depthTextureOf(bound);
        if (depth == 0) {
            capture(main, true);
            halfDepth = target.getDepthTextureId();
            halfColor = target.getColorTextureId();
            return;
        }
        if (half == null || half.width != hw || half.height != hh) {
            if (half != null) {
                half.destroyBuffers();
            }
            half = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
            half.setFilterMode(GL11.GL_LINEAR);
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // not ours
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, bound);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, half.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, hw, hh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
        halfDepth = depth;
        halfColor = half.getColorTextureId();
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR && !warned) {
            warned = true;
            AE2GravityTech.LOGGER.warn("Half scene snapshot failed: GL error {}, framebuffer {} (main {}), {} x {}", error, bound, main.frameBufferId, w, h);
        }
    }

    /** The depth texture attached to the framebuffer, 0 if there is none or it is not a texture. */
    private static int depthTextureOf(int framebuffer) {
        if (framebuffer == 0) {
            return 0;
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // not ours
        }
        int type = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        int name = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        return GL11.glGetError() == GL11.GL_NO_ERROR && type == GL11.GL_TEXTURE ? name : 0;
    }

    /** Copies the colour of the framebuffer being drawn into, as big as the viewport, into the snapshot. */
    public static void capture(RenderTarget main) {
        capture(main, false);
    }

    /**
     * As {@link #capture(RenderTarget)}; with {@code depth} the depth buffer is copied too and bound as Sampler1, so a lens can tell
     * what is nearer than itself (and must not be dragged into it).
     */
    public static void capture(RenderTarget main, boolean depth) {
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int w = viewport[2] > 0 ? viewport[2] : main.width, h = viewport[3] > 0 ? viewport[3] : main.height;
        if (target == null || target.width != w || target.height != h) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // not ours
        }
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
        GlStateManager._bindTexture(target.getColorTextureId());
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        if (depth) {
            GlStateManager._bindTexture(target.getDepthTextureId());
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        }
        GlStateManager._bindTexture(texture);
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR && !warned) {
            warned = true;
            AE2GravityTech.LOGGER.warn("Scene snapshot failed: GL error {}, framebuffer {} (main {}), {} x {}", error, bound, main.frameBufferId, w, h);
        }
    }
}
