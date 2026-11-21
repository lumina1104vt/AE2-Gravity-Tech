package com.lumina.ae2_gvt.client;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Draws in the world with the matrices the world itself was drawn with. Everything the mod puts in the frame after the world (holes,
 * ripples, the collector's and the rift's distortion, the link tool) is given in blocks from the camera, so it is placed right only if
 * the model-view matrix is the camera's rotation and the projection is the world's. Vanilla leaves both in RenderSystem at that point,
 * but a mod that changes them for its own pass and does not put them back (or draws in between) leaves ours drawn somewhere else - a hole
 * turned up away from its block. So both are set from the event, which carries what the level was drawn with, for the draw, and put
 * back after it.
 * <p>
 * The same goes for the viewport and the scissor box: a mod that narrows them for its own pass and does not restore them (Accelerated
 * Rendering is set not to, by its config) squeezes everything drawn after into a corner of the screen - the hole and its ripples in the
 * bottom left. They are set to the whole frame for the draw and put back after it.
 */
final class LevelPass {
    /** The matrices of the pass being drawn. */
    private static final Matrix4f VIEW = new Matrix4f(), PROJECTION = new Matrix4f();

    private LevelPass() {
    }

    /**
     * The part of the screen (pixels: x0, y0, x1, y1, within the viewport) a camera-facing square of the given half-size round
     * {@code centre} (blocks from the camera) covers, a little wider; the whole viewport when it reaches behind the camera, null when it
     * is off the screen altogether.
     */
    static int[] screenRect(Vector3f centre, Vector3f right, Vector3f up, float half, int width, int height) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int behind = 0;
        for (int[] k : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            Vector3f p = new Vector3f(centre).add(new Vector3f(right).mul(k[0] * half)).add(new Vector3f(up).mul(k[1] * half));
            Vector4f clip = PROJECTION.transform(VIEW.transform(new Vector4f(p, 1f)));
            if (clip.w <= 0.05f) {
                behind++;
                continue;
            }
            float x = (clip.x / clip.w * 0.5f + 0.5f) * width, y = (clip.y / clip.w * 0.5f + 0.5f) * height;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (behind == 4) {
            return null;
        }
        if (behind > 0) {
            return new int[] {0, 0, width, height};
        }
        int x0 = Math.max(0, (int) Math.floor(minX) - 2), y0 = Math.max(0, (int) Math.floor(minY) - 2);
        int x1 = Math.min(width, (int) Math.ceil(maxX) + 2), y1 = Math.min(height, (int) Math.ceil(maxY) + 2);
        return x1 > x0 && y1 > y0 ? new int[] {x0, y0, x1, y1} : null;
    }

    static void run(RenderLevelStageEvent event, Runnable draw) {
        PoseStack view = RenderSystem.getModelViewStack();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        VIEW.set(event.getPoseStack().last().pose());
        PROJECTION.set(event.getProjectionMatrix());
        int[] viewport = new int[4], scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        boolean scissorOn = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        view.pushPose();
        view.last().pose().set(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.viewport(0, 0, main.width, main.height);
        GlStateManager._disableScissorTest();
        try {
            draw.run();
        } finally {
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (scissorOn) {
                GlStateManager._enableScissorTest();
                GlStateManager._scissorBox(scissor[0], scissor[1], scissor[2], scissor[3]);
            }
            view.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }
}
