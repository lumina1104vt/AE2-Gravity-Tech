package com.lumina.ae2_gvt.prototype.fusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * The fusion plasma's activity, around a star of radius {@link FusionPlasmaRenderer#RADIUS} at the
 * origin, inside the containment shell ({@link FusionPlasmaRenderer#SHELL}). Worked out in 2D first
 * (tools/fusion_eruption_mock.gif, tools/fusion_prominence_snap.gif) and tuned with the user.
 * <ul>
 *   <li><b>Prominences</b> ({@link #SLOTS} at a time, each {@link #LIFE} s): an arc rises with both
 *       feet in the surface, springs outward, one foot snaps loose and the freed end swings over
 *       the top to the other side and into the plasma, then the arc shrinks back into its root.
 *       Every phase eases in and out and overlaps the next, so it never stops dead. Drawn as many
 *       fine fibres, each a little behind the last, so they fan into threads; threads stretch
 *       from the torn foot until they thin out; faint after-images trail the motion. Squeezed
 *       softly below the shell, so they bend along it instead of passing through.</li>
 *   <li><b>Eruptions</b> (every {@link #ERUPT_PERIOD} s): a flash at the surface, then a cloud of
 *       clumps thrown out in a widening cone, slowing as the star pulls them back; those reaching
 *       the shell slide along it and fade, and the shell ripples from the hit.</li>
 * </ul>
 * Everything comes from hashing the event number with the controller position, so every player
 * sees the same. Ribbons (layer 3) and blobs (layer 4) go through fusion_plasma.fsh.
 */
final class FusionActivity {
    static final int SLOTS = 5;
    static final double LIFE = 7.2;
    static final double ERUPT_PERIOD = 8.0;
    /** When an eruption's first clumps reach the shell, and how long the shell ripples. */
    static final double HIT_AT = 0.32, RIPPLE = 2.0;

    private static final float R = FusionPlasmaRenderer.RADIUS, SHELL = FusionPlasmaRenderer.SHELL;
    private static final double PHI0 = 0.35;
    private static final int POINTS = 44, THREAD_POINTS = 16, GHOSTS = 3;
    private static final int BLOBS = 140;
    /** Units of the 2D mock (pixels, star radius 62) -> blocks. */
    private static final double PX = R / 62.0;

    private FusionActivity() {
    }

    // ---- prominences ----

    static void prominences(PoseStack.Pose pose, VertexConsumer vc, int rgb, double time, long seed, Vec3 eye) {
        for (int k = 0; k < SLOTS; k++) {
            double t = time / LIFE + (double) k / SLOTS;
            long n = (long) Math.floor(t);
            prominence(pose, vc, rgb, t - n, time, seed, n * 131 + k * 17 + 3, eye);
        }
    }

    private static void prominence(PoseStack.Pose pose, VertexConsumer vc, int rgb, double p, double time, long seed, long id, Vec3 eye) {
        Vec3 a = randomDir(seed, id);                                      // foot A
        Vec3 along = a.cross(randomDir(seed, id + 1)).normalize();         // towards foot B
        Vec3 lateral = a.cross(along);
        double length = R * (0.6 + 0.15 * rand(seed, id + 2));
        double fade = smoother(0, 0.08, p) * (1 - smoother(0.9, 1, p));
        int fibres = 8 + (int) (rand(seed, id + 3) * 3);
        for (int f = 0; f < fibres; f++) {
            long fid = id * 31 + f;
            double off = (rand(seed, fid + 4) - 0.5) * 0.1;
            double side = (rand(seed, fid + 5) - 0.5) * 0.5;
            double lk = 0.92 + 0.14 * rand(seed, fid + 6);
            double phase = rand(seed, fid + 7) * 6.28;
            double lag = rand(seed, fid + 8) * 0.06;
            for (int g = GHOSTS; g >= 0; g--) {                          // after-images first, the fibre on top
                double pg = p - lag - g * 0.05 / LIFE;
                if (pg <= 0) {
                    continue;
                }
                double[][] s = strand(pg, length * lk);
                Vec3[] pts = new Vec3[s[0].length];
                float[] alpha = new float[pts.length];
                for (int i = 0; i < pts.length; i++) {
                    double u = (double) i / (pts.length - 1);
                    double x = s[0][i] + off * R * Math.sin(Math.PI * u) * 0.35, y = s[1][i] + off * R * 0.2 * u;
                    pts[i] = place(a, along, lateral, x, y, side * R * 0.25 * Math.sin(Math.PI * u));
                    double flow = 0.5 + 0.5 * Math.sin(u * 16 - time * 1.8 + phase);
                    alpha[i] = (float) ((0.35 + 0.65 * flow * flow * flow) * fade * 0.6 * Math.pow(0.6, g) * (s[0][i] < -0.01 ? 0 : 1));
                }
                ribbon(pose, vc, rgb, pts, alpha, 0.022f, eye);
            }
        }
        threads(pose, vc, rgb, p, seed, id, a, along, lateral, length, fade, eye);
    }

    /** From the torn-off foot to points on the freed strand, thinning as they stretch, then gone. */
    private static void threads(PoseStack.Pose pose, VertexConsumer vc, int rgb, double p, long seed, long id, Vec3 a, Vec3 along, Vec3 lateral,
                                double length, double fade, Vec3 eye) {
        if (recoil(p) <= 0) {
            return;
        }
        double[][] snap = strand(0.38, length), now = strand(p, length);
        double fx = snap[0][snap[0].length - 1], fy = snap[1][snap[1].length - 1];
        for (int k = 0; k < 5; k++) {
            double where = 0.55 + 0.4 * rand(seed, id * 7 + k + 40), limit = 0.9 + 0.7 * rand(seed, id * 7 + k + 50);
            int at = (int) (where * (now[0].length - 1)), at0 = (int) (where * (snap[0].length - 1));
            double tx = now[0][at], ty = now[1][at];
            double d0 = Math.hypot(snap[0][at0] - fx, snap[1][at0] - fy) + 4 * PX;
            double d = Math.hypot(tx - fx, ty - fy), stretch = d / d0;
            double thin = (1 - smoother(1 + limit * 0.6, 1 + limit, stretch)) / (stretch * stretch);
            if (thin <= 0.002) {
                continue;
            }
            Vec3[] pts = new Vec3[THREAD_POINTS + 1];
            float[] alpha = new float[pts.length];
            for (int i = 0; i <= THREAD_POINTS; i++) {
                double u = (double) i / THREAD_POINTS;
                double x = fx + (tx - fx) * u - Math.sin(Math.PI * u) * d * 0.15, y = fy + (ty - fy) * u;
                pts[i] = place(a, along, lateral, x, y, 0);
                alpha[i] = (float) (0.7 * thin * fade * (x < -0.01 ? 0 : 1));
            }
            ribbon(pose, vc, rgb, pts, alpha, 0.014f, eye);
        }
    }

    /**
     * One strand at life fraction p: points (x outward from foot A, y along the surface towards
     * foot B), in blocks. The arch's tangent angle runs from PHI0 at A over the top to pi - PHI0
     * at B; after the snap the free tip leads the swing over to the other side and down, and the
     * whole arc shrinks into its root at the end.
     */
    static double[][] strand(double p, double length) {
        double rise = smoother(0, 0.32, p), spring = smoother(0.12, 0.52, p), recoil = recoil(p), sink = smoother(0.62, 1, p);
        double l = length * (0.3 + 0.7 * rise) * (1 + 0.35 * spring) * (1 - sink);
        double[] xs = new double[POINTS + 1], ys = new double[POINTS + 1];
        double x = 0, y = 0;
        for (int i = 1; i <= POINTS; i++) {
            double s = (double) i / POINTS;
            double loop = PHI0 + (Math.PI - 2 * PHI0) * s, back = PHI0 - (Math.PI + 0.9) * s;
            double w = ease(recoil * 1.25 - (1 - s) * 0.25);
            double phi = loop + (back - loop) * w;
            x += Math.cos(phi) * l / POINTS;
            y += Math.sin(phi) * l / POINTS;
            xs[i] = x;
            ys[i] = y;
        }
        return new double[][] {xs, ys};
    }

    private static double recoil(double p) {
        return ease((p - 0.22) / 0.74);
    }

    /** Local strand coordinates -> a point in space: along the surface from A, height squeezed below the shell. */
    private static Vec3 place(Vec3 a, Vec3 along, Vec3 lateral, double x, double y, double side) {
        double room = SHELL - R - 0.1;
        double r = R + (x > 0 ? room * Math.tanh(x / room) : x);
        double ang = y / R;
        return a.scale(Math.cos(ang)).add(along.scale(Math.sin(ang))).scale(r).add(lateral.scale(side));
    }

    // ---- eruptions ----

    /** The current eruption's direction and how far into it we are (seconds). */
    record Eruption(Vec3 dir, double t, long id) {
        /** The shell ripple's age 0..1, or 0 when there is none. */
        double hitAge() {
            double a = (t - HIT_AT) / RIPPLE;
            return a > 0 && a < 1 ? a : 0;
        }
    }

    static Eruption eruption(double time, long seed) {
        long n = (long) Math.floor(time / ERUPT_PERIOD);
        return new Eruption(randomDir(seed, n * 31 + 7), time - n * ERUPT_PERIOD, n);
    }

    static void eruptionCloud(PoseStack.Pose pose, VertexConsumer vc, int rgb, Eruption e, long seed, Vec3 eye) {
        double dt = e.t();
        if (dt > 4.5) {
            return;
        }
        // the flash where it breaks out
        double flash = Math.exp(-dt * 3.0);
        if (flash > 0.02) {
            blob(pose, vc, 0xFFFFFF, e.dir().scale(R), (float) ((7 + 16 * dt) * PX * 2), (float) (0.75 * flash), eye);
        }
        Vec3 ref = Math.abs(e.dir().y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = e.dir().cross(ref).normalize(), v = e.dir().cross(u);
        for (int b = 0; b < BLOBS; b++) {
            long bid = e.id() * 1009 + b;
            double spread = gauss(seed, bid) * 0.32, azimuth = rand(seed, bid + 1) * 2 * Math.PI;
            double speed = (0.6 + 0.65 * rand(seed, bid + 2)) * (1.15 - Math.min(1, Math.abs(spread))) * 150 * PX;
            double size = (1.5 + 2.1 * rand(seed, bid + 3)) * PX, delay = rand(seed, bid + 4) * 0.25;
            double slide = (rand(seed, bid + 5) < 0.5 ? -1 : 1) * (0.2 + 0.5 * rand(seed, bid + 6));
            double tt = dt - delay;
            if (tt <= 0) {
                continue;
            }
            double g = 90 * PX;                                      // pulled back towards the star
            double r = R + speed * tt - 0.5 * g * tt * tt;
            double ang = spread * (1 + tt * 0.6);                    // the cloud widens as it rises
            Vec3 tilt = u.scale(Math.cos(azimuth)).add(v.scale(Math.sin(azimuth)));
            Vec3 dir = e.dir().scale(Math.cos(ang)).add(tilt.scale(Math.sin(ang)));
            double fade = 1;
            if (r >= SHELL) {                                        // caught by the shell: slides along it and fades
                double tHit = (speed - Math.sqrt(Math.max(0, speed * speed - 2 * g * (SHELL - R)))) / g;
                double after = tt - tHit;
                r = SHELL - 0.04;
                double turn = slide * after;
                dir = dir.scale(Math.cos(turn)).add(tilt.scale(Math.sin(turn)));
                fade = Math.max(0, 1 - after / 1.6);
            } else if (r < R) {
                continue;
            }
            double sz = size * (1 + tt * 1.2);
            blob(pose, vc, rgb, dir.normalize().scale(r), (float) (sz * 2.5), (float) (0.5 * fade / (1 + tt * 0.8)), eye);
        }
    }

    // ---- geometry ----

    /** A strip along the points, turned to face the eye; UV0.y runs -1..1 across it. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer vc, int rgb, Vec3[] pts, float[] alpha, float width, Vec3 eye) {
        for (int i = 0; i < pts.length - 1; i++) {
            if (alpha[i] <= 0 && alpha[i + 1] <= 0) {
                continue;
            }
            Vec3 w0 = across(pts, i, eye).scale(width), w1 = across(pts, i + 1, eye).scale(width);
            vertex(pose, vc, pts[i].subtract(w0), rgb, alpha[i], 3, -1, 0, 0);
            vertex(pose, vc, pts[i].add(w0), rgb, alpha[i], 3, 1, 0, 0);
            vertex(pose, vc, pts[i + 1].add(w1), rgb, alpha[i + 1], 3, 1, 0, 0);
            vertex(pose, vc, pts[i + 1].subtract(w1), rgb, alpha[i + 1], 3, -1, 0, 0);
        }
    }

    private static Vec3 across(Vec3[] pts, int i, Vec3 eye) {
        Vec3 tangent = pts[Math.min(i + 1, pts.length - 1)].subtract(pts[Math.max(i - 1, 0)]);
        Vec3 side = tangent.cross(eye.subtract(pts[i]));
        double len = side.length();
        return len < 1e-6 ? Vec3.ZERO : side.scale(1 / len);
    }

    /** A soft round spot facing the eye; its corner coordinates (-1..1) go in the normal. */
    private static void blob(PoseStack.Pose pose, VertexConsumer vc, int rgb, Vec3 c, float size, float alpha, Vec3 eye) {
        Vec3 view = eye.subtract(c).normalize();
        Vec3 right = view.cross(Math.abs(view.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
        Vec3 up = right.cross(view);
        for (int[] k : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            Vec3 p = c.add(right.scale(k[0] * size)).add(up.scale(k[1] * size));
            vertex(pose, vc, p, rgb, alpha, 4, 0, k[0], k[1]);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, int rgb, float alpha, float layer, float v, float nx, float ny) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, Math.round(Math.max(0, Math.min(1, alpha)) * 255))
                .setUv(layer, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(nx, ny, 0);
    }

    // ---- easing / random ----

    /** 0 before e0, 1 after e1, smootherstep between (no jump in speed at either end). */
    static double smoother(double e0, double e1, double p) {
        double x = Math.max(0, Math.min(1, (p - e0) / (e1 - e0)));
        return x * x * x * (x * (6 * x - 15) + 10);
    }

    static double ease(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** 0..1 from (seed, salt). */
    static double rand(long seed, long salt) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Roughly normal, mean 0, deviation 1. */
    private static double gauss(long seed, long salt) {
        double u1 = Math.max(1e-9, rand(seed, salt * 3 + 101)), u2 = rand(seed, salt * 3 + 102);
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }

    static Vec3 randomDir(long seed, long salt) {
        double z = rand(seed, salt) * 2 - 1, a = rand(seed, salt + 1) * 2 * Math.PI, r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }
}
