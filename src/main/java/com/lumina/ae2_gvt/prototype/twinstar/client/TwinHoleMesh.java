package com.lumina.ae2_gvt.prototype.twinstar.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.LightCoordsUtil;

/**
 * PROTOTYPE - geometry for the twin holes (port of twin_holes() in the offline mock).
 * <p>
 * The visible surface is the smooth union (polynomial smin, blend radius {@code k}) of two spheres,
 * so a neck grows between them as they merge. The black mesh is cast from the black centre and the
 * white one from the white centre; each keeps only its own side of the dividing plane (clipped
 * exactly, so the two halves meet cleanly). Colour is left to the shader, which gets the pair-frame
 * coordinates, the signed distance to the divide and the proximity through UV0 / UV1.
 */
final class TwinHoleMesh {
    private TwinHoleMesh() {
    }

    private record V(Vector3f p, Vector3f n) {
    }

    /**
     * @param axis unit vector from the white centre to the black centre (the pair's axis)
     * @param s    centre-to-centre separation
     */
    static void draw(PoseStack.Pose pose, VertexConsumer vc, Vector3f axis, float s, float r, float k, int lats, int lons) {
        Vector3f black = new Vector3f(axis).mul(s / 2), white = new Vector3f(axis).mul(-s / 2);
        float prox = Math.max(0, Math.min(1, 1 - (s - 2 * r) / 3f));
        Vector3f e1 = new Vector3f(axis).cross(0, 1, 0);
        if (e1.lengthSquared() < 1e-6f) {
            e1.set(axis).cross(1, 0, 0);
        }
        e1.normalize();
        Vector3f e2 = new Vector3f(axis).cross(e1);
        Frame frame = new Frame(axis, e1, e2, prox);
        drawHalf(pose, vc, black, white, 1, r, k, s, lats, lons, frame);
        drawHalf(pose, vc, white, black, -1, r, k, s, lats, lons, frame);
    }

    private record Frame(Vector3f n, Vector3f e1, Vector3f e2, float prox) {
    }

    private static void drawHalf(PoseStack.Pose pose, VertexConsumer vc, Vector3f centre, Vector3f other, int side,
                                 float r, float k, float s, int lats, int lons, Frame f) {
        V[][] grid = new V[lats + 1][lons + 1];
        for (int i = 0; i <= lats; i++) {
            double th = Math.PI * i / lats;
            for (int j = 0; j <= lons; j++) {
                double ph = 2 * Math.PI * j / lons;
                Vector3f d = new Vector3f((float) (Math.sin(th) * Math.cos(ph)), (float) Math.cos(th), (float) (Math.sin(th) * Math.sin(ph)));
                grid[i][j] = surfacePoint(centre, other, d, r, k, s);
            }
        }
        for (int i = 0; i < lats; i++) {
            for (int j = 0; j < lons; j++) {
                V[] quad = {grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]};
                // keep our colour's side of the divide (h * side >= 0); clip straddling quads exactly
                List<V> poly = new ArrayList<>(5);
                for (int q = 0; q < 4; q++) {
                    V a = quad[q], b = quad[(q + 1) % 4];
                    float da = a.p.dot(f.n) * side, db = b.p.dot(f.n) * side;
                    if (da >= 0) {
                        poly.add(a);
                    }
                    if ((da >= 0) != (db >= 0)) {
                        float t = da / (da - db);
                        poly.add(new V(new Vector3f(a.p).lerp(b.p, t), new Vector3f(a.n).lerp(b.n, t).normalize()));
                    }
                }
                // fan out as quads (a triangle repeats its last corner)
                for (int q = 1; q + 1 < poly.size(); q += 2) {
                    V last = q + 2 < poly.size() ? poly.get(q + 2) : poly.get(q + 1);
                    emit(pose, vc, poly.get(0), f);
                    emit(pose, vc, poly.get(q), f);
                    emit(pose, vc, poly.get(q + 1), f);
                    emit(pose, vc, last, f);
                }
            }
        }
    }

    /** First point where the ray from {@code centre} along {@code d} meets the union surface. */
    private static V surfacePoint(Vector3f centre, Vector3f other, Vector3f d, float r, float k, float s) {
        Vector3f onSphere = new Vector3f(d).mul(r).add(centre);
        // far from the other hole the union is just our own sphere: no search needed
        if (onSphere.distance(other) - r >= k) {
            return new V(onSphere, new Vector3f(d));
        }
        float tMax = r + 2 * k + s + 0.5f;
        int steps = 48;
        float lo = 0, hi = tMax;
        for (int i = 1; i <= steps; i++) {
            float t = tMax * i / steps;
            if (field(new Vector3f(d).mul(t).add(centre), centre, other, r, k) >= 0) {
                lo = tMax * (i - 1) / steps;
                hi = t;
                break;
            }
        }
        for (int i = 0; i < 12; i++) {
            float mid = (lo + hi) / 2;
            if (field(new Vector3f(d).mul(mid).add(centre), centre, other, r, k) < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        Vector3f p = new Vector3f(d).mul(lo).add(centre);
        float e = 0.02f;
        Vector3f n = new Vector3f(
                field(new Vector3f(p).add(e, 0, 0), centre, other, r, k) - field(new Vector3f(p).sub(e, 0, 0), centre, other, r, k),
                field(new Vector3f(p).add(0, e, 0), centre, other, r, k) - field(new Vector3f(p).sub(0, e, 0), centre, other, r, k),
                field(new Vector3f(p).add(0, 0, e), centre, other, r, k) - field(new Vector3f(p).sub(0, 0, e), centre, other, r, k));
        return new V(p, n.lengthSquared() > 1e-12f ? n.normalize() : new Vector3f(d));
    }

    /** Smooth union of the two spheres (negative inside). */
    private static float field(Vector3f p, Vector3f a, Vector3f b, float r, float k) {
        float da = p.distance(a) - r, db = p.distance(b) - r;
        if (k <= 1e-6f) {
            return Math.min(da, db);
        }
        float h = Math.max(0, Math.min(1, 0.5f + 0.5f * (db - da) / k));
        return db * (1 - h) + da * h - k * h * (1 - h);
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer vc, V v, Frame f) {
        float h = v.p.dot(f.n);
        vc.addVertex(pose, v.p.x, v.p.y, v.p.z)
                .setColor(255, 255, 255, 255)
                .setUv(v.p.dot(f.e1), v.p.dot(f.e2))
                .setUv1(Math.round(h * 1000), Math.round(f.prox * 1000))
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, v.n.x, v.n.y, v.n.z);
    }
}
