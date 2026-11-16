package com.lumina.ae2_gvt.client.renderer;

import org.joml.Vector3f;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.HexagramHubClient;
import com.lumina.ae2_gvt.client.TwinHoleRenderTypes;
import com.lumina.ae2_gvt.client.mesh.TwinHoleMesh;
import com.lumina.ae2_gvt.hub.HexagramHub;
import com.lumina.ae2_gvt.hub.TwinStarHubBlock;
import com.lumina.ae2_gvt.hub.TwinStarHubBlockEntity;
import com.lumina.lmb.client.ClientStructures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The formed hexagram hub's sphere, drawn from the controller (design 2026-10-27): a slowly turning
 * universe (only the sphere's far wall, so what is inside stays in front of it) with the black and
 * the white hole orbiting each other in the middle, each trailing a tail that spirals out behind it -
 * the black tail dark, the white one glowing. Nothing of that is drawn until the lib reports the hub formed.
 */
public class HexagramHubRenderer implements BlockEntityRenderer<TwinStarHubBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/prototype/white.png");
    private static final RenderType DARK_TAIL = RenderType.entityTranslucent(WHITE);
    private static final RenderType BRIGHT_TAIL = RenderType.entityTranslucentEmissive(WHITE);

    // the ring blocks' nearest corners are 14.85 from the centre: stay clear of them
    private static final float SPHERE = HexagramHub.SPHERE_RADIUS - 0.25f;
    private static final float HOLE = 1.8f, ORBIT = 3.6f;                   // hole radius, centre distance from the middle
    private static final int TAIL_STEPS = 64;
    private static final float TAIL_TURN = 3.9f, TAIL_GROWTH = 7.0f, TAIL_WIDTH = 1.5f;   // radians behind, extra radius, width at the hole

    @Override
    public void render(TwinStarHubBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float t = (float) ((be.getLevel().getGameTime() + partialTick) / 20.0);
        if (!formed(be)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + HexagramHub.SPHERE_ABOVE_CONTROLLER, 0.5);
        PoseStack.Pose pose = poseStack.last();

        cosmos(pose, buffers.getBuffer(HexagramHubClient.COSMOS), SPHERE, 32, 64);

        // orbit plane: tilted 25 degrees, the tilt itself slowly going round
        float tilt = (float) Math.toRadians(25), precess = t * 0.05f;
        Vector3f normal = new Vector3f((float) (Math.sin(tilt) * Math.cos(precess)), (float) Math.cos(tilt), (float) (Math.sin(tilt) * Math.sin(precess)));
        Vector3f e1 = new Vector3f(normal).cross(0, 0, 1).normalize();
        Vector3f e2 = new Vector3f(normal).cross(e1).normalize();
        float angle = t * 0.9f;   // about one orbit every 7 s
        Vector3f axis = along(e1, e2, angle, 1);   // white -> black, through the middle

        TwinHoleMesh.draw(pose, buffers.getBuffer(TwinHoleRenderTypes.TWIN_HOLE), axis, 2 * ORBIT, HOLE, 0, 24, 40);
        tail(pose, buffers.getBuffer(DARK_TAIL), e1, e2, normal, angle, 0x0c0a16, 235);
        tail(pose, buffers.getBuffer(BRIGHT_TAIL), e1, e2, normal, angle + (float) Math.PI, 0xffffff, 255);
        poseStack.popPose();
    }

    private static boolean formed(TwinStarHubBlockEntity be) {
        BlockState look = ClientStructures.lookAt(be.getBlockPos(), be.getBlockState());
        return look != null && look.hasProperty(TwinStarHubBlock.FORMED) && look.getValue(TwinStarHubBlock.FORMED);
    }

    private static Vector3f along(Vector3f e1, Vector3f e2, float angle, float radius) {
        return new Vector3f(e1).mul((float) Math.cos(angle) * radius).add(new Vector3f(e2).mul((float) Math.sin(angle) * radius));
    }

    /**
     * The sphere wound inwards: with back faces culled only its far wall shows. Normals point outwards
     * and give the cosmos shader its rim.
     */
    private static void cosmos(PoseStack.Pose pose, VertexConsumer vc, float r, int lats, int lons) {
        float[] dirs = sphere(lats, lons);
        int row = (lons + 1) * 3;
        for (int i = 0; i < lats; i++) {
            for (int j = 0; j < lons; j++) {
                // a (i, j), b (i + 1, j), c (i + 1, j + 1), d (i, j + 1)
                int a = i * row + j * 3, b = a + row, c = b + 3, d = a + 3;
                cosmosVertex(pose, vc, dirs, a, r);
                cosmosVertex(pose, vc, dirs, b, r);
                cosmosVertex(pose, vc, dirs, c, r);
                cosmosVertex(pose, vc, dirs, d, r);
            }
        }
    }

    private static void cosmosVertex(PoseStack.Pose pose, VertexConsumer vc, float[] dirs, int at, float r) {
        float x = dirs[at], y = dirs[at + 1], z = dirs[at + 2];
        vc.addVertex(pose, x * r, y * r, z * r).setColor(255, 255, 255, 255).setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, x, y, z);
    }

    private static float[] sphereDirs;
    private static int sphereLats, sphereLons;

    /** The unit directions of the sphere's grid, worked out once: (lats + 1) rows of (lons + 1) points, three floats each. */
    private static synchronized float[] sphere(int lats, int lons) {
        if (sphereDirs == null || sphereLats != lats || sphereLons != lons) {
            float[] out = new float[(lats + 1) * (lons + 1) * 3];
            int n = 0;
            for (int i = 0; i <= lats; i++) {
                for (int j = 0; j <= lons; j++) {
                    double th = Math.PI * i / lats, ph = 2 * Math.PI * j / lons;
                    out[n++] = (float) (Math.sin(th) * Math.cos(ph));
                    out[n++] = (float) Math.cos(th);
                    out[n++] = (float) (Math.sin(th) * Math.sin(ph));
                }
            }
            sphereDirs = out;
            sphereLats = lats;
            sphereLons = lons;
        }
        return sphereDirs;
    }

    /**
     * A tail behind the hole now at {@code angle}: it follows the orbit backwards, spiralling outwards,
     * thinning and fading. Two crossed ribbons (in the orbit plane and across it) so it reads from any side.
     */
    private static void tail(PoseStack.Pose pose, VertexConsumer vc, Vector3f e1, Vector3f e2, Vector3f normal, float angle, int rgb, int alpha) {
        for (int i = 0; i < TAIL_STEPS; i++) {
            float f0 = (float) i / TAIL_STEPS, f1 = (float) (i + 1) / TAIL_STEPS;
            Vector3f p0 = point(e1, e2, angle, f0), p1 = point(e1, e2, angle, f1);
            Vector3f out0 = new Vector3f(p0).normalize(), out1 = new Vector3f(p1).normalize();
            float w0 = TAIL_WIDTH * (1 - f0), w1 = TAIL_WIDTH * (1 - f1);
            int a0 = Math.round(alpha * (1 - f0) * (1 - f0)), a1 = Math.round(alpha * (1 - f1) * (1 - f1));
            ribbon(pose, vc, p0, p1, out0, out1, w0, w1, rgb, a0, a1, normal);
            ribbon(pose, vc, p0, p1, normal, normal, w0, w1, rgb, a0, a1, out0);
        }
    }

    /** Point on the tail, {@code f} = 0 at the hole .. 1 at the tip. */
    private static Vector3f point(Vector3f e1, Vector3f e2, float angle, float f) {
        return along(e1, e2, angle - f * TAIL_TURN, ORBIT + f * f * TAIL_GROWTH);
    }

    private static void ribbon(PoseStack.Pose pose, VertexConsumer vc, Vector3f p0, Vector3f p1, Vector3f side0, Vector3f side1,
                               float w0, float w1, int rgb, int a0, int a1, Vector3f n) {
        vertex(pose, vc, new Vector3f(side0).mul(w0 / 2).add(p0), rgb, a0, n);
        vertex(pose, vc, new Vector3f(side0).mul(-w0 / 2).add(p0), rgb, a0, n);
        vertex(pose, vc, new Vector3f(side1).mul(-w1 / 2).add(p1), rgb, a1, n);
        vertex(pose, vc, new Vector3f(side1).mul(w1 / 2).add(p1), rgb, a1, n);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f p, int rgb, int a, Vector3f n) {
        vc.addVertex(pose, p.x, p.y, p.z).setColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, a).setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, n.x, n.y, n.z);
    }

    @Override
    public boolean shouldRenderOffScreen(TwinStarHubBlockEntity be) {
        return true;   // the sphere is 30 blocks across; the controller is usually out of view
    }

    @Override
    public int getViewDistance() {
        return 192;
    }

    @Override
    public AABB getRenderBoundingBox(TwinStarHubBlockEntity be) {
        BlockPos p = be.getBlockPos();
        double cy = p.getY() + 0.5 + HexagramHub.SPHERE_ABOVE_CONTROLLER, r = HexagramHub.SPHERE_RADIUS + 1;
        return new AABB(p.getX() + 0.5 - r, cy - r, p.getZ() + 0.5 - r, p.getX() + 0.5 + r, cy + r, p.getZ() + 0.5 + r);
    }
}
