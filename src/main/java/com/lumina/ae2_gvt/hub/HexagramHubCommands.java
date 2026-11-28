package com.lumina.ae2_gvt.hub;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.core.Engine;
import com.lumina.lmb.core.Instance;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Test commands.
 * <p>
 * {@code /gvt hub build [pos]}: builds the whole Twin-Star Hub with its controller (the tip of the lower
 * anchor, facing south) at {@code pos} (default: where the player stands), with one hub port right above the
 * controller; the L1 core is 60 above it. The box is 235 × 121 × 235. The controller goes in last, so the
 * lib sees a complete structure.
 * <p>
 * {@code /gvt hub clear [controller]}: takes it down again (without a position: the nearest one in this
 * dimension). Only cells that still hold the structure's block become air.
 */
public final class HexagramHubCommands {
    private HexagramHubCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gvt").requires(s -> s.hasPermission(2))
                .then(Commands.literal("hub")
                        .then(Commands.literal("build")
                                .executes(c -> build(c.getSource(), BlockPos.containing(c.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(c -> build(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))))
                        .then(Commands.literal("clear")
                                .executes(c -> clearNearest(c.getSource()))
                                .then(Commands.argument("controller", BlockPosArgument.blockPos())
                                        .executes(c -> clear(c.getSource(), BlockPosArgument.getBlockPos(c, "controller")))))));
    }

    private static int build(CommandSourceStack source, BlockPos controller) {
        ServerLevel level = source.getLevel();
        int lowest = level.getMinBuildHeight(), highest = level.getMaxBuildHeight() - HubShape.H;
        if (controller.getY() < lowest || controller.getY() > highest) {
            source.sendFailure(Component.translatable("message.ae2_gvt.hexagram_hub.too_high", controller.getY(), lowest, highest));
            return 0;
        }
        int r = HubShape.R, placed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < HubShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    int cell = HubShape.cell(x, y, z);
                    if (cell == 0 || cell == HubShape.CTRL) continue;
                    // no neighbour updates: tens of thousands of blocks, and none of them needs one
                    level.setBlock(p.setWithOffset(controller, x, y, z), HexagramHub.stateOf(cell), Block.UPDATE_CLIENTS);
                    placed++;
                }
            }
        }
        // one port right above the controller, where a cable can reach it from any side
        level.setBlock(controller.above(), HubStructureBlocks.PORT_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(controller, HubStructureBlocks.CONTROLLER_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), Block.UPDATE_ALL);
        int total = placed + 1;
        AE2GravityTech.LOGGER.info("twin-star hub built: {} blocks, controller at {}", total, controller.toShortString());
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.hexagram_hub.built", total, controller.toShortString()), true);
        return total;
    }

    private static int clearNearest(CommandSourceStack source) {
        BlockPos from = BlockPos.containing(source.getPosition()), nearest = null;
        Engine<BlockState> engine = Multiblocks.engine(source.getLevel());
        if (engine != null) {
            for (Instance<BlockState> instance : engine.all()) {
                BlockPos at = BlockPos.of(instance.anchor());
                if (source.getLevel().getBlockState(at).is(HubStructureBlocks.CONTROLLER_BLOCK.get())
                        && (nearest == null || at.distSqr(from) < nearest.distSqr(from))) {
                    nearest = at;
                }
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.translatable("message.ae2_gvt.hexagram_hub.none"));
            return 0;
        }
        return clear(source, nearest);
    }

    private static int clear(CommandSourceStack source, BlockPos controller) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(controller);
        if (!state.is(HubStructureBlocks.CONTROLLER_BLOCK.get())) {
            source.sendFailure(Component.translatable("message.ae2_gvt.hexagram_hub.not_controller", controller.toShortString()));
            return 0;
        }
        Direction front = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockState air = Blocks.AIR.defaultBlockState();
        // the controller first, so the lib drops the structure instead of reporting every block missing
        level.setBlock(controller, air, Block.UPDATE_ALL);
        int r = HubShape.R, removed = 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < HubShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    int cell = HubShape.cell(x, y, z);
                    if (cell == 0 || cell == HubShape.CTRL) continue;
                    // the structure turns with the controller (unturned: facing south)
                    switch (front) {
                        case WEST -> p.setWithOffset(controller, -z, y, x);
                        case NORTH -> p.setWithOffset(controller, -x, y, -z);
                        case EAST -> p.setWithOffset(controller, z, y, -x);
                        default -> p.setWithOffset(controller, x, y, z);
                    }
                    BlockState here = level.getBlockState(p);
                    if (here.is(HexagramHub.block(cell & HubShape.ROLE_MASK).get())
                            || HexagramHub.isPortPlace(cell, y) && here.is(HubStructureBlocks.PORT_BLOCK.get())) {
                        level.setBlock(p, air, Block.UPDATE_CLIENTS);
                        removed++;
                    }
                }
            }
        }
        int total = removed;
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.hexagram_hub.cleared", total, controller.toShortString()), true);
        return total;
    }
}
