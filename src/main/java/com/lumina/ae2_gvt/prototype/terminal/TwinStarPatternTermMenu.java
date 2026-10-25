package com.lumina.ae2_gvt.prototype.terminal;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * AE2's pattern encoding menu, locked to the processing layout, plus the machine ghost slot.
 * Extending AE2's menu keeps its ME grid, encoding and recipe-transfer handling.
 */
public class TwinStarPatternTermMenu extends PatternEncodingTermMenu {
    private final FakeSlot machineSlot;

    public TwinStarPatternTermMenu(MenuType<?> menuType, int id, Inventory ip, TwinStarPatternTerminalPart host) {
        super(menuType, id, ip, host, true);
        this.machineSlot = new FakeSlot(host.getMachineInv(), 0);
        this.machineSlot.setHideAmount(true);
        this.addSlot(this.machineSlot, TwinStarTerminal.MACHINE_SLOT);

        // one combined pattern type: no crafting / smithing / stonecutting modes
        this.mode = EncodingMode.PROCESSING;
        if (this.isServerSide()) {
            host.getLogic().setMode(EncodingMode.PROCESSING);
        }
    }

    /**
     * AE2 encodes a processing pattern; we turn it into a synthesis pattern (same data) and stamp
     * the machine on it (or clear a stale one).
     */
    @Override
    public void encode() {
        super.encode();
        if (this.isClientSide()) {
            return;
        }
        Slot encoded = this.getSlots(SlotSemantics.ENCODED_PATTERN).get(0);
        ItemStack pattern = encoded.getItem();
        if (!PatternDetailsHelper.isEncodedPattern(pattern)) {
            return;
        }
        if (!pattern.is(TwinStarTerminal.PATTERN.get())) {
            pattern = pattern.transmuteCopy(TwinStarTerminal.PATTERN.get());
        }
        ItemStack machine = this.machineSlot.getItem();
        if (machine.isEmpty()) {
            pattern.remove(TwinStarTerminal.PATTERN_MACHINE.get());
        } else {
            pattern.set(TwinStarTerminal.PATTERN_MACHINE.get(), machine.getItemHolder());
        }
        encoded.set(pattern);
    }

    public FakeSlot getMachineSlot() {
        return machineSlot;
    }

    @Override
    public void setMode(EncodingMode mode) {
        // the mode is fixed
    }
}
