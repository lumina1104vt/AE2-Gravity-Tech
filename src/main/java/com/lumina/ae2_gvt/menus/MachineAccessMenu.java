package com.lumina.ae2_gvt.menus;

import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;
import com.lumina.twinstar.api.IMachineAccess;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.service.TwinStarService;

import appeng.api.networking.IGrid;
import appeng.api.stacks.GenericStack;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/**
 * The machine access interface's screen: the machine it stands for (a ghost slot), blocking, how
 * materials go in, and what its machine is doing.
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
    /** what its machine is doing; the rest says more about it ({@link TwinStarService.MachineView}) */
    @GuiSync(12)
    public TwinStarService.MachineState state = TwinStarService.MachineState.IDLE;
    @GuiSync(13)
    public @Nullable GenericStack output;
    @GuiSync(14)
    public long fed;
    @GuiSync(15)
    public long total;
    @GuiSync(16)
    public long runsIn;
    @GuiSync(17)
    public boolean chance;

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

    public MachineAccessInterface getPart() {
        return part;
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
            TwinStarService.MachineView view = TwinStarService.MachineView.of(TwinStarService.MachineState.NO_HUB);
            IGrid grid = part.getMainNode().getGrid();
            if (!part.isAccessActive()) {
                view = TwinStarService.MachineView.of(TwinStarService.MachineState.INACTIVE);
            } else if (grid != null && grid.getService(ITwinStarService.class) instanceof TwinStarService service) {
                view = service.machineView(part);
            }
            state = view.state();
            output = view.output() == null ? null : new GenericStack(view.output(), 0);
            fed = view.fed();
            total = view.total();
            runsIn = view.runsIn();
            chance = view.chance();
        }
        super.broadcastChanges();
    }
}
