package com.lumina.ae2_gvt.blocks;

import appeng.block.AEBaseEntityBlock;
import com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity;
import com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One of the three generators ({@link Kind}); they differ only in what they burn. */
public class GeneratorBlock extends AEBaseEntityBlock<PowerGeneratorBlockEntity> {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Kind kind;

    public GeneratorBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
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

    /** The flywheel and the cage let skylight through, as glass does. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return kind != Kind.CRYSTAL;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return kind != Kind.CRYSTAL ? 1.0f : super.getShadeBrightness(state, level, pos);
    }

    public Kind kind() {
        return kind;
    }

    /** Dust can be put in by hand (a network with no power can't hand any out). */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                                     InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.CRYSTAL && level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity be && be.acceptsFuel(stack)) {
            if (!level.isClientSide()) {
                ItemStack rest = be.getFuel().insertItem(0, stack.copy(), false);
                if (rest.getCount() != stack.getCount() && !player.isCreative())
                    player.setItemInHand(hand, rest);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity be)
            appeng.menu.MenuOpener.open(com.lumina.ae2_gvt.menus.Menus.generator(kind).get(), player,
                    appeng.menu.locator.MenuLocators.forBlockEntity(be));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof PowerGeneratorBlockEntity be) {
            ItemStack left = be.getFuel().getStackInSlot(0);
            if (!left.isEmpty())
                Block.popResource(level, pos, left.copy());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
