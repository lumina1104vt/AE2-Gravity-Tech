package com.lumina.ae2_gvt.prototype.hub;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Controller of the hexagram hub ({@link HexagramHub}), in the middle of the domed floor. The lib
 * finds and tracks the structure on its own; {@code formed} is never set in the world - it is the look
 * the lib draws while the hub is formed. A full block ("6 樣板框": AE2's pattern provider recoloured, the
 * inner squares glowing violet once formed). Right-click: what is still wrong (the units GUI comes later).
 */
public class TwinStarHubBlock extends Block implements EntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TwinStarHubBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TwinStarHubBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Engine<BlockState> engine = Multiblocks.engine(server);
        Instance<BlockState> hub = engine == null ? null : engine.at(pos.asLong());
        if (hub == null) {
            player.displayClientMessage(Component.translatable("message.ae2_gvt.hexagram_hub.unknown"), false);
        } else if (hub.isFormed()) {
            player.displayClientMessage(Component.translatable("message.ae2_gvt.hexagram_hub.formed"), false);
        } else {
            Map<String, Integer> missing = engine.missing(hub);
            String byRole = missing.entrySet().stream()
                    .map(e -> Component.translatable("message.ae2_gvt.hexagram_hub.role." + e.getKey()).getString() + " " + e.getValue())
                    .collect(Collectors.joining(", "));
            player.displayClientMessage(Component.translatable(hub.state() == Instance.State.DAMAGED
                    ? "message.ae2_gvt.hexagram_hub.damaged" : "message.ae2_gvt.hexagram_hub.pending", byRole), false);
            for (Problem p : engine.problems(hub, 3)) {
                player.displayClientMessage(Component.translatable("message.ae2_gvt.hexagram_hub.problem",
                        Component.translatable("message.ae2_gvt.hexagram_hub.role." + p.role()), Pos.str(p.pos())), false);
            }
        }
        return InteractionResult.CONSUME;
    }
}
