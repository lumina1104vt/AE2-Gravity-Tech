package com.lumina.ae2_gvt.menus;

import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;
import com.lumina.twinstar.api.IMachineAccess;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.service.TwinStarService;

import appeng.api.networking.IGrid;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * The machine access interface's screen: the machine it stands for (a ghost slot), blocking, how
 * materials go in, and what it is feeding now.
 */
public class MachineAccessMenu extends AEBaseMenu {
    public static final SlotSemantic MACHINE = SlotSemantics.register("AE2GVT_ACCESS_MACHINE", false);
    private static final String TOGGLE_BLOCKING = "toggleBlocking";
    private static final String CYCLE_MODE = "cycleInsertMode";

    private final MachineAccessInterface part;

    @GuiSync(10)
    public boolean blocking;
    @GuiSync(11)
    public int insertMode;
    /** "" = idle; otherwise output name, amount in and amount to go in, tab-separated */
    @GuiSync(12)
    public String feeding = "";

    public MachineAccessMenu(MenuType<?> menuType, int id, Inventory playerInventory, MachineAccessInterface part) {
        super(menuType, id, playerInventory, part);
        this.part = part;
        FakeSlot slot = new FakeSlot(part.getMachineInv(), 0);
        slot.setHideAmount(true);
        addSlot(slot, MACHINE);
        createPlayerInventorySlots(playerInventory);
        registerClientAction(TOGGLE_BLOCKING, this::toggleBlocking);
        registerClientAction(CYCLE_MODE, this::cycleInsertMode);
    }

    public IMachineAccess.InsertMode getInsertMode() {
        IMachineAccess.InsertMode[] all = IMachineAccess.InsertMode.values();
        return all[Math.floorMod(insertMode, all.length)];
    }

    public void toggleBlocking() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_BLOCKING);
            return;
        }
        part.setBlocking(!part.isBlocking());
    }

    public void cycleInsertMode() {
        if (isClientSide()) {
            sendClientAction(CYCLE_MODE);
            return;
        }
        IMachineAccess.InsertMode[] all = IMachineAccess.InsertMode.values();
        part.setInsertMode(all[(part.getInsertMode().ordinal() + 1) % all.length]);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            blocking = part.isBlocking();
            insertMode = part.getInsertMode().ordinal();
            feeding = "";
            IGrid grid = part.getMainNode().getGrid();
            if (grid != null && grid.getService(ITwinStarService.class) instanceof TwinStarService service) {
                TwinStarService.FeedView view = service.feedView(part);
                if (view != null) {
                    String name = view.output() == null ? "?" : view.output().getDisplayName().getString();
                    feeding = name + "\t" + view.fed() + "\t" + view.total();
                }
            }
        }
        super.broadcastChanges();
    }
}
