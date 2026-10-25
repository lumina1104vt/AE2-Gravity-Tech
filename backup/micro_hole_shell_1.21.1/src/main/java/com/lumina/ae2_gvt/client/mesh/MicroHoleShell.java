package com.lumina.ae2_gvt.client.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * PROTOTYPE - the hub's centre: a spherical shell of micro black and white holes (replaces the
 * twin holes).
 * <ul>
 *   <li>{@link #COUNT} slots spread evenly over the sphere (Fibonacci lattice), half holding a
 *       black hole, half a white one</li>
 *   <li>every {@link #STEP_TICKS} ticks (0.25 s) {@link #SWAPS_PER_STEP} black / white pairs,
 *       picked at random anywhere on the shell, start trading places. Each pair cuts straight
 *       through the middle of the sphere to the other's slot over {@link #TRAVEL_STEPS} steps,
 *       sidestepping a little to pass each other; a hole on its way is not picked again until it
 *       arrives</li>
 *   <li>ripples: every so often a ripple starts at a random point and spreads over the shell as
 *       a ring, like a drop in water, fading as it goes</li>
 * </ul>
 * Everything comes from a fixed seed and the step number, so every client sees the same sequence
 * with nothing synced. Drawn with the hole body shader (HoleRenderTypes.BODY): Color.r = 1 for a
 * white hole, Normal = the point's direction from the hole's centre.
 */
public final class MicroHoleShell {
    public static final int COUNT = 400;
    public static final int STEP_TICKS = 5;
    public static final int SWAPS_PER_STEP = 4;
    /** How long a swap takes, in steps (6 = 1.5 s). */
    public static final int TRAVEL_STEPS = 6;
    private static final long SEED = 0x7a1157a2L;
    private static final int LATS = 9, LONS = 16;   // fine enough that each hole reads as round

    /** A new ripple every this many steps (8 = 2 s); each lives RIPPLE_LIFE seconds. */
    private static final int RIPPLE_EVERY = 8;
    private static final float RIPPLE_LIFE = 6f, RIPPLE_SPEED = 0.7f /* rad/s */, RIPPLE_WIDTH = 0.28f /* rad */;
    private static final float RIPPLE_HEIGHT = 0.07f;   // of the shell radius

    private final Vector3f[] slots = new Vector3f[COUNT];
    /** occupant of each slot once nothing is moving: true = white hole */
    private final boolean[] white = new boolean[COUNT];
    /** step at which the swap holding this slot ends; the slot is free again at or after it */
    private final long[] busyUntil = new long[COUNT];

    private record Swap(long start, int from, int to) {   // from = the black hole's slot, to = the white one's
    }

    private final List<Swap> active = new ArrayList<>();
    /** steps before this one are fully simulated (their swaps started) */
    private long simulated;

    public MicroHoleShell() {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < COUNT; i++) {
            float y = 1 - 2 * (i + 0.5f) / COUNT;
            float rr = (float) Math.sqrt(1 - y * y);
            double a = golden * i;
            slots[i] = new Vector3f((float) (rr * Math.cos(a)), y, (float) (rr * Math.sin(a)));
        }
        reset();
    }

    private void reset() {
        SplittableRandom rng = new SplittableRandom(SEED);
        int[] order = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            order[i] = i;
        }
        for (int i = COUNT - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        for (int i = 0; i < COUNT; i++) {
            white[order[i]] = i >= COUNT / 2;
            busyUntil[i] = 0;
        }
        active.clear();
        simulated = 0;
    }

    /** Run the swap schedule up to and including {@code step} (replays from the seed after a jump back). */
    private void simulateTo(long step) {
        if (step < simulated - 1) {
            reset();
        }
        while (simulated <= step) {
            long s = simulated;
            // swaps that end now land: the two holes have traded slots
            active.removeIf(sw -> {
                if (sw.start + TRAVEL_STEPS <= s) {
                    white[sw.from] = true;
                    white[sw.to] = false;
                    return true;
                }
                return false;
            });
            SplittableRandom rng = new SplittableRandom(SEED ^ (s * 0x9E3779B97F4A7C15L));
            for (int k = 0; k < SWAPS_PER_STEP; k++) {
                int b = freeSlot(rng, false, s), w = freeSlot(rng, true, s);
                if (b < 0 || w < 0) {
                    break;
                }
                busyUntil[b] = busyUntil[w] = s + TRAVEL_STEPS;
                active.add(new Swap(s, b, w));
            }
            simulated++;
        }
    }

    /** A random slot holding the given colour that isn't part of a swap, or -1. */
    private int freeSlot(SplittableRandom rng, boolean wantWhite, long step) {
        for (int tries = 0; tries < 64; tries++) {
            int i = rng.nextInt(COUNT);
            if (white[i] == wantWhite && busyUntil[i] <= step) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param ticks  game time in ticks (with partial tick)
     * @param radius shell radius
     * @param holeR  micro hole radius
     * @param orient overall orientation of the shell
     */
    public void draw(PoseStack.Pose pose, VertexConsumer vc, double ticks, float radius, float holeR, Quaternionfc orient) {
        long step = Math.max(0, (long) Math.floor(ticks / STEP_TICKS));
        simulateTo(step);
        List<Vector3f> ripples = new ArrayList<>();
        List<Float> ages = new ArrayList<>();
        collectRipples(ticks, ripples, ages);

        boolean[] moving = new boolean[COUNT];
        for (Swap sw : active) {
            float t = (float) ((ticks - sw.start * (double) STEP_TICKS) / (TRAVEL_STEPS * STEP_TICKS));
            t = Math.max(0, Math.min(1, t));
            float e = t * t * (3 - 2 * t);
            moving[sw.from] = moving[sw.to] = true;
            // the two cut straight through the middle, each to the other's slot; they sidestep a
            // little mid-way so they pass each other instead of meeting head-on
            Vector3f a = surface(slots[sw.from], radius, ripples, ages), b = surface(slots[sw.to], radius, ripples, ages);
            Vector3f chord = new Vector3f(b).sub(a);
            Vector3f side = new Vector3f(chord).cross(a);
            if (side.lengthSquared() < 1e-6f) {
                side.set(chord).cross(0, 1, 0);
            }
            side.normalize().mul(holeR * 1.3f * (float) Math.sin(Math.PI * t));
            Vector3f black = new Vector3f(a).lerp(b, e).add(side);     // the black hole travels from -> to
            Vector3f whiteHole = new Vector3f(b).lerp(a, e).sub(side); // the white one to -> from
            sphere(pose, vc, orient.transform(black), holeR, false);
            sphere(pose, vc, orient.transform(whiteHole), holeR, true);
        }
        for (int i = 0; i < COUNT; i++) {
            if (!moving[i]) {
                sphere(pose, vc, orient.transform(surface(slots[i], radius, ripples, ages)), holeR, white[i]);
            }
        }
    }

    /** The ripples alive at this time: their centres and ages in seconds. */
    private static void collectRipples(double ticks, List<Vector3f> centres, List<Float> ages) {
        long period = (long) RIPPLE_EVERY * STEP_TICKS;
        long newest = (long) Math.floor(ticks / period);
        int alive = (int) Math.ceil(RIPPLE_LIFE * 20 / period) + 1;
        for (long n = newest; n > newest - alive && n >= 0; n--) {
            float age = (float) ((ticks - n * period) / 20.0);
            if (age > RIPPLE_LIFE) {
                continue;
            }
            SplittableRandom rng = new SplittableRandom(SEED * 31 + n * 0xBF58476D1CE4E5B9L);
            double z = rng.nextDouble() * 2 - 1, a = rng.nextDouble() * 2 * Math.PI, r = Math.sqrt(1 - z * z);
            centres.add(new Vector3f((float) (r * Math.cos(a)), (float) z, (float) (r * Math.sin(a))));
            ages.add(age);
        }
    }

    /** Outward displacement (fraction of the radius) of the shell at direction {@code d}. */
    private static float ripple(Vector3f d, List<Vector3f> centres, List<Float> ages) {
        float f = 0;
        for (int i = 0; i < centres.size(); i++) {
            float age = ages.get(i);
            float dist = (float) Math.acos(Math.max(-1, Math.min(1, d.dot(centres.get(i)))));   // angle from the drop
            float front = RIPPLE_SPEED * age;                                                    // ring's angle
            float x = (dist - front) / RIPPLE_WIDTH;
            // a ring: one crest with a trough each side, fading with age; starts gently
            float ring = (float) (Math.cos(x * Math.PI) * Math.exp(-x * x * 0.8));
            float fade = (1 - age / RIPPLE_LIFE) * Math.min(1, age * 2);
            f += RIPPLE_HEIGHT * ring * fade * fade;
        }
        return f;
    }

    /** A slot's place on the rippling shell (shell-local, before the shell's own turning). */
    private static Vector3f surface(Vector3f dir, float radius, List<Vector3f> ripples, List<Float> ages) {
        return new Vector3f(dir).mul(radius * (1 + ripple(dir, ripples, ages)));
    }

    private static void sphere(PoseStack.Pose pose, VertexConsumer vc, Vector3f c, float holeR, boolean whiteHole) {
        int flag = whiteHole ? 255 : 0;
        for (int i = 0; i < LATS; i++) {
            for (int j = 0; j < LONS; j++) {
                vertex(pose, vc, c, holeR, i, j, flag);
                vertex(pose, vc, c, holeR, i, j + 1, flag);
                vertex(pose, vc, c, holeR, i + 1, j + 1, flag);
                vertex(pose, vc, c, holeR, i + 1, j, flag);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f c, float r, int i, int j, int white) {
        double theta = Math.PI * i / LATS, phi = 2 * Math.PI * j / LONS;
        float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
        vc.addVertex(pose, c.x + x * r, c.y + y * r, c.z + z * r)
                .setColor(white, 0, 0, 255)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(x, y, z);
    }
}
