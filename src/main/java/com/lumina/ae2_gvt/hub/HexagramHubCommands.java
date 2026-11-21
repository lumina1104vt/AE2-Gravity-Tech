package com.lumina.ae2_gvt.hub;

import java.util.ArrayList;
import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Test command: {@code /gvt hub build [pos]} builds the whole hexagram hub with its floor's rim at
 * {@code pos} (default: where the player stands) and the controller 6 above it, facing the unturned way
 * (star tips north/south). Clears the empty space inside the structure; leaves the ground under the dome alone.
 * The controller goes in last, so the lib sees a complete structure and forms it at once.
 * <p>
 * {@code /gvt hub clear <controller>} takes a hub down again: every cell of the box except the ground under
 * the dome and the space above the ceiling becomes air (so older layouts, such as the radius-5 cylinders,
 * go too). It is given the controller's position, the one block a player can always find.
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
                                .then(Commands.argument("controller", BlockPosArgument.blockPos())
                                        .executes(c -> clear(c.getSource(), BlockPosArgument.getBlockPos(c, "controller")))))));
    }

    private static int build(CommandSourceStack source, BlockPos rim) {
        ServerLevel level = source.getLevel();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        List<BlockPos> rings = new ArrayList<>();
        int placed = 0;
        for (int x = HexagramHub.MIN_X; x <= HexagramHub.MAX_X; x++) {
            for (int z = HexagramHub.MIN_Z; z <= HexagramHub.MAX_Z; z++) {
                for (int y = 0; y <= HexagramHub.MAX_Y; y++) {
                    HexagramHub.Cell cell = HexagramHub.cell(x, y, z);
                    if (cell == null || cell == HexagramHub.Cell.CONTROLLER) {
                        continue;
                    }
                    BlockPos pos = rim.offset(x, y, z);
                    BlockState state = switch (cell) {
                        case FRAME -> Blocks.MACHINE_FRAME.get().defaultBlockState();
                        // one port on the east side of the bottom layer (no cylinder there), reachable from outside
                        case RIM -> z == 0 && y == 0 && HexagramHub.cell(x + 1, 0, 0) == null
                                ? block(HubStructureBlocks.PORT_BLOCK.get()) : Blocks.MACHINE_FRAME.get().defaultBlockState();
                        case GRAVITY -> block(AE2GravityTech.GRAVITY.block().get().getBlock());
                        case ANTIGRAVITY -> block(AE2GravityTech.ANTIGRAVITY.block().get().getBlock());
                        case PARADOX -> block(AE2GravityTech.PARADOX.block().get().getBlock());
                        case PILLAR -> block(HubStructureBlocks.PILLAR.get().getBlock());
                        case RING -> block(HubStructureBlocks.RING.get().getBlock())
                                .setValue(RotatedPillarBlock.AXIS, HexagramHub.ringAxis(x, y, z));
                        case HELIX_BLACK -> block(HubStructureBlocks.HELIX_BLACK.get().getBlock());
                        case HELIX_WHITE -> block(HubStructureBlocks.HELIX_WHITE.get().getBlock());
                        default -> air;   // SPHERE, CLEAR
                    };
                    // no neighbour updates: 30k blocks, and nothing here needs them except the ring tiles below
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    if (cell == HexagramHub.Cell.RING) {
                        rings.add(pos);
                    } else if (state != air) {
                        placed++;
                    }
                }
            }
        }
        HubRingBlock ring = (HubRingBlock) HubStructureBlocks.RING.get().getBlock();
        for (BlockPos pos : rings) {
            level.setBlock(pos, ring.withPart(level, pos, level.getBlockState(pos)), Block.UPDATE_CLIENTS);
        }
        BlockPos controller = rim.above(HexagramHub.CONTROLLER_Y);
        level.setBlock(controller, HubStructureBlocks.CONTROLLER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        int total = placed + rings.size() + 1;
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.hexagram_hub.built", total, controller.toShortString()), true);
        return total;
    }

    private static int clear(CommandSourceStack source, BlockPos controller) {
        ServerLevel level = source.getLevel();
        BlockPos rim = controller.below(HexagramHub.CONTROLLER_Y);
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        // the controller first, so the lib drops the structure instead of reporting 30k missing blocks
        level.setBlock(controller, air, Block.UPDATE_ALL);
        int removed = 1;
        for (int x = HexagramHub.MIN_X; x <= HexagramHub.MAX_X; x++) {
            for (int z = HexagramHub.MIN_Z; z <= HexagramHub.MAX_Z; z++) {
                for (int y = 0; y <= HexagramHub.MAX_Y; y++) {
                    BlockPos pos = rim.offset(x, y, z);
                    if (HexagramHub.cell(x, y, z) != null && !level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                        removed++;
                    }
                }
            }
        }
        int total = removed;
        source.sendSuccess(() -> Component.translatable("message.ae2_gvt.hexagram_hub.cleared", total, controller.toShortString()), true);
        return total;
    }

    private static BlockState block(Block block) {
        return block.defaultBlockState();
    }
}
