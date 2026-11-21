package com.lumina.ae2_gvt.client.mesh;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * PROTOTYPE - the twin holes' gravity shown as a bent 3D grid, the way space-time is often drawn:
 * straight grid lines through the hub's sphere, pulled in towards the black hole and pushed out
 * round the white hole (it bulges outward - a white hole repels). The grid stays put while the
 * holes orbit through it, so the lines keep bending and straightening as they pass. The lines are
 * clearest next to a hole and fade with distance from it (and are gone by the unit shell). Drawn as vanilla lines (RenderType.lines()).
 */
public final class GravityLattice {
    /** Grid lines per axis and spacing between them. */
    private static final int LINES = 9;
    private static final int SAMPLES = 48;
    /** How hard the holes bend the grid: displacement = STRENGTH * r / (r^2 + SOFT^2), towards / away. */
    private static final float STRENGTH = 9f, SOFT = 2.2f;
    private static final int COLOUR = 0xE8E8EE, MAX_ALPHA = 200;
    /** Distance from a hole at which the grid has faded to a fifth of its strength. */
    private static final float NEAR = 4.5f;

    private GravityLattice() {
    }

    /**
     * @param black  black hole centre (relative to the sphere centre)
     * @param white  white hole centre
     * @param extent radius of the sphere the grid fills (fades out before it)
     * @param amount 0..1 formation fade-in (also scales the bending)
     */
    public static void draw(PoseStack.Pose pose, VertexConsumer vc, Vector3f black, Vector3f white, float extent, float amount) {
        if (amount <= 0) {
            return;
        }
        float spacing = 2 * extent / (LINES + 1);
        Vector3f p = new Vector3f(), q = new Vector3f(), prev = new Vector3f(), cur = new Vector3f();
        for (int axis = 0; axis < 3; axis++) {
            for (int a = 1; a <= LINES; a++) {
                for (int b = 1; b <= LINES; b++) {
                    float u = -extent + a * spacing, v = -extent + b * spacing;
                    float across = (float) Math.sqrt(u * u + v * v);
                    if (across >= extent) {
                        continue;   // this line misses the sphere
                    }
                    float half = (float) Math.sqrt(extent * extent - across * across);
                    float prevAlpha = 0;
                    for (int s = 0; s <= SAMPLES; s++) {
                        float t = -half + 2 * half * s / SAMPLES;
                        set(p, axis, t, u, v);
                        bend(p, black, white, amount, cur);
                        float alpha = amount * nearness(p, black, white) * fade(p.length(), extent);
                        if (s > 0 && (alpha > 0.01f || prevAlpha > 0.01f)) {
                            q.set(cur).sub(prev);
                            line(pose, vc, prev, prevAlpha, cur, alpha, q);
                        }
                        prev.set(cur);
                        prevAlpha = alpha;
                    }
                }
            }
        }
    }

    /** Point on grid line: {@code t} along {@code axis}, ({@code u}, {@code v}) on the other two. */
    private static void set(Vector3f out, int axis, float t, float u, float v) {
        switch (axis) {
            case 0 -> out.set(t, u, v);
            case 1 -> out.set(u, t, v);
            default -> out.set(u, v, t);
        }
    }

    /** The grid point moved by the two holes: towards the black one, away from the white one. */
    private static void bend(Vector3f p, Vector3f black, Vector3f white, float amount, Vector3f out) {
        out.set(p);
        push(p, black, -STRENGTH * amount, out);
        push(p, white, STRENGTH * amount, out);
    }

    private static void push(Vector3f p, Vector3f hole, float strength, Vector3f out) {
        float dx = p.x - hole.x, dy = p.y - hole.y, dz = p.z - hole.z;
        float r2 = dx * dx + dy * dy + dz * dz;
        float r = (float) Math.sqrt(r2);
        if (r < 1e-4f) {
            return;
        }
        // displacement peaks near the hole's surface and falls off with distance
        float k = strength / (r2 + SOFT * SOFT);
        out.add(dx * k, dy * k, dz * k);
    }

    /** 1 at a hole, falling off with distance from the nearer one. */
    private static float nearness(Vector3f p, Vector3f black, Vector3f white) {
        float d = Math.min(p.distance(black), p.distance(white)) / NEAR;
        return 1 / (1 + 4 * d * d);
    }

    /** Full strength in the middle of the sphere, fading to nothing at its edge. */
    private static float fade(float r, float extent) {
        float x = Math.max(0, Math.min(1, (extent - r) / (extent * 0.45f)));
        return x * x * (3 - 2 * x);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer vc, Vector3f a, float alphaA, Vector3f b, float alphaB, Vector3f dir) {
        float len = dir.length();
        if (len < 1e-5f) {
            return;
        }
        float nx = dir.x / len, ny = dir.y / len, nz = dir.z / len;
        vertex(pose, vc, a, alphaA, nx, ny, nz);
        vertex(pose, vc, b, alphaB, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f p, float alpha, float nx, float ny, float nz) {
        vc.vertex(pose.pose(), p.x, p.y, p.z).color((COLOUR >> 16) & 0xff, (COLOUR >> 8) & 0xff, COLOUR & 0xff, (int) (MAX_ALPHA * alpha)).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
