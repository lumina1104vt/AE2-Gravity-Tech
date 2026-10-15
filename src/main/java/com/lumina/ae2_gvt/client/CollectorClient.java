package com.lumina.ae2_gvt.client;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.blockentities.GravitonCollector;
import org.joml.Vector3f;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.hole.client.SceneSnapshot;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The graviton collector's space distortion (gravitons themselves are invisible): inside the open
 * frame and the glass chamber the world behind swirls toward the core and is magnified near it,
 * with rings running inward. Done like the holes' lensing - after the world, a copy of the frame
 * ({@link SceneSnapshot}) is redrawn bent on a camera-facing panel at each core; depth-tested, so
 * the pillars and glass in front stay put. Shader: collector_lens.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class CollectorClient {
    private static final RenderPipeline LENS_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET,
                    RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "pipeline/collector_lens"))
            .withVertexShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/collector_lens"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "core/collector_lens"))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    private static final RenderType LENS = RenderType.create("ae2_gvt_collector_lens", RenderSetup.builder(LENS_PIPELINE)
            .withTexture("Sampler0", SceneSnapshot.ID, () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .createRenderSetup());

    /** The two distortion regions, from the base block's corner: centre y, half width, half height (blocks). */
    private static final float[][] REGIONS = {{15.5f / 16, 0.45f, 0.75f}, {57f / 16, 0.3f, 0.32f}};
    private static final double RANGE = 64;
    /** Towers drawn per frame (each costs a copy of the frame). */
    private static final int MAX = 6;

    private CollectorClient() {
    }

    @SubscribeEvent
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(LENS_PIPELINE);
    }

    @SubscribeEvent
    static void afterWorld(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        List<GravitonCollector> towers = new ArrayList<>();
        for (GravitonCollector be : GravitonCollector.loadedOnClient()) {
            if (be.getLevel() == mc.level && !be.isRemoved() && be.getBlockPos().getCenter().distanceToSqr(cam) < RANGE * RANGE) {
                towers.add(be);
            }
        }
        if (towers.isEmpty()) {
            return;
        }
        // far to near, each with a fresh copy of the frame, so a nearer tower bends the farther ones
        towers.sort(java.util.Comparator.comparingDouble((GravitonCollector be) -> be.getBlockPos().getCenter().distanceToSqr(cam)).reversed());
        if (towers.size() > MAX) {
            towers = towers.subList(towers.size() - MAX, towers.size());
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack.Pose pose = new PoseStack().last();
        Vector3f right = new Vector3f(camera.leftVector()).negate();
        Vector3f up = new Vector3f(camera.upVector());
        for (GravitonCollector be : towers) {
            SceneSnapshot.capture(mc.getMainRenderTarget());
            VertexConsumer vc = buffers.getBuffer(LENS);
            for (float[] r : REGIONS) {
                Vector3f c = new Vector3f((float) (be.getBlockPos().getX() + 0.5 - cam.x), (float) (be.getBlockPos().getY() + r[0] - cam.y),
                        (float) (be.getBlockPos().getZ() + 0.5 - cam.z));
                for (int[] k : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                    Vector3f p = new Vector3f(c).add(new Vector3f(right).mul(k[0] * r[1])).add(new Vector3f(up).mul(k[1] * r[2]));
                    vc.addVertex(pose, p.x, p.y, p.z)
                            .setColor(255, 255, 255, 255)
                            .setUv(k[0], k[1])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(0, 0, 1);
                }
            }
            buffers.endBatch(LENS);
        }
    }
}
