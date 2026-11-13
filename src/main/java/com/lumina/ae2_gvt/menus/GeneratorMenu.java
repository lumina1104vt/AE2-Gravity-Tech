package com.lumina.ae2_gvt.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** A generator's GUI: what it makes now, how long the fuel burns, the dust slot of the crystal one, the white holes of the accretion one. */
public class GeneratorMenu extends AEBaseMenu {
    private final PowerGeneratorBlockEntity generator;

    @GuiSync(10)
    public long output;
    @GuiSync(11)
    public int burn;
    @GuiSync(12)
    public int holes;
    @GuiSync(13)
    public int take;

    private static final String SET_TAKE = "setTake";

    public GeneratorMenu(MenuType<?> menuType, int id, Inventory playerInventory, PowerGeneratorBlockEntity generator) {
        super(menuType, id, playerInventory, generator);
        this.generator = generator;
        registerClientAction(SET_TAKE, Integer.class, this::setTake);
        if (generator.kind() == PowerGeneratorBlockEntity.Kind.CRYSTAL)
            addSlot(new AppEngSlot(generator.getFuel(), 0), SlotSemantics.MACHINE_INPUT);
        createPlayerInventorySlots(playerInventory);
    }

    /** The button: the next thing to take (any, graviton, anti-graviton). */
    public void cycleTake() {
        int next = (take + 1) % PowerGeneratorBlockEntity.Take.values().length;
        if (isClientSide())
            sendClientAction(SET_TAKE, next);
        else
            setTake(next);
    }

    private void setTake(int value) {
        if (generator.kind() == PowerGeneratorBlockEntity.Kind.GRAVITON)
            generator.setTake(PowerGeneratorBlockEntity.Take.values()[Math.floorMod(value, PowerGeneratorBlockEntity.Take.values().length)]);
    }

    public PowerGeneratorBlockEntity.Kind kind() {
        return generator.kind();
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            output = generator.lastOutput();
            burn = generator.burnTicks();
            holes = generator.holes();
            take = generator.take().ordinal();
        }
        super.broadcastChanges();
    }
}
