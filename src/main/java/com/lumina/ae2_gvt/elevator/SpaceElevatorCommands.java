package com.lumina.ae2_gvt.elevator;

import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Test commands.
 * <p>
 * {@code /gvt elevator build [pos]}: builds the whole space elevator with the middle of its bottom layer
 * at {@code pos} (default: where the player stands), controller facing south. The top is 318 above, so
 * {@code pos} must be at Y ≤ 1 in a normal overworld (world top 319); otherwise nothing is built. The
 * controller goes in last, so the lib sees a complete structure and forms it at once.
 * <p>
 * {@code /gvt elevator clear <controller>}: takes it down again (every structure cell becomes air),
 * whichever way the controller faces: the structure is the same all round, only the controller says
 * where its middle is.
 */
public final class SpaceElevatorCommands {
    private SpaceElevatorCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gvt").requires(s -> s.hasPermission(2))
                .then(Commands.literal("elevator")
                        .then(Commands.literal("build")
                                .executes(c -> build(c.getSource(), BlockPos.containing(c.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(c -> build(c.getSource(), BlockPosArgument.getBlockPos(c, "pos")))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("controller", BlockPosArgument.blockPos())
                                        .executes(c -> clear(c.getSource(), BlockPosArgument.getBlockPos(c, "controller")))))));
    }

    private static int build(CommandSourceStack source, BlockPos bottom) {
        ServerLevel level = source.getLevel();
        int top = bottom.getY() + SpaceElevatorShape.H - 1;
        if (bottom.getY() < level.getMinBuildHeight() || top >= level.getMaxBuildHeight()) {
            int highest = level.getMaxBuildHeight() - SpaceElevatorShape.H;
            source.sendFailure(Component.translatable("message.ae2_gvt.space_elevator.too_high", bottom.getY(), highest));
            return 0;
        }
        BlockState[] states = new BlockState[SpaceElevator.BLOCK_IDS.length];
        for (int i = 1; i < states.length; i++) {
            if (SpaceElevator.BLOCK_IDS[i] != null) states[i] = SpaceElevator.lookup(SpaceElevator.BLOCK_IDS[i]).get().defaultBlockState();
        }
        int r = SpaceElevatorShape.R, placed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < SpaceElevatorShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    int id = SpaceElevatorShape.role(x, y, z);
                    if (id == 0 || id == SpaceElevatorShape.CTRL) continue;
                    // no neighbour updates: 143k blocks, and none of them needs one
                    level.setBlock(p.setWithOffset(bottom, x, y, z), states[id], Block.UPDATE_CLIENTS);
                    placed++;
                }
            }
        }
        BlockPos controller = bottom.offset(SpaceElevatorShape.CTRL_X, SpaceElevatorShape.CTRL_Y, SpaceElevatorShape.CTRL_Z);
        level.setBlock(controller, SpaceElevator.BLOCK.get().defaultBlockState().setValue(SpaceElevatorBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        int total = placed + 1;
        AE2GravityTech.LOGGER.info("space elevator built: {} blocks, controller at {}", total, controller.toShortString());
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.space_elevator.built", total, controller.toShortString()), true);
        return total;
    }

    private static int clear(CommandSourceStack source, BlockPos controller) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(controller);
        if (!state.is(SpaceElevator.BLOCK.get())) {
            source.sendFailure(Component.translatable("message.ae2_gvt.space_elevator.not_controller", controller.toShortString()));
            return 0;
        }
        Direction front = state.getValue(SpaceElevatorBlock.FACING);
        BlockPos bottom = controller.relative(front, -SpaceElevatorShape.CTRL_Z).below(SpaceElevatorShape.CTRL_Y);
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        // the controller first, so the lib drops the structure instead of reporting 143k missing blocks
        level.setBlock(controller, air, Block.UPDATE_ALL);
        int r = SpaceElevatorShape.R, removed = 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < SpaceElevatorShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    if (SpaceElevatorShape.role(x, y, z) == 0) continue;
                    p.setWithOffset(bottom, x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, air, Block.UPDATE_CLIENTS);
                        removed++;
                    }
                }
            }
        }
        int total = removed;
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.space_elevator.cleared", total, controller.toShortString()), true);
        return total;
    }
}
