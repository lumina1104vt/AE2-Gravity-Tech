package com.lumina.ae2_gvt.blocks.cpu;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DummyTwinStarCPU extends AEBaseEntityBlock<DummyTwinStarCPUBlockEntity> {
    public DummyTwinStarCPU(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DummyTwinStarCPUBlockEntity cpu) {
            MenuOpener.open(Menus.DUMMY_CPU.get(), player, MenuLocators.forBlockEntity(cpu));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
