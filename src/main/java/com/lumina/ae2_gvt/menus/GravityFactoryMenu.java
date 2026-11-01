package com.lumina.ae2_gvt.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;
import com.lumina.ae2_gvt.blockentities.GravityFactoryBlockEntity;
import com.lumina.ae2_gvt.blocks.GravityFactory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class GravityFactoryMenu extends AEBaseMenu {
    private final GravityFactoryBlockEntity factory;

    /** 0 … 1000 */
    @GuiSync(10)
    public int progress;
    @GuiSync(11)
    public long energy;
    @GuiSync(12)
    public long energyCapacity;
    @GuiSync(13)
    public long parallel = 1;

    public GravityFactoryMenu(MenuType<?> menuType, int id, Inventory playerInventory, GravityFactoryBlockEntity factory) {
        super(menuType, id, playerInventory, factory);
        this.factory = factory;
        var inputs = factory.getInputs().createMenuWrapper();
        for (int i = 0; i < inputs.size(); i++) {
            this.addSlot(new AppEngSlot(inputs, i), GravityFactory.INPUT_ROWS[i / GravityFactory.ROW_SLOTS]);
        }
        var outputs = factory.getOutputs().createMenuWrapper();
        for (int i = 0; i < outputs.size(); i++) {
            this.addSlot(new OutputSlot(outputs, i, null), GravityFactory.OUTPUT_ROWS[i / GravityFactory.ROW_SLOTS]);
        }
        this.addSlot(new AppEngSlot(factory.getCatalyst(), 0), GravityFactory.CATALYST);
        setupUpgrades(factory.getUpgrades());
        this.createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            progress = factory.progressPermille();
            energy = factory.energyStored();
            energyCapacity = factory.energyCapacity();
            parallel = factory.parallel();
        }
        super.broadcastChanges();
    }
}
