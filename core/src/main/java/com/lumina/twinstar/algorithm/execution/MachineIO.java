package com.lumina.twinstar.algorithm.execution;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * What the hub does to a machine behind a machine access interface, through the machine's own
 * item and fluid capabilities (every face and no face; one handler seen from several faces counts
 * once). There is no slot role in a capability, so, by pure queries: a slot or tank that does not
 * accept what is in it ({@code isItemValid} / {@code isFluidValid} false) is an output; so is an item
 * slot with room that refuses one more of what is in it (a simulated insert: Mekanism's output slots
 * accept anything but take nothing from outside). Anything else is an input.
 * <p>
 * Other kinds of stacks (Mekanism's chemicals, through Applied Mekanistics) go through AE2's
 * external storage strategies of their key type on every face: what can be taken out from
 * outside is an output.
 * Server thread only.
 */
final class MachineIO {
    /** every face, then no face */
    private static final @Nullable Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null};

    private MachineIO() {
    }

    static List<IItemHandler> itemHandlers(ServerLevel level, BlockPos pos) {
        Set<IItemHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<IItemHandler> out = new ArrayList<>();
        for (Direction d : SIDES) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, d);
            if (h != null && seen.add(h))
                out.add(h);
        }
        return out;
    }

    static List<IFluidHandler> fluidHandlers(ServerLevel level, BlockPos pos) {
        Set<IFluidHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<IFluidHandler> out = new ArrayList<>();
        for (Direction d : SIDES) {
            IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, d);
            if (h != null && seen.add(h))
                out.add(h);
        }
        return out;
    }

    /** Slot {@code s} holds {@code in} (not empty) as an output. */
    private static boolean isOutput(IItemHandler h, int s, ItemStack in) {
        if (!h.isItemValid(s, in))
            return true;
        int room = Math.min(h.getSlotLimit(s), in.getMaxStackSize()) - in.getCount();
        return room > 0 && h.insertItem(s, in.copyWithCount(1), true).getCount() == 1;
    }

    /**
     * AE2's external storage strategies of the key types other than items and fluids, per machine and
     * face. Made once: each holds a capability cache the level keeps a listener for.
     */
    private static final Map<ServerLevel, Map<BlockPos, List<List<ExternalStorageStrategy>>>> STRATEGIES = new WeakHashMap<>();

    private static List<List<ExternalStorageStrategy>> strategies(ServerLevel level, BlockPos pos) {
        return STRATEGIES.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(pos.immutable(), p -> {
            List<List<ExternalStorageStrategy>> byFace = new ArrayList<>();
            for (Direction d : Direction.values()) {
                List<ExternalStorageStrategy> list = new ArrayList<>();
                StackWorldBehaviors.createExternalStorageStrategies(level, p, d).forEach((type, strategy) -> {
                    if (type != AEKeyType.items() && type != AEKeyType.fluids())
                        list.add(strategy);
                });
                byFace.add(list);
            }
            return byFace;
        });
    }

    /** The machine at {@code pos} is no longer used (its interface is gone). */
    static void forget(ServerLevel level, BlockPos pos) {
        Map<BlockPos, List<List<ExternalStorageStrategy>>> m = STRATEGIES.get(level);
        if (m != null)
            m.remove(pos);
    }

    /** Storages of those other key types (Mekanism's chemicals) on every face, or only on {@code only}. */
    private static List<MEStorage> otherStorages(ServerLevel level, BlockPos pos, boolean extractableOnly, @Nullable Direction only) {
        List<MEStorage> out = new ArrayList<>();
        List<List<ExternalStorageStrategy>> byFace = strategies(level, pos);
        for (Direction d : Direction.values()) {
            if (only != null && d != only)
                continue;
            for (ExternalStorageStrategy strategy : byFace.get(d.ordinal())) {
                MEStorage storage = strategy.createWrapper(extractableOnly, () -> {
                });
                if (storage != null)
                    out.add(storage);
            }
        }
        return out;
    }

    /** Takes everything out of the output slots and tanks; each amount taken goes to {@code taken}. */
    static void extractOutputs(ServerLevel level, BlockPos pos, BiConsumer<AEKey, Long> taken) {
        for (IItemHandler h : itemHandlers(level, pos))
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack in = h.getStackInSlot(s);
                if (in.isEmpty() || !isOutput(h, s, in))
                    continue;
                ItemStack got = h.extractItem(s, in.getCount(), false);
                if (!got.isEmpty())
                    taken.accept(AEItemKey.of(got), (long) got.getCount());
            }
        for (IFluidHandler h : fluidHandlers(level, pos))
            for (int t = 0; t < h.getTanks(); t++) {
                FluidStack in = h.getFluidInTank(t);
                if (in.isEmpty() || h.isFluidValid(t, in))
                    continue;
                FluidStack got = h.drain(in.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (!got.isEmpty())
                    taken.accept(AEFluidKey.of(got), (long) got.getAmount());
            }
        for (MEStorage storage : otherStorages(level, pos, true, null))
            for (var e : storage.getAvailableStacks()) {
                long got = storage.extract(e.getKey(), e.getLongValue(), Actionable.MODULATE, IActionSource.empty());
                if (got > 0)
                    taken.accept(e.getKey(), got);
            }
    }

    /** Some input slot or tank holds one of {@code keys}. */
    static boolean holdsAny(ServerLevel level, BlockPos pos, Set<AEKey> keys) {
        if (keys.isEmpty())
            return false;
        for (IItemHandler h : itemHandlers(level, pos))
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack in = h.getStackInSlot(s);
                if (!in.isEmpty() && keys.contains(AEItemKey.of(in)) && !isOutput(h, s, in))
                    return true;
            }
        for (IFluidHandler h : fluidHandlers(level, pos))
            for (int t = 0; t < h.getTanks(); t++) {
                FluidStack in = h.getFluidInTank(t);
                if (!in.isEmpty() && h.isFluidValid(t, in) && keys.contains(AEFluidKey.of(in)))
                    return true;
            }
        boolean others = false;
        for (AEKey k : keys)
            others |= !(k instanceof AEItemKey) && !(k instanceof AEFluidKey);
        if (others)
            for (MEStorage storage : otherStorages(level, pos, false, null)) {
                KeyCounter all = storage.getAvailableStacks();
                for (AEKey k : keys)
                    if (all.get(k) > 0)
                        return true;
            }
        return false;
    }

    /**
     * Puts up to {@code amount} of {@code key} in through {@code faces} (a null face: no face; for
     * other key types, any face), the first face first; returns how much went in.
     */
    static long insert(ServerLevel level, BlockPos pos, List<@Nullable Direction> faces, AEKey key, long amount) {
        long left = amount;
        for (Direction face : faces) {
            if (left <= 0)
                break;
            if (key instanceof AEItemKey item) {
                IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
                if (h == null)
                    continue;
                int max = Math.max(1, item.getMaxStackSize());
                while (left > 0) {
                    int n = (int) Math.min(max, left);
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(h, item.toStack(n), false);
                    int in = n - rest.getCount();
                    left -= in;
                    if (in < n)
                        break;      // full: next face
                }
            } else if (key instanceof AEFluidKey fluid) {
                IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
                if (h == null)
                    continue;
                while (left > 0) {
                    int n = (int) Math.min(Integer.MAX_VALUE, left);
                    int in = h.fill(fluid.toStack(n), IFluidHandler.FluidAction.EXECUTE);
                    left -= in;
                    if (in < n)
                        break;
                }
            } else {
                // no face: any face that takes it
                for (MEStorage storage : otherStorages(level, pos, false, face)) {
                    left -= storage.insert(key, left, Actionable.MODULATE, IActionSource.empty());
                    if (left <= 0)
                        break;
                }
            }
        }
        return amount - left;
    }
}
