package com.lumina.ae2_gvt.client.renderer;

import com.lumina.ae2_gvt.client.mesh.GravityLattice;
import com.lumina.ae2_gvt.client.mesh.PreviewMesh;
import com.lumina.ae2_gvt.client.mesh.TwinHoleMesh;
import com.lumina.ae2_gvt.client.TwinHoleRenderTypes;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.twinstar.TwinStarPreviewBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * PROTOTYPE - draws the whole Twin-Star Hub look around the controller's master cell.
 * <ul>
 *   <li>nested gyroscope ("groove" rings): the outer ring spins on Y, the middle ring rides on the
 *       outer ring's X axle, the inner ring on the middle ring's Z axle</li>
 *   <li>twin holes, black-hole-merger style: spiral in, merge (smooth-union surface) and spin, spiral
 *       back out; drawn with the custom twin-hole shader (screen-space starfield, black / inverted
 *       white, gradient + mutual contamination at the divide, opposite spins, rims)</li>
 *   <li>the pair's gravity as a bent 3D grid: pulled in towards the black hole, bulging out round
 *       the white hole ({@link GravityLattice})</li>
 *   <li>a ghost of the unit shell</li>
 *   <li>formation replay: shell reveal, gyroscope spins up, holes emerge</li>
 * </ul>
 * The sphere centre and shell radius come from the saved structure (via the block entity).
 * Rebuilt for 1.21.1 from the 26.1 original (backup/twin_star_preview_26.1).
 */
public class TwinStarPreviewRenderer implements BlockEntityRenderer<TwinStarPreviewBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/prototype/white.png");
    private static final RenderType SOLID = RenderType.entityCutout(WHITE);
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(WHITE);

    // flat bands (1.0 radial x 0.6 axial), spaced so nothing touches however they tumble:
    // shell | inner shell+0.15..+1.17 | middle +1.35..+2.37 | outer +3.05..+4.05
    private static final float OUTER_GAP = 3.55f, MIDDLE_GAP = 1.85f, INNER_GAP = 0.65f;
    private static final float BAND_HALF_WIDTH = 0.5f, BAND_HALF_THICK = 0.3f, PIN_HALF = 0.28f;

    @Override
    public void render(TwinStarPreviewBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!be.formed || be.getLevel() == null) {
            return;   // a lone controller / incomplete structure shows nothing
        }
        double time = be.getLevel().getGameTime() + partialTick;
        be.advance(time);
        final float t = (float) (time / 20.0);
        final double since = be.sinceFormed(time);
        // formation timeline (ticks): shell reveal 0-20, gyroscope spin-up 30-60 (in the block
        // entity), holes emerge 35-55
        final float reveal = (float) TwinStarPreviewBlockEntity.smoothstep(0, 20, since);
        final float grow = (float) TwinStarPreviewBlockEntity.smoothstep(35, 55, since);

        poseStack.pushPose();
        poseStack.translate(be.sphereX, be.sphereY, be.sphereZ);
        final float shell = be.shellRadius;
        PoseStack.Pose pose = poseStack.last();

        // gimbal orientations: child rotation applied inside the parent's
        final Quaternionf outer = new Quaternionf().rotateY((float) Math.toRadians(be.outerAngle));
        final Quaternionf middle = new Quaternionf(outer).rotateX((float) Math.toRadians(be.middleAngle));
        final Quaternionf inner = new Quaternionf(middle).rotateZ((float) Math.toRadians(be.innerAngle));

        VertexConsumer vc = buffers.getBuffer(SOLID);
        // rings: outer = gravity (black), middle = paradox (black front / white back), inner =
        // anti-gravity (white)
        drawRing(pose, vc, outer, shell + OUTER_GAP, 0);
        drawRing(pose, vc, new Quaternionf(middle).rotateX((float) (Math.PI / 2)), shell + MIDDLE_GAP, 1);
        drawRing(pose, vc, new Quaternionf(inner).rotateY((float) (Math.PI / 2)), shell + INNER_GAP, 2);

        // axles: outer on fixed +-Y pins, middle on the outer ring's X, inner on the middle ring's Z
        int pin = PreviewMesh.grey(0x8a, 255);
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            // pins bridge the gap between neighbouring rings, sunk a little into both
            PreviewMesh.prism(pose, vc, outer.transform(new Vector3f(0, sgn * (shell + 2.4f), 0)), outer.transform(new Vector3f(0, sgn * (shell + 3.8f), 0)), PIN_HALF, pin, false);
            PreviewMesh.prism(pose, vc, outer.transform(new Vector3f(sgn * (shell + 2.0f), 0, 0)), outer.transform(new Vector3f(sgn * (shell + 3.3f), 0, 0)), PIN_HALF, pin, false);
            PreviewMesh.prism(pose, vc, middle.transform(new Vector3f(0, 0, sgn * (shell + 0.9f))), middle.transform(new Vector3f(0, 0, sgn * (shell + 1.6f))), PIN_HALF, pin, false);
        }

        if (grow > 0) {
            // twin holes: pair axis turns with the orbit, whose plane tumbles freely (see the block
            // entity); separation follows the spiral-in / merged / spiral-out cycle
            Vector3d a = be.twinAxis();
            final Vector3f axis = new Vector3f((float) a.x, (float) a.y, (float) a.z).normalize();
            final float r = (float) TwinStarPreviewBlockEntity.TWIN_RADIUS * grow;
            final float sep = (float) TwinStarPreviewBlockEntity.twinSeparation(be.twinClock) * grow;
            final float overlap = Math.max(0, 1 - sep / (2 * r));
            final float k = 1.4f * grow * (float) TwinStarPreviewBlockEntity.smoothstep(0, 1, overlap * 2.5);
            TwinHoleMesh.draw(pose, buffers.getBuffer(TwinHoleRenderTypes.TWIN_HOLE), axis, sep, r, k, 40, 64);

            // the pair's gravity as a bent 3D grid: pulled into the black hole, bulging away
            // from the white one
            GravityLattice.draw(pose, buffers.getBuffer(RenderType.lines()), new Vector3f(axis).mul(sep / 2),
                    new Vector3f(axis).mul(-sep / 2), shell - 0.4f, grow);
        }

        // ghost of the unit shell (real units will be translucent blocks), revealed bottom-up
        PreviewMesh.sphere(pose, buffers.getBuffer(GLOW), new Vector3f(), shell, new Vector3f(0, 1, 0), 1, 1, 24, 48,
                (p, u, v) -> PreviewMesh.grey(Math.sin(v * Math.PI * 10 + u * Math.PI * 4 - t * 1.5) > 0 ? 0xf4 : 0x8c, 30),
                true, -shell + 2 * shell * reveal);

        poseStack.popPose();
    }

    /**
     * One gimbal ring, "groove" style: solid band, contrasting rims, a raised contrasting line along
     * each face. {@code ring}: 0 outer (gravity, black body / white accents), 1 middle (paradox,
     * black front / white back), 2 inner (anti-gravity, white body / black accents).
     */
    private static void drawRing(PoseStack.Pose pose, VertexConsumer vc, Quaternionf orient, float radius, int ring) {
        int dark = PreviewMesh.grey(0x12, 255), light = PreviewMesh.grey(0xee, 255), mid = PreviewMesh.grey(0x8c, 255);
        int front = ring == 2 ? light : dark;                 // +Z face
        int back = ring == 0 ? dark : light;                  // -Z face
        int edge = ring == 0 ? light : ring == 2 ? dark : mid; // inner / outer rims
        int accentFront = front == dark ? light : dark, accentBack = back == dark ? light : dark;
        float rIn = radius - BAND_HALF_WIDTH, rOut = radius + BAND_HALF_WIDTH, h = BAND_HALF_THICK;
        double full = 2 * Math.PI;
        PreviewMesh.ringPiece(pose, vc, orient, rIn, rOut, -h, h, 0, full, 160, edge, edge, front, back, edge);
        PreviewMesh.ringPiece(pose, vc, orient, radius - 0.1f, radius + 0.1f, h, h + 0.05f, 0, full, 160,
                accentFront, accentFront, accentFront, accentFront, accentFront);
        PreviewMesh.ringPiece(pose, vc, orient, radius - 0.1f, radius + 0.1f, -h - 0.05f, -h, 0, full, 160,
                accentBack, accentBack, accentBack, accentBack, accentBack);
    }

    @Override
    public boolean shouldRenderOffScreen(TwinStarPreviewBlockEntity be) {
        return true;   // the effect is ~30 blocks across; the controller is often out of view
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    @Override
    public AABB getRenderBoundingBox(TwinStarPreviewBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        double reach = be.shellRadius + 5;   // outermost ring plus margin
        double cx = pos.getX() + be.sphereX, cy = pos.getY() + be.sphereY, cz = pos.getZ() + be.sphereZ;
        return new AABB(cx - reach, Math.min(pos.getY() - 1, cy - reach), cz - reach, cx + reach, cy + reach, cz + reach);
    }
}
