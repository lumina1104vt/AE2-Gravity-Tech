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
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import com.lumina.twinstar.host.ITwinStarHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Test hub. Also a pattern provider: the encoded patterns in its slots are offered to the network as
 * hub-internal patterns (the twin-star engine finishes them inside the black hole, P runs per tick),
 * so parallelism can be tested without external machines.
 * <p>
 * Patterns also come in from outside: AE2's pattern access terminal lists it like a pattern
 * provider, and hoppers / pipes can insert (never extract) encoded patterns.
 */
public class DummyTwinStarCPUBlockEntity extends AENetworkedBlockEntity
        implements InternalInventoryHost, ITwinStarHost, ICraftingProvider, PatternContainer {
    /** Pattern slots; the GUI only creates the visible ones (a menu with 50,000 slots would sync them all). */
    public static final int MAX_PATTERNS = 50_000;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, MAX_PATTERNS, 1);
    /** Stored patterns: always slots 0..stored-1. */
    private int stored;
    /** 0 = unlimited; any size. */
    private BigInteger capacity = BigInteger.ZERO;
    /** 0 = unlimited; any size. */
    private BigInteger parallel = BigInteger.ZERO;
    private boolean compacting;
    /** decoded patterns of the slots; null = rebuild on the next request */
    private @Nullable List<IPatternDetails> decoded;
    /** per stack: one change must not decode all 50,000 again (stacks keep their identity when moved up) */
    private Map<ItemStack, IPatternDetails> decodeCache = new IdentityHashMap<>();
    private final IItemHandler insertOnly = new InsertOnly();

    public DummyTwinStarCPUBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntities.TWINSTAR_CPU.get(), pos, blockState);
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

    public BigInteger getCapacity() {
        return capacity;
    }

    public void setCapacity(BigInteger capacity) {
        this.capacity = capacity.max(BigInteger.ZERO);
        saveChanges();
    }

    public BigInteger getParallel() {
        return parallel;
    }

    public void setParallel(BigInteger parallel) {
        this.parallel = parallel.max(BigInteger.ZERO);
        saveChanges();
    }

    @Override
    public @Nullable BigInteger getHostParallelism() {
        return parallel.signum() == 0 ? null : parallel;
    }

    @Override
    public @Nullable BigInteger getHostCapacity() {
        return capacity.signum() == 0 ? null : capacity;
    }

    @Override
    public boolean isHostActive() {
        return getMainNode().isActive();
    }

    @Override
    public CpuSelectionMode getHostWorkerMode() {
        return CpuSelectionMode.ANY;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
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
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (compacting || isClientSide()) {
            return;
        }
        patternsChanged();
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
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        patterns.writeToNBT(data, "patterns", registries);
        data.putString("capacity", capacity.toString());
        data.putString("parallel", parallel.toString());
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        patterns.readFromNBT(data, "patterns", registries);
        repack();
        decoded = null;
        capacity = readAmount(data, "capacity");
        parallel = readAmount(data, "parallel");
    }

    /** Saved as a decimal string (any size); older saves stored a long. */
    private static BigInteger readAmount(CompoundTag data, String key) {
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
}
