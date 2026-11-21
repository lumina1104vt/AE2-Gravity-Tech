package com.lumina.ae2_gvt.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.locator.MenuLocators;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import com.lumina.ae2_gvt.blockentities.GravityFactoryBlockEntity;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class GravityFactory extends appeng.block.AEBaseEntityBlock<GravityFactoryBlockEntity> {
    private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final SlotSemantic CATALYST = SlotSemantics.register("GRAVITY_CATALYST", false);
    public static final SlotSemantic[] INPUT_ROWS = rows("GRAVITY_FACTORY_INPUT_ROW_", false);
    public static final SlotSemantic[] OUTPUT_ROWS = rows("GRAVITY_FACTORY_OUTPUT_ROW_", false);
    public static final int ROW_SLOTS = 4;

    private static SlotSemantic[] rows(String prefix, boolean playerSide) {
        SlotSemantic[] rows = new SlotSemantic[3];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = SlotSemantics.register(prefix + i, playerSide);
        }
        return rows;
    }

    public GravityFactory(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof GravityFactoryBlockEntity be) {
                be.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GravityFactoryBlockEntity factory) {
            MenuOpener.open(Menus.GRAVITY_FACTORY.get(), player, MenuLocators.forBlockEntity(factory));
        }
        return InteractionResult.SUCCESS;
    }
}
