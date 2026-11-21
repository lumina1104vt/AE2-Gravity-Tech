package com.lumina.ae2_gvt.dev;

import appeng.core.definitions.AEBlocks;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

/**
 * Development only (the dev server sets {@code -Dae2_gvt.devNetworkTest=true}): a creative energy cell, an ME controller and two twin-star
 * CPUs are put side by side, the server runs a few seconds and stops, so what happens when a CPU joins a network shows in the log without a
 * client.
 */
public final class DevChecks {
    private static int ticks;

    private DevChecks() {
    }

    public static void init() {
        if (!Boolean.getBoolean("ae2_gvt.devNetworkTest"))
            return;
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            ServerLevel level = e.getServer().overworld();
            Block[] row = {AEBlocks.CREATIVE_ENERGY_CELL.block(), AEBlocks.CONTROLLER.block(), Blocks.TWINSTAR_CPU.get(), Blocks.TWINSTAR_CPU.get()};
            for (int i = 0; i < row.length; i++) {
                BlockPos pos = new BlockPos(i, 100, 0);
                level.setBlock(pos, row[i].defaultBlockState(), 3);
            }
            AE2GravityTech.LOGGER.info("dev network test: blocks placed");
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END || ++ticks < 100)
                return;
            if (ticks == 100) {
                var level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().overworld();
                for (int i = 0; i < 4; i++)
                    AE2GravityTech.LOGGER.info("dev network test: {} -> {}", i, level.getBlockEntity(new BlockPos(i, 100, 0)));
                net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().halt(false);
            }
        });
    }
}
