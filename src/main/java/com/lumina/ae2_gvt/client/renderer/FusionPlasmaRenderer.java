package com.lumina.ae2_gvt.client.renderer;

import com.lumina.ae2_gvt.client.FusionActivity;
import com.lumina.ae2_gvt.client.FusionPlasmaRenderTypes;

import com.lumina.ae2_gvt.blocks.FusionController;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.structure.FusionStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the fusion plasma - a small star whose colour follows its temperature - and its corona.
 * The surface itself is the shader's work (fusion_plasma.fsh); this builds the geometry and picks
 * the colour. Drawn at the centre of the formed chamber, inside the containment shell
 * ({@link #SHELL}, transparent; it ripples where an eruption hits it). The prominences and
 * eruptions are {@link FusionActivity}.
 */
public class FusionPlasmaRenderer implements BlockEntityRenderer<FusionControllerBlockEntity> {
    public static final float RADIUS = 1.5f;
    static final float CORONA = 1.6f;          // corona shell radius / star radius
    private static final int LATS = 32, LONS = 48;
    /** Containment shell radius: inside the constrainers (their faces are 2.5 from the centre). */
    public static final float SHELL = 2.42f;

    /** Exaggerated temperature colours (Kelvin, rgb): red -> orange -> yellow -> white -> blue -> violet. */
    private static final double[][] PATH = {
            {1500, 200, 30, 10}, {3000, 255, 90, 20}, {5000, 255, 190, 60}, {8000, 255, 245, 210},
            {12000, 150, 220, 255}, {20000, 60, 120, 255}, {40000, 110, 50, 255}, {80000, 170, 40, 255}};

    @Override
    public void render(FusionControllerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!be.getBlockState().getValue(FusionController.FORMED) || be.chamber() == null) {
            return;
        }
        int rgb = colour(be.temperature());
        Vec3 centre = centre(be);
        double time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() + partialTick) / 20.0;
        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        PoseStack.Pose pose = poseStack.last();
        sphere(pose, buffers.getBuffer(FusionPlasmaRenderTypes.STAR), RADIUS, rgb, 0, 1);
        sphere(pose, buffers.getBuffer(FusionPlasmaRenderTypes.CORONA), RADIUS * CORONA, rgb, 1, CORONA);
        // the camera in the star's frame, for ribbons and blobs that face it
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 eye = cam.subtract(Vec3.atLowerCornerOf(be.getBlockPos()).add(centre));
        long seed = be.getBlockPos().asLong();
        FusionActivity.Eruption e = FusionActivity.eruption(time, seed);
        shell(pose, buffers.getBuffer(FusionPlasmaRenderTypes.SHELL), rgb, e);
        VertexConsumer flare = buffers.getBuffer(FusionPlasmaRenderTypes.FLARE);
        FusionActivity.prominences(pose, flare, rgb, time, seed, eye);
        FusionActivity.eruptionCloud(pose, flare, rgb, e, seed, eye);
        poseStack.popPose();
    }

    /** The shell: UV0.y = angle from the hit point, alpha = the hit's age (0 = no hit). */
    private static void shell(PoseStack.Pose pose, VertexConsumer vc, int rgb, FusionActivity.Eruption e) {
        int age = e.hitAge() > 0 && e.hitAge() < 1 ? (int) Math.round(e.hitAge() * 254) + 1 : 0;
        for (int i = 0; i < LATS; i++) {
            for (int j = 0; j < LONS; j++) {
                for (int[] k : new int[][] {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}}) {
                    double theta = Math.PI * k[0] / LATS, phi = 2 * Math.PI * k[1] / LONS;
                    float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
                    float angle = (float) Math.acos(Math.max(-1, Math.min(1, x * e.dir().x + y * e.dir().y + z * e.dir().z)));
                    vc.addVertex(pose, x * SHELL, y * SHELL, z * SHELL)
                            .setColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, age)
                            .setUv(2, angle)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(x, y, z);
                }
            }
        }
    }


    /** UV0 carries the layer (0 star, 1 corona) and the shell scale; the normal is the raw direction. */
    private static void sphere(PoseStack.Pose pose, VertexConsumer vc, float r, int rgb, float layer, float scale) {
        for (int i = 0; i < LATS; i++) {
            for (int j = 0; j < LONS; j++) {
                if (layer == 0) {
                    // star: counter-clockwise seen from outside, back-face culling keeps the near side
                    vertex(pose, vc, i, j, r, rgb, layer, scale);
                    vertex(pose, vc, i, j + 1, r, rgb, layer, scale);
                    vertex(pose, vc, i + 1, j + 1, r, rgb, layer, scale);
                    vertex(pose, vc, i + 1, j, r, rgb, layer, scale);
                } else {
                    // corona: wound the other way, so only the far half of the shell is drawn - the
                    // star hides it over the disc by depth, so its inner edge always meets the limb
                    // exactly (a shader-computed cut drifted off the star with view bobbing)
                    vertex(pose, vc, i, j, r, rgb, layer, scale);
                    vertex(pose, vc, i + 1, j, r, rgb, layer, scale);
                    vertex(pose, vc, i + 1, j + 1, r, rgb, layer, scale);
                    vertex(pose, vc, i, j + 1, r, rgb, layer, scale);
                }
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int i, int j, float r, int rgb, float layer, float scale) {
        double theta = Math.PI * i / LATS, phi = 2 * Math.PI * j / LONS;
        float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
        vc.addVertex(pose, x * r, y * r, z * r)
                .setColor((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, 255)
                .setUv(layer, scale)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(x, y, z);
    }

    /** The star's colour for a temperature, along {@link #PATH} on a log scale. */
    public static int colour(double kelvin) {
        double k = Math.max(PATH[0][0], Math.min(PATH[PATH.length - 1][0], kelvin));
        for (int i = 0; i < PATH.length - 1; i++) {
            double[] a = PATH[i], b = PATH[i + 1];
            if (k <= b[0]) {
                double t = (Math.log(k) - Math.log(a[0])) / (Math.log(b[0]) - Math.log(a[0]));
                int r = (int) Math.round(a[1] + (b[1] - a[1]) * t);
                int g = (int) Math.round(a[2] + (b[2] - a[2]) * t);
                int bl = (int) Math.round(a[3] + (b[3] - a[3]) * t);
                return (r << 16) | (g << 8) | bl;
            }
        }
        double[] last = PATH[PATH.length - 1];
        return ((int) last[1] << 16) | ((int) last[2] << 8) | (int) last[3];
    }

    /** The chamber's centre relative to the controller (its own position while not formed). */
    private static Vec3 centre(FusionControllerBlockEntity be) {
        BlockPos min = be.chamber();
        if (min == null) {
            return new Vec3(0.5, 0.5, 0.5);
        }
        double half = FusionStructure.SIZE / 2.0;
        return Vec3.atLowerCornerOf(min.subtract(be.getBlockPos())).add(half, half, half);
    }

    @Override
    public boolean shouldRenderOffScreen(FusionControllerBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(FusionControllerBlockEntity be) {
        Vec3 c = Vec3.atLowerCornerOf(be.getBlockPos()).add(centre(be));
        double reach = SHELL + 0.5;
        return new AABB(c.x - reach, c.y - reach, c.z - reach, c.x + reach, c.y + reach, c.z + reach)
                .minmax(new AABB(be.getBlockPos()));
    }
}
