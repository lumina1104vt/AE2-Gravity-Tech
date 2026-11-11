package com.lumina.ae2_gvt.elevator;

import java.util.Map;
import java.util.stream.Collectors;

import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.core.Engine;
import com.lumina.lmb.core.Instance;
import com.lumina.lmb.core.Pos;
import com.lumina.lmb.core.Problem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Controller of the space elevator ({@link SpaceElevator}): goes on the core's face at the top of a ramp,
 * facing out (placed facing the player). The lib finds and tracks the structure on its own; {@code formed}
 * is never set in the world, it is the look the lib draws while the elevator stands complete. Right-click:
 * whether it is formed, and if not, how many cells are wrong per role and where the first few are.
 */
public class SpaceElevatorBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public SpaceElevatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.SOUTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Engine<BlockState> engine = Multiblocks.engine(server);
        Instance<BlockState> elevator = engine == null ? null : engine.at(pos.asLong());
        if (elevator == null) {
            player.displayClientMessage(Component.translatable("message.ae2_gvt.space_elevator.unknown"), false);
        } else if (elevator.isFormed()) {
            player.displayClientMessage(Component.translatable("message.ae2_gvt.space_elevator.formed"), false);
        } else {
            Map<String, Integer> missing = engine.missing(elevator);
            String byRole = missing.entrySet().stream()
                    .map(e -> Component.translatable("message.ae2_gvt.space_elevator.role." + e.getKey()).getString() + " " + e.getValue())
                    .collect(Collectors.joining(", "));
            player.displayClientMessage(Component.translatable(elevator.state() == Instance.State.DAMAGED
                    ? "message.ae2_gvt.space_elevator.damaged" : "message.ae2_gvt.space_elevator.pending", byRole), false);
            for (Problem p : engine.problems(elevator, 3)) {
                player.displayClientMessage(Component.translatable("message.ae2_gvt.space_elevator.problem",
                        Component.translatable("message.ae2_gvt.space_elevator.role." + p.role()), Pos.str(p.pos())), false);
            }
        }
        return InteractionResult.CONSUME;
    }
}
