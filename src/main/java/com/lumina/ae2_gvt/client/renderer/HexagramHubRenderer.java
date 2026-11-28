package com.lumina.ae2_gvt.client.renderer;

import java.util.Random;

import org.joml.Vector3f;

import com.lumina.ae2_gvt.client.HexagramHubClient;
import com.lumina.ae2_gvt.config.ClientConfig;
import com.lumina.ae2_gvt.hub.HubShape;
import com.lumina.ae2_gvt.hub.TwinStarHubBlock;
import com.lumina.ae2_gvt.hub.TwinStarHubBlockEntity;
import com.lumina.lmb.client.ClientStructures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the formed Twin-Star Hub renders rather than builds (the draft 3d.html), drawn from its controller with the
 * draft's own numbers: the black and the white hole with their coronas, lensing halo, blocky disks and
 * polar jets, the stars the white hole throws out, the wormhole bridge and its six filaments, and the sixteen data arcs with sparks running
 * along them (the draft's loose cubes looked like stray blocks in game: no drones). Coordinates are the draft's: origin at the L1 core, x from the black hole to
 * the white one, one unit = one block.
 */
public class HexagramHubRenderer implements BlockEntityRenderer<TwinStarHubBlockEntity> {
    private static final int BLACK_HOLE = 0, WHITE_HOLE = 1, CORONA = 2, DISK = 3, BRIDGE = 4, FILAMENT = 5, PLAIN = 6, JET = 7;
    private static final float BH = -28, WH = 28, LOAD = 0.78f;
    private static final float TAU = (float) (Math.PI * 2);

    private static final int PACKETS = 32, ARC_STEPS = 32, FILAMENTS = 6;
    private static final Vector3f[][] ARCS = arcs();
    private static final Vector3f[][] FILAMENT_PATHS = filaments();
    private static final int SPARKS = 220;
    private static final Vector3f[] SPARK_DIR = new Vector3f[SPARKS];
    private static final float[] SPARK_START = new float[SPARKS], SPARK_SPEED = new float[SPARKS], SPARK_LENGTH = new float[SPARKS];
    private static final float[] PACKET_START = new float[PACKETS], PACKET_SPEED = new float[PACKETS];

    static {
        Random random = new Random(1127);
        for (int i = 0; i < PACKETS; i++) {
            PACKET_START[i] = random.nextFloat();
            PACKET_SPEED[i] = (0.14f + random.nextFloat() * 0.22f) * (i % 3 == 0 ? -1 : 1);
        }
        for (int i = 0; i < SPARKS; i++) {
            // evenly over the sphere
            float y = random.nextFloat() * 2 - 1, a = random.nextFloat() * TAU, w = (float) Math.sqrt(1 - y * y);
            SPARK_DIR[i] = new Vector3f((float) Math.cos(a) * w, y, (float) Math.sin(a) * w);
            SPARK_START[i] = random.nextFloat();
            SPARK_SPEED[i] = 0.12f + random.nextFloat() * 0.18f;
            float l = random.nextFloat();
            SPARK_LENGTH[i] = 0.25f + 1.1f * l * l;
        }
    }

    @Override
    public void render(TwinStarHubBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null || !formed(be)) {
            return;
        }
        float time = (be.getLevel().getGameTime() % 1_728_000 + partialTick) / 20f;   // seconds
        float yaw = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
        BlockPos p = be.getBlockPos();
        // the camera in the draft's coordinates, for what turns to face it
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f eye = new Vector3f((float) (camera.x - p.getX() - 0.5), (float) (camera.y - p.getY() - 0.5 - HubShape.CY),
                (float) (camera.z - p.getZ() - 0.5)).rotateY((float) Math.toRadians(yaw));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + HubShape.CY, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        PoseStack.Pose pose = poseStack.last();

        // all of the opaque pass first: asking for the other type ends this one
        VertexConsumer solid = buffers.getBuffer(HexagramHubClient.SOLID);
        int detail = ClientConfig.HUB_DETAIL.get();
        int lats = 16 + 8 * detail, lons = 24 + 12 * detail;
        sphere(pose, solid, BH, 6.8f, lats, lons, BLACK_HOLE, 0, 0, 255);
        sphere(pose, solid, WH, 6.0f, lats, lons, WHITE_HOLE, 0, 0, 255);

        VertexConsumer glow = buffers.getBuffer(HexagramHubClient.GLOW);
        sphere(pose, glow, BH, 9.0f, 20, 32, CORONA, 145, 0xc2410c, 255);
        sphere(pose, glow, WH, 8.3f, 20, 32, CORONA, 165, 0xffffff, 255);
        sphere(pose, glow, WH, 10.6f, 20, 32, CORONA, 115, 0xcfe3ff, 255);
        disk(pose, glow, BH, 8.0f, 22.5f, (float) (Math.PI / 2 + 0.14), 0);
        disk(pose, glow, WH, 7.4f, 20.0f, (float) (Math.PI / 2 - 0.14), 1);
        halo(pose, glow, eye);
        jets(pose, glow, eye, time, detail);
        sparks(pose, glow, eye, time, 100 + 60 * detail);
        bridge(pose, glow);
        for (int f = 0; f < FILAMENTS; f++) {
            tube(pose, glow, FILAMENT_PATHS[f], 0.11f, 6, f);
        }
        if (detail > 0) {
            motes(pose, glow, eye, time);
        }
        for (int i = 0; i < (detail == 0 ? 0 : ARCS.length); i++) {
            for (int s = 0; s < ARC_STEPS; s++) {
                ribbon(pose, glow, ARCS[i][s], ARCS[i][s + 1], eye, 0.16f, i % 2 == 0 ? 0x38bdf8 : 0xf97316, 56);
            }
        }
        if (detail > 0) {
            packets(pose, glow, eye, time);
        }
        poseStack.popPose();
    }

    private static boolean formed(TwinStarHubBlockEntity be) {
        BlockState look = ClientStructures.lookAt(be.getBlockPos(), be.getBlockState());
        return look != null && look.hasProperty(TwinStarHubBlock.FORMED) && look.getValue(TwinStarHubBlock.FORMED);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, int rgb, int alpha,
                               float u, float v, int mode, int param, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z).setColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha).setUv(u, v)
                .setUv1(mode, param).setUv2(0, 0).setNormal(pose, nx, ny, nz);
    }

    private static void plain(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, int rgb, int alpha) {
        vertex(pose, vc, x, y, z, rgb, alpha, 0, 0, PLAIN, 0, 0, 1, 0);
    }

    private static void sphere(PoseStack.Pose pose, VertexConsumer vc, float cx, float r, int lats, int lons,
                               int mode, int param, int rgb, int alpha) {
        for (int i = 0; i < lats; i++) {
            for (int j = 0; j < lons; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corners) {
                    double th = Math.PI * c[0] / lats, ph = 2 * Math.PI * c[1] / lons;
                    float nx = (float) (Math.sin(th) * Math.cos(ph)), ny = (float) Math.cos(th), nz = (float) (Math.sin(th) * Math.sin(ph));
                    float u = (float) Math.hypot(nx, nz) * r, v = ny * r;
                    vertex(pose, vc, cx + nx * r, ny * r, nz * r, rgb, alpha, u, v, mode, param, nx, ny, nz);
                }
            }
        }
    }

    /** A flat ring tilted about x; the shader cuts it into one-block cells. */
    private static void disk(PoseStack.Pose pose, VertexConsumer vc, float cx, float inner, float outer, float tilt, int which) {
        float cos = (float) Math.cos(tilt), sin = (float) Math.sin(tilt);
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float a0 = TAU * i / segments, a1 = TAU * (i + 1) / segments;
            float[][] corners = {{inner, a0}, {outer, a0}, {outer, a1}, {inner, a1}};
            for (float[] c : corners) {
                float lx = (float) Math.cos(c[1]) * c[0], ly = (float) Math.sin(c[1]) * c[0];
                vertex(pose, vc, cx + lx, ly * cos, ly * sin, 0xffffff, 255, lx, ly, DISK, which, 0, 1, 0);
            }
        }
    }

    /** The black hole's lensing ring: a thin torus that always faces the viewer. */
    private static void halo(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye) {
        Vector3f n = new Vector3f(eye).sub(BH, 0, 0);
        if (n.lengthSquared() < 1e-4f) {
            return;
        }
        n.normalize();
        Vector3f a = new Vector3f(n).cross(0, 1, 0);
        if (a.lengthSquared() < 1e-4f) {
            a.set(1, 0, 0);
        }
        a.normalize();
        Vector3f b = new Vector3f(n).cross(a);
        int around = 80, tube = 8;
        for (int i = 0; i < around; i++) {
            for (int j = 0; j < tube; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corners) {
                    float u = TAU * c[0] / around, v = TAU * c[1] / tube;
                    float r = 8.5f + 0.42f * (float) Math.cos(v), h = 0.42f * (float) Math.sin(v);
                    float x = (float) Math.cos(u) * r, y = (float) Math.sin(u) * r;
                    plain(pose, vc, BH + a.x * x + b.x * y + n.x * h, a.y * x + b.y * y + n.y * h,
                            a.z * x + b.z * y + n.z * h, 0xfb923c, 184);
                }
            }
        }
    }

    private static void jets(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye, float time, int detail) {
        int sides = 16 + 8 * detail;
        for (int dir = -1; dir <= 1; dir += 2) {
            // a soft sheath round a thin bright core, both narrow at the pole
            jet(pose, vc, BH, dir * 6.5f, dir * 37.5f, 0.55f, 2.6f, sides, 0xea580c, 0);
            jet(pose, vc, BH, dir * 6.5f, dir * 37.5f, 0.20f, 0.45f, 10, 0xfdba74, 1);
            jet(pose, vc, WH, dir * 5.8f, dir * 33f, 0.80f, 3.2f, sides, 0xa8ccff, 0);
            jet(pose, vc, WH, dir * 5.8f, dir * 33f, 0.24f, 0.55f, 10, 0xe0efff, 1);
            if (detail > 0) {
                // the black hole's jets are wound tighter and the other way round from the white one's
                threads(pose, vc, eye, BH, dir * 6.5f, dir * 33f, 0.55f, 2.6f, 3.0f, dir * time * 2.6f, 0xffb36b);
                threads(pose, vc, eye, WH, dir * 5.8f, dir * 29f, 0.80f, 3.2f, -2.0f, -dir * time * 2.0f, 0xcfe3ff);
            }
        }
    }

    /** The jet's radius a fraction {@code t} of the way out. */
    private static float jetRadius(float r0, float r1, float t) {
        return r0 + (r1 - r0) * (float) Math.pow(t, 0.7);
    }

    private static void jet(PoseStack.Pose pose, VertexConsumer vc, float cx, float y0, float y1, float r0, float r1,
                            int sides, int rgb, int core) {
        int along = 24;
        for (int i = 0; i < along; i++) {
            for (int j = 0; j < sides; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corners) {
                    float t = (float) c[0] / along, u = (float) c[1] / sides;
                    float r = jetRadius(r0, r1, t), x = (float) Math.cos(u * TAU), z = (float) Math.sin(u * TAU);
                    vertex(pose, vc, cx + x * r, y0 + (y1 - y0) * t, z * r, rgb, 255, u, t, JET, core, x, 0, z);
                }
            }
        }
    }

    /** Three thin threads spiralling out along a jet. */
    private static void threads(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye, float cx, float y0, float y1,
                                float r0, float r1, float turns, float spin, int rgb) {
        int steps = 48;
        for (int k = 0; k < 3; k++) {
            Vector3f last = null;
            for (int s = 0; s <= steps; s++) {
                float t = (float) s / steps, a = TAU * (k / 3f + turns * t) - spin;
                float r = jetRadius(r0, r1, t) * 1.12f;
                Vector3f at = new Vector3f(cx + (float) Math.cos(a) * r, y0 + (y1 - y0) * t, (float) Math.sin(a) * r);
                if (last != null) {
                    float was = (float) (s - 1) / steps;
                    fadingRibbon(pose, vc, last, at, eye, 0.11f, rgb, threadAlpha(was), threadAlpha(t));
                }
                last = at;
            }
        }
    }

    private static int threadAlpha(float t) {
        return Math.round(190 * Math.min(1, t * 8) * (1 - t));
    }

    /** Small bright motes carried along the filaments, from the black hole to the white one. */
    private static void motes(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye, float time) {
        for (int i = 0; i < FILAMENTS * 5; i++) {
            int f = i % FILAMENTS;
            Vector3f[] path = FILAMENT_PATHS[f];
            float t = (i / FILAMENTS) / 5f + f * 0.037f + time * (0.16f + 0.02f * f);
            t -= (float) Math.floor(t);
            int s = Math.min((int) (t * (path.length - 1)), path.length - 3);
            int alpha = Math.round(255 * (float) Math.sin(Math.PI * t));
            fadingRibbon(pose, vc, path[s], path[s + 1], eye, 0.30f, 0xfff4e6, 0, alpha / 2);
            fadingRibbon(pose, vc, path[s + 1], path[s + 2], eye, 0.30f, 0xfff4e6, alpha / 2, alpha);
        }
    }

    /** {@link #ribbon} with its own strength at each end. */
    private static void fadingRibbon(PoseStack.Pose pose, VertexConsumer vc, Vector3f p0, Vector3f p1, Vector3f eye,
                                     float width, int rgb, int alpha0, int alpha1) {
        Vector3f side = new Vector3f(p1).sub(p0).cross(new Vector3f(eye).sub(p0));
        if (side.lengthSquared() < 1e-8f) {
            return;
        }
        side.normalize(width / 2);
        plain(pose, vc, p0.x + side.x, p0.y + side.y, p0.z + side.z, rgb, alpha0);
        plain(pose, vc, p0.x - side.x, p0.y - side.y, p0.z - side.z, rgb, alpha0);
        plain(pose, vc, p1.x - side.x, p1.y - side.y, p1.z - side.z, rgb, alpha1);
        plain(pose, vc, p1.x + side.x, p1.y + side.y, p1.z + side.z, rgb, alpha1);
    }

    /** The white hole throws its stars out (the black one keeps them inside): short streaks, fading as they go. */
    private static void sparks(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye, float time, int count) {
        for (int i = 0; i < Math.min(count, SPARKS); i++) {
            float age = SPARK_START[i] + time * SPARK_SPEED[i];
            age -= (float) Math.floor(age);
            float head = 6.3f + age * 6.5f, tail = Math.max(6.05f, head - SPARK_LENGTH[i] * (0.4f + age));
            Vector3f d = SPARK_DIR[i];
            Vector3f p0 = new Vector3f(d).mul(tail).add(WH, 0, 0), p1 = new Vector3f(d).mul(head).add(WH, 0, 0);
            Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(p1));
            if (side.lengthSquared() < 1e-8f) {
                continue;
            }
            side.normalize(0.07f);
            int rgb = i % 5 == 0 ? 0x8ccaff : i % 5 == 1 ? 0xbfdbfe : 0xf4f9ff;
            // comes out of the surface, then dies away
            int alpha = Math.round(255 * Math.min(1, age * 8) * (1 - age * age));
            plain(pose, vc, p0.x + side.x, p0.y + side.y, p0.z + side.z, rgb, 0);
            plain(pose, vc, p0.x - side.x, p0.y - side.y, p0.z - side.z, rgb, 0);
            plain(pose, vc, p1.x - side.x, p1.y - side.y, p1.z - side.z, rgb, alpha);
            plain(pose, vc, p1.x + side.x, p1.y + side.y, p1.z + side.z, rgb, alpha);
        }
    }

    /** The pinched plasma stream from the black hole through the L1 core to the white hole: a core in a sheath. */
    private static void bridge(PoseStack.Pose pose, VertexConsumer vc) {
        stream(pose, vc, 0.80f, 2.60f, 32, 0);
        stream(pose, vc, 0.22f, 0.80f, 12, 1);
    }

    private static void stream(PoseStack.Pose pose, VertexConsumer vc, float waist, float flare, int around, int core) {
        int along = 56;
        for (int i = 0; i < along; i++) {
            for (int j = 0; j < around; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corners) {
                    float t = (float) c[0] / along, u = (float) c[1] / around;
                    float r = waist + flare * (float) Math.pow(Math.abs(t - 0.5f) * 2, 1.65);
                    float y = (float) Math.cos(u * TAU), z = (float) Math.sin(u * TAU);
                    vertex(pose, vc, BH + t * (WH - BH), y * r, z * r, 0xffffff, 255, u, t, BRIDGE, core, 0, y, z);
                }
            }
        }
    }

    /** A round tube along {@code path}; coord.x runs 0..1 from its start. */
    private static void tube(PoseStack.Pose pose, VertexConsumer vc, Vector3f[] path, float radius, int sides, int filament) {
        int n = path.length - 1;
        Vector3f[][] ring = new Vector3f[path.length][sides + 1];
        for (int i = 0; i <= n; i++) {
            Vector3f dir = new Vector3f(path[Math.min(i + 1, n)]).sub(path[Math.max(i - 1, 0)]).normalize();
            Vector3f a = new Vector3f(dir).cross(0, 1, 0);
            if (a.lengthSquared() < 1e-6f) {
                a.set(dir).cross(1, 0, 0);
            }
            a.normalize();
            Vector3f b = new Vector3f(dir).cross(a);
            for (int j = 0; j <= sides; j++) {
                float ang = TAU * j / sides;
                ring[i][j] = new Vector3f(a).mul((float) Math.cos(ang) * radius)
                        .add(new Vector3f(b).mul((float) Math.sin(ang) * radius)).add(path[i]);
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < sides; j++) {
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corners) {
                    Vector3f v = ring[c[0]][c[1]];
                    vertex(pose, vc, v.x, v.y, v.z, 0xffffff, 255, (float) c[0] / n, 0, FILAMENT, filament, 0, 1, 0);
                }
            }
        }
    }

    /** A line of the given width that turns its face to the viewer. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer vc, Vector3f p0, Vector3f p1, Vector3f eye, float width,
                               int rgb, int alpha) {
        Vector3f side = new Vector3f(p1).sub(p0).cross(new Vector3f(eye).sub(p0));
        if (side.lengthSquared() < 1e-8f) {
            return;
        }
        side.normalize(width / 2);
        plain(pose, vc, p0.x + side.x, p0.y + side.y, p0.z + side.z, rgb, alpha);
        plain(pose, vc, p0.x - side.x, p0.y - side.y, p0.z - side.z, rgb, alpha);
        plain(pose, vc, p1.x - side.x, p1.y - side.y, p1.z - side.z, rgb, alpha);
        plain(pose, vc, p1.x + side.x, p1.y + side.y, p1.z + side.z, rgb, alpha);
    }

    /** The draft's data packets, as short bright streaks along their arcs. */
    private static void packets(PoseStack.Pose pose, VertexConsumer vc, Vector3f eye, float time) {
        float speed = 0.6f + LOAD * 1.4f;
        for (int i = 0; i < PACKETS; i++) {
            float t = PACKET_START[i] + time * PACKET_SPEED[i] * speed;
            t -= (float) Math.floor(t);
            Vector3f[] arc = ARCS[i % ARCS.length];
            int s = Math.min((int) (t * ARC_STEPS), ARC_STEPS - 1);
            float f = t * ARC_STEPS - s;
            Vector3f from = new Vector3f(arc[s]).lerp(arc[s + 1], f);
            Vector3f dir = new Vector3f(arc[s + 1]).sub(arc[s]).normalize(1.6f);
            ribbon(pose, vc, from, new Vector3f(from).add(dir), eye, 0.3f, 0x7dd3fc, 220);
        }
    }

    /** The data arcs out of the L1 core: smooth curves through three points each, ending on a ring's core. */
    private static Vector3f[][] arcs() {
        Vector3f[][] out = new Vector3f[16][ARC_STEPS + 1];
        for (int i = 0; i < out.length; i++) {
            float ang = TAU * i / 16, r = HubShape.ringPoint(i % 4, 0)[0], apex = (i % 2 == 0 ? 1 : -1) * (20 + (i % 3) * 8);
            Vector3f p0 = new Vector3f();
            Vector3f p1 = new Vector3f((float) Math.cos(ang) * r * 0.45f, apex, (float) Math.sin(ang) * r * 0.45f);
            int[] end = HubShape.ringPoint(i % 4, ang + 0.35f);
            Vector3f p2 = new Vector3f(end[0], end[1], end[2]);
            Vector3f before = new Vector3f(p0).mul(2).sub(p1), after = new Vector3f(p2).mul(2).sub(p1);
            for (int s = 0; s <= ARC_STEPS; s++) {
                float t = 2f * s / ARC_STEPS;
                out[i][s] = t <= 1 ? catmull(before, p0, p1, p2, t) : catmull(p0, p1, p2, after, t - 1);
            }
        }
        return out;
    }

    private static Vector3f catmull(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float t) {
        float t2 = t * t, t3 = t2 * t;
        return new Vector3f(b).mul(2)
                .add(new Vector3f(c).sub(a).mul(t))
                .add(new Vector3f(a).mul(2).sub(new Vector3f(b).mul(5)).add(new Vector3f(c).mul(4)).sub(d).mul(t2))
                .add(new Vector3f(b).sub(c).mul(3).add(d).sub(a).mul(t3))
                .mul(0.5f);
    }

    private static Vector3f[][] filaments() {
        int steps = 72;
        Vector3f[][] out = new Vector3f[FILAMENTS][steps + 1];
        for (int f = 0; f < FILAMENTS; f++) {
            float start = TAU * f / FILAMENTS;
            int turn = f % 2 == 0 ? 1 : -1;
            for (int s = 0; s <= steps; s++) {
                float t = (float) s / steps;
                float r = 0.85f + 3.1f * (float) Math.pow(Math.abs(t - 0.5f) * 2, 1.35) + (f % 2) * 0.35f;
                float a = start + t * (float) Math.PI * 4.5f * turn;
                out[f][s] = new Vector3f(BH + t * (WH - BH), (float) Math.sin(a) * r, (float) Math.cos(a) * r);
            }
        }
        return out;
    }

    @Override
    public boolean shouldRenderOffScreen(TwinStarHubBlockEntity be) {
        return true;   // the controller is 60 below the middle and usually out of view
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public AABB getRenderBoundingBox(TwinStarHubBlockEntity be) {
        BlockPos p = be.getBlockPos();
        int r = HubShape.R;
        return new AABB(p.getX() - r, p.getY(), p.getZ() - r, p.getX() + r + 1, p.getY() + HubShape.H, p.getZ() + r + 1);
    }
}
