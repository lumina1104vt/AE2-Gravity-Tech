package com.lumina.ae2_gvt.prototype.terminal;

import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartItem;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** AE2's pattern encoding terminal plus the remembered machine (a ghost item, never dropped). */
public class TwinStarPatternTerminalPart extends PatternEncodingTerminalPart {
    private final AppEngInternalInventory machineInv = new AppEngInternalInventory(this, 1, 1);

    public TwinStarPatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    /** One ghost slot: the machine the encoded recipe runs in. */
    public InternalInventory getMachineInv() {
        return machineInv;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        machineInv.clear();
    }

    @Override
    public void readFromNBT(ValueInput input) {
        super.readFromNBT(input);
        machineInv.readFromNBT(input, "machine");
    }

    @Override
    public void writeToNBT(ValueOutput output) {
        super.writeToNBT(output);
        machineInv.writeToNBT(output, "machine");
    }

    @Override
    public MenuType<?> getMenuType(Player p) {
        return TwinStarTerminal.MENU.get();
    }
}
