package com.lumina.ae2_gvt.storage;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.BiConsumer;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.lumina.twinstar.api.IBigStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * An open black hole disk: any number of types of anything, any amount each. AE2 sees it through its
 * long-based MEStorage, amounts above a long showing as the long maximum; the twin-star engine finds it in the
 * drive and takes exact amounts through {@link IBigStorage}.
 */
final class BlackHoleCellInventory implements StorageCell, IBigStorage {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final double IDLE_DRAIN = 10;

    private final ItemStack stack;
    private final BlackHoleDisks disks;
    private final Map<AEKey, BigInteger> contents;
    private final ISaveProvider host;

    BlackHoleCellInventory(ItemStack stack, BlackHoleDisks disks, Map<AEKey, BigInteger> contents, ISaveProvider host) {
        this.stack = stack;
        this.disks = disks;
        this.contents = contents;
        this.host = host;
    }

    @Override
    public CellState getStatus() {
        return contents.isEmpty() ? CellState.EMPTY : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return IDLE_DRAIN;
    }

    @Override
    public void persist() {
        disks.setDirty();
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return amount <= 0 ? 0 : add(what, BigInteger.valueOf(amount), mode == Actionable.SIMULATE).longValue();
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return amount <= 0 ? 0 : take(what, BigInteger.valueOf(amount), mode == Actionable.SIMULATE).longValue();
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        // several storages add into the same counter: saturate instead of wrapping to negative
        contents.forEach((key, v) -> {
            long add = v.min(LONG_MAX).longValue();
            long cur = out.get(key);
            long sum = cur + add;
            out.set(key, ((cur ^ sum) & (add ^ sum)) < 0 ? Long.MAX_VALUE : sum);
        });
    }

    @Override
    public Component getDescription() {
        return stack.getHoverName();
    }

    @Override
    public BigInteger getAmount(AEKey key) {
        return contents.getOrDefault(key, BigInteger.ZERO);
    }

    @Override
    public BigInteger insertBig(AEKey key, BigInteger amount, boolean simulate) {
        BigInteger in = add(key, amount, simulate);
        if (!simulate && in.signum() > 0)
            contentsChangedOutsideAe2();
        return in;
    }

    @Override
    public BigInteger extractBig(AEKey key, BigInteger amount, boolean simulate) {
        BigInteger out = take(key, amount, simulate);
        if (!simulate && out.signum() > 0)
            contentsChangedOutsideAe2();
        return out;
    }

    @Override
    public void forEachStored(BiConsumer<AEKey, BigInteger> action) {
        contents.forEach(action);
    }

    private BigInteger add(AEKey key, BigInteger amount, boolean simulate) {
        // not into itself, nor any other: the contents would be out of reach
        if (amount.signum() <= 0 || key instanceof AEItemKey item && item.getItem() instanceof BlackHoleCell)
            return BigInteger.ZERO;
        if (!simulate) {
            contents.merge(key, amount, BigInteger::add);
            saved();
        }
        return amount;      // no limit
    }

    private BigInteger take(AEKey key, BigInteger amount, boolean simulate) {
        BigInteger have = getAmount(key);
        BigInteger take = have.min(amount.max(BigInteger.ZERO));
        if (!simulate && take.signum() > 0) {
            BigInteger left = have.subtract(take);
            if (left.signum() == 0)
                contents.remove(key);
            else
                contents.put(key, left);
            saved();
        }
        return take;
    }

    private void saved() {
        disks.setDirty();
        if (host != null)
            host.saveChanges();
    }

    /** AE2 caches the network's contents; writes that did not go through it must say so. */
    private void contentsChangedOutsideAe2() {
        IGridNode node = host instanceof IActionHost drive ? drive.getActionableNode() : null;
        if (node != null && node.getGrid() != null)
            node.getGrid().getStorageService().invalidateCache();
    }
}
