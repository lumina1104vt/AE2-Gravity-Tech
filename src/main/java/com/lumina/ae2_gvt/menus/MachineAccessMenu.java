package com.lumina.ae2_gvt.menus;

import com.lumina.ae2_gvt.items.parts.MachineAccessCore;
import com.lumina.ae2_gvt.items.parts.MachineAccessHost;
import com.lumina.twinstar.api.IMachineAccess;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.service.TwinStarService;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.GenericStack;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The machine access interface's screen. The upper part lists the machines it stands for, a page of
 * {@link #PER_PAGE} at a time, each with a slot for the machine it also stands for; one is selected. The lower part
 * is the selected machine's: in provider mode its settings and what it is doing, in ME interface mode what
 * it keeps stocked in it (and the network's count of each). The two parts page apart.
 */
public class MachineAccessMenu extends AEBaseMenu {
    /** the optional machine of each listed machine, like AE2's slots in a row */
    public static final SlotSemantic MACHINE = SlotSemantics.register("AE2GVT_ACCESS_MACHINE", false);
    /** what the interface keeps in the selected machine, like an ME interface's config slots */
    public static final SlotSemantic STOCK = SlotSemantics.register("AE2GVT_ACCESS_STOCK", false);
    public static final int PER_PAGE = 9;
    private static final String TOGGLE_BLOCKING = "toggleBlocking";
    private static final String CYCLE_MODE = "cycleInsertMode";
    private static final String CYCLE_GROUP = "cycleGroup";
    private static final String CYCLE_RANGE = "cycleSubRange";
    private static final String TOGGLE_PULSE = "toggleRedstonePulse";
    private static final String NET_CLICK = "netClick";
    private static final String SELECT = "selectMachine";
    private static final String STEP_PAGE = "stepMachinePage";
    private static final String SET_MODE = "setAccessMode";
    private static final String TOGGLE_RECOVER = "toggleRecover", TOGGLE_RECOVER_ENERGY = "toggleRecoverEnergy";
    private static final String RECOVER_FACE = "toggleRecoverFace";
    private static final String CYCLE_ROLE = "cycleRole", STEP_SLOT = "stepSlot";
    /** Pairing numbers: 0 (none) to this. */
    private static final int MAX_GROUP = 16;

    private final MachineAccessCore part;

    @GuiSync(10)
    public int blockMode;
    @GuiSync(11)
    public int insertMode;
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
    @GuiSync(18)
    public int group;
    @GuiSync(19)
    public int subRange = 3;
    @GuiSync(20)
    public boolean redstonePulse;
    /** machines listed (1 when it only has the block it sits on), the selected one, and the page shown */
    @GuiSync(21)
    public int machineCount = 1;
    @GuiSync(22)
    public int selected;
    @GuiSync(23)
    public int machinePage;
    /** {@link MachineAccessCore.Mode} ordinal */
    @GuiSync(24)
    public int mode;
    /** ME interface mode takes back the machine's output: on or off, and the faces ({@link net.minecraft.core.Direction#ordinal} bits) */
    @GuiSync(25)
    public boolean recover;
    @GuiSync(26)
    public int recoverMask;
    /** whether the FE a machine gives out is taken back too */
    @GuiSync(27)
    public boolean recoverEnergy;
    /** the selected block's role ({@link IMachineAccess.PortRole} ordinal), its slot number as set (0 = by place) and as it counts */
    @GuiSync(28)
    public int role;
    @GuiSync(29)
    public int slot;
    @GuiSync(39)
    public int slotNow;
    /** the network's count of what the selected machine's stock slots hold, one per slot */
    @GuiSync(30) public long net0;
    @GuiSync(31) public long net1;
    @GuiSync(32) public long net2;
    @GuiSync(33) public long net3;
    @GuiSync(34) public long net4;
    @GuiSync(35) public long net5;
    @GuiSync(36) public long net6;
    @GuiSync(37) public long net7;
    @GuiSync(38) public long net8;

    private final FakeSlot machineSlot;
    private final List<FakeSlot> stockSlots = new ArrayList<>();

    public MachineAccessMenu(MenuType<?> menuType, int id, Inventory playerInventory, MachineAccessHost host) {
        super(menuType, id, playerInventory, host);
        this.part = host.core();
        // the optional-machine slot is the selected machine's
        InternalInventory alt = new InternalInventory() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return selected < part.machineCount() ? part.settings(selected).getMachineStack() : ItemStack.EMPTY;
            }

            @Override
            public void setItemDirect(int slot, ItemStack stack) {
                if (selected < part.machineCount())
                    part.settings(selected).setMachineStack(stack);
            }
        };
        machineSlot = new FakeSlot(alt, 0);
        machineSlot.setHideAmount(true);
        addSlot(machineSlot, MACHINE);
        // the stock slots are the selected machine's
        InternalInventory stock = new InternalInventory() {
            @Override
            public int size() {
                return MachineAccessCore.STOCK_SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return part.settings(selected).getStockWrapper().getStackInSlot(slot);
            }

            @Override
            public void setItemDirect(int slot, ItemStack stack) {
                part.settings(selected).getStockWrapper().setItemDirect(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return part.settings(selected).getStockWrapper().getSlotLimit(slot);
            }
        };
        for (int i = 0; i < MachineAccessCore.STOCK_SLOTS; i++) {
            FakeSlot slot = new FakeSlot(stock, i);
            stockSlots.add(slot);
            addSlot(slot, STOCK);
        }
        createPlayerInventorySlots(playerInventory);
        updateSlots();
        registerClientAction(TOGGLE_BLOCKING, this::toggleBlocking);
        registerClientAction(CYCLE_MODE, this::cycleInsertMode);
        registerClientAction(CYCLE_GROUP, this::cycleGroup);
        registerClientAction(CYCLE_RANGE, Boolean.class, this::cycleSubRange);
        registerClientAction(TOGGLE_PULSE, this::toggleRedstonePulse);
        registerClientAction(NET_CLICK, Integer.class, this::netClick);
        registerClientAction(SELECT, Integer.class, this::selectMachine);
        registerClientAction(STEP_PAGE, Boolean.class, this::stepMachinePage);
        registerClientAction(SET_MODE, Integer.class, this::setAccessMode);
        registerClientAction(TOGGLE_RECOVER, this::toggleRecover);
        registerClientAction(TOGGLE_RECOVER_ENERGY, this::toggleRecoverEnergy);
        registerClientAction(RECOVER_FACE, Integer.class, this::toggleRecoverFace);
        registerClientAction(CYCLE_ROLE, Boolean.class, this::cycleRole);
        registerClientAction(STEP_SLOT, Integer.class, this::stepSlot);
    }

    public MachineAccessCore getPart() {
        return part;
    }

    public MachineAccessCore.Mode getAccessMode() {
        MachineAccessCore.Mode[] all = MachineAccessCore.Mode.values();
        return all[Math.floorMod(mode, all.length)];
    }

    public int machinePages() {
        return Math.max(1, (machineCount + PER_PAGE - 1) / PER_PAGE);
    }

    /** The optional-machine slot of the selected machine. */
    public FakeSlot machineSlot() {
        return machineSlot;
    }

    public List<FakeSlot> stockSlots() {
        return stockSlots;
    }

    /** The network's count for stock slot {@code i} of the selected machine (as last synced). */
    public long networkCount(int i) {
        return switch (i) {
            case 0 -> net0;
            case 1 -> net1;
            case 2 -> net2;
            case 3 -> net3;
            case 4 -> net4;
            case 5 -> net5;
            case 6 -> net6;
            case 7 -> net7;
            default -> net8;
        };
    }

    /** Only the listed machines' slots are on, and the stock slots only in ME interface mode. */
    public void updateSlots() {
        machineSlot.setSlotEnabled(getAccessMode() == MachineAccessCore.Mode.PROVIDER);
        boolean stock = getAccessMode() == MachineAccessCore.Mode.INTERFACE;
        for (FakeSlot s : stockSlots)
            s.setSlotEnabled(stock);
    }

    public IMachineAccess.BlockMode getBlockMode() {
        IMachineAccess.BlockMode[] all = IMachineAccess.BlockMode.values();
        return all[Math.floorMod(blockMode, all.length)];
    }

    public IMachineAccess.InsertMode getInsertMode() {
        IMachineAccess.InsertMode[] all = IMachineAccess.InsertMode.values();
        return all[Math.floorMod(insertMode, all.length)];
    }

    public IMachineAccess.PortRole getRole() {
        IMachineAccess.PortRole[] all = IMachineAccess.PortRole.values();
        return all[Math.floorMod(role, all.length)];
    }

    /** Whether the selected block is a hatch of a machine (not a machine of its own). */
    public boolean isHatch() {
        return getRole() != IMachineAccess.PortRole.MACHINE;
    }

    /** Whether the selected block is an input, which is numbered. */
    public boolean isNumbered() {
        return getRole() == IMachineAccess.PortRole.ITEM_IN || getRole() == IMachineAccess.PortRole.FLUID_IN;
    }

    // ── actions (client asks, server does) ──

    /** The selected block is something else: the next (or, {@code back}, the previous) role. */
    public void cycleRole(Boolean back) {
        if (isClientSide()) {
            sendClientAction(CYCLE_ROLE, back);
            return;
        }
        IMachineAccess.PortRole[] all = IMachineAccess.PortRole.values();
        var s = part.settings(selected);
        s.setRole(all[Math.floorMod(s.getRole().ordinal() + (back ? -1 : 1), all.length)]);
    }

    /** The slot number of an input hatch moves by {@code delta} (0 = by its place among the machine's hatches of the kind). */
    public void stepSlot(Integer delta) {
        if (isClientSide()) {
            sendClientAction(STEP_SLOT, delta);
            return;
        }
        var s = part.settings(selected);
        s.setSlot(s.getSlot() + delta);
    }

    public void setAccessMode(Integer ordinal) {
        if (isClientSide()) {
            sendClientAction(SET_MODE, ordinal);
            return;
        }
        part.setMode(MachineAccessCore.Mode.values()[Math.floorMod(ordinal, MachineAccessCore.Mode.values().length)]);
    }

    public void toggleRecover() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_RECOVER);
            return;
        }
        var s = part.settings(selected);
        s.setRecover(!s.isRecover());
    }

    public void toggleRecoverEnergy() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_RECOVER_ENERGY);
            return;
        }
        var s = part.settings(selected);
        s.setRecoverEnergy(!s.isRecoverEnergy());
    }

    public void toggleRecoverFace(Integer face) {
        if (isClientSide()) {
            sendClientAction(RECOVER_FACE, face);
            return;
        }
        part.settings(selected).toggleRecoverFace(net.minecraft.core.Direction.values()[Math.floorMod(face, 6)]);
    }

    public boolean recoversFrom(net.minecraft.core.Direction d) {
        return (recoverMask & (1 << d.ordinal())) != 0;
    }

    /**
     * A click on the cell under stock slot {@code slot}, which is that item as the network has it: with an item on the cursor
     * it goes in (all of it, or one on a right click); with an empty cursor a stack comes out (half on a right click), or with
     * shift goes straight to the inventory. {@code code} is slot | right click << 4 | shift << 5.
     */
    public void netClick(Integer code) {
        if (isClientSide()) {
            sendClientAction(NET_CLICK, code);
            return;
        }
        int slot = code & 15;
        boolean right = (code & 16) != 0, shift = (code & 32) != 0;
        if (slot >= MachineAccessCore.STOCK_SLOTS)
            return;
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {      // whatever is on the cursor goes into the network, it is not kept in the cell
            long in = part.networkInsert(appeng.api.stacks.AEItemKey.of(carried), right ? 1 : carried.getCount(), getPlayer());
            carried.shrink((int) in);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            return;
        }
        GenericStack g = part.settings(selected).getStock().getStack(slot);
        if (g == null || !(g.what() instanceof appeng.api.stacks.AEItemKey key))
            return;
        long max = key.getMaxStackSize();
        long out = part.networkExtract(key, right && !shift ? (max + 1) / 2 : max, getPlayer());
        if (out <= 0)
            return;
        ItemStack stack = key.toStack((int) out);
        if (shift)
            getPlayer().getInventory().placeItemBackInInventory(stack);
        else
            setCarried(stack);
    }

    public void selectMachine(Integer index) {
        if (isClientSide()) {
            sendClientAction(SELECT, index);
            return;
        }
        if (index >= 0 && index < part.machineCount())
            selected = index;
    }

    /** The next (or, {@code back}, the previous) page of listed machines. */
    public void stepMachinePage(Boolean back) {
        if (isClientSide()) {
            sendClientAction(STEP_PAGE, back);
            return;
        }
        machinePage = Math.floorMod(machinePage + (back ? -1 : 1), machinePages());
    }

    public void toggleBlocking() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_BLOCKING);
            return;
        }
        IMachineAccess.BlockMode[] all = IMachineAccess.BlockMode.values();
        var s = part.settings(selected);
        s.setBlockMode(all[(s.getBlockMode().ordinal() + 1) % all.length]);
    }

    public void cycleInsertMode() {
        if (isClientSide()) {
            sendClientAction(CYCLE_MODE);
            return;
        }
        IMachineAccess.InsertMode[] all = IMachineAccess.InsertMode.values();
        var s = part.settings(selected);
        s.setInsertMode(all[(s.getInsertMode().ordinal() + 1) % all.length]);
    }

    public void cycleGroup() {
        if (isClientSide()) {
            sendClientAction(CYCLE_GROUP);
            return;
        }
        var s = part.settings(selected);
        s.setGroup((s.getGroup() + 1) % (MAX_GROUP + 1));
    }

    public void toggleRedstonePulse() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_PULSE);
            return;
        }
        var s = part.settings(selected);
        s.setRedstonePulse(!s.isRedstonePulse());
    }

    /** One more (or, {@code back}, one less), round from 8 to 1. */
    public void cycleSubRange(Boolean back) {
        if (isClientSide()) {
            sendClientAction(CYCLE_RANGE, back);
            return;
        }
        int max = MachineAccessCore.MAX_SUB_RANGE;
        var s = part.settings(selected);
        s.setSubRange(Math.floorMod(s.getSubRange() - 1 + (back ? -1 : 1), max) + 1);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            machineCount = part.machineCount();
            selected = Math.max(0, Math.min(selected, machineCount - 1));
            machinePage = Math.max(0, Math.min(machinePage, machinePages() - 1));
            mode = part.getMode().ordinal();
            var s = part.settings(selected);
            group = s.getGroup();
            role = s.getRole().ordinal();
            slot = s.getSlot();
            slotNow = part.effectiveSlot(selected);
            subRange = s.getSubRange();
            redstonePulse = s.isRedstonePulse();
            recover = s.isRecover();
            recoverEnergy = s.isRecoverEnergy();
            int mask = 0;
            for (var d : net.minecraft.core.Direction.values())
                if (s.recoversFrom(d))
                    mask |= 1 << d.ordinal();
            recoverMask = mask;
            blockMode = s.getBlockMode().ordinal();
            insertMode = s.getInsertMode().ordinal();
            var inv = s.getStock();
            long[] counts = new long[MachineAccessCore.STOCK_SLOTS];
            for (int i = 0; i < counts.length; i++) {
                GenericStack g = inv.getStack(i);
                counts[i] = g == null ? 0 : part.networkAmount(g.what());
            }
            net0 = counts[0]; net1 = counts[1]; net2 = counts[2]; net3 = counts[3]; net4 = counts[4];
            net5 = counts[5]; net6 = counts[6]; net7 = counts[7]; net8 = counts[8];
            TwinStarService.MachineView view = TwinStarService.MachineView.of(TwinStarService.MachineState.NO_HUB);
            IGrid grid = part.host().node().getGrid();
            if (!part.isAccessActive()) {
                view = TwinStarService.MachineView.of(TwinStarService.MachineState.INACTIVE);
            } else if (grid != null && grid.getService(ITwinStarService.class) instanceof TwinStarService service) {
                IMachineAccess machine = part.getMode() == MachineAccessCore.Mode.PROVIDER ? part.accessOf(selected) : null;
                if (machine != null)
                    view = service.machineView(machine);
            }
            state = view.state();
            output = view.output() == null ? null : new GenericStack(view.output(), 0);
            fed = view.fed();
            total = view.total();
            runsIn = view.runsIn();
            chance = view.chance();
            updateSlots();
        }
        super.broadcastChanges();
    }
}
