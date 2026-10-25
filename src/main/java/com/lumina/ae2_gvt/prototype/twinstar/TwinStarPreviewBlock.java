package com.lumina.ae2_gvt.prototype.twinstar;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.prototype.twinstar.gui.TwinStarHub;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * PROTOTYPE - a cell of the 2x2x2 Twin-Star Hub controller; eight of them replace the bottom two
 * layers of the paradox pillar. The animation only runs once the controller is complete and the
 * structure around its centre matches the blueprint. Right-click any cell: re-check now and, if
 * formed, open the hub GUI (otherwise the result shows in the action bar); sneak + right-click:
 * toggle idle / busy.
 */
public class TwinStarPreviewBlock extends Block implements EntityBlock {
    /** Hub formed: show this cell's quarter of the big controller design instead of the mini chip. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Which half of the 2x2x2 this cell is in along x / y / z (only meaningful while formed). */
    public static final BooleanProperty PX = BooleanProperty.create("px");
    public static final BooleanProperty PY = BooleanProperty.create("py");
    public static final BooleanProperty PZ = BooleanProperty.create("pz");

    public TwinStarPreviewBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(PX, false).setValue(PY, false).setValue(PZ, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, PX, PY, PZ);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TwinStarPreviewBlockEntity(pos, state);
    }

    /** The master cell going away: put the hidden structure back and drop its patterns. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TwinStarPreviewBlockEntity be) {
            be.onRemoved(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TwinStarPreview.BLOCK_ENTITY.get()) {
            return null;
        }
        return (l, p, s, be) -> TwinStarPreviewBlockEntity.serverTick(l, p, s, (TwinStarPreviewBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // act on the controller's master cell (its min corner) whichever cell was clicked
        BlockPos min = TwinStarBlueprint.findControllerMin(level, pos);
        if (!(level.getBlockEntity(min != null ? min : pos) instanceof TwinStarPreviewBlockEntity preview)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && !player.isShiftKeyDown()) {
            TwinStarBlueprint.Result r = preview.recheck(server);
            if (r.formed() && min != null && level.getBlockEntity(min) instanceof TwinStarPreviewBlockEntity master) {
                // formed: the controller opens the hub GUI
                MenuOpener.open(TwinStarHub.MENU.get(), player, MenuLocators.forBlockEntity(master));
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(!r.controllerComplete()
                    ? Component.literal("[PROTOTYPE] 控制器未完成：需要 2x2x2 共 8 個預覽方塊")
                    : r.wrongCores()
                    ? Component.translatable("gui.ae2_gvt.prototype.cores", r.blackCores(), r.whiteCores())
                    : r.formed()
                    ? Component.literal("[PROTOTYPE] 結構完整 (" + r.total() + ")")
                    : Component.literal("[PROTOTYPE] 結構不完整：" + r.mismatches() + " / " + r.total() + " 格不符，例如 "
                            + r.firstBad().toShortString() + " 應為 " + r.expected().getName().getString()), true);
        }
        if (level.isClientSide()) {
            // animation controls are purely visual, so only the client copy needs them
            if (player.isShiftKeyDown()) {
                preview.toggleBusy();
            }
        }
        return InteractionResult.SUCCESS;
    }
}
