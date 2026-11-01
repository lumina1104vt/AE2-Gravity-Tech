package com.lumina.ae2_gvt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Draws the 引力波探測器: its base, and its pointer turned to the black hole on the stack's lodestone
 * tracker (relative to where the player looks, like a compass). With no black hole found, or one in
 * another dimension, the pointer spins.
 * <p>
 * Pulses: a ring runs out from the dial's centre and fades, the more often the nearer the black hole
 * (about every 5 s at 1000 blocks, every second at 100, a flicker next to it). No sound. None while
 * nothing is found.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class WaveDetectorClient {
    private static final ModelResourceLocation BASE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "item/gravity_wave_detector_base"));
    private static final ModelResourceLocation POINTER = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "item/gravity_wave_detector_pointer"));
    private static final ModelResourceLocation RING = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "item/gravity_wave_detector_ring"));
    /** ticks a ring takes to run out and fade (at most; shorter when pulses come faster) */
    private static final float RING_LIFE = 24;

    /** pulse phase, in pulses (its fraction is where the current one is); advanced by real time */
    private static double phase;
    private static long lastMillis = Util.getMillis();

    private WaveDetectorClient() {
    }

    @SubscribeEvent
    static void models(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(POINTER);
        event.register(RING);
    }

    @SubscribeEvent
    static void extensions(RegisterClientExtensionsEvent event) {
        BlockEntityWithoutLevelRenderer renderer = new Renderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, Items.GRAVITY_WAVE_DETECTOR.get());
    }

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        Renderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                                 int light, int overlay) {
            Minecraft mc = Minecraft.getInstance();
            var models = mc.getModelManager();
            var vertices = buffers.getBuffer(ItemBlockRenderTypes.getRenderType(stack, true));
            BakedModel base = models.getModel(BASE);
            draw(mc, base, stack, pose, vertices, light, overlay);
            // the display transform turns the whole detector (45° in hand and in the GUI); undo that for the
            // pointer, so "straight ahead" (or "up" in the GUI) is where it really points
            float display = base.getTransforms().getTransform(context).rotation.y();
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(angle(mc, stack) - display));
            pose.translate(-0.5, 0, -0.5);
            draw(mc, models.getModel(POINTER), stack, pose, vertices, light, overlay);
            pose.popPose();
            pulse(mc, models.getModel(RING), stack, pose, buffers, overlay);
        }

        /** The ring of the current pulse, if one is running out. */
        private static void pulse(Minecraft mc, BakedModel ring, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int overlay) {
            long now = Util.getMillis();
            double ticks = Math.min(now - lastMillis, 1000) / 50.0;
            lastMillis = now;
            Double distance = distance(mc, stack);
            if (distance == null)
                return;
            // ticks between pulses: 6 next to it … 100 at 1000 blocks
            float period = (float) Mth.clamp(6 + distance / 10, 6, 100);
            phase += ticks / period;
            float life = Math.min(period, RING_LIFE);
            float t = (float) (phase - Math.floor(phase)) * period / life;
            if (t >= 1)
                return;
            float scale = 0.15f + 0.85f * t, alpha = 1 - t;
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.scale(scale, 1, scale);
            pose.translate(-0.5, 0, -0.5);
            var vertices = buffers.getBuffer(Sheets.translucentItemSheet());
            for (BakedQuad quad : ring.getQuads(null, null, RandomSource.create(42)))
                vertices.putBulkData(pose.last(), quad, 1, 1, 1, alpha, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }

        /** Blocks from the player to the tracked black hole, or null when there is none here. */
        private static Double distance(Minecraft mc, ItemStack stack) {
            Player player = mc.player;
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (player == null || tracker == null || tracker.target().isEmpty()
                    || tracker.target().get().dimension() != player.level().dimension())
                return null;
            return Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(tracker.target().get().pos())));
        }

        private static void draw(Minecraft mc, BakedModel model, ItemStack stack, PoseStack pose,
                                 com.mojang.blaze3d.vertex.VertexConsumer vertices, int light, int overlay) {
            mc.getItemRenderer().renderModelLists(model, stack, light, overlay, pose, vertices);
        }

        /** Degrees about +Y that turn the pointer (drawn towards north, -Z) to the target as seen by the player. */
        private static float angle(Minecraft mc, ItemStack stack) {
            Player player = mc.player;
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (player == null || tracker == null || tracker.target().isEmpty()
                    || tracker.target().get().dimension() != player.level().dimension()) {
                // nothing found: spin
                return (float) (mc.level == null ? 0 : (mc.level.getGameTime() % 40) * 9 + mc.getTimer().getGameTimeDeltaPartialTick(true) * 9);
            }
            // between ticks, so the pointer turns smoothly with the view
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
            Vec3 to = Vec3.atCenterOf(tracker.target().get().pos()).subtract(player.getPosition(partial));
            // world yaw towards the target, in Minecraft's convention (0 = south, 90 = west)
            float targetYaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
            float relative = Mth.wrapDegrees(targetYaw - player.getViewYRot(partial));
            // straight ahead → pointer at rest (-Z); to the right (yaw grows turning right) → clockwise seen from above
            return -relative;
        }
    }
}
