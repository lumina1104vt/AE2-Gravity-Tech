package com.lumina.ae2_gvt.items.parts;

import com.lumina.twinstar.algorithm.execution.Ae2Adapters;
import com.lumina.twinstar.port.World;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.items.tools.MachineLinkTool;
import com.lumina.twinstar.api.IMachineAccess;
import com.lumina.twinstar.api.IMachineAccess.Port;
import com.lumina.twinstar.api.IMachineAccess.PortRole;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import com.google.common.collect.ImmutableSet;
import appeng.helpers.externalstorage.GenericStackFluidStorage;
import appeng.helpers.externalstorage.GenericStackItemStorage;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.lumina.ae2_gvt.storage.FeEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The machine access interface's logic, shared by the cable part and the wireless block. One interface has
 * a {@link Mode}: <b>provider</b> (the hub runs its patterns on the machines: blocking, how materials go in,
 * pairing, sub-machine range, redstone pulse) or <b>ME interface</b> (it keeps stock in the machines like an ME
 * interface and the hub leaves them alone). Either way it takes back what its machines put out into it.
 * <p>
 * Every machine it stands for has settings of its own ({@link Settings}): with no machine bound it is the
 * block the part sits on, otherwise each bound machine (the 機器連結器).
 */
public class MachineAccessCore implements IMachineAccess, InternalInventoryHost, IGridTickable, ICraftingRequester, IUpgradeableObject {
    public static final int MAX_SUB_RANGE = 8;
    public static final int STOCK_SLOTS = 9;
    /** wireless range, in blocks, same dimension */
    public static final int WIRELESS_RANGE = 64;
    /** machines one ME interface stands for; provider mode has no limit */
    public static final int MAX_INTERFACE_MACHINES = 9;
    /** a hatch's slot number: far more than any machine has */
    public static final int MAX_SLOT = 9999;

    public enum Mode {
        PROVIDER, INTERFACE
    }

    /** What the interface sits on: a cable (a part) or a block of its own. */
    public interface Host {
        @Nullable Level level();

        /** the host block */
        @Nullable BlockPos pos();

        /** the side a part sits on (it works on the block there); null for a block, which only works wirelessly */
        @Nullable Direction side();

        /** the name the player gave the host, or null */
        default @Nullable net.minecraft.network.chat.Component customName() {
            return null;
        }

        IManagedGridNode node();

        appeng.api.networking.security.IActionHost actionHost();

        /** the item the crafting card is registered for ({@code Upgrades.add}): the part's item, or the block's */
        ItemLike machineItem();

        void save();

        void update();

        void notifyNeighbors();
    }

    /** One machine's settings. */
    public final class Settings {
        /** a machine it also stands for, besides the block (a ghost: a multiblock's port, another tier) */
        private ItemStack machine = ItemStack.EMPTY;
        private BlockMode blockMode = BlockMode.NONE;
        private InsertMode insertMode = InsertMode.DEFAULT;
        private int group;
        /**
         * What the block is: a machine of its own, or a hatch of the machine before it in the interface's list (an input bus, a
         * fluid hatch, an output, an energy hatch). The hatches of one machine are numbered by their place in its own list, so
         * no number is shared with another machine.
         */
        private PortRole role = PortRole.MACHINE;
        /** an input hatch's slot number (what a pattern's material slot names); 0 = its place among the machine's hatches of the kind */
        private int slot;
        /** how far its sub-machines are looked for */
        private int subRange = 3;
        /** a redstone pulse into the machine once a run's materials are all in */
        private boolean redstonePulse;
        /** ME interface mode takes back what the machine gives out, from the faces in the mask ({@link Direction#ordinal} bits) */
        private boolean recover = true;
        private boolean recoverEnergy = true;
        private int recoverMask = 0x3F;
        /** what it keeps in the machine (ME interface mode), and how much (ghost stacks) */
        private final ConfigInventory stock = ConfigInventory.configStacks(STOCK_SLOTS)
                .supportedTypes(AEKeyType.items(), AEKeyType.fluids()).allowOverstacking(true)
                .changeListener(MachineAccessCore.this::markForSave).build();
        private @Nullable InternalInventory stockWrapper;
        /** pulses still to send; the one going: ticks left of its on (or off) part */
        private int pulsesQueued, pulseTicks;
        private boolean signal;

        public ItemStack getMachineStack() {
            return machine;
        }

        public void setMachineStack(ItemStack stack) {
            machine = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            markForSave();
        }

        public BlockMode getBlockMode() {
            return blockMode;
        }

        public void setBlockMode(BlockMode m) {
            blockMode = m;
            markForSave();
        }

        public InsertMode getInsertMode() {
            return insertMode;
        }

        public void setInsertMode(InsertMode m) {
            insertMode = m;
            markForSave();
        }

        public int getGroup() {
            return group;
        }

        public PortRole getRole() {
            return role;
        }

        public void setRole(PortRole r) {
            role = r;
            markForSave();
            host.update();
        }

        public int getSlot() {
            return slot;
        }

        /** 0 (by place) and up, no upper limit worth the name. */
        public void setSlot(int n) {
            slot = Math.max(0, Math.min(MAX_SLOT, n));
            markForSave();
            host.update();
        }

        public void setGroup(int g) {
            group = g;
            markForSave();
        }

        public int getSubRange() {
            return subRange;
        }

        public void setSubRange(int r) {
            subRange = Math.max(1, Math.min(MAX_SUB_RANGE, r));
            markForSave();
        }

        public boolean isRedstonePulse() {
            return redstonePulse;
        }

        public void setRedstonePulse(boolean on) {
            redstonePulse = on;
            markForSave();
        }

        /** Whether the FE a machine gives out is taken back as well. */
        public boolean isRecoverEnergy() {
            return recoverEnergy;
        }

        public void setRecoverEnergy(boolean on) {
            recoverEnergy = on;
            markForSave();
        }

        public boolean isRecover() {
            return recover;
        }

        public void setRecover(boolean on) {
            recover = on;
            markForSave();
        }

        public boolean recoversFrom(Direction d) {
            return (recoverMask & (1 << d.ordinal())) != 0;
        }

        public void toggleRecoverFace(Direction d) {
            recoverMask ^= 1 << d.ordinal();
            markForSave();
        }

        public void setRecoverFace(Direction d, boolean on) {
            recoverMask = on ? recoverMask | 1 << d.ordinal() : recoverMask & ~(1 << d.ordinal());
            markForSave();
        }

        public ConfigInventory getStock() {
            return stock;
        }

        /** The stock as the menu's slots see it (one wrapper kept, so the slots stay in step). */
        public InternalInventory getStockWrapper() {
            if (stockWrapper == null)
                stockWrapper = stock.createMenuWrapper();
            return stockWrapper;
        }

        void write(CompoundTag tag, HolderLookup.Provider registries) {
            if (!machine.isEmpty())
                tag.put("machine", machine.save(registries));
            tag.putString("blockMode", blockMode.name());
            tag.putString("insertMode", insertMode.name());
            tag.putInt("group", group);
            tag.putString("role", role.name());
            tag.putInt("slot", slot);
            tag.putInt("subRange", subRange);
            tag.putBoolean("redstonePulse", redstonePulse);
            tag.putBoolean("recover", recover);
            tag.putBoolean("recoverEnergy", recoverEnergy);
            tag.putInt("recoverMask", recoverMask);
            stock.writeToChildTag(tag, "stock", registries);
        }

        void read(CompoundTag tag, HolderLookup.Provider registries) {
            machine = tag.contains("machine") ? ItemStack.parseOptional(registries, tag.getCompound("machine")) : ItemStack.EMPTY;
            try {
                blockMode = BlockMode.valueOf(tag.getString("blockMode"));
            } catch (IllegalArgumentException e) {
                blockMode = tag.getBoolean("blocking") ? BlockMode.BLOCKING : BlockMode.NONE;
            }
            try {
                insertMode = InsertMode.valueOf(tag.getString("insertMode"));
            } catch (IllegalArgumentException e) {
                insertMode = InsertMode.DEFAULT;
            }
            group = tag.getInt("group");
            try {
                role = PortRole.valueOf(tag.getString("role"));
            } catch (IllegalArgumentException e) {
                role = PortRole.MACHINE;
            }
            slot = Math.max(0, tag.getInt("slot"));
            subRange = tag.contains("subRange") ? Math.max(1, Math.min(MAX_SUB_RANGE, tag.getInt("subRange"))) : 3;
            redstonePulse = tag.getBoolean("redstonePulse");
            recover = !tag.contains("recover") || tag.getBoolean("recover");
            recoverEnergy = !tag.contains("recoverEnergy") || tag.getBoolean("recoverEnergy");
            recoverMask = tag.contains("recoverMask") ? tag.getInt("recoverMask") & 0x3F : 0x3F;
            stock.readFromChildTag(tag, "stock", registries);
        }
    }

    /** A machine bound from afar: where, its settings, and its access for the hub. */
    private final class Entry {
        final MachineLinkTool.Link link;
        final Settings settings = new Settings();
        final LinkedAccess access = new LinkedAccess(this);

        Entry(MachineLinkTool.Link link) {
            this.link = link;
        }
    }

    private Mode mode = Mode.PROVIDER;
    /** whether a setting changed for one machine goes to every machine of the same kind in the list too (false: only that one) */
    private boolean editSame;
    private final Settings own = new Settings();
    private final List<Entry> entries = new ArrayList<>();
    /** what the machines put out into it, until the hub takes it */
    private final ConfigInventory received = ConfigInventory.storage(STOCK_SLOTS)
            .supportedTypes(AEKeyType.items(), AEKeyType.fluids()).changeListener(this::markForSave).build();
    private final IItemHandler receivedItems = new GenericStackItemStorage(received);
    private final IFluidHandler receivedFluids = new GenericStackFluidStorage(received);
    /** ticks since the hub last took what was put out into it; then it goes to network storage */
    private int receivedIdle;
    private int stockTicks;

    private final Host host;

    /** the crafting card slot (one): with the card, ME interface mode asks the network to craft what a machine's stock is short of */
    private final IUpgradeInventory upgrades;
    /** what is asked for, per item (decisions in {@link StockCrafting}); the plan being calculated and the job running for each */
    private final StockCrafting<AEKey> stockCrafting = new StockCrafting<>();
    private final Map<AEKey, Future<ICraftingPlan>> stockPlans = new LinkedHashMap<>();
    private final Map<AEKey, ICraftingLink> stockLinks = new LinkedHashMap<>();

    public MachineAccessCore(Host host) {
        this.host = host;
        this.upgrades = UpgradeInventories.forMachine(host.machineItem(), 1, this::onUpgradesChanged);
    }

    public Host host() {
        return host;
    }

    @Override
    public boolean isClientSide() {
        return host.level() == null || host.level().isClientSide;
    }

    /** Picks the interface up on the grid: the host's node runs this. */
    public void setup(IManagedGridNode node) {
        node.setFlags().setIdlePowerUsage(10).addService(IGridTickable.class, this).addService(ICraftingRequester.class, this);
    }

    private void markForSave() {
        host.save();
    }

    // ── mode and machines ──

    public Mode getMode() {
        return mode;
    }

    /** Changes the mode; false when it can't (an ME interface stands for at most {@link #MAX_INTERFACE_MACHINES} machines). */
    public boolean setMode(Mode m) {
        if (m == Mode.INTERFACE && entries.size() > MAX_INTERFACE_MACHINES)
            return false;
        mode = m;
        markForSave();
        host.update();
        return true;
    }

    public boolean isWireless() {
        return !entries.isEmpty();
    }

    public List<MachineLinkTool.Link> getLinks() {
        List<MachineLinkTool.Link> out = new ArrayList<>();
        for (Entry e : entries)
            out.add(e.link);
        return out;
    }

    /** How many machines the screen lists: the bound ones, or the one block the part sits on. */
    public int machineCount() {
        return Math.max(1, entries.size());
    }

    public Settings settings(int i) {
        return entries.isEmpty() ? own : entries.get(Math.max(0, Math.min(i, entries.size() - 1))).settings;
    }

    /** Where machine {@code i} is, or null for the block the part sits on. */
    public @Nullable MachineLinkTool.Link linkAt(int i) {
        return entries.isEmpty() ? null : entries.get(Math.max(0, Math.min(i, entries.size() - 1))).link;
    }

    /** Links the machine at {@code target} (or lets go of it when it already is); returns what to tell the player. */
    public Component toggleLink(MachineLinkTool.Link target) {
        var level = host.level();
        if (level == null || !target.dimension().equals(level.dimension()))
            return Component.translatable("message.ae2_gvt.link_tool.other_dimension");
        if (!inWirelessRange(target.pos()))
            return Component.translatable("message.ae2_gvt.link_tool.too_far", WIRELESS_RANGE);
        boolean had = entries.removeIf(e -> e.link.pos().equals(target.pos()));
        if (!had) {
            if (mode == Mode.INTERFACE && entries.size() >= MAX_INTERFACE_MACHINES)
                return Component.translatable("message.ae2_gvt.link_tool.limit", MAX_INTERFACE_MACHINES);
            entries.add(new Entry(target));
        }
        markForSave();
        host.update();
        return Component.translatable(had ? "message.ae2_gvt.link_tool.removed" : "message.ae2_gvt.link_tool.added",
                target.pos().getX(), target.pos().getY(), target.pos().getZ(), entries.size());
    }

    /**
     * Ctrl + right click: links the machine at {@code clicked} and every other block of the same kind within {@code radius} blocks of it
     * (nearest first, at most {@code max} in one go, the clicked face for each), skipping the ones already linked and the ones out of
     * wireless range or unloaded. It only adds, never lets go of one. Returns what to tell the player.
     */
    public Component linkAllSame(MachineLinkTool.Link clicked, int radius, int max) {
        var level = host.level();
        if (level == null || !clicked.dimension().equals(level.dimension()))
            return Component.translatable("message.ae2_gvt.link_tool.other_dimension");
        if (!inWirelessRange(clicked.pos()))
            return Component.translatable("message.ae2_gvt.link_tool.too_far", WIRELESS_RANGE);
        var kind = level.getBlockState(clicked.pos());
        if (kind.isAir())
            return Component.translatable("message.ae2_gvt.link_tool.nothing_there");
        int room = mode == Mode.INTERFACE ? MAX_INTERFACE_MACHINES - entries.size() : Integer.MAX_VALUE;
        if (room <= 0)
            return Component.translatable("message.ae2_gvt.link_tool.limit", MAX_INTERFACE_MACHINES);
        java.util.Set<Long> linked = new java.util.HashSet<>();
        for (Entry e : entries)
            linked.add(e.link.pos().asLong());
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        var scan = SameMachines.scan(clicked.pos().getX(), clicked.pos().getY(), clicked.pos().getZ(), radius, Math.min(max, room),
                (x, y, z) -> {
                    at.set(x, y, z);
                    return inWirelessRange(at) && level.isLoaded(at) && level.getBlockState(at).is(kind.getBlock());
                },
                (x, y, z) -> linked.contains(BlockPos.asLong(x, y, z)));
        if (scan.cells().isEmpty())
            return Component.translatable("message.ae2_gvt.link_tool.all_already", kind.getBlock().getName(), radius);
        for (int[] c : scan.cells())
            entries.add(new Entry(new MachineLinkTool.Link(level.dimension(), new BlockPos(c[0], c[1], c[2]), clicked.face())));
        markForSave();
        host.update();
        Component done = Component.translatable("message.ae2_gvt.link_tool.added_all", scan.cells().size(), kind.getBlock().getName(),
                radius, entries.size());
        if (!scan.cut())
            return done;
        // some were left out: the limit of one click, or the room an ME interface has
        int left = scan.total() - scan.cells().size();
        return done.copy().append(" ").append(Component.translatable("message.ae2_gvt.link_tool.all_cut", left));
    }

    /** The block each listed machine is (null when it is not loaded or there is no machine listed), for telling the same ones. */
    private List<net.minecraft.world.level.block.Block> machineKinds() {
        List<net.minecraft.world.level.block.Block> out = new ArrayList<>();
        var level = host.level();
        for (Entry e : entries)
            out.add(level != null && level.isLoaded(e.link.pos()) ? level.getBlockState(e.link.pos()).getBlock() : null);
        return out;
    }

    /** The listed places of the machines that are the same machine (the same block) as listed machine {@code i}, itself included. */
    public List<Integer> sameMachines(int i) {
        if (entries.isEmpty())
            return List.of();
        List<Boolean> isMachine = new ArrayList<>();
        for (Entry e : entries)
            isMachine.add(e.settings.role == PortRole.MACHINE);
        return SameMachines.indicesLike(machineKinds(), isMachine, Math.max(0, Math.min(i, entries.size() - 1)));
    }

    /**
     * Gives every machine of the same kind as listed machine {@code i} the optional machine that one has (empty too: it is
     * copied as it is). Returns how many other machines were set; 0 when there are none of the same kind.
     */
    public int applyMachineToSame(int i) {
        ItemStack machine = settings(i).getMachineStack();
        int set = 0;
        int self = Math.max(0, Math.min(i, entries.size() - 1));
        for (int k : sameMachines(i)) {
            if (k == self)
                continue;
            entries.get(k).settings.setMachineStack(machine);
            set++;
        }
        return set;
    }

    public boolean isEditSame() {
        return editSame;
    }

    public void setEditSame(boolean on) {
        editSame = on;
        markForSave();
    }

    /**
     * The settings a change made on listed machine {@code i} goes to: that one, and in "same machines together" mode every other
     * machine of the same kind in this interface (never a hatch, never another kind). The selected one is first.
     */
    public List<Settings> editTargets(int i) {
        Settings self = settings(i);
        List<Settings> out = new ArrayList<>();
        out.add(self);
        if (!editSame || entries.isEmpty() || self.role != PortRole.MACHINE)
            return out;
        int at = Math.max(0, Math.min(i, entries.size() - 1));
        for (int k : sameMachines(at))
            if (k != at)
                out.add(entries.get(k).settings);
        return out;
    }

    /**
     * What a removal in {@code scope} would take out, as listed places (sorted): the marked ones (the selected one when none is marked),
     * the machines of the same kind as {@code selected}, or all; each machine with the hatches that follow it.
     */
    public List<Integer> removalSet(MachineRemoval.Scope scope, int selected, java.util.Collection<Integer> marks) {
        if (entries.isEmpty())
            return List.of();
        List<Boolean> isMachine = new ArrayList<>();
        for (Entry e : entries)
            isMachine.add(e.settings.role == PortRole.MACHINE);
        int self = Math.max(0, Math.min(selected, entries.size() - 1));
        List<Integer> picked = switch (scope) {
            case ALL -> {
                List<Integer> all = new ArrayList<>();
                for (int k = 0; k < entries.size(); k++)
                    all.add(k);
                yield all;
            }
            case SAME -> sameMachines(self);
            case MARKED -> {
                var valid = MachineRemoval.validMarks(marks, entries.size());
                yield valid.isEmpty() ? List.of(self) : new ArrayList<>(valid);
            }
        };
        return MachineRemoval.withHatches(isMachine, picked);
    }

    /** Lets go of the machines at {@code places} (entry and settings together, the others keep their order); how many went. */
    public int removeEntries(java.util.Collection<Integer> places) {
        List<Entry> gone = new ArrayList<>();
        for (int p : new java.util.TreeSet<>(places))
            if (p >= 0 && p < entries.size())
                gone.add(entries.get(p));
        int n = MachineRemoval.removeAt(entries, places);
        for (Entry e : gone)
            setSignal(e.settings, e.link, false);       // a pulse going when it was let go must not leave the machine powered
        if (n > 0) {
            markForSave();
            host.update();
        }
        return n;
    }

    /** The optional machine of listed machine {@code i}, which the screen draws small in its cell; empty for none. */
    public ItemStack optionalMachineAt(int i) {
        if (i < 0 || i >= machineCount())
            return ItemStack.EMPTY;
        if (isWireless() && settings(i).getRole() != PortRole.MACHINE)
            return ItemStack.EMPTY;     // a hatch has no optional machine
        return settings(i).getMachineStack();
    }

    private boolean inWirelessRange(BlockPos target) {
        BlockPos here = host.pos();
        if (here == null)
            return false;
        long dx = target.getX() - here.getX(), dy = target.getY() - here.getY(), dz = target.getZ() - here.getZ();
        return dx * dx + dy * dy + dz * dz <= (long) WIRELESS_RANGE * WIRELESS_RANGE;
    }

    /** wireless: same dimension, within range, and the machine's chunk loaded */
    private @Nullable ServerLevel linkLevel(MachineLinkTool.Link l) {
        if (!(host.level() instanceof ServerLevel level))
            return null;
        if (!l.dimension().equals(level.dimension()) || !inWirelessRange(l.pos()) || !level.isLoaded(l.pos()))
            return null;
        return level;
    }

    /** One bound machine, for the hub: its own settings, its own place. */
    private final class LinkedAccess implements IMachineAccess {
        private final Entry entry;

        LinkedAccess(Entry entry) {
            this.entry = entry;
        }

        private boolean first() {
            for (Entry e : entries)
                if (e.settings.role == PortRole.MACHINE)
                    return e == entry;
            return false;
        }

        /** The hatches after this machine, up to the next machine: each with its slot number. */
        @Override
        public List<Port> ports() {
            return portsAfter(entry);
        }

        @Override
        public @Nullable Item getMachine() {
            ItemStack s = entry.settings.machine;
            return s.isEmpty() ? null : s.getItem();
        }

        @Override public boolean isBlocking() { return entry.settings.blockMode != BlockMode.NONE; }
        @Override public BlockMode getBlockMode() { return entry.settings.blockMode; }
        @Override public InsertMode getInsertMode() { return entry.settings.insertMode; }
        @Override public int getGroup() { return entry.settings.group; }
        @Override public int getSubRange() { return entry.settings.subRange; }
        @Override public void onRunFed() { queuePulse(entry.settings); }
        @Override public Set<AEKey> getStockedKeys() { return entry.settings.stock.keySet(); }
        @Override public boolean isAccessActive() { return MachineAccessCore.this.isAccessActive(); }
        @Override public BlockPos getMachinePos() { return entry.link.pos(); }
        @Override public Direction getMachineFace() { return entry.link.face(); }
        @Override public @Nullable net.minecraft.core.GlobalPos interfacePlace() { return MachineAccessCore.this.interfacePlace(); }
        @Override public int interfaceSide() { return MachineAccessCore.this.interfaceSide(); }
        @Override public @Nullable net.minecraft.network.chat.Component interfaceName() { return MachineAccessCore.this.interfaceName(); }

        @Override
        public void takeReceived(BiConsumer<AEKey, Long> taken) {
            if (first())
                MachineAccessCore.this.takeReceived(taken);
        }

        @Override
        public @Nullable ServerLevel getMachineLevel() {
            return linkLevel(entry.link);
        }

        @Override
        public @Nullable World getMachineWorld() {
            return Ae2Adapters.world(getMachineLevel());
        }
    }

    /** What the hub may use: nothing in ME interface mode, each bound machine (not its hatches), or the block the part sits on. */
    @Override
    public List<IMachineAccess> accessPoints() {
        if (mode == Mode.INTERFACE)
            return List.of();
        if (entries.isEmpty())
            return List.of(this);
        List<IMachineAccess> out = new ArrayList<>();
        for (Entry e : entries)
            if (e.settings.role == PortRole.MACHINE)
                out.add(e.access);
        return out;
    }

    /** The hatches of machine {@code owner}: the entries after it up to the next machine, an input numbered by its slot or its place. */
    private List<Port> portsAfter(Entry owner) {
        int at = entries.indexOf(owner);
        if (at < 0)
            return List.of();
        List<Port> out = new ArrayList<>();
        int items = 0, fluids = 0;
        for (int i = at + 1; i < entries.size(); i++) {
            Entry e = entries.get(i);
            PortRole role = e.settings.role;
            if (role == PortRole.MACHINE)
                break;
            int slot = 0;
            if (role == PortRole.ITEM_IN)
                slot = e.settings.slot > 0 ? e.settings.slot : ++items;
            else if (role == PortRole.FLUID_IN)
                slot = e.settings.slot > 0 ? e.settings.slot : ++fluids;
            out.add(new Port(e.link.pos(), role, slot));
        }
        return out;
    }

    /** The machine entry {@code i} is, or belongs to (a hatch is the machine before it); null when it has none. */
    public @Nullable IMachineAccess accessOf(int i) {
        if (entries.isEmpty())
            return this;
        for (int k = Math.max(0, Math.min(i, entries.size() - 1)); k >= 0; k--)
            if (entries.get(k).settings.role == PortRole.MACHINE)
                return entries.get(k).access;
        return null;
    }

    /** The slot number input hatch {@code i} goes by (its own or its place), 0 for the others. */
    public int effectiveSlot(int i) {
        if (entries.isEmpty() || i < 0 || i >= entries.size())
            return 0;
        Entry e = entries.get(i);
        if (e.settings.role != PortRole.ITEM_IN && e.settings.role != PortRole.FLUID_IN)
            return 0;
        for (int k = i - 1; k >= -1; k--) {
            if (k < 0 || entries.get(k).settings.role == PortRole.MACHINE) {
                if (k < 0)
                    return 0;
                for (Port p : portsAfter(entries.get(k)))
                    if (p.pos().equals(e.link.pos()))
                        return p.slot();
                return 0;
            }
        }
        return 0;
    }

    // ── what the machines put out into it ──

    public IItemHandler getReceivedItems() {
        return receivedItems;
    }

    public IFluidHandler getReceivedFluids() {
        return receivedFluids;
    }

    @Override
    public Set<AEKey> getStockedKeys() {
        return own.stock.keySet();
    }

    @Override
    public void takeReceived(BiConsumer<AEKey, Long> taken) {
        receivedIdle = 0;
        for (int i = 0; i < received.size(); i++) {
            GenericStack s = received.getStack(i);
            if (s == null)
                continue;
            long got = received.extract(i, s.what(), s.amount(), Actionable.MODULATE);
            if (got > 0)
                taken.accept(s.what(), got);
        }
    }

    // ── IMachineAccess: the block the part sits on (no machine bound) ──

    @Override
    public @Nullable Item getMachine() {
        return own.machine.isEmpty() ? null : own.machine.getItem();
    }

    @Override
    public boolean isBlocking() {
        return own.blockMode != BlockMode.NONE;
    }

    @Override
    public BlockMode getBlockMode() {
        return own.blockMode;
    }

    @Override
    public InsertMode getInsertMode() {
        return own.insertMode;
    }

    @Override
    public int getGroup() {
        return own.group;
    }

    @Override
    public int getSubRange() {
        return own.subRange;
    }

    @Override
    public void onRunFed() {
        queuePulse(own);
    }

    private static void queuePulse(Settings s) {
        if (s.redstonePulse)
            s.pulsesQueued = Math.min(s.pulsesQueued + 1, 4);   // a few queued at most; the hub asks again while materials are left
    }

    /** Each pulse: on for 2 ticks, then off for 2, so a machine sees every one as a rising edge. */
    private void tickPulse(Settings s, @Nullable MachineLinkTool.Link link) {
        if (s.pulseTicks > 0 && --s.pulseTicks > 0)
            return;
        if (s.signal) {
            setSignal(s, link, false);
            s.pulseTicks = 2;
        } else if (s.pulsesQueued > 0) {
            s.pulsesQueued--;
            setSignal(s, link, true);
            s.pulseTicks = 2;
        }
    }

    private void setSignal(Settings s, @Nullable MachineLinkTool.Link link, boolean on) {
        if (s.signal == on)
            return;
        s.signal = on;
        if (link == null) {
            host.notifyNeighbors();
            return;
        }
        // bound from afar: no block, the machine is told it is powered (RemoteSignals answers the redstone questions about it)
        ServerLevel level = linkLevel(link);
        if (level != null)
            com.lumina.ae2_gvt.redstone.RemoteSignals.set(level, link.pos(), on);
    }

    public boolean canConnectRedstone() {
        return true;
    }

    public int isProvidingStrongPower() {
        return own.signal ? 15 : 0;
    }

    public int isProvidingWeakPower() {
        return own.signal ? 15 : 0;
    }

    @Override
    public @Nullable ServerLevel getMachineLevel() {
        if (!(host.level() instanceof ServerLevel level) || host.side() == null)
            return null;       // a block has no machine of its own: only the bound ones
        return level;
    }

    @Override
    public @Nullable World getMachineWorld() {
        return Ae2Adapters.world(getMachineLevel());
    }

    @Override
    public BlockPos getMachinePos() {
        BlockPos here = host.pos();
        return here == null || host.side() == null ? BlockPos.ZERO : here.relative(host.side());
    }

    @Override
    public Direction getMachineFace() {
        return host.side() == null ? Direction.UP : host.side().getOpposite();
    }

    @Override
    public boolean isAccessActive() {
        return host.node().isActive();
    }

    // ── for the list of what machines use up ──

    @Override
    public @Nullable net.minecraft.core.GlobalPos interfacePlace() {
        return host.level() == null || host.pos() == null ? null : net.minecraft.core.GlobalPos.of(host.level().dimension(), host.pos());
    }

    @Override
    public int interfaceSide() {
        return host.side() == null ? -1 : host.side().ordinal();
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component interfaceName() {
        return host.customName();
    }

    /** Every machine this interface works on: the bound ones, or the block the part sits on. */
    @Override
    public List<net.minecraft.core.GlobalPos> machinePlaces() {
        List<net.minecraft.core.GlobalPos> out = new ArrayList<>();
        for (Target t : targets())
            out.add(net.minecraft.core.GlobalPos.of(t.level().dimension(), t.pos()));
        return out;
    }

    /** The network's count of {@code key} (for the screen's "in the network" row). Server side. */
    public long networkAmount(AEKey key) {
        var grid = host.node().getGrid();
        return grid == null ? 0 : grid.getStorageService().getCachedInventory().get(key);
    }

    /** Puts {@code amount} of {@code key} into the network for a player; how many went in. Server side. */
    public long networkInsert(AEKey key, long amount, net.minecraft.world.entity.player.Player player) {
        var grid = host.node().getGrid();
        if (grid == null)
            return 0;
        return grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE,
                IActionSource.ofPlayer(player, host.actionHost()));
    }

    /** Takes up to {@code amount} of {@code key} out of the network for a player; how many came out. Server side. */
    public long networkExtract(AEKey key, long amount, net.minecraft.world.entity.player.Player player) {
        var grid = host.node().getGrid();
        if (grid == null)
            return 0;
        return grid.getStorageService().getInventory().extract(key, amount, Actionable.MODULATE,
                IActionSource.ofPlayer(player, host.actionHost()));
    }

    // ── FE, stock, redstone ──

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    /** One machine this interface works on: its settings, where it is and its access (the hub's view of it). */
    private record Target(Settings settings, @Nullable MachineLinkTool.Link link, ServerLevel level, BlockPos pos, Direction face,
                          @Nullable IMachineAccess access) {
    }

    private List<Target> targets() {
        List<Target> out = new ArrayList<>();
        if (entries.isEmpty()) {
            ServerLevel level = getMachineLevel();
            if (level != null)
                out.add(new Target(own, null, level, getMachinePos(), getMachineFace(), this));
            return out;
        }
        for (Entry e : entries) {
            ServerLevel level = linkLevel(e.link);
            if (level != null)
                out.add(new Target(e.settings, e.link, level, e.link.pos(), e.link.face(), e.access));
        }
        return out;
    }

    /**
     * Keeps every machine charged from the FE on the network (a white hole energy cell, Applied Flux's
     * cells), sends the redstone pulses, and in ME interface mode keeps the machines stocked.
     */
    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        // the host's own level, not getMachineLevel(): the wireless block has no machine of its own
        if (!(host.level() instanceof ServerLevel) || !node.isActive())
            return TickRateModulation.SAME;
        MEStorage network = node.getGrid().getStorageService().getInventory();
        IActionSource source = IActionSource.ofMachine(host.actionHost());
        flushReceived(network, source);
        boolean stockNow = ++stockTicks >= 10;
        if (stockNow)
            stockTicks = 0;
        // what each stocked item is wanted at and held at across the machines, for the crafting card
        boolean craftNow = stockNow && mode == Mode.INTERFACE && upgrades.isInstalled(AEItems.CRAFTING_CARD);
        Map<AEKey, long[]> stocked = new LinkedHashMap<>();
        for (Target t : targets()) {
            tickPulse(t.settings(), t.link());
            if (mode == Mode.INTERFACE && stockNow)
                recover(t.settings(), t.level(), network, source, t.pos(), t.face());
            if (t.settings().insertMode == InsertMode.OUTPUT)
                continue;
            if (mode == Mode.INTERFACE && stockNow)
                topUp(t.settings(), t.level(), network, source, t.pos(), t.face());
            if (craftNow)
                countStock(t.settings(), t.level(), t.pos(), t.face(), stocked);
            charge(t.level().getCapability(Capabilities.EnergyStorage.BLOCK, t.pos(), t.face()), network, source);
            if (mode == Mode.PROVIDER && t.access() != null
                    && node.getGrid().getService(com.lumina.twinstar.api.ITwinStarService.class)
                    instanceof com.lumina.twinstar.service.TwinStarService service) {
                for (BlockPos sub : service.subMachinesOf(t.access())) {
                    IEnergyStorage e = t.level().getCapability(Capabilities.EnergyStorage.BLOCK, sub, null);
                    if (e == null) {
                        for (Direction d : Direction.values()) {
                            e = t.level().getCapability(Capabilities.EnergyStorage.BLOCK, sub, d);
                            if (e != null)
                                break;
                        }
                    }
                    charge(e, network, source);
                }
            }
        }
        if (stockNow && (craftNow || !stockCrafting.isEmpty()))
            craftMissing(node.getGrid(), (ServerLevel) host.level(), source, stocked, craftNow);
        return TickRateModulation.SAME;
    }

    // ── the crafting card: ask the network to craft what a machine's stock is short of ──

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public boolean hasCraftingCard() {
        return upgrades.isInstalled(AEItems.CRAFTING_CARD);
    }

    private void onUpgradesChanged() {
        markForSave();
        if (!hasCraftingCard())
            cancelCrafting();
    }

    /** The card came out: what was asked for is cancelled, like AE2's interface does. */
    private void cancelCrafting() {
        for (ICraftingLink l : stockLinks.values())
            l.cancel();
        for (Future<ICraftingPlan> f : stockPlans.values())
            f.cancel(true);
        stockLinks.clear();
        stockPlans.clear();
        stockCrafting.clear();
    }

    /**
     * Adds what this machine's stock wants and what it holds now (after the top-up), per item, to {@code into}: [wanted, held].
     * Handlers are looked up as the top-up does it.
     */
    private void countStock(Settings settings, ServerLevel level, BlockPos pos, Direction face0, Map<AEKey, long[]> into) {
        if (settings.stock.isEmpty())
            return;
        Direction face = settings.insertMode == InsertMode.ANY ? null : face0;
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (items == null)
            items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (fluids == null)
            fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        Map<AEKey, Long> want = new LinkedHashMap<>();
        for (int i = 0; i < settings.stock.size(); i++) {
            GenericStack g = settings.stock.getStack(i);
            if (g != null && g.amount() > 0)
                want.merge(g.what(), g.amount(), Long::sum);
        }
        for (Map.Entry<AEKey, Long> e : want.entrySet()) {
            long held = 0;
            if (e.getKey() instanceof AEItemKey item && items != null) {
                for (int s = 0; s < items.getSlots(); s++)
                    if (item.matches(items.getStackInSlot(s)))
                        held += items.getStackInSlot(s).getCount();
            } else if (e.getKey() instanceof AEFluidKey fluid && fluids != null) {
                for (int t = 0; t < fluids.getTanks(); t++)
                    if (fluid.matches(fluids.getFluidInTank(t)))
                        held += fluids.getFluidInTank(t).getAmount();
            }
            long[] sum = into.computeIfAbsent(e.getKey(), k -> new long[2]);
            sum[0] += e.getValue();
            sum[1] += held;
        }
    }

    /**
     * Once every half second: for each stocked item, asks the network to craft what is missing (what is wanted, less what the
     * machines hold and what the network has), the way AE2's interface does with a crafting card: one calculation
     * ({@code beginCraftingCalculation}), then {@code submitJob} with this interface as the requester, which keeps the job's
     * link. {@link StockCrafting} decides when (no second request for an item while one runs, waits after failures).
     * {@code enabled} false (mode changed, card out): nothing new is asked, what is under way is only looked after.
     */
    private void craftMissing(@Nullable IGrid grid, ServerLevel level, IActionSource source, Map<AEKey, long[]> stocked, boolean enabled) {
        if (grid == null)
            return;
        var crafting = grid.getCraftingService();
        var cached = grid.getStorageService().getCachedInventory();
        long now = level.getGameTime();
        Set<AEKey> keys = new LinkedHashSet<>(stocked.keySet());
        keys.addAll(stockCrafting.keys());
        for (AEKey key : keys) {
            long[] sum = stocked.get(key);
            long need = enabled && sum != null ? StockCrafting.shortfall(sum[0], sum[1], cached.get(key)) : 0;
            ICraftingLink link = stockLinks.get(key);
            if (link != null && (link.isDone() || link.isCanceled())) {
                stockLinks.remove(key);
                stockCrafting.jobEnded(key, link.isDone(), now);
            }
            Future<ICraftingPlan> future = stockPlans.get(key);
            if (future != null && future.isDone()) {
                stockPlans.remove(key);
                ICraftingPlan plan = null;
                try {
                    plan = future.get();
                } catch (Exception e) {
                    // cancelled or the calculation failed: counts as an unusable plan
                }
                boolean usable = plan != null && !plan.simulation();
                if (stockCrafting.planFinished(key, usable, need > 0, now)) {
                    var result = crafting.submitJob(plan, this, null, false, source);
                    boolean ok = result.successful() && result.link() != null;
                    if (ok)
                        stockLinks.put(key, result.link());
                    stockCrafting.submitted(key, ok, now);
                    markForSave();
                }
            }
            if (stockCrafting.poll(key, need, now) == StockCrafting.Step.CALCULATE)
                stockPlans.put(key, crafting.beginCraftingCalculation(level, () -> source, key, need, CalculationStrategy.CRAFT_LESS));
        }
    }

    /** What the stock slot holding {@code key} shows: 0 nothing going on, 1 asked the network to craft it, 2 could not (retrying later). */
    public int craftState(@Nullable AEKey key) {
        if (key == null)
            return 0;
        return switch (stockCrafting.phaseOf(key)) {
            case CALCULATING, RUNNING -> 1;
            case RETRY -> 2;
            default -> 0;
        };
    }

    // the crafting requester: AE2 tells it about the jobs it asked for

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        ImmutableSet.Builder<ICraftingLink> out = ImmutableSet.builder();
        for (ICraftingLink l : stockLinks.values())
            if (!l.isDone() && !l.isCanceled())
                out.add(l);
        return out.build();
    }

    /** What the job makes goes into network storage (the machine is topped up from there), so none is taken here. */
    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        // looked at in craftMissing: a finished or cancelled link is let go there
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return host.actionHost().getActionableNode();
    }

    /** What the machines put out into it and no hub took for two seconds goes into network storage. */
    private void flushReceived(MEStorage network, IActionSource source) {
        if (received.isEmpty()) {
            receivedIdle = 0;
            return;
        }
        if (++receivedIdle < 40)
            return;
        for (int i = 0; i < received.size(); i++) {
            GenericStack s = received.getStack(i);
            if (s == null)
                continue;
            long went = network.insert(s.what(), s.amount(), Actionable.MODULATE, source);
            if (went > 0)
                received.extract(i, s.what(), went, Actionable.MODULATE);
        }
    }

    /**
     * ME interface mode takes back what the machine will give out (its output slots, through the bound face) into the
     * network: everything except the items it is set to keep in it, so the stock it put in is not taken out again.
     */
    private void recover(Settings settings, ServerLevel level, MEStorage network, IActionSource source, BlockPos pos, Direction face) {
        if (!settings.recover)
            return;
        Set<AEKey> keep = settings.stock.keySet();
        // each chosen face, each handler once (a machine often gives the same one on several faces)
        List<Object> seen = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (!settings.recoversFrom(d))
                continue;
            IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, d);
            if (items != null && seen.stream().noneMatch(o -> o == items)) {
                seen.add(items);
                recoverItems(items, keep, network, source);
            }
            IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, d);
            if (fluids != null && seen.stream().noneMatch(o -> o == fluids)) {
                seen.add(fluids);
                recoverFluids(fluids, keep, network, source);
            }
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
            if (settings.recoverEnergy && energy != null && seen.stream().noneMatch(o -> o == energy)) {
                seen.add(energy);
                recoverEnergy(energy, settings, level.getBlockEntity(pos) instanceof com.lumina.ae2_gvt.blockentities.EnergyPortBlockEntity, network, source);
            }
        }
    }

    /**
     * The FE a machine gives out (a generator, an energy port's output) goes into the network's FE storage. A machine that also takes
     * FE in (a battery) is left alone unless it is set to insert nothing (the output setting): it would be charged and drained again.
     * (The setting is only on the screen in provider mode, so the fusion generator's energy port, which takes and gives in two buffers
     * of its own, is taken back whatever it is set to.)
     */
    private static void recoverEnergy(IEnergyStorage energy, Settings settings, boolean energyPort, MEStorage network, IActionSource source) {
        if (!energy.canExtract() || (energy.canReceive() && !energyPort && settings.insertMode != InsertMode.OUTPUT))
            return;
        int offered = energy.extractEnergy(Integer.MAX_VALUE, true);
        if (offered <= 0)
            return;
        long room = network.insert(FeEnergy.key(), offered, Actionable.SIMULATE, source);
        if (room <= 0)
            return;
        int taken = energy.extractEnergy((int) room, false);
        if (taken > 0)
            network.insert(FeEnergy.key(), taken, Actionable.MODULATE, source);
    }

    private static void recoverItems(IItemHandler items, Set<AEKey> keep, MEStorage network, IActionSource source) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack in = items.getStackInSlot(slot);
            AEItemKey key = in.isEmpty() ? null : AEItemKey.of(in);
            if (key == null || keep.contains(key))
                continue;
            ItemStack offer = items.extractItem(slot, in.getCount(), true);     // what the machine lets out of this slot
            if (offer.isEmpty())
                continue;
            long fits = network.insert(key, offer.getCount(), Actionable.SIMULATE, source);
            if (fits <= 0)
                continue;
            ItemStack taken = items.extractItem(slot, (int) Math.min(fits, offer.getCount()), false);
            if (taken.isEmpty())
                continue;
            long went = network.insert(key, taken.getCount(), Actionable.MODULATE, source);
            if (went < taken.getCount()) {                      // the network took less than it said: the rest goes back
                ItemStack back = ItemHandlerHelper.insertItemStacked(items, key.toStack((int) (taken.getCount() - went)), false);
                if (!back.isEmpty())
                    network.insert(key, back.getCount(), Actionable.MODULATE, source);
            }
        }
    }

    private static void recoverFluids(IFluidHandler fluids, Set<AEKey> keep, MEStorage network, IActionSource source) {
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack in = fluids.getFluidInTank(tank);
            AEFluidKey key = in.isEmpty() ? null : AEFluidKey.of(in);
            if (key == null || keep.contains(key))
                continue;
            FluidStack offer = fluids.drain(in.copyWithAmount(in.getAmount()), IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty())
                continue;
            long fits = network.insert(key, offer.getAmount(), Actionable.SIMULATE, source);
            if (fits <= 0)
                continue;
            FluidStack taken = fluids.drain(in.copyWithAmount((int) Math.min(fits, offer.getAmount())), IFluidHandler.FluidAction.EXECUTE);
            if (taken.isEmpty())
                continue;
            long went = network.insert(key, taken.getAmount(), Actionable.MODULATE, source);
            if (went < taken.getAmount())
                fluids.fill(key.toStack((int) (taken.getAmount() - went)), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Fills the machine up to its stock amounts from the network: through its face, or no face when set to "any". */
    private void topUp(Settings settings, ServerLevel level, MEStorage network, IActionSource source, BlockPos pos, Direction face0) {
        if (settings.stock.isEmpty())
            return;
        Map<AEKey, Long> want = new LinkedHashMap<>();
        for (int i = 0; i < settings.stock.size(); i++) {
            GenericStack g = settings.stock.getStack(i);
            if (g != null && g.amount() > 0)
                want.merge(g.what(), g.amount(), Long::sum);
        }
        Direction face = settings.insertMode == InsertMode.ANY ? null : face0;
        // what goes into the machine counts as used up there (the list of what machines use)
        BiConsumer<AEKey, Long> used = (k, n) -> com.lumina.twinstar.usage.UsageStore.record(level, pos, interfacePlace(), interfaceSide(), k, n,
                com.lumina.twinstar.usage.UsageStore.STOCK);
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (items == null)
            items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (fluids == null)
            fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        for (Map.Entry<AEKey, Long> e : want.entrySet()) {
            if (e.getKey() instanceof AEItemKey item && items != null)
                topUpItem(items, item, e.getValue(), network, source, used);
            else if (e.getKey() instanceof AEFluidKey fluid && fluids != null)
                topUpFluid(fluids, fluid, e.getValue(), network, source, used);
        }
    }

    private static void topUpItem(IItemHandler items, AEItemKey key, long amount, MEStorage network, IActionSource source,
                                  BiConsumer<AEKey, Long> used) {
        long have = 0;
        for (int s = 0; s < items.getSlots(); s++) {
            ItemStack in = items.getStackInSlot(s);
            if (key.matches(in))
                have += in.getCount();
        }
        long missing = amount - have;
        if (missing <= 0)
            return;
        int chunk = (int) Math.min(missing, key.getItem().getDefaultMaxStackSize());
        long available = network.extract(key, chunk, Actionable.SIMULATE, source);
        if (available <= 0)
            return;
        ItemStack rest = ItemHandlerHelper.insertItemStacked(items, key.toStack((int) available), true);
        int fits = (int) available - rest.getCount();
        if (fits <= 0)
            return;
        long taken = network.extract(key, fits, Actionable.MODULATE, source);
        if (taken <= 0)
            return;
        ItemStack back = ItemHandlerHelper.insertItemStacked(items, key.toStack((int) taken), false);
        if (!back.isEmpty())
            network.insert(key, back.getCount(), Actionable.MODULATE, source);
        used.accept(key, taken - back.getCount());
    }

    private static void topUpFluid(IFluidHandler fluids, AEFluidKey key, long amount, MEStorage network, IActionSource source,
                                   BiConsumer<AEKey, Long> used) {
        long have = 0;
        for (int t = 0; t < fluids.getTanks(); t++) {
            FluidStack in = fluids.getFluidInTank(t);
            if (key.matches(in))
                have += in.getAmount();
        }
        long missing = Math.min(amount - have, Integer.MAX_VALUE);
        if (missing <= 0)
            return;
        long available = network.extract(key, missing, Actionable.SIMULATE, source);
        if (available <= 0)
            return;
        int fits = fluids.fill(key.toStack((int) available), IFluidHandler.FluidAction.SIMULATE);
        if (fits <= 0)
            return;
        long taken = network.extract(key, fits, Actionable.MODULATE, source);
        if (taken <= 0)
            return;
        int filled = fluids.fill(key.toStack((int) taken), IFluidHandler.FluidAction.EXECUTE);
        if (filled < taken)
            network.insert(key, taken - filled, Actionable.MODULATE, source);
        used.accept(key, (long) filled);
    }

    /** As much FE as {@code machine} takes, from the network's. */
    private static void charge(@Nullable IEnergyStorage machine, MEStorage network, IActionSource source) {
        if (machine == null || !machine.canReceive())
            return;
        int wanted = machine.receiveEnergy(Integer.MAX_VALUE, true);
        if (wanted <= 0)
            return;
        long available = network.extract(FeEnergy.key(), wanted, Actionable.SIMULATE, source);
        if (available > 0) {
            int taken = machine.receiveEnergy((int) available, false);
            if (taken > 0)
                network.extract(FeEnergy.key(), taken, Actionable.MODULATE, source);
        }
    }


    // ── to the client (the linker draws the links) ──

    public void writeStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        data.writeByte(mode.ordinal());
        data.writeVarInt(entries.size());
        for (Entry e : entries) {
            data.writeBlockPos(e.link.pos());
            data.writeByte(e.link.face().get3DDataValue());
            data.writeByte(e.settings.role.ordinal());
            data.writeVarInt(e.settings.slot);
        }
    }

    public void readStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        mode = Mode.values()[Math.floorMod(data.readByte(), Mode.values().length)];
        int n = data.readVarInt();
        var dim = host.level() == null ? null : host.level().dimension();
        List<MachineLinkTool.Link> now = new ArrayList<>();
        List<PortRole> roles = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos p = data.readBlockPos();
            Direction f = Direction.from3DDataValue(data.readByte());
            PortRole role = PortRole.values()[Math.floorMod(data.readByte(), PortRole.values().length)];
            int slot = data.readVarInt();
            if (dim != null) {
                now.add(new MachineLinkTool.Link(dim, p, f));
                roles.add(role);
                slots.add(slot);
            }
        }
        // keep the entries (and their synced slots) that are still there; the client's copies only hold what the menu syncs
        List<Entry> next = new ArrayList<>();
        for (int i = 0; i < now.size(); i++) {
            MachineLinkTool.Link l = now.get(i);
            Entry old = null;
            for (Entry e : entries)
                if (e.link.equals(l))
                    old = e;
            Entry e = old != null ? old : new Entry(l);
            e.settings.role = roles.get(i);
            e.settings.slot = slots.get(i);
            next.add(e);
        }
        entries.clear();
        entries.addAll(next);
    }

    // ── saving ──

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        host.save();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
    }

    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            mode = Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = Mode.PROVIDER;
        }
        editSame = tag.getBoolean("editSame");
        received.readFromChildTag(tag, "received", registries);
        upgrades.readFromNBT(tag, "upgrades", registries);
        // the jobs it had asked for: the links come back, so nothing is asked for twice
        stockLinks.clear();
        stockPlans.clear();
        stockCrafting.clear();
        ListTag asked = tag.getList("craftLinks", 10);
        for (int i = 0; i < asked.size(); i++) {
            CompoundTag t = asked.getCompound(i);
            AEKey what = AEKey.fromTagGeneric(registries, t.getCompound("what"));
            if (what == null)
                continue;
            stockLinks.put(what, StorageHelper.loadCraftingLink(t.getCompound("link"), this));
            stockCrafting.restore(what, t.getLong("amount"));
        }
        entries.clear();
        if (tag.contains("own")) {
            own.read(tag.getCompound("own"), registries);
            ListTag list = tag.getList("entries", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                MachineLinkTool.Link l = MachineLinkTool.Link.fromTag(t.getCompound("link"));
                if (l == null)
                    continue;
                Entry e = new Entry(l);
                e.settings.read(t.getCompound("settings"), registries);
                entries.add(e);
            }
            return;
        }
        // saved before every machine had settings of its own: one set of settings for the block, links without any
        own.read(tag, registries);
        var legacySlots = new AppEngInternalInventory(this, 33, 1);
        legacySlots.readFromNBT(tag, "machine", registries);
        own.machine = legacySlots.getStackInSlot(0).isEmpty() ? ItemStack.EMPTY : legacySlots.getStackInSlot(0).copyWithCount(1);
        ListTag list = tag.getList("links", 10);
        for (int i = 0; i < list.size(); i++) {
            MachineLinkTool.Link l = MachineLinkTool.Link.fromTag(list.getCompound(i));
            if (l == null)
                continue;
            Entry e = new Entry(l);
            ItemStack m = legacySlots.getStackInSlot(i + 1);
            e.settings.machine = m.isEmpty() ? ItemStack.EMPTY : m.copyWithCount(1);
            entries.add(e);
        }
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("mode", mode.name());
        tag.putBoolean("editSame", editSame);
        received.writeToChildTag(tag, "received", registries);
        upgrades.writeToNBT(tag, "upgrades", registries);
        ListTag asked = new ListTag();
        for (Map.Entry<AEKey, ICraftingLink> l : stockLinks.entrySet()) {
            if (l.getValue().isDone() || l.getValue().isCanceled())
                continue;
            CompoundTag t = new CompoundTag();
            t.put("what", l.getKey().toTagGeneric(registries));
            CompoundTag link = new CompoundTag();
            l.getValue().writeToNBT(link);
            t.put("link", link);
            t.putLong("amount", stockCrafting.amountOf(l.getKey()));
            asked.add(t);
        }
        tag.put("craftLinks", asked);
        CompoundTag o = new CompoundTag();
        own.write(o, registries);
        tag.put("own", o);
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag t = new CompoundTag();
            t.put("link", e.link.toTag());
            CompoundTag s = new CompoundTag();
            e.settings.write(s, registries);
            t.put("settings", s);
            list.add(t);
        }
        tag.put("entries", list);
    }

    public void drops(List<ItemStack> drops) {
        // the machine slot and the stock are ghost slots; what the machine put out into it is real; so is the card
        for (ItemStack card : upgrades)
            if (!card.isEmpty())
                drops.add(card);
        for (int i = 0; i < received.size(); i++) {
            GenericStack s = received.getStack(i);
            if (s != null && s.what() instanceof AEItemKey item)
                for (long left = s.amount(); left > 0; ) {
                    int n = (int) Math.min(left, item.getItem().getDefaultMaxStackSize());
                    drops.add(item.toStack(n));
                    left -= n;
                }
        }
    }

    public void clear() {
        upgrades.clear();
        received.clear();
    }
}
