package com.lumina.ae2_gvt.menus;

import java.math.BigInteger;
import java.util.List;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.RestrictedInputSlot;
import com.lumina.ae2_gvt.hub.TwinStarHubBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The hexagram hub controller's GUI: the black-hole and white-hole unit slots with what they give,
 * whether the hub is formed and running, and the hub's patterns (a window over all of them, as the
 * test hub's).
 */
public class HubControllerMenu extends AEBaseMenu {
    public static final SlotSemantic BLACK_HOLE_UNITS = SlotSemantics.register("AE2GVT_HUB_BLACK_HOLE_UNITS", false);
    public static final SlotSemantic WHITE_HOLE_UNITS = SlotSemantics.register("AE2GVT_HUB_WHITE_HOLE_UNITS", false);
    public static final int COLUMNS = 9, VISIBLE_ROWS = 4, WINDOW = COLUMNS * VISIBLE_ROWS;
    private static final String SCROLL = "scroll";

    private final TwinStarHubBlockEntity hub;
    private final PatternWindow window;

    /** Decimal strings; "" = no unit, "∞" = unlimited. */
    @GuiSync(10)
    public String capacity = "";
    @GuiSync(11)
    public String parallel = "";
    @GuiSync(12)
    public boolean formed;
    /** the engine runs on this hub now (formed, on a powered network, both slots filled) */
    @GuiSync(13)
    public boolean active;
    @GuiSync(14)
    public int ports;
    @GuiSync(15)
    public int stored;
    @GuiSync(16)
    public int scrollRow;

    public HubControllerMenu(MenuType<?> menuType, int id, Inventory playerInventory, TwinStarHubBlockEntity hub) {
        super(menuType, id, playerInventory, hub);
        this.hub = hub;
        addSlot(new AppEngSlot(hub.getUnits(), TwinStarHubBlockEntity.BLACK_SLOT), BLACK_HOLE_UNITS);
        addSlot(new AppEngSlot(hub.getUnits(), TwinStarHubBlockEntity.WHITE_SLOT), WHITE_HOLE_UNITS);
        this.window = new PatternWindow(isServerSide() ? hub.getPatterns() : null, WINDOW);
        for (int i = 0; i < WINDOW; i++) {
            RestrictedInputSlot slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, window, i);
            slot.setStackLimit(1);
            addSlot(slot, SlotSemantics.ENCODED_PATTERN);
        }
        createPlayerInventorySlots(playerInventory);
        registerClientAction(SCROLL, Integer.class, this::setScrollRow);
    }

    public List<Slot> getPatternSlots() {
        return getSlots(SlotSemantics.ENCODED_PATTERN);
    }

    /** Shift-click: a pattern goes to the next free slot of all 50,000 (not only the window); a unit to its slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot from = slots.get(idx);
        if (!isPlayerSideSlot(from) || !from.hasItem() || !PatternDetailsHelper.isEncodedPattern(from.getItem()))
            return super.quickMoveStack(player, idx);
        if (isServerSide()) {
            ItemStack rest = from.getItem();
            while (!rest.isEmpty()) {
                ItemStack next = hub.getInsertOnlyHandler().insertItem(0, rest, false);
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
        int max = Math.max(0, (TwinStarHubBlockEntity.MAX_PATTERNS + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS);
        row = Math.clamp(row, 0, max);
        if (isClientSide()) {
            if (row != scrollRow) {
                scrollRow = row;
                sendClientAction(SCROLL, row);
            }
        } else {
            scrollRow = row;
            window.setOffset(row * COLUMNS);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            capacity = text(hub.getUnits().getStackInSlot(TwinStarHubBlockEntity.BLACK_SLOT).isEmpty(), hub.getHostCapacity());
            parallel = text(hub.getUnits().getStackInSlot(TwinStarHubBlockEntity.WHITE_SLOT).isEmpty(), hub.getHostParallelism());
            formed = hub.isFormed();
            active = hub.isHostActive();
            ports = hub.linkedPorts();
            stored = hub.getStoredPatterns();
        }
        super.broadcastChanges();
    }

    private static String text(boolean empty, @Nullable BigInteger value) {
        return empty ? "" : value == null ? "∞" : value.toString();
    }
}
