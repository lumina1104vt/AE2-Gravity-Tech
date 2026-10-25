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

    @GuiSync(10)
    public int progress;

    public GravityFactoryMenu(MenuType<?> menuType, int id, Inventory playerInventory, GravityFactoryBlockEntity factory) {
        super(menuType, id, playerInventory, factory);
        this.factory = factory;
        for (int i = 0; i < factory.getInputs().size(); i++) {
            this.addSlot(new AppEngSlot(factory.getInputs(), i), GravityFactory.INPUT_ROWS[i / GravityFactory.ROW_SLOTS]);
        }
        for (int i = 0; i < factory.getOutputs().size(); i++) {
            this.addSlot(new OutputSlot(factory.getOutputs(), i, null), GravityFactory.OUTPUT_ROWS[i / GravityFactory.ROW_SLOTS]);
        }
        this.addSlot(new AppEngSlot(factory.getCatalyst(), 0), GravityFactory.CATALYST);
        this.createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            progress = factory.progress;
        }
        super.broadcastChanges();
    }
}
