package com.lumina.ae2_gvt.items;

import appeng.core.definitions.AEBlocks;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.hole.GravityHole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 悖論水晶種子: used on an AE2 fluix block with a black hole on one side and a white hole on the
 * opposite side (east / west, north / south or up / down), turns it into a budding paradox crystal
 * (the best tier there is: almost flawless). It decays like AE2's budding certus, back to a fluix
 * block, which can be seeded again.
 */
public class ParadoxSeedItem extends Item {
    public ParadoxSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(AEBlocks.FLUIX_BLOCK.block()))
            return InteractionResult.PASS;
        if (!betweenHoles(level, pos)) {
            if (context.getPlayer() != null && !level.isClientSide())
                context.getPlayer().displayClientMessage(Component.translatable("message.ae2_gvt.paradox_seed.needs_holes"), true);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX.get().defaultBlockState());
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** A black hole on one side and a white hole on the opposite one. */
    private static boolean betweenHoles(Level level, BlockPos pos) {
        for (Direction d : new Direction[] {Direction.EAST, Direction.SOUTH, Direction.UP}) {
            BlockState a = level.getBlockState(pos.relative(d)), b = level.getBlockState(pos.relative(d.getOpposite()));
            if (a.is(GravityHole.BLACK_HOLE.get()) && b.is(GravityHole.WHITE_HOLE.get())
                    || a.is(GravityHole.WHITE_HOLE.get()) && b.is(GravityHole.BLACK_HOLE.get()))
                return true;
        }
        return false;
    }
}
