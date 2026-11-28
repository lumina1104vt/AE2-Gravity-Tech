package com.lumina.ae2_gvt.hub;

import java.util.Map;
import java.util.stream.Collectors;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.lumina.ae2_gvt.menus.Menus;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Controller of the Twin-Star Hub ({@link HexagramHub}), at the tip of its lower anchor: the
 * twin-star engine's host ({@link TwinStarHubBlockEntity}). The lib finds and tracks the structure on
 * its own; {@code formed} is never set in the world - it is the look the lib draws while the hub is
 * formed. Right-click: the controller's GUI (units, patterns), and in the chat what is still wrong
 * while the hub is not formed.
 */
public class TwinStarHubBlock extends AEBaseEntityBlock<TwinStarHubBlockEntity> {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TwinStarHubBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /** A front with the screen: placed facing the player, turned by AE2's wrench. */
    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof TwinStarHubBlockEntity controller)
            MenuOpener.open(Menus.TWIN_STAR_HUB.get(), player, MenuLocators.forBlockEntity(controller));
        Engine<BlockState> engine = Multiblocks.engine(server);
        Instance<BlockState> hub = engine == null ? null : engine.at(pos.asLong());
        if (hub == null) {
            player.displayClientMessage(Component.translatable("message.ae2_gvt.hexagram_hub.unknown"), false);
        } else if (!hub.isFormed()) {
            Map<String, Integer> missing = engine.missing(hub);
            String byRole = missing.entrySet().stream()
                    .map(e -> HexagramHub.roleName(e.getKey()).getString() + " " + e.getValue())
                    .collect(Collectors.joining(", "));
            player.displayClientMessage(Component.translatable(hub.state() == Instance.State.DAMAGED
                    ? "message.ae2_gvt.hexagram_hub.damaged" : "message.ae2_gvt.hexagram_hub.pending", byRole), false);
            for (Problem p : engine.problems(hub, 3)) {
                player.displayClientMessage(Component.translatable("message.ae2_gvt.hexagram_hub.problem",
                        HexagramHub.roleName(p.role()), Pos.str(p.pos())), false);
            }
        }
        return InteractionResult.CONSUME;
    }
}
