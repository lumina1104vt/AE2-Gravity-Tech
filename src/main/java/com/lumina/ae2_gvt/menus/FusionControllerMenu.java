package com.lumina.ae2_gvt.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.OutputSlot;
import appeng.menu.guisync.GuiSync;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** The fusion controller's GUI: its slots; temperature, status and energy to show; the energy-input cap to set. */
public class FusionControllerMenu extends AEBaseMenu implements IProgressProvider {
    private static final SlotSemantic[] MARK_ROWS = rows("FUSION_MARK_ROW_"), INPUT_ROWS = rows("FUSION_INPUT_ROW_"),
            OUTPUT_ROWS = rows("FUSION_OUTPUT_ROW_");

    private static final String SET_CAP = "setEnergyCap", SET_MARK = "setMarkAmount";

    private final FusionControllerBlockEntity controller;

    @GuiSync(10)
    public long temperature;
    @GuiSync(11)
    public long equilibrium;
    @GuiSync(12)
    public long need;
    @GuiSync(13)
    public long energyCap;
    @GuiSync(14)
    public long energyIn;
    @GuiSync(15)
    public long energyOut;
    @GuiSync(16)
    public boolean formed;
    @GuiSync(17)
    public int progress;
    @GuiSync(18)
    public int time = 1;

    public FusionControllerMenu(MenuType<?> menuType, int id, Inventory playerInventory, FusionControllerBlockEntity controller) {
        super(menuType, id, playerInventory, controller);
        this.controller = controller;
        registerClientAction(SET_CAP, Long.class, this::setEnergyCap);
        registerClientAction(SET_MARK, MarkAmount.class, m -> setMarkAmount(m.slot, m.amount));
        energyCap = controller.energyCap();
        var marks = controller.getMarks().createMenuWrapper();
        for (int i = 0; i < marks.size(); i++) {
            addSlot(new FakeSlot(marks, i), MARK_ROWS[i / 3]);
        }
        var inputs = controller.getInputs().createMenuWrapper();
        for (int i = 0; i < inputs.size(); i++) {
            addSlot(new AppEngSlot(inputs, i), INPUT_ROWS[i / 3]);
        }
        for (int i = 0; i < controller.getOutputs().size(); i++) {
            addSlot(new OutputSlot(controller.getOutputs(), i, null), OUTPUT_ROWS[i / 5]);
        }
        createPlayerInventorySlots(playerInventory);
    }

    private static SlotSemantic[] rows(String prefix) {
        SlotSemantic[] rows = new SlotSemantic[4];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = SlotSemantics.register(prefix + i, false);
        }
        return rows;
    }

    @Override
    public int getCurrentProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return time;
    }

    /** How much a marked slot is kept at. */
    public void setMarkAmount(int slot, long amount) {
        if (isClientSide()) {
            MarkAmount m = new MarkAmount();
            m.slot = slot;
            m.amount = amount;
            sendClientAction(SET_MARK, m);
        } else {
            controller.setMarkAmount(slot, amount);
        }
    }

    public static final class MarkAmount {
        public int slot;
        public long amount;
    }

    public void setEnergyCap(long cap) {
        if (isClientSide()) {
            energyCap = cap;
            sendClientAction(SET_CAP, cap);
        } else {
            controller.setEnergyCap(cap);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            temperature = Math.round(controller.temperature());
            equilibrium = Math.round(controller.equilibrium());
            need = controller.need();
            energyCap = controller.energyCap();
            energyIn = controller.energyIn();
            energyOut = controller.energyOut();
            formed = controller.formed();
            progress = controller.progress();
            time = controller.reactionTime();
        }
        super.broadcastChanges();
    }

    /** 0 idle (not formed), 1 heating (will reach the recipe), 2 running, 3 too cold (the cap can't get there). */
    public int status() {
        if (!formed) {
            return 0;
        }
        if (temperature >= need) {
            return 2;
        }
        return equilibrium >= need ? 1 : 3;
    }
}
