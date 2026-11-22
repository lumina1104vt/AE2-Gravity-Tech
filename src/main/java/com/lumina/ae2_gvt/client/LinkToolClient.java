package com.lumina.ae2_gvt.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import org.jetbrains.annotations.Nullable;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.blockentities.MachineAccessBlockEntity;
import com.lumina.ae2_gvt.items.parts.MachineAccessCore;
import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * While the 機器連結器 is held and an interface is picked: the interface glows, each machine it is bound
 * to has the bound face glowing, and a thin line runs from the interface to each machine. Drawn without
 * depth test so the links show through walls, through our own buffers (not the shared ones, see
 * {@link SceneSnapshot}).
 */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class LinkToolClient extends ShardBase {
    /** Right clicking a machine with the linker while Ctrl is down: tell the server, it cannot see keys (the click follows this). */
    public static void reportCtrl() {
        if (net.minecraft.client.gui.screens.Screen.hasControlDown())
            com.lumina.ae2_gvt.payload.ModNet.toServer(com.lumina.ae2_gvt.payload.LinkAllPayload.INSTANCE);
    }

    private static final RenderType FILL = RenderType.create("ae2_gvt_link_fill", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1024, false, true, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType LINES = RenderType.create("ae2_gvt_link_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1024, false, true, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(1.5)))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** the machines' faces are depth tested: a face behind a wall is not drawn through it */
    private static final RenderType FACE_FILL = RenderType.create("ae2_gvt_link_face_fill", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1024, false, true, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType FACE_LINES = RenderType.create("ae2_gvt_link_face_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1024, false, true, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final MultiBufferSource.BufferSource FACE_FILL_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(1 << 15));
    private static final MultiBufferSource.BufferSource FACE_LINE_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(1 << 15));
    /** one source for each type: a source with a single builder ends the first type's vertices when the second is asked for */
    private static final MultiBufferSource.BufferSource FILL_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(1 << 16));
    private static final MultiBufferSource.BufferSource LINE_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(1 << 16));

    private static final float[] GLOW = {0.62f, 0.82f, 1.0f};

    private LinkToolClient() {
    }

    private static ItemStack heldTool(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(Items.MACHINE_LINK_TOOL.get()))
            return main;
        ItemStack off = player.getOffhandItem();
        return off.is(Items.MACHINE_LINK_TOOL.get()) ? off : ItemStack.EMPTY;
    }

    /** A machine lit up from the screen (double click): the machine's face and block, and the line to its interface. */
    private record Flash(BlockPos machine, Direction face, @Nullable BlockPos host, @Nullable Direction hostSide, long until) {
    }

    private static final List<Flash> FLASHES = new ArrayList<>();
    private static final long FLASH_TICKS = 120;

    /** Lights the machine up for a few seconds; {@code host}/{@code hostSide} is the interface (the line goes to it). */
    public static void flash(BlockPos machine, Direction face, @Nullable BlockPos host, @Nullable Direction hostSide) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        FLASHES.removeIf(f -> f.machine().equals(machine));
        FLASHES.add(new Flash(machine.immutable(), face, host, hostSide, mc.level.getGameTime() + FLASH_TICKS));
    }



    private static void draw(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        long now = mc.level.getGameTime();
        FLASHES.removeIf(f -> f.until() < now);
        ItemStack tool = heldTool(mc.player);
        MachineLinkTool.Link picked = tool.isEmpty() ? null : MachineLinkTool.getSelected(tool);
        MachineAccessCore part = picked == null ? null : MachineLinkTool.interfaceOf(mc.level, picked);
        if (part == null && FLASHES.isEmpty())
            return;

        Vec3 cam = event.getCamera().getPosition();
        Matrix4f m = new PoseStack().last().pose();
        VertexConsumer fill = FILL_BUFFERS.getBuffer(FILL);
        VertexConsumer lines = LINE_BUFFERS.getBuffer(LINES);
        VertexConsumer faceFill = FACE_FILL_BUFFERS.getBuffer(FACE_FILL);
        VertexConsumer faceLines = FACE_LINE_BUFFERS.getBuffer(FACE_LINES);
        float pulse = 0.5f + 0.5f * (float) Math.sin((now + event.getPartialTick()) * 0.2);

        if (part != null) {
            // the interface: a glowing box around the part, flush with the block face it sits on
            boolean wholeBlock = mc.level.getBlockEntity(picked.pos()) instanceof MachineAccessBlockEntity;
            Direction side = picked.face();
            Vec3 partCentre = wholeBlock ? Vec3.atCenterOf(picked.pos()).subtract(cam) : blockSideCentre(picked.pos(), side, 14f / 16).subtract(cam);
            box(fill, lines, m, picked.pos(), side, wholeBlock, cam, 0.18f + 0.12f * pulse);
            for (MachineLinkTool.Link l : part.getLinks()) {
                Vec3 faceCentre = blockSideCentre(l.pos(), l.face(), 1.0 + 0.002).subtract(cam);
                if (exposed(mc.level, l.pos(), l.face()))
                    face(faceFill, faceLines, m, l.pos(), l.face(), cam, 0.22f + 0.12f * pulse, 0.9f);
                line(lines, m, partCentre, faceCentre, 0.85f);
            }
        }
        if (part != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos at = hit.getBlockPos();
            boolean interfaceOrCable = mc.level.getBlockEntity(at) instanceof MachineAccessBlockEntity
                    || mc.level.getBlockEntity(at) instanceof appeng.api.parts.IPartHost;
            boolean already = part.getLinks().stream().anyMatch(l -> l.pos().equals(at) && l.face() == hit.getDirection());
            if (!interfaceOrCable && !already)       // the face a click would set: an outline, brighter as it pulses
                face(faceFill, faceLines, m, at, hit.getDirection(), cam, 0.06f + 0.06f * pulse, 0.55f + 0.45f * pulse);
        }
        for (Flash f : FLASHES) {
            // lit up from the screen: the whole machine block, its face, and the line to the interface
            box(fill, lines, m, f.machine(), Direction.UP, true, cam, 0.12f + 0.12f * pulse);
            if (exposed(mc.level, f.machine(), f.face()))
                face(faceFill, faceLines, m, f.machine(), f.face(), cam, 0.3f + 0.15f * pulse, 0.9f);
            if (f.host() != null) {
                boolean wholeHost = mc.level.getBlockEntity(f.host()) instanceof MachineAccessBlockEntity;
                Vec3 from = wholeHost || f.hostSide() == null ? Vec3.atCenterOf(f.host()).subtract(cam)
                        : blockSideCentre(f.host(), f.hostSide(), 14f / 16).subtract(cam);
                line(lines, m, from, blockSideCentre(f.machine(), f.face(), 1.0 + 0.002).subtract(cam), 0.85f);
            }
        }
        FILL_BUFFERS.endBatch(FILL);
        LINE_BUFFERS.endBatch(LINES);
        FACE_FILL_BUFFERS.endBatch(FACE_FILL);
        FACE_LINE_BUFFERS.endBatch(FACE_LINES);
    }

    /** The middle of the block's {@code side}, {@code depth} (0 = block centre, 1 = the face) along it. */
    private static Vec3 blockSideCentre(BlockPos pos, Direction side, double depth) {
        return Vec3.atCenterOf(pos).add(side.getStepX() * (depth - 0.5),
                side.getStepY() * (depth - 0.5), side.getStepZ() * (depth - 0.5));
    }

    /** The part's box: 12/16 square, 4/16 deep, against {@code side} of the block. */
    private static void box(VertexConsumer fill, VertexConsumer lines, Matrix4f m, BlockPos pos, Direction side, boolean whole, Vec3 cam, float alpha) {
        double[] lo = {2 / 16.0, 2 / 16.0, 2 / 16.0}, hi = {14 / 16.0, 14 / 16.0, 14 / 16.0};
        int axis = side.getAxis().ordinal();
        if (whole) {
            lo = new double[] {-0.002, -0.002, -0.002};
            hi = new double[] {1.002, 1.002, 1.002};
        } else if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            lo[axis] = 12 / 16.0;
            hi[axis] = 16 / 16.0;
        } else {
            lo[axis] = 0;
            hi[axis] = 4 / 16.0;
        }
        float x0 = (float) (pos.getX() + lo[0] - cam.x), y0 = (float) (pos.getY() + lo[1] - cam.y), z0 = (float) (pos.getZ() + lo[2] - cam.z);
        float x1 = (float) (pos.getX() + hi[0] - cam.x), y1 = (float) (pos.getY() + hi[1] - cam.y), z1 = (float) (pos.getZ() + hi[2] - cam.z);
        float[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] quads = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (int[] q : quads)
            for (int i : q)
                fill.vertex(m, c[i][0], c[i][1], c[i][2]).color(GLOW[0], GLOW[1], GLOW[2], alpha).endVertex();
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges)
            line(lines, m, new Vec3(c[e[0]][0], c[e[0]][1], c[e[0]][2]), new Vec3(c[e[1]][0], c[e[1]][1], c[e[1]][2]), 0.9f);
    }

    /** A machine's bound face, a hair off the block so it does not flicker against it. */
    /** A machine's face is shown only when nothing solid stands in front of it. */
    private static boolean exposed(net.minecraft.world.level.Level level, BlockPos pos, Direction side) {
        BlockPos in = pos.relative(side);
        return !level.getBlockState(in).isSolidRender(level, in);
    }

    private static void face(VertexConsumer fill, VertexConsumer lines, Matrix4f m, BlockPos pos, Direction side, Vec3 cam, float alpha,
                             float lineAlpha) {
        Vec3 c = Vec3.atCenterOf(pos).subtract(cam).add(side.getStepX() * 0.502, side.getStepY() * 0.502, side.getStepZ() * 0.502);
        Vector3f u = tangent(side, true), v = tangent(side, false);
        Vec3[] p = new Vec3[4];
        float[][] k = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < 4; i++)
            p[i] = c.add(u.x * k[i][0] * 0.5, u.y * k[i][0] * 0.5, u.z * k[i][0] * 0.5)
                    .add(v.x * k[i][1] * 0.5, v.y * k[i][1] * 0.5, v.z * k[i][1] * 0.5);
        for (Vec3 q : p)
            fill.vertex(m, (float) q.x, (float) q.y, (float) q.z).color(GLOW[0], GLOW[1], GLOW[2], alpha).endVertex();
        for (int i = 0; i < 4; i++)
            line(lines, m, p[i], p[(i + 1) % 4], lineAlpha);
    }

    private static Vector3f tangent(Direction side, boolean first) {
        return switch (side.getAxis()) {
            case X -> first ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
            case Y -> first ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
            case Z -> first ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        };
    }

    private static void line(VertexConsumer lines, Matrix4f m, Vec3 a, Vec3 b, float alpha) {
        Vec3 d = b.subtract(a);
        float len = (float) d.length();
        if (len < 1e-4f)
            return;
        float nx = (float) d.x / len, ny = (float) d.y / len, nz = (float) d.z / len;
        lines.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(GLOW[0], GLOW[1], GLOW[2], alpha).normal(nx, ny, nz).endVertex();
        lines.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(GLOW[0], GLOW[1], GLOW[2], alpha).normal(nx, ny, nz).endVertex();
    }

    @SubscribeEvent
    public static void afterParticles(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            LevelPass.run(event, () -> draw(event));
    }
}
