package com.lumina.ae2_gvt.client.screen;

import com.lumina.ae2_gvt.menus.MachineAccessMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How much of a stock slot's item the machine access interface keeps in its machine: AE2's own stock
 * amount dialog (its layout, assets/ae2/screens/set_stock_amount.json); 0 empties the slot.
 */
public class SetStockAmountScreen extends AESubScreen<MachineAccessMenu, MachineAccessScreen> {
    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;

    public SetStockAmountScreen(MachineAccessScreen parent, GenericStack currentStack, Slot slot) {
        super(parent, "/screens/set_stock_amount.json");
        this.currentStack = currentStack;
        this.slot = slot;
        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        ItemStack icon = new ItemStack(com.lumina.ae2_gvt.items.Items.MACHINE_ACCESS_INTERFACE.get());
        widgets.add("back", new TabButton(Icon.BACK, Component.translatable("item.ae2_gvt.machine_access_interface"),
                btn -> returnToParent()));
        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        amount.setLongValue(currentStack.amount());
        amount.setMaxValue(getMaxAmount());
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);
        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(MachineAccessMenu.STOCK, true);
        setSlotsHidden(MachineAccessMenu.MACHINE, true);
    }

    private void confirm() {
        amount.getLongValue().ifPresent(value -> {
            value = Longs.constrainToRange(value, 0, getMaxAmount());
            GenericStack stack = value <= 0 ? null : new GenericStack(currentStack.what(), value);
            PacketDistributor.sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index,
                    GenericStack.wrapInItemStack(stack)));
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return Integer.MAX_VALUE;
    }
}
