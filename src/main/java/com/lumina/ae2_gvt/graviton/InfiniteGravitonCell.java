package com.lumina.ae2_gvt.graviton;

import java.util.List;
import java.util.Optional;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * 無限引力子儲存元件, as AE2's creative cell: no recipe; in a drive it gives gravitons and anti-gravitons without
 * end and swallows any put in. It holds nothing, so there is nothing to save. The twin-star engine sees an
 * infinite source: extracting a long's worth succeeds.
 */
public final class InfiniteGravitonCell extends AEBaseItem {
    public InfiniteGravitonCell(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    /** As AE2's creative cell: the name, then the icons of what it gives. */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new StorageCellTooltipComponent(List.of(),
                List.of(new GenericStack(GravitonKey.GRAVITON, 1), new GenericStack(GravitonKey.ANTIGRAVITON, 1)), false, false));
    }

    /** Lets AE2 open the cell (during common setup). */
    public static void registerHandler() {
        StorageCells.addCellHandler(new Handler());
    }

    private static final class Handler implements ICellHandler {
        @Override
        public boolean isCell(ItemStack stack) {
            return stack.getItem() instanceof InfiniteGravitonCell;
        }

        @Override
        public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) {
            return isCell(stack) ? new Inventory(stack) : null;
        }
    }

    private record Inventory(ItemStack stack) implements StorageCell {
        @Override
        public CellState getStatus() {
            return CellState.NOT_EMPTY;
        }

        @Override
        public double getIdleDrain() {
            return 0;
        }

        @Override
        public void persist() {
        }

        @Override
        public boolean canFitInsideCell() {
            return false;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            return what instanceof GravitonKey ? Math.max(amount, 0) : 0;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            return what instanceof GravitonKey ? Math.max(amount, 0) : 0;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.set(GravitonKey.GRAVITON, Long.MAX_VALUE);
            out.set(GravitonKey.ANTIGRAVITON, Long.MAX_VALUE);
        }

        @Override
        public Component getDescription() {
            return stack.getHoverName();
        }
    }
}
