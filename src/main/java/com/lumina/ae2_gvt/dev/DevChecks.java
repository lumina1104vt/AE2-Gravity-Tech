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
        if (Boolean.getBoolean("ae2_gvt.devGregtech")) {
            // what GregTech's item input bus gives at each side (the machine access interface asks the same way)
            final int[] wait = {0};
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
                if (e.phase != TickEvent.Phase.END)
                    return;
                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server == null || ++wait[0] != 60)
                    return;
                var level = server.overworld();
                for (var entry : net.minecraftforge.registries.ForgeRegistries.BLOCKS.getEntries()) {
                    String path = entry.getKey().location().getPath();
                    if (!entry.getKey().location().getNamespace().equals("gtceu") || !(path.equals("lv_item_import_bus") || path.equals("lv_input_bus")))
                        continue;
                    BlockPos pos = new BlockPos(0, 100, 0);
                    level.setBlock(pos, entry.getValue().defaultBlockState(), 3);
                    AE2GravityTech.LOGGER.info("dev gregtech: placed {} -> {}", path, level.getBlockEntity(pos));
                    var be = level.getBlockEntity(pos);
                    for (var side : new net.minecraft.core.Direction[] {null, net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH,
                            net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST})
                        AE2GravityTech.LOGGER.info("dev gregtech: item handler at {} = {} ({}); facing {}", side,
                                com.lumina.twinstar.util.Caps.item(level, pos, side), be == null ? "-" : be.getClass().getSimpleName(), level.getBlockState(pos));
                    server.halt(false);
                    return;
                }
                AE2GravityTech.LOGGER.info("dev gregtech: no input bus block found");
                server.halt(false);
            });
        }
        if (Boolean.getBoolean("ae2_gvt.devQuantum")) {
            // a quantum controller a few blocks from whoever joins, its settings screen opened for them
            final java.util.List<Object[]> waiting = new java.util.ArrayList<>();
            MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
                if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    BlockPos pos = player.blockPosition().offset(0, 0, 3);
                    player.serverLevel().setBlock(pos, com.lumina.ae2_gvt.quantum.QuantumController.BLOCK.get().defaultBlockState(), 3);
                    waiting.add(new Object[] {player, pos, 60});
                }
            });
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
                if (e.phase != TickEvent.Phase.END)
                    return;
                for (var it = waiting.iterator(); it.hasNext(); ) {
                    Object[] w = it.next();
                    if ((int) w[2] - 1 > 0) {
                        w[2] = (int) w[2] - 1;
                        continue;
                    }
                    it.remove();
                    var player = (net.minecraft.server.level.ServerPlayer) w[0];
                    if (player.serverLevel().getBlockEntity((BlockPos) w[1]) instanceof com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity be) {
                        appeng.menu.MenuOpener.open(com.lumina.ae2_gvt.menus.Menus.QUANTUM_CONTROLLER.get(), player,
                                appeng.menu.locator.MenuLocators.forBlockEntity(be));
                        AE2GravityTech.LOGGER.info("dev quantum: menu opened");
                    } else {
                        AE2GravityTech.LOGGER.error("dev quantum: no quantum controller at {}", w[1]);
                    }
                }
            });
        }
        if (Boolean.getBoolean("ae2_gvt.devHoles")) {
            // a black and a white hole in front of the spawn, for a test client to look at (the server keeps running)
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                ServerLevel level = e.getServer().overworld();
                BlockPos spawn = level.getSharedSpawnPos();
                level.setBlock(spawn.offset(-2, 3, 12), com.lumina.ae2_gvt.hole.GravityHole.BLACK_HOLE.get().defaultBlockState(), 3);
                level.setBlock(spawn.offset(-10, 3, 20), com.lumina.ae2_gvt.hole.GravityHole.WHITE_HOLE.get().defaultBlockState(), 3);
                AE2GravityTech.LOGGER.info("dev holes placed round {}", spawn);
            });
            // whoever joins is put in front of them, looking their way
            MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
                if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    BlockPos spawn = player.serverLevel().getSharedSpawnPos();
                    player.teleportTo(player.serverLevel(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 5.5, 21f, -20f);
                }
            });
        }
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
            secondaryOutputCheck();
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

    /** A pattern making stone and, second, dirt: dirt must be craftable too (AE2 alone lists the first output only), and gone again once the provider is. */
    private static void secondaryOutputCheck() {
        try {
            var stone = appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.STONE);
            var dirt = appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.DIRT);
            appeng.api.crafting.IPatternDetails pattern = new appeng.api.crafting.IPatternDetails() {
                @Override public appeng.api.stacks.AEItemKey getDefinition() { return stone; }
                @Override public IInput[] getInputs() { return new IInput[0]; }
                @Override public appeng.api.stacks.GenericStack[] getOutputs() {
                    return new appeng.api.stacks.GenericStack[] {new appeng.api.stacks.GenericStack(stone, 1), new appeng.api.stacks.GenericStack(dirt, 2)};
                }
            };
            appeng.api.networking.crafting.ICraftingProvider provider = new appeng.api.networking.crafting.ICraftingProvider() {
                @Override public java.util.List<appeng.api.crafting.IPatternDetails> getAvailablePatterns() { return java.util.List.of(pattern); }
                @Override public int getPatternPriority() { return 0; }
                @Override public boolean pushPattern(appeng.api.crafting.IPatternDetails d, appeng.api.stacks.KeyCounter[] in) { return false; }
                @Override public boolean isBusy() { return false; }
            };
            appeng.api.networking.IGridNode node = (appeng.api.networking.IGridNode) java.lang.reflect.Proxy.newProxyInstance(
                    DevChecks.class.getClassLoader(), new Class<?>[] {appeng.api.networking.IGridNode.class},
                    (proxy, m, args) -> m.getName().equals("getService") ? provider : m.getName().equals("hashCode") ? 1 : m.getName().equals("equals") ? proxy == args[0] : null);
            var providers = new appeng.me.service.helpers.NetworkCraftingProviders();
            providers.addProvider(node);
            boolean listed = providers.getCraftableKeys().contains(stone) && providers.getCraftableKeys().contains(dirt)
                    && providers.getCraftingFor(dirt).contains(pattern) && providers.getCraftingFor(stone).size() == 1;
            providers.removeProvider(node);
            boolean gone = providers.getCraftableKeys().isEmpty() && providers.getCraftingFor(dirt).isEmpty();
            AE2GravityTech.LOGGER.info("dev craftable test: second output listed={} and unlisted again={}", listed, gone);
        } catch (Throwable t) {
            AE2GravityTech.LOGGER.error("dev craftable test failed", t);
        }
    }
}
