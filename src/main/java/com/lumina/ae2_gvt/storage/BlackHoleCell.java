package com.lumina.ae2_gvt.storage;

import java.util.List;
import java.util.UUID;

import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.items.AEBaseItem;
import com.lumina.ae2_gvt.components.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** 黑洞存儲磁盤: goes in an ME drive; holds anything, without limit. The item has an id, the world has the contents. */
public final class BlackHoleCell extends AEBaseItem {
    public BlackHoleCell(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.ae2_gvt.black_hole_storage_cell.tooltip").withStyle(ChatFormatting.GRAY));
    }

    /** Lets AE2 open the disks (during common setup). */
    public static void registerHandler() {
        StorageCells.addCellHandler(new Handler());
    }

    private static final class Handler implements ICellHandler {
        @Override
        public boolean isCell(ItemStack stack) {
            return stack.getItem() instanceof BlackHoleCell;
        }

        @Override
        public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || !isCell(stack))
                return null;
            UUID id = stack.get(DataComponents.BLACK_HOLE_DISK.get());
            if (id == null) {
                id = UUID.randomUUID();
                stack.set(DataComponents.BLACK_HOLE_DISK.get(), id);
                if (host != null)
                    host.saveChanges();
            }
            BlackHoleDisks disks = BlackHoleDisks.of(server);
            return new BlackHoleCellInventory(stack, disks, disks.contents(id), host);
        }
    }
}
