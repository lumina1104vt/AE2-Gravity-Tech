package com.lumina.ae2_gvt.client.renderer;

import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.hole.GravityHoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A black or white hole where the world's own drawing ({@code client.HoleRenderer}: shaders, lensing of the frame) does not
 * run, a guide's scene for one: a plain sphere and an accretion ring lying nearly flat, tilted a little, no lensing. A black hole is a dark ball in a bright
 * ring, a white hole a bright ball in a dark ring (so the two differ by more than colour). In the real world it draws nothing.
 */
public final class SimpleHoleRenderer implements BlockEntityRenderer<GravityHoleBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int LATS = 12, LONS = 20, RING = 32;
    private static final float RING_IN = 1.25f, RING_OUT = 2.7f, TILT = (float) Math.toRadians(20);

    @Override
    public void render(GravityHoleBlockEntity hole, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (hole.getLevel() == Minecraft.getInstance().level) {
            return;     // the world draws it
        }
        boolean white = hole.isWhite();
        float r = GravityHole.RADIUS;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        PoseStack.Pose p = pose.last();
        float ball = white ? 0.95f : 0.02f;
        sphere(vc, p, r, ball);
        // the ring, tilted: bright at its inner edge, fading outwards; a white hole's is dark
        float ca = (float) Math.cos(TILT), sa = (float) Math.sin(TILT);
        for (int i = 0; i < RING; i++) {
            double a0 = Math.PI * 2 * i / RING, a1 = Math.PI * 2 * (i + 1) / RING;
            ringQuad(vc, p, r, a0, a1, ca, sa, white);
        }
        pose.popPose();
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose p, float r, float shade) {
        for (int i = 0; i < LATS; i++) {
            double t0 = Math.PI * i / LATS, t1 = Math.PI * (i + 1) / LATS;
            for (int j = 0; j < LONS; j++) {
                double f0 = Math.PI * 2 * j / LONS, f1 = Math.PI * 2 * (j + 1) / LONS;
                vertex(vc, p, pt(r, t0, f0), shade);
                vertex(vc, p, pt(r, t1, f0), shade);
                vertex(vc, p, pt(r, t1, f1), shade);
                vertex(vc, p, pt(r, t0, f1), shade);
            }
        }
    }

    private static float[] pt(float r, double theta, double phi) {
        return new float[]{(float) (r * Math.sin(theta) * Math.cos(phi)), (float) (r * Math.cos(theta)), (float) (r * Math.sin(theta) * Math.sin(phi))};
    }

    private static void ringQuad(VertexConsumer vc, PoseStack.Pose p, float r, double a0, double a1, float ca, float sa, boolean white) {
        // in the disc's own plane, then tilted about the x axis
        float[][] corner = {
                tilt(r * RING_IN * (float) Math.cos(a0), r * RING_IN * (float) Math.sin(a0), ca, sa),
                tilt(r * RING_OUT * (float) Math.cos(a0), r * RING_OUT * (float) Math.sin(a0), ca, sa),
                tilt(r * RING_OUT * (float) Math.cos(a1), r * RING_OUT * (float) Math.sin(a1), ca, sa),
                tilt(r * RING_IN * (float) Math.cos(a1), r * RING_IN * (float) Math.sin(a1), ca, sa)};
        float inner = white ? 0.05f : 1.0f, outer = white ? 0.35f : 0.25f;
        vertex(vc, p, corner[0], inner);
        vertex(vc, p, corner[1], outer);
        vertex(vc, p, corner[2], outer);
        vertex(vc, p, corner[3], inner);
    }

    private static float[] tilt(float x, float z, float ca, float sa) {
        return new float[]{x, z * sa, z * ca};
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float[] v, float shade) {
        int c = Math.round(Math.max(0, Math.min(1, shade)) * 255);
        vc.addVertex(p, v[0], v[1], v[2]).setColor(c, c, c, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
