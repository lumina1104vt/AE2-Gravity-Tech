package com.lumina.ae2_gvt.blockentities.cpu;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import appeng.api.config.CpuSelectionMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.inventories.InternalInventory;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lumina.twinstar.api.ITwinStarService;
import com.lumina.twinstar.host.ITwinStarHost;
import com.lumina.twinstar.service.TwinStarService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A block hosting the twin-star engine (the hexagram hub's controller, the test hub). Also a pattern
 * provider: the encoded patterns in its slots are offered to the network as hub-internal patterns
 * (the twin-star engine finishes them inside the black hole, P runs per tick).
 * <p>
 * Patterns also come in from outside: AE2's pattern access terminal lists it like a pattern
 * provider, and hoppers / pipes can insert (never extract) encoded patterns. The jobs running on it
 * are saved with it and handed back when it joins a grid again. Subclasses give the capacity and the
 * parallel count.
 */
public abstract class TwinStarHostBlockEntity extends AENetworkBlockEntity
        implements InternalInventoryHost, ITwinStarHost, ICraftingProvider, PatternContainer {
    /** Pattern slots; the GUI only creates the visible ones (a menu with 50,000 slots would sync them all). */
    public static final int MAX_PATTERNS = 50_000;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, MAX_PATTERNS, 1);
    /** Stored patterns: always slots 0..stored-1. */
    private int stored;
    private boolean compacting;
    /** decoded patterns of the slots; null = rebuild on the next request */
    private @Nullable List<IPatternDetails> decoded;
    /** per stack: one change must not decode all 50,000 again (stacks keep their identity when moved up) */
    private Map<ItemStack, IPatternDetails> decodeCache = new IdentityHashMap<>();
    private final IItemHandler insertOnly = new InsertOnly();
    /** jobs the engine left here while the hub is off a grid (chunk unloaded, network split) */
    private @Nullable CompoundTag parkedJobs;

    protected TwinStarHostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        getMainNode().addService(ICraftingProvider.class, this);
        patterns.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return PatternDetailsHelper.isEncodedPattern(stack);
            }
        });
    }

    // ── pattern provider ──

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if (decoded == null) {
            List<IPatternDetails> list = new ArrayList<>(stored);
            Map<ItemStack, IPatternDetails> cache = new IdentityHashMap<>(stored * 2);
            Level level = getLevel();
            if (level != null)
                for (int i = 0; i < stored; i++) {
                    ItemStack stack = patterns.getStackInSlot(i);
                    if (stack.isEmpty())
                        continue;
                    IPatternDetails details = decodeCache.get(stack);
                    if (details == null)
                        details = PatternDetailsHelper.decodePattern(stack, level);
                    if (details != null) {
                        list.add(details);
                        cache.put(stack, details);
                    }
                }
            decoded = list;
            decodeCache = cache;
        }
        return decoded;
    }

    @Override
    public int getPatternPriority() {
        return 0;
    }

    /** Never pushed to: the twin-star engine runs hub-internal patterns itself. */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return false;
    }

    /** Busy for AE2's own CPUs, so a vanilla CPU never tries (and fails) to push to the hub. */
    @Override
    public boolean isBusy() {
        return true;
    }

    private void patternsChanged() {
        decoded = null;
        ICraftingProvider.requestUpdate(getMainNode());
    }

    @Override
    public void onReady() {
        super.onReady();
        patternsChanged();
    }

    public InternalInventory getPatterns() {
        return patterns;
    }

    public int getStoredPatterns() {
        return stored;
    }

    /** For hoppers and pipes: one slot that is always the next free one; nothing can be pulled out. */
    public IItemHandler getInsertOnlyHandler() {
        return insertOnly;
    }

    private final class InsertOnly implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || stored >= MAX_PATTERNS || !PatternDetailsHelper.isEncodedPattern(stack))
                return stack;
            if (!simulate)
                patterns.setItemDirect(stored, stack.copyWithCount(1));
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PatternDetailsHelper.isEncodedPattern(stack);
        }
    }

    // ── AE2 pattern access terminal ──

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return patterns;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        return new PatternContainerGroup(AEItemKey.of(getBlockState().getBlock()), getBlockState().getBlock().getName(), List.of());
    }

    @Override
    public boolean isHostActive() {
        return getMainNode().isActive();
    }

    @Override
    public CpuSelectionMode getHostWorkerMode() {
        return CpuSelectionMode.ANY;
    }

    // ── jobs saved with the hub ──

    @Override
    public void setParkedJobs(@Nullable CompoundTag jobs) {
        parkedJobs = jobs;
        saveChanges();
    }

    @Override
    public @Nullable CompoundTag takeParkedJobs() {
        CompoundTag jobs = parkedJobs;
        parkedJobs = null;
        return jobs;
    }

    @Override
    public void markJobsChanged() {
        saveChanges();
    }

    protected @Nullable TwinStarService service() {
        IGrid grid = getMainNode().getGrid();
        return grid != null && grid.getService(ITwinStarService.class) instanceof TwinStarService s ? s : null;
    }

    @Override
    public void saveChanges() {
        // packing moves up to 50,000 stacks one by one: one save at the end, not one per move
        // (per move, taking out the first of 1,600 patterns cost 2.5 million saves; breaking the
        // block froze the server for 24 s)
        if (!compacting)
            saveChanges();
    }

    /**
     * The stored patterns are always slots 0..stored-1: a pattern taken out leaves no gap (the ones
     * after it move up), one put in further back moves to the front of the free slots. Only the
     * changed slot is looked at, never all 50,000.
     */
    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        if (compacting || isClientSide()) {
            return;
        }
        compacting = true;
        try {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) {
                if (slot < stored) {
                    for (int i = slot; i < stored - 1; i++)
                        inv.setItemDirect(i, inv.getStackInSlot(i + 1));
                    inv.setItemDirect(stored - 1, ItemStack.EMPTY);
                    stored--;
                }
            } else if (slot >= stored) {
                if (slot > stored) {
                    inv.setItemDirect(stored, stack);
                    inv.setItemDirect(slot, ItemStack.EMPTY);
                }
                stored++;
            }
        } finally {
            compacting = false;
        }
        // only now: AE2 reads the patterns back at once, and only the first `stored` slots
        patternsChanged();
        saveChanges();
    }

    /** After loading (older saves may have gaps): pack and count. */
    private void repack() {
        compacting = true;
        int to = 0;
        for (int from = 0; from < patterns.size(); from++) {
            ItemStack stack = patterns.getStackInSlot(from);
            if (stack.isEmpty())
                continue;
            if (from != to) {
                patterns.setItemDirect(to, stack);
                patterns.setItemDirect(from, ItemStack.EMPTY);
            }
            to++;
        }
        stored = to;
        compacting = false;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        patterns.writeToNBT(data, "patterns");
        TwinStarService service = service();
        CompoundTag jobs = service != null ? service.jobsFor(this) : null;
        if (jobs == null)
            jobs = parkedJobs;
        if (jobs != null)
            data.put(JOBS_TAG, jobs);
    }

    private static final String JOBS_TAG = "twinstarJobs";

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        patterns.readFromNBT(data, "patterns");
        repack();
        decoded = null;
        parkedJobs = data.contains(JOBS_TAG, Tag.TAG_COMPOUND) ? data.getCompound(JOBS_TAG).copy() : null;
    }

    /** Saved as a decimal string (any size); older saves stored a long. */
    protected static BigInteger readAmount(CompoundTag data, String key) {
        if (data.contains(key, Tag.TAG_STRING)) {
            try {
                return new BigInteger(data.getString(key)).max(BigInteger.ZERO);
            } catch (NumberFormatException e) {
                return BigInteger.ZERO;
            }
        }
        return BigInteger.valueOf(Math.max(0, data.getLong(key)));
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        // broken: every job ends and gives back what it holds; items the network has no room for drop
        TwinStarService service = service();
        if (service != null)
            service.hostBroken(this, drops);
        for (int i = 0; i < stored; i++) {
            drops.add(patterns.getStackInSlot(i));
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        compacting = true;      // 50,000 slots emptied: no packing or save per slot
        try {
            patterns.clear();
        } finally {
            compacting = false;
        }
        stored = 0;
        patternsChanged();
        saveChanges();
    }

    // ── capabilities (1.20.1: the block entity answers for them) ──

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
                                                                            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
            return net.minecraftforge.common.util.LazyOptional.of(this::getInsertOnlyHandler).cast();
        return super.getCapability(cap, side);
    }
}
