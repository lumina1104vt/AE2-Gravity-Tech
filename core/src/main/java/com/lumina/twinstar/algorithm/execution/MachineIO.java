package com.lumina.twinstar.algorithm.execution;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * What the hub does to a machine behind a machine access interface, through the machine's own
 * item and fluid capabilities (every face and no face; one handler seen from several faces counts
 * once). There is no slot role in a capability, so: a slot or tank that does not accept what is in
 * it ({@code isItemValid} / {@code isFluidValid} false, a pure query) is an output, anything else
 * an input. Server thread only.
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

    /** Takes everything out of the output slots and tanks; each amount taken goes to {@code taken}. */
    static void extractOutputs(ServerLevel level, BlockPos pos, BiConsumer<AEKey, Long> taken) {
        for (IItemHandler h : itemHandlers(level, pos))
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack in = h.getStackInSlot(s);
                if (in.isEmpty() || h.isItemValid(s, in))
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
    }

    /** Some input slot or tank holds one of {@code keys}. */
    static boolean holdsAny(ServerLevel level, BlockPos pos, Set<AEKey> keys) {
        if (keys.isEmpty())
            return false;
        for (IItemHandler h : itemHandlers(level, pos))
            for (int s = 0; s < h.getSlots(); s++) {
                ItemStack in = h.getStackInSlot(s);
                if (!in.isEmpty() && h.isItemValid(s, in) && keys.contains(AEItemKey.of(in)))
                    return true;
            }
        for (IFluidHandler h : fluidHandlers(level, pos))
            for (int t = 0; t < h.getTanks(); t++) {
                FluidStack in = h.getFluidInTank(t);
                if (!in.isEmpty() && h.isFluidValid(t, in) && keys.contains(AEFluidKey.of(in)))
                    return true;
            }
        return false;
    }

    /**
     * Puts up to {@code amount} of {@code key} in through {@code faces} (a null face: no face), the
     * first face first; returns how much went in. Keys that are neither items nor fluids go nowhere.
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
            }
        }
        return amount - left;
    }
}
