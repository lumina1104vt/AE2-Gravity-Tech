package com.lumina.ae2_gvt.menus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import appeng.api.inventories.InternalInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.util.inv.AppEngInternalInventory;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.twinstar.util.AmountParser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The dummy CPU's GUI: capacity and parallel (0 = unlimited) and its patterns. The block entity has
 * 50,000 pattern slots; the menu only has the visible {@link #WINDOW} and moves them over the
 * patterns as the player scrolls (every menu slot is synced each tick, so all 50,000 would be far
 * too many). The stored patterns are packed at the front; the screen shows them plus one empty slot.
 */
public class DummyCpuMenu extends AEBaseMenu {
    public static final int COLUMNS = 9, VISIBLE_ROWS = 4, WINDOW = COLUMNS * VISIBLE_ROWS;
    private static final String SET_CAPACITY = "setCapacity";
    private static final String SET_PARALLEL = "setParallel";
    private static final String SCROLL = "scroll";

    private final DummyTwinStarCPUBlockEntity cpu;

    /** Decimal strings (any size); "0" = unlimited. */
    @GuiSync(10)
    public String capacity = "0";
    @GuiSync(11)
    public String parallel = "0";
    /** stored patterns (the screen sizes its scrollbar with it) */
    @GuiSync(12)
    public int stored;
    /** first visible row of the window */
    @GuiSync(13)
    public int scrollRow;

    private final PatternWindow window;

    public DummyCpuMenu(MenuType<?> menuType, int id, Inventory playerInventory, DummyTwinStarCPUBlockEntity cpu) {
        super(menuType, id, playerInventory, cpu);
        this.cpu = cpu;
        this.window = new PatternWindow(isServerSide() ? cpu.getPatterns() : null);
        for (int i = 0; i < WINDOW; i++) {
            RestrictedInputSlot slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, window, i);
            slot.setStackLimit(1);
            addSlot(slot, SlotSemantics.ENCODED_PATTERN);
        }
        createPlayerInventorySlots(playerInventory);
        registerClientAction(SET_CAPACITY, String.class, this::setCapacity);
        registerClientAction(SET_PARALLEL, String.class, this::setParallel);
        registerClientAction(SCROLL, Integer.class, this::setScrollRow);
        capacity = cpu.getCapacity().toString();
        parallel = cpu.getParallel().toString();
    }

    public List<Slot> getPatternSlots() {
        return getSlots(SlotSemantics.ENCODED_PATTERN);
    }

    /**
     * Shift-click from the player's inventory: the pattern goes to the next free slot of all 50,000,
     * not only into the visible window.
     */
    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int idx) {
        Slot from = slots.get(idx);
        if (from instanceof RestrictedInputSlot || !from.hasItem())
            return super.quickMoveStack(player, idx);
        ItemStack stack = from.getItem();
        if (!appeng.api.crafting.PatternDetailsHelper.isEncodedPattern(stack))
            return super.quickMoveStack(player, idx);
        if (isServerSide()) {
            ItemStack rest = stack;
            while (!rest.isEmpty()) {
                ItemStack next = cpu.getInsertOnlyHandler().insertItem(0, rest, false);
                if (next.getCount() == rest.getCount())
                    break;      // full
                rest = next;
            }
            from.set(rest);
        }
        return ItemStack.EMPTY;
    }

    /** Moves the window; the server then sends the slots whose contents changed. */
    public void setScrollRow(int row) {
        int max = Math.max(0, (DummyTwinStarCPUBlockEntity.MAX_PATTERNS + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS);
        row = Math.clamp(row, 0, max);
        if (isClientSide()) {
            if (row != scrollRow) {
                scrollRow = row;
                sendClientAction(SCROLL, row);
            }
        } else {
            scrollRow = row;
            window.offset = row * COLUMNS;
        }
    }

    /**
     * The visible slots. Server: a view of the block entity's patterns from {@code offset}. Client:
     * its own slots, holding exactly what the server sent for each menu slot (the client's block
     * entity has no patterns, and reading it at a moved offset would show stale items).
     */
    private static final class PatternWindow implements InternalInventory {
        private final @Nullable InternalInventory patterns;
        private final AppEngInternalInventory client = new AppEngInternalInventory(WINDOW);
        int offset;

        PatternWindow(@Nullable InternalInventory patterns) {
            this.patterns = patterns;
        }

        @Override
        public int size() {
            return WINDOW;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (patterns == null)
                return client.getStackInSlot(slot);
            int i = offset + slot;
            return i < patterns.size() ? patterns.getStackInSlot(i) : ItemStack.EMPTY;
        }

        @Override
        public void setItemDirect(int slot, ItemStack stack) {
            if (patterns == null) {
                client.setItemDirect(slot, stack);
                return;
            }
            int i = offset + slot;
            if (i < patterns.size())
                patterns.setItemDirect(i, stack);
        }
    }

    public void setCapacity(String value) {
        BigInteger v = parse(value);
        if (v == null)
            return;
        if (isClientSide()) {
            capacity = v.toString();
            sendClientAction(SET_CAPACITY, capacity);
        } else {
            cpu.setCapacity(v);
        }
    }

    public void setParallel(String value) {
        BigInteger v = parse(value);
        if (v == null)
            return;
        if (isClientSide()) {
            parallel = v.toString();
            sendClientAction(SET_PARALLEL, parallel);
        } else {
            cpu.setParallel(v);
        }
    }

    private static final int MAX_SYNC_DIGITS = 30_000;

    /** Plain digits (any length) or twin-star's amount form (3E, 2e18, 5K...); empty = 0 (unlimited). */
    private static @Nullable BigInteger parse(String value) {
        String s = value.trim();
        if (s.isEmpty())
            return BigInteger.ZERO;
        BigDecimal d = AmountParser.parse(s);
        try {
            if (d == null)
                d = new BigDecimal(s);
            // synced to the screen as a decimal string, which AE2 caps at 32767 characters
            if ((long) d.precision() - d.scale() > MAX_SYNC_DIGITS)
                return null;
            return d.setScale(0, RoundingMode.DOWN).toBigInteger().max(BigInteger.ZERO);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            capacity = cpu.getCapacity().toString();
            parallel = cpu.getParallel().toString();
            stored = cpu.getStoredPatterns();
        }
        super.broadcastChanges();
    }
}
