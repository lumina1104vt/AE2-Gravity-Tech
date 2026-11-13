package com.lumina.ae2_gvt.blocks;

import java.util.ArrayList;

import com.lumina.ae2_gvt.blockentities.MachineAccessBlockEntity;
import com.lumina.ae2_gvt.menus.Menus;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 機器接入方塊: the machine access interface as a block, for machines bound from afar. */
public class MachineAccessBlock extends appeng.block.AEBaseEntityBlock<MachineAccessBlockEntity> {
    public MachineAccessBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineAccessBlockEntity be)
            MenuOpener.open(Menus.MACHINE_ACCESS.get(), player, MenuLocators.forBlockEntity(be));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof MachineAccessBlockEntity be) {
            var drops = new ArrayList<ItemStack>();
            be.core().drops(drops);
            for (ItemStack s : drops)
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
