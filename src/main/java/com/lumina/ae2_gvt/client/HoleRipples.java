package com.lumina.ae2_gvt.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.hole.GravityHoleBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * Gravitational-wave ripples: clear spherical shells running out from a hole that only warp the
 * world behind them (shader hole_ripple, {@link ClientShaders}); drawn by {@link HoleRenderer}.
 * <ul>
 *   <li>every hole sends out a small one now and then: out to {@link #AMBIENT_REACH} blocks, faint</li>
 *   <li>a hole blown up sends out a big one ({@link #burst}): three shells out to
 *       {@link #BURST_REACH} blocks, strong at first, fading as they go</li>
 * </ul>
 */
public final class HoleRipples {
    static final RenderType RIPPLE = RenderType.create("ae2_gvt_hole_ripple", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeRipple))
                    .setTextureState(SceneSnapshot.TEXTURE_HALF)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /** Small ripples: ticks apart (each runs that long), how far out (blocks), push at the start (blocks). */
    private static final float AMBIENT_PERIOD = 50, AMBIENT_REACH = 6, AMBIENT_PUSH = 0.12f;
    /** Big ripple: ticks it runs, and for one 48 blocks far (larger ones scale): push at the start, gap between its shells. */
    private static final float BURST_LIFE = 60, BURST_PUSH = 1.0f, BURST_GAP = 1.4f;
    /** A ripple this far or further also flashes white (an 湮滅 TNT, not a hole), for this many ticks. */
    private static final float FLASH_REACH = 100, FLASH_LIFE = 24;

    private record Burst(Vec3 at, double start, float reach) {
    }

    private static final List<Burst> BURSTS = new ArrayList<>();

    private HoleRipples() {
    }

    /** A big ripple runs out from {@code pos}, {@code reach} blocks far. */
    public static void burst(net.minecraft.core.BlockPos pos, float reach) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            BURSTS.add(new Burst(Vec3.atCenterOf(pos), mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true), reach));
    }

    /** The hole's small ripple (the snapshot must be fresh). */
    static void ambient(MultiBufferSource.BufferSource buffers, Vec3 cam, GravityHoleBlockEntity hole, double now) {
        int offset = Math.floorMod(hole.getBlockPos().hashCode(), (int) AMBIENT_PERIOD);
        double t = ((now + offset) % AMBIENT_PERIOD) / AMBIENT_PERIOD;
        float fadeIn = (float) Math.min(1, t / 0.08);
        float fade = (float) ((1 - t) * (1 - t)) * fadeIn;
        float radius = GravityHole.RADIUS + (float) t * (AMBIENT_REACH - GravityHole.RADIUS);
        shell(buffers.getBuffer(RIPPLE), Vec3.atCenterOf(hole.getBlockPos()).subtract(cam), radius,
                AMBIENT_PUSH * fade, 0.03f * fade, 24, 48);
        buffers.endBatch(RIPPLE);
    }

    /** The big ripples running now, each over a fresh snapshot; the finished ones are dropped. */
    static void bursts(MultiBufferSource.BufferSource buffers, Vec3 cam, double now) {
        BURSTS.removeIf(b -> now - b.start() > BURST_LIFE || now < b.start() - 1);
        Minecraft mc = Minecraft.getInstance();
        for (Burst b : BURSTS) {
            if (b.at().distanceTo(cam) > b.reach() * 3.4)
                continue;
            float t = (float) Math.max(0, (now - b.start()) / BURST_LIFE);
            float out = 1 - (float) Math.pow(1 - t, 2.5);     // fast at first, slowing down
            float fade = (float) Math.pow(1 - t, 1.5);
            SceneSnapshot.captureHalf(mc.getMainRenderTarget());
            VertexConsumer vc = buffers.getBuffer(RIPPLE);
            Vec3 at = b.at().subtract(cam);
            // the leading shell pushes out, the next pulls in, the last pushes a little: a wave, not a step
            float[] push = {1, -0.6f, 0.3f};
            for (int k = 0; k < push.length; k++) {
                float radius = out * b.reach() - k * BURST_GAP * b.reach() / 48;
                if (radius > 0.2f)
                    shell(vc, at, radius, BURST_PUSH * b.reach() / 48 * push[k] * fade, k == 0 ? 0.12f * fade : 0, 48, 96);
            }
            buffers.endBatch(RIPPLE);
        }
    }

    /** How white the screen is now from an 湮滅 TNT (0 … 1): fades with time and distance. */
    static float flash(Vec3 cam, double now) {
        float white = 0;
        for (Burst b : BURSTS)
            if (b.reach() >= FLASH_REACH && now >= b.start() && now - b.start() < FLASH_LIFE)
                white = Math.max(white, (float) ((1 - (now - b.start()) / FLASH_LIFE) * Math.max(0, 1 - b.at().distanceTo(cam) / (b.reach() * 3.4))));
        return white;
    }

    /** A sphere; UV0 carries the push and the glow to the shader, Normal the outward normal. */
    private static void shell(VertexConsumer vc, Vec3 at, float radius, float push, float glow, int lats, int lons) {
        PoseStack.Pose pose = new PoseStack().last();
        Vector3f c = new Vector3f((float) at.x, (float) at.y, (float) at.z);
        for (int i = 0; i < lats; i++) {
            for (int j = 0; j < lons; j++) {
                vertex(vc, pose, c, radius, i, j, lats, lons, push, glow);
                vertex(vc, pose, c, radius, i, j + 1, lats, lons, push, glow);
                vertex(vc, pose, c, radius, i + 1, j + 1, lats, lons, push, glow);
                vertex(vc, pose, c, radius, i + 1, j, lats, lons, push, glow);
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, float r, int i, int j, int lats, int lons,
                               float push, float glow) {
        double theta = Math.PI * i / lats, phi = 2 * Math.PI * j / lons;
        float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
        vc.addVertex(pose, c.x + x * r, c.y + y * r, c.z + z * r)
                .setColor(255, 255, 255, 255)
                .setUv(push, glow)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(x, y, z);
    }
}
