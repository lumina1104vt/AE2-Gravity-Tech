package com.lumina.ae2_gvt.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * A copy of the world as drawn so far this frame, for the holes' gravitational lensing and the
 * graviton collector's space distortion to bend (a shader can't read the target it is drawing
 * into). The main target's colour is blitted into an off-screen target, resized with the window;
 * {@link #TEXTURE} binds it as Sampler0 for a RenderType.
 * <p>
 * With Fabulous graphics the translucent layers live in separate targets until the end of the
 * frame, so they are not in the copy.
 */
public final class SceneSnapshot {
    private static TextureTarget target;

    /** Binds the snapshot as Sampler0. */
    public static final RenderStateShard.EmptyTextureStateShard TEXTURE = new RenderStateShard.EmptyTextureStateShard(
            () -> RenderSystem.setShaderTexture(0, target == null ? 0 : target.getColorTextureId()), () -> {
            });

    private SceneSnapshot() {
    }

    /** Copies the main target's colour into the snapshot, then rebinds whatever was being drawn into. */
    public static void capture(RenderTarget main) {
        int w = main.width, h = main.height;
        if (target == null || target.width != w || target.height != h) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
    }
}
