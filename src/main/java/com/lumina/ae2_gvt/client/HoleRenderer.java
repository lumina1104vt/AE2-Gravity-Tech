package com.lumina.ae2_gvt.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.hole.GravityHoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.lumina.ae2_gvt.AE2GravityTech;

/**
 * Draws every black / white hole block near the camera (in the middle of its cell), after the world (translucent particles) so the
 * lensing has the finished scene to bend:
 * <ol>
 *   <li>copy the frame so far ({@link SceneSnapshot}) - again for every hole, far to near, so a
 *       nearer hole bends the farther ones</li>
 *   <li>lens panel: the world behind each hole redrawn bent round it - an Einstein ring for the
 *       black hole, the opposite (light pushed out, a shrunken view) for the white hole</li>
 *   <li>the hole: a starfield sphere (inverted for the white hole)</li>
 *   <li>the accretion disc, tilted per hole: matter glowing as it spirals in, or dark matter
 *       thrown out (opposite spin)</li>
 *   <li>its small gravitational-wave ripple ({@link HoleRipples}), over another copy</li>
 * </ol>
 * Then the big ripples of holes blown up.
 * Vertex colour flags (red = white hole, green = disc layer) and UV0 (position in hole radii) carry
 * the per-vertex data to the shaders.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class HoleRenderer {
    /** Lens panel half-size and accretion disc radii, in hole radii. */
    static final float LENS = 7f, DISC_IN = 1.25f, DISC_OUT = 2.7f;
    /** By the client's hole detail (low, medium, high): how far a hole is drawn, the mesh of its body and disc, and how many at once. */
    private static final double[] RANGES = {48, 72, 96};
    private static final int[] LAT_STEPS = {10, 16, 24}, LON_STEPS = {14, 22, 32}, DISC_STEPS = {24, 40, 64};
    /** Holes drawn per frame: the bending costs by the screen area a hole covers, the small ripple of the highest detail as much again. */
    private static final int[] MAX_BY_DETAIL = {16, 8, 8};
    private static double range = RANGES[2];
    private static int LATS = 24, LONS = 32, DISC_SEGMENTS = 64, MAX_HOLES = 8;

    private HoleRenderer() {
    }

    @SubscribeEvent
    static void afterParticles(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            LevelPass.run(event, () -> draw(event));
    }

    private static void draw(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int detail = com.lumina.ae2_gvt.config.ClientConfig.HOLE_DETAIL.get();
        range = RANGES[detail];
        LATS = LAT_STEPS[detail];
        LONS = LON_STEPS[detail];
        DISC_SEGMENTS = DISC_STEPS[detail];
        MAX_HOLES = MAX_BY_DETAIL[detail];
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        List<GravityHoleBlockEntity> holes = new ArrayList<>();
        for (GravityHoleBlockEntity hole : GravityHoleBlockEntity.CLIENT_HOLES) {
            if (hole.getLevel() == mc.level && !hole.isRemoved() && centre(hole).distanceToSqr(cam) < range * range) {
                holes.add(hole);
            }
        }
        MultiBufferSource.BufferSource buffers = SceneSnapshot.BUFFERS;
        double now = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true);
        if (holes.isEmpty()) {
            HoleRipples.bursts(buffers, cam, now);
            return;
        }
        // far to near, each bent from the frame as it is then: a nearer hole bends the farther ones (their discs, rings and
        // all) like the rest of the scene. Only the nearest few are drawn.
        holes.sort(java.util.Comparator.comparingDouble((GravityHoleBlockEntity h) -> centre(h).distanceToSqr(cam)).reversed());
        if (holes.size() > MAX_HOLES) {
            holes = holes.subList(holes.size() - MAX_HOLES, holes.size());
        }
        PoseStack.Pose pose = new PoseStack().last();
        Vector3f right = new Vector3f(camera.getLeftVector()).negate();
        Vector3f up = new Vector3f(camera.getUpVector());
        float r = GravityHole.RADIUS;
        boolean bends = detail > 0;
        for (GravityHoleBlockEntity hole : holes) {
            Vec3 at = centre(hole).subtract(cam);
            Vector3f c = new Vector3f((float) at.x, (float) at.y, (float) at.z);
            int white = hole.isWhite() ? 255 : 0;

            // the lowest detail bends nothing: only the rings, over the scene. Otherwise the bent picture is made over the part of
            // the screen the panel covers, reading the frame where it stands, and laid over the frame (if the frame cannot be read
            // - another mod's framebuffer - the lens is left out)
            int[] size = LensPass.viewport();
            if (!bends || LensPass.begin(mc.getMainRenderTarget(), LevelPass.screenRect(c, right, up, LENS * r, size[0], size[1]))) {
                RenderType lensType = bends ? HoleRenderTypes.LENS : HoleRenderTypes.LENS_FLAT;
                VertexConsumer lens = buffers.getBuffer(lensType);
                for (int[] k : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                    Vector3f p = new Vector3f(c).add(new Vector3f(right).mul(k[0] * LENS * r)).add(new Vector3f(up).mul(k[1] * LENS * r));
                    vertex(lens, pose, p, k[0] * LENS, k[1] * LENS, white, bends ? 0 : 255, 0, 0, 1);
                }
                buffers.endBatch(lensType);
                if (bends)
                    LensPass.end();
            }

            sphere(buffers.getBuffer(HoleRenderTypes.BODY), pose, c, r, white);
            buffers.endBatch(HoleRenderTypes.BODY);

            disc(buffers.getBuffer(HoleRenderTypes.DISC), pose, c, r, white, tilt(hole));
            buffers.endBatch(HoleRenderTypes.DISC);

            // the small ripple warps the frame with the hole in it (only at the highest detail)
            if (detail == 2)
                HoleRipples.ambient(buffers, cam, hole, now, right, up);
        }
        HoleRipples.bursts(buffers, cam, now);
    }

    /**
     * The selection box of a hole is a wireframe drawn before the hole is, so the lens copied it with the rest of the frame and bent it
     * (a cube of lines warped around the hole); a hole is not selected visibly at all.
     */
    @SubscribeEvent
    static void noHoleOutline(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockState(event.getTarget().getBlockPos()).getBlock() instanceof com.lumina.ae2_gvt.hole.GravityHoleBlock)
            event.setCanceled(true);
    }

    /** The white flash of an 湮滅 TNT, over everything. */
    @SubscribeEvent
    static void flash(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float white = HoleRipples.flash(mc.gameRenderer.getMainCamera().getPosition(), mc.level.getGameTime() + partial);
        if (white > 0)
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(),
                    (int) (white * 255) << 24 | 0xFFFFFF);
    }

    /** The middle of the hole's block. */
    private static Vec3 centre(GravityHoleBlockEntity hole) {
        return net.minecraft.world.phys.Vec3.atCenterOf(hole.getBlockPos());
    }

    /** Each hole's disc leans its own way (fixed by where it is). */
    private static Quaternionf tilt(GravityHoleBlockEntity hole) {
        int id = Math.floorMod(hole.getBlockPos().hashCode(), 100_000);
        return new Quaternionf().rotateY((float) Math.toRadians((id * 47) % 360)).rotateX((float) Math.toRadians(18 + (id * 13) % 22));
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, float r, int white) {
        for (int i = 0; i < LATS; i++) {
            for (int j = 0; j < LONS; j++) {
                // counter-clockwise seen from outside
                sphereVertex(vc, pose, c, r, i, j, white);
                sphereVertex(vc, pose, c, r, i, j + 1, white);
                sphereVertex(vc, pose, c, r, i + 1, j + 1, white);
                sphereVertex(vc, pose, c, r, i + 1, j, white);
            }
        }
    }

    private static void sphereVertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, float r, int i, int j, int white) {
        double theta = Math.PI * i / LATS, phi = 2 * Math.PI * j / LONS;
        float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
        vertex(vc, pose, new Vector3f(c).add(x * r, y * r, z * r), 0, 0, white, 0, x, y, z);
    }

    /** Flat annulus in the hole's tilted plane; UV0 = position in that plane, in hole radii. */
    private static void disc(VertexConsumer vc, PoseStack.Pose pose, Vector3f c, float r, int white, Quaternionf tilt) {
        Vector3f n = tilt.transform(new Vector3f(0, 1, 0));
        for (int s = 0; s < DISC_SEGMENTS; s++) {
            double a0 = 2 * Math.PI * s / DISC_SEGMENTS, a1 = 2 * Math.PI * (s + 1) / DISC_SEGMENTS;
            float[][] corners = {{DISC_IN, (float) a0}, {DISC_OUT, (float) a0}, {DISC_OUT, (float) a1}, {DISC_IN, (float) a1}};
            for (float[] k : corners) {
                float u = (float) (k[0] * Math.cos(k[1])), v = (float) (k[0] * Math.sin(k[1]));
                Vector3f p = tilt.transform(new Vector3f(u * r, 0, v * r)).add(c);
                vertex(vc, pose, p, u, v, white, 255, n.x, n.y, n.z);
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, float u, float v, int white, int disc,
                               float nx, float ny, float nz) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(white, disc, 0, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(nx, ny, nz);
    }
}
