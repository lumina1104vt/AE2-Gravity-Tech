package com.lumina.ae2_gvt.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.rift.SpaceRift;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * What the eye sees of torn space: the tear itself (an irregular slit of stars with cracks branching
 * off it, turned to the viewer round its upright axis, bending the world round its edges; shader
 * space_rift), the broken pieces of space adrift round it, and the sky of 破碎星空 (stars crossed by the
 * cracks of a broken sky; shader starfield_sky).
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class RiftClient {
    private static final RenderType TEAR = RenderType.create("ae2_gvt_space_rift", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.spaceRift))
                    .setTextureState(SceneSnapshot.TEXTURE)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** Draws one face of the sky into its texture. */
    private static final RenderType SKY_BAKE = RenderType.create("ae2_gvt_starfield_bake", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.starfieldBake))
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** Pixels along a side of a baked sky face. */
    private static final int SKY_FACE = 1536;
    /** The sky's six faces, baked the first time it is seen: the full shader for every pixel of every frame cost most of the frame. */
    private static TextureTarget[] skyFaces;
    private static final RenderType[] SKY = new RenderType[6];

    static {
        for (int i = 0; i < SKY.length; i++) {
            int face = i;
            SKY[i] = RenderType.create("ae2_gvt_starfield_sky_" + i, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
                    false, false, RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.starfieldSky))
                            .setTextureState(new RenderStateShard.EmptyTextureStateShard(
                                    () -> RenderSystem.setShaderTexture(0, skyFaces[face].getColorTextureId()), () -> {
                                    }))
                            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .createCompositeState(false));
        }
    }

    /** The shards: starry like the holes, seen from every side. */
    private static final RenderType SHARD = RenderType.create("ae2_gvt_rift_shard", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 2048,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientShaders.holeBody))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** Half the tear's panel: width, height (blocks). */
    private static final float WIDE = 3.5f, TALL = 4.5f;
    private static final int SHARDS = 48;
    /** The four faces of a shard, by corner. */
    private static final int[][] FACES = {{0, 1, 2}, {0, 3, 1}, {0, 2, 3}, {1, 3, 2}};
    private static final double RANGE = 128;
    /** Tears drawn per frame (each costs a copy of the frame). */
    private static final int MAX = 4;
    private static final float[][][] CUBE = {
            {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}}, {{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}},
            {{-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}}, {{1, -1, -1}, {1, -1, 1}, {1, 1, 1}, {1, 1, -1}},
            {{-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1}}, {{-1, 1, -1}, {1, 1, -1}, {1, 1, 1}, {-1, 1, 1}}};
    /** Where a face's corners are in its texture. */
    private static final float[][] FACE_UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    private RiftClient() {
    }

    @SubscribeEvent
    static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "shattered_starfield"), new StarfieldEffects());
    }

    private static final class StarfieldEffects extends DimensionSpecialEffects {
        StarfieldEffects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return Vec3.ZERO;
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }

        @Override
        public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                                 Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            Matrix4fStack view = RenderSystem.getModelViewStack();
            view.pushMatrix();
            view.mul(modelViewMatrix);
            RenderSystem.applyModelViewMatrix();
            MultiBufferSource.BufferSource buffers = SceneSnapshot.BUFFERS;
            if (skyFaces == null)
                bakeSky(buffers);
            for (int i = 0; i < CUBE.length; i++) {
                face(buffers.getBuffer(SKY[i]), CUBE[i]);
                buffers.endBatch(SKY[i]);
            }
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            return true;
        }
    }

    private static void face(VertexConsumer vc, float[][] corners) {
        for (int k = 0; k < corners.length; k++)
            vc.addVertex(corners[k][0] * 50, corners[k][1] * 50, corners[k][2] * 50)
                    .setColor(255, 255, 255, 255).setUv(FACE_UV[k][0], FACE_UV[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private static void bakeSky(MultiBufferSource.BufferSource buffers) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        skyFaces = new TextureTarget[CUBE.length];
        for (int i = 0; i < CUBE.length; i++) {
            skyFaces[i] = new TextureTarget(SKY_FACE, SKY_FACE, false, Minecraft.ON_OSX);
            skyFaces[i].setFilterMode(GL11.GL_LINEAR);
            skyFaces[i].bindWrite(true);
            face(buffers.getBuffer(SKY_BAKE), CUBE[i]);
            buffers.endBatch(SKY_BAKE);
        }
        if (bound == main.frameBufferId) {
            main.bindWrite(false);
        } else {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
        }
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    @SubscribeEvent
    static void afterParticles(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Vec3 cam = event.getCamera().getPosition();
        List<SpaceRift.RiftEntity> rifts = new ArrayList<>();
        for (SpaceRift.RiftEntity rift : SpaceRift.RiftEntity.CLIENT)
            if (rift.getLevel() == mc.level && !rift.isRemoved() && Vec3.atCenterOf(rift.getBlockPos()).distanceToSqr(cam) < RANGE * RANGE)
                rifts.add(rift);
        if (rifts.isEmpty())
            return;
        rifts.sort(java.util.Comparator.comparingDouble((SpaceRift.RiftEntity r) -> Vec3.atCenterOf(r.getBlockPos()).distanceToSqr(cam)).reversed());
        if (rifts.size() > MAX)
            rifts = rifts.subList(rifts.size() - MAX, rifts.size());
        MultiBufferSource.BufferSource buffers = SceneSnapshot.BUFFERS;
        PoseStack.Pose pose = new PoseStack().last();
        double now = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true);
        for (SpaceRift.RiftEntity rift : rifts) {
            // before the copy: the tear bends the shards behind it too
            shards(buffers.getBuffer(SHARD), pose, Vec3.atCenterOf(rift.getBlockPos()).subtract(cam), rift.getBlockPos().hashCode(), now);
            buffers.endBatch(SHARD);
            SceneSnapshot.capture(mc.getMainRenderTarget());
            Vec3 at = Vec3.atCenterOf(rift.getBlockPos()).subtract(cam);
            // upright, turned to the viewer
            Vector3f right = new Vector3f((float) at.z, 0, (float) -at.x);
            if (right.lengthSquared() < 0.0001f)
                right.set(1, 0, 0);
            right.normalize().mul(WIDE);
            float seed = (rift.getBlockPos().hashCode() & 255) / 255f;
            VertexConsumer vc = buffers.getBuffer(TEAR);
            for (int[] k : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}})
                vc.addVertex(pose, (float) at.x + right.x * k[0], (float) at.y + TALL * k[1], (float) at.z + right.z * k[0])
                        .setColor(seed, 1f, 1f, 1f)
                        .setUv(k[0], k[1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(0, 0, 1);
            buffers.endBatch(TEAR);
        }
    }

    /** Broken pieces adrift round the tear: fixed by the tear's place, thicker near the slit, turning slowly. */
    private static void shards(VertexConsumer vc, PoseStack.Pose pose, Vec3 at, int seed, double now) {
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < SHARDS; i++) {
            // squared: most stay near the slit
            float x = (random.nextFloat() - 0.5f), z = (random.nextFloat() - 0.5f);
            Vector3f home = new Vector3f(x * Math.abs(x) * 12, (random.nextFloat() - 0.5f) * 10, z * Math.abs(z) * 12);
            float size = 0.05f + 0.3f * random.nextFloat() * random.nextFloat();
            float speed = 0.004f + 0.012f * random.nextFloat();
            float phase = random.nextFloat() * Mth.TWO_PI;
            Vector3f axis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
            Vector3f[] corners = new Vector3f[4];
            for (int c = 0; c < 4; c++)
                corners[c] = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).mul(size * 2.4f);
            float angle = (float) (now * speed * 3 + phase);
            float bob = (float) Math.sin(now * speed + phase) * 0.35f;
            Quaternionf turn = new Quaternionf().rotationAxis(angle, axis);
            Vector3f centre = new Vector3f((float) at.x, (float) at.y, (float) at.z).add(home).add(0, bob, 0);
            for (Vector3f c : corners)
                turn.transform(c).add(centre);
            for (int[] f : FACES) {
                Vector3f a = corners[f[0]], b = corners[f[1]], c = corners[f[2]];
                Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
                vertex(vc, pose, a, n);
                vertex(vc, pose, b, n);
                vertex(vc, pose, c, n);
                vertex(vc, pose, c, n);     // a triangle, in a quad's four corners
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, Vector3f n) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(0, 0, 0, 255)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(n.x, n.y, n.z);
    }
}
