package com.lumina.ae2_gvt.structure;

import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.blocks.FusionController;
import com.lumina.ae2_gvt.hub.HubStructureBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for the fusion chamber. {@code /gvt fusion build [pos]} builds a chamber whose north wall
 * is one block south of {@code pos} (default: where the player stands), with the controller in the
 * middle of that wall and a hub port west of it. {@code /gvt fusion free} gives the nearest controller all the energy it can burn,
 * as no test world has a source of tens of millions of FE/t (until a restart). {@code /gvt fusion info}
 * tells how it is doing.
 */
public final class FusionCommands {
    /** How far the nearest controller is looked for, in blocks. */
    private static final int REACH = 32;

    private FusionCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gvt").requires(s -> s.hasPermission(2))
                .then(Commands.literal("fusion")
                        .then(Commands.literal("build")
                                .executes(c -> build(c.getSource(), BlockPos.containing(c.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(c -> build(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))))
                        .then(Commands.literal("free").executes(c -> free(c.getSource())))
                        .then(Commands.literal("info").executes(c -> info(c.getSource())))));
    }

    private static int build(CommandSourceStack source, BlockPos at) {
        ServerLevel level = source.getLevel();
        BlockPos min = at.offset(-4, 0, 1);
        int last = FusionStructure.SIZE - 1;
        BlockState glass = Blocks.CONTAINMENT_GLASS.get().defaultBlockState();
        for (int x = 0; x <= last; x++)
            for (int y = 0; y <= last; y++)
                for (int z = 0; z <= last; z++) {
                    int outer = (x == 0 || x == last ? 1 : 0) + (y == 0 || y == last ? 1 : 0) + (z == 0 || z == last ? 1 : 0);
                    BlockState state = outer >= 2 ? Blocks.STRUCTURE_FRAME.get().defaultBlockState()
                            : outer == 1 ? glass : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    level.setBlockAndUpdate(min.offset(x, y, z), state);
                }
        int mid = FusionStructure.SIZE / 2;
        for (int[] c : new int[][] {{1, mid, mid}, {last - 1, mid, mid}, {mid, 1, mid}, {mid, last - 1, mid}, {mid, mid, 1}, {mid, mid, last - 1}})
            level.setBlockAndUpdate(min.offset(c[0], c[1], c[2]), Blocks.GRAVITY_CONSTRAINER.get().defaultBlockState());
        level.setBlockAndUpdate(min.offset(2, mid, 0), HubStructureBlocks.PORT_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(min.offset(mid, mid, 0), Blocks.FUSION_CONTROLLER.get().defaultBlockState().setValue(FusionController.FACING, Direction.NORTH));
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.fusion.built"), true);
        return 1;
    }

    private static int free(CommandSourceStack source) {
        FusionControllerBlockEntity c = FusionControllerBlockEntity.nearest(source.getLevel(), BlockPos.containing(source.getPosition()), REACH);
        if (c == null) {
            source.sendFailure(Component.translatable("message.ae2_gvt.fusion.no_controller", REACH));
            return 0;
        }
        boolean on = c.toggleFree();
        source.sendSuccess(() -> Component.translatable(on ? "message.ae2_gvt.fusion.free_on" : "message.ae2_gvt.fusion.free_off"), true);
        return 1;
    }

    private static int info(CommandSourceStack source) {
        FusionControllerBlockEntity c = FusionControllerBlockEntity.nearest(source.getLevel(), BlockPos.containing(source.getPosition()), REACH);
        if (c == null) {
            source.sendFailure(Component.translatable("message.ae2_gvt.fusion.no_controller", REACH));
            return 0;
        }
        source.sendSuccess(c::describe, false);
        return 1;
    }
}
