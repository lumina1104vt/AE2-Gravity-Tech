package com.lumina.ae2_gvt.client.mesh;

import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;

/**
 * PROTOTYPE - hand-built geometry for the Twin-Star Hub preview (same idea as Lightning Tech's
 * CoreEffectMesh: spheres, ring bands and prisms generated in code, not block models). All
 * vertices are full-bright; colours carry the black / white look.
 */
public final class PreviewMesh {
    private PreviewMesh() {
    }

    /** Grey level + alpha for a point on a surface. */
    public interface Shade {
        int argb(Vector3f worldPos, float u, float v);
    }

    public static int grey(int g, int a) {
        g = Math.max(0, Math.min(255, g));
        return (a << 24) | (g << 16) | (g << 8) | g;
    }

    public static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f p, Vector3f n, int argb) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, (argb >>> 24) & 0xff)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, n.x, n.y, n.z);
    }

    public static void quad(PoseStack.Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                     Vector3f n, int ca, int cb, int cc, int cd, boolean doubleSided) {
        vertex(pose, vc, a, n, ca);
        vertex(pose, vc, b, n, cb);
        vertex(pose, vc, c, n, cc);
        vertex(pose, vc, d, n, cd);
        if (doubleSided) {
            Vector3f back = new Vector3f(n).negate();
            vertex(pose, vc, d, back, cd);
            vertex(pose, vc, c, back, cc);
            vertex(pose, vc, b, back, cb);
            vertex(pose, vc, a, back, ca);
        }
    }

    /** Quad with its own normal per corner - smooth (Gouraud) shading across curved surfaces. */
    public static void quadSmooth(PoseStack.Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                           Vector3f na, Vector3f nb, Vector3f nc, Vector3f nd, int ca, int cb, int cc, int cd, boolean doubleSided) {
        vertex(pose, vc, a, na, ca);
        vertex(pose, vc, b, nb, cb);
        vertex(pose, vc, c, nc, cc);
        vertex(pose, vc, d, nd, cd);
        if (doubleSided) {
            vertex(pose, vc, d, new Vector3f(nd).negate(), cd);
            vertex(pose, vc, c, new Vector3f(nc).negate(), cc);
            vertex(pose, vc, b, new Vector3f(nb).negate(), cb);
            vertex(pose, vc, a, new Vector3f(na).negate(), ca);
        }
    }

    /**
     * Ellipsoid-able sphere. {@code squashAxis} (unit) is scaled by {@code along}, the plane
     * across it by {@code across} - used to show the two spheres pushing each other away.
     */
    public static void sphere(PoseStack.Pose pose, VertexConsumer vc, Vector3f centre, float radius,
                              Vector3f squashAxis, float along, float across, int lats, int lons,
                              Shade shade, boolean doubleSided, float maxWorldY) {
        Vector3f[][] pts = new Vector3f[lats + 1][lons + 1];
        Vector3f[][] nrm = new Vector3f[lats + 1][lons + 1];
        for (int i = 0; i <= lats; i++) {
            double theta = Math.PI * i / lats;
            for (int j = 0; j <= lons; j++) {
                double phi = 2 * Math.PI * j / lons;
                Vector3f dir = new Vector3f((float) (Math.sin(theta) * Math.cos(phi)), (float) Math.cos(theta),
                        (float) (Math.sin(theta) * Math.sin(phi)));
                float k = dir.dot(squashAxis);
                Vector3f parallel = new Vector3f(squashAxis).mul(k);
                Vector3f perp = new Vector3f(dir).sub(parallel);
                Vector3f p = parallel.mul(along).add(perp.mul(across)).mul(radius).add(centre);
                pts[i][j] = p;
                nrm[i][j] = dir;
            }
        }
        for (int i = 0; i < lats; i++) {
            for (int j = 0; j < lons; j++) {
                Vector3f a = pts[i][j], b = pts[i + 1][j], c = pts[i + 1][j + 1], d = pts[i][j + 1];
                if (Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)) > maxWorldY) {
                    continue; // formation reveal: only the part below the rising line exists yet
                }
                float u0 = (float) j / lons, u1 = (float) (j + 1) / lons, v0 = (float) i / lats, v1 = (float) (i + 1) / lats;
                quadSmooth(pose, vc, a, b, c, d, nrm[i][j], nrm[i + 1][j], nrm[i + 1][j + 1], nrm[i][j + 1],
                        shade.argb(a, u0, v0), shade.argb(b, u0, v1), shade.argb(c, u1, v1), shade.argb(d, u1, v0),
                        doubleSided);
            }
        }
    }

    /**
     * Arc piece of rectangular cross-section: radius {@code rIn}..{@code rOut}, axial
     * {@code zMin}..{@code zMax}, angles {@code a0}..{@code a1}, around the local Z axis, then
     * rotated by {@code orient}. Partial arcs get end caps.
     */
    public static void ringPiece(PoseStack.Pose pose, VertexConsumer vc, Quaternionfc orient, float rIn, float rOut,
                                 float zMin, float zMax, double a0, double a1, int steps,
                                 int cOuter, int cInner, int cFront, int cBack, int cCap) {
        for (int s = 0; s < steps; s++) {
            double b0 = a0 + (a1 - a0) * s / steps, b1 = a0 + (a1 - a0) * (s + 1) / steps;
            float c0 = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
            Vector3f[] p = {
                    new Vector3f(c0 * rOut, s0 * rOut, zMin), new Vector3f(c1 * rOut, s1 * rOut, zMin),
                    new Vector3f(c1 * rOut, s1 * rOut, zMax), new Vector3f(c0 * rOut, s0 * rOut, zMax),
                    new Vector3f(c0 * rIn, s0 * rIn, zMin), new Vector3f(c1 * rIn, s1 * rIn, zMin),
                    new Vector3f(c1 * rIn, s1 * rIn, zMax), new Vector3f(c0 * rIn, s0 * rIn, zMax)};
            for (Vector3f v : p) {
                orient.transform(v);
            }
            // rims shade smoothly around the ring (normal per corner), the flat faces stay crisp
            Vector3f out0 = orient.transform(new Vector3f(c0, s0, 0)), out1 = orient.transform(new Vector3f(c1, s1, 0));
            Vector3f in0 = new Vector3f(out0).negate(), in1 = new Vector3f(out1).negate();
            Vector3f frontN = orient.transform(new Vector3f(0, 0, 1));
            Vector3f backN = new Vector3f(frontN).negate();
            quadSmooth(pose, vc, p[0], p[1], p[2], p[3], out0, out1, out1, out0, cOuter, cOuter, cOuter, cOuter, false);
            quadSmooth(pose, vc, p[7], p[6], p[5], p[4], in0, in1, in1, in0, cInner, cInner, cInner, cInner, false);
            quad(pose, vc, p[3], p[2], p[6], p[7], frontN, cFront, cFront, cFront, cFront, false);
            quad(pose, vc, p[4], p[5], p[1], p[0], backN, cBack, cBack, cBack, cBack, false);
            if (a1 - a0 < 2 * Math.PI - 1e-4) {
                if (s == 0) {
                    Vector3f n = orient.transform(new Vector3f(s0, -c0, 0));
                    quad(pose, vc, p[4], p[0], p[3], p[7], n, cCap, cCap, cCap, cCap, false);
                }
                if (s == steps - 1) {
                    Vector3f n = orient.transform(new Vector3f(-s1, c1, 0));
                    quad(pose, vc, p[1], p[5], p[6], p[2], n, cCap, cCap, cCap, cCap, false);
                }
            }
        }
    }

    /** Square prism from {@code from} to {@code to} (axle pins, beams). */
    public static void prism(PoseStack.Pose pose, VertexConsumer vc, Vector3f from, Vector3f to, float halfSize,
                             int argb, boolean doubleSided) {
        Vector3f dir = new Vector3f(to).sub(from);
        if (dir.lengthSquared() < 1e-6f) {
            return;
        }
        dir.normalize();
        Vector3f side = Math.abs(dir.y) < 0.9f ? new Vector3f(dir).cross(0, 1, 0) : new Vector3f(dir).cross(1, 0, 0);
        side.normalize().mul(halfSize);
        Vector3f up = new Vector3f(dir).cross(side).normalize().mul(halfSize);
        Vector3f[] off = {new Vector3f(side).add(up), new Vector3f(side).sub(up),
                new Vector3f(side).negate().sub(up), new Vector3f(side).negate().add(up)};
        for (int k = 0; k < 4; k++) {
            Vector3f o0 = off[k], o1 = off[(k + 1) % 4];
            Vector3f n = new Vector3f(o0).add(o1).normalize();
            quad(pose, vc, new Vector3f(from).add(o0), new Vector3f(to).add(o0), new Vector3f(to).add(o1),
                    new Vector3f(from).add(o1), n, argb, argb, argb, argb, doubleSided);
        }
    }

    /** Axis-aligned box, for the controller footprint ghost and the formation pulse. */
    public static void box(PoseStack.Pose pose, VertexConsumer vc, Vector3f min, Vector3f max, int argb, boolean doubleSided) {
        float x0 = min.x, y0 = min.y, z0 = min.z, x1 = max.x, y1 = max.y, z1 = max.z;
        quad(pose, vc, new Vector3f(x0, y1, z0), new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z0), new Vector3f(0, 1, 0), argb, argb, argb, argb, doubleSided);
        quad(pose, vc, new Vector3f(x0, y0, z1), new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y0, z1), new Vector3f(0, -1, 0), argb, argb, argb, argb, doubleSided);
        quad(pose, vc, new Vector3f(x0, y0, z0), new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y0, z0), new Vector3f(0, 0, -1), argb, argb, argb, argb, doubleSided);
        quad(pose, vc, new Vector3f(x1, y0, z1), new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1), new Vector3f(x0, y0, z1), new Vector3f(0, 0, 1), argb, argb, argb, argb, doubleSided);
        quad(pose, vc, new Vector3f(x0, y0, z1), new Vector3f(x0, y1, z1), new Vector3f(x0, y1, z0), new Vector3f(x0, y0, z0), new Vector3f(-1, 0, 0), argb, argb, argb, argb, doubleSided);
        quad(pose, vc, new Vector3f(x1, y0, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1), new Vector3f(x1, y0, z1), new Vector3f(1, 0, 0), argb, argb, argb, argb, doubleSided);
    }
}
