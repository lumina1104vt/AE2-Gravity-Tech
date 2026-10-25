package com.lumina.ae2_gvt.client.mesh;

import java.util.SplittableRandom;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * PROTOTYPE - the hub's gimbal drawn as three rings of floating debris instead of solid bands:
 * blocky black and white shards strung loosely along each ring, drifting round it while each one
 * tumbles on its own, with the odd glowing spark among them. The rings themselves still turn like
 * the gimbal (orientations from the renderer).
 * <ul>
 *   <li>outer ring (gravity): mostly dark shards</li>
 *   <li>middle ring (paradox): dark and light mixed</li>
 *   <li>inner ring (anti-gravity): mostly light shards</li>
 * </ul>
 * Every shard's size, spread, tumble and shade come from a fixed seed, so all clients agree.
 * Shards: textures/prototype/twin_star_debris.png (tools/twin_star_debris_texture.py), a 64x32
 * sheet - row 0 dark tiles, row 1 light tiles.
 */
public final class DebrisRings {
    private static final int PIECES = 70;
    private static final float[] SIZES = {0.18f, 0.25f, 0.3f, 0.42f, 0.55f};
    /** Chance a shard is light, per ring (outer, middle, inner). */
    private static final float[] LIGHT_SHARE = {0.15f, 0.5f, 0.85f};
    /** Drift round the ring, radians per second, per ring. */
    private static final float[] DRIFT = {0.10f, 0.14f, 0.18f};
    private static final long SEED = 0x5ed5ab1eL;

    private record Shard(float angle, float drift, float radial, float lift, float size, Vector3f spinAxis, float spinRate,
                         float spinPhase, int tile, boolean spark) {
    }

    private static final Shard[][] RINGS = new Shard[3][];

    static {
        for (int ring = 0; ring < 3; ring++) {
            SplittableRandom rng = new SplittableRandom(SEED + ring * 977L);
            Shard[] shards = new Shard[PIECES];
            for (int k = 0; k < PIECES; k++) {
                float angle = (float) (2 * Math.PI * k / PIECES + rng.nextDouble(-0.03, 0.03));
                float drift = k % 3 == 0 ? 1.25f : 1f;
                float radial = (float) rng.nextDouble(-0.9, 0.9), lift = (float) rng.nextDouble(-0.6, 0.6);
                float size = SIZES[rng.nextInt(SIZES.length)];
                Vector3f axis = new Vector3f((float) rng.nextDouble(-1, 1), (float) rng.nextDouble(-1, 1), (float) rng.nextDouble(-1, 1));
                if (axis.lengthSquared() < 1e-3f) {
                    axis.set(0, 1, 0);
                }
                axis.normalize();
                boolean light = rng.nextDouble() < LIGHT_SHARE[ring];
                int tile = (light ? 4 : 0) + rng.nextInt(4);
                shards[k] = new Shard(angle, drift, radial, lift, size, axis, (float) rng.nextDouble(0.5, 2.0),
                        (float) rng.nextDouble(0, 2 * Math.PI), tile, k % 9 == 0);
            }
            RINGS[ring] = shards;
        }
    }

    private DebrisRings() {
    }

    /**
     * Draws one ring: either its shards or its sparks (two passes, as they use different render
     * types and a buffer can't be written to after switching away from it).
     *
     * @param vc      shards: entityCutout on the debris sheet; sparks: emissive on a white texture
     * @param ring    0 outer, 1 middle, 2 inner
     * @param orient  the ring's orientation (ring in its local XY plane)
     * @param radius  ring radius
     * @param scale   0..1 formation grow-in
     */
    public static void draw(PoseStack.Pose pose, VertexConsumer vc, boolean sparksPass, int ring, Quaternionfc orient,
                            float radius, float seconds, float scale) {
        if (scale <= 0) {
            return;
        }
        Vector3f radial = new Vector3f(), tangent = new Vector3f(), normal = orient.transform(new Vector3f(0, 0, 1));
        for (Shard s : RINGS[ring]) {
            float a = s.angle + seconds * DRIFT[ring] * s.drift;
            orient.transform(radial.set((float) Math.cos(a), (float) Math.sin(a), 0));
            orient.transform(tangent.set((float) -Math.sin(a), (float) Math.cos(a), 0));
            Vector3f centre = new Vector3f(radial).mul(radius + s.radial).add(new Vector3f(normal).mul(s.lift));
            Quaternionf spin = new Quaternionf().rotateAxis(s.spinPhase + seconds * s.spinRate, s.spinAxis);
            if (sparksPass) {
                if (s.spark) {
                    float g = 0.08f * scale;
                    Vector3f c = new Vector3f(centre).add(new Vector3f(normal).mul(0.7f));
                    box(pose, vc, c, new Vector3f(radial).mul(g), new Vector3f(tangent).mul(g), new Vector3f(normal).mul(g), -1,
                            0xC8F4F4F8);
                }
                continue;
            }
            float h = s.size * scale;
            // a cube: half-axes radial, tangent, normal (right-handed, so the faces wind outward)
            box(pose, vc, centre, spin.transform(new Vector3f(radial).mul(h)), spin.transform(new Vector3f(tangent).mul(h)),
                    spin.transform(new Vector3f(normal).mul(h)), s.tile, 0xFFFFFFFF);
        }
    }

    /** Box with right-handed half-axes ex, ey, ez; {@code tile} = sheet tile (or -1: whole texture). */
    private static void box(PoseStack.Pose pose, VertexConsumer vc, Vector3f c, Vector3f ex, Vector3f ey, Vector3f ez, int tile, int argb) {
        face(pose, vc, c, ex, ey, ez, tile, argb);
        face(pose, vc, c, new Vector3f(ex).negate(), ez, ey, tile, argb);
        face(pose, vc, c, ey, ez, ex, tile, argb);
        face(pose, vc, c, new Vector3f(ey).negate(), ex, ez, tile, argb);
        face(pose, vc, c, ez, ex, ey, tile, argb);
        face(pose, vc, c, new Vector3f(ez).negate(), ey, ex, tile, argb);
    }

    /** The face whose outward half-axis is {@code n}; u, v span it (u x v points along n). */
    private static void face(PoseStack.Pose pose, VertexConsumer vc, Vector3f c, Vector3f n, Vector3f u, Vector3f v, int tile, int argb) {
        float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
        if (tile >= 0) {
            u0 = (tile % 4) / 4f;
            v0 = (tile / 4) / 2f;
            u1 = u0 + 0.25f;
            v1 = v0 + 0.5f;
        }
        Vector3f p = new Vector3f(c).add(n);
        Vector3f nn = new Vector3f(n).normalize();
        vertex(pose, vc, new Vector3f(p).sub(u).sub(v), u0, v1, nn, argb);
        vertex(pose, vc, new Vector3f(p).add(u).sub(v), u1, v1, nn, argb);
        vertex(pose, vc, new Vector3f(p).add(u).add(v), u1, v0, nn, argb);
        vertex(pose, vc, new Vector3f(p).sub(u).add(v), u0, v0, nn, argb);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f p, float u, float v, Vector3f n, int argb) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, (argb >>> 24) & 0xff)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, n.x, n.y, n.z);
    }
}
