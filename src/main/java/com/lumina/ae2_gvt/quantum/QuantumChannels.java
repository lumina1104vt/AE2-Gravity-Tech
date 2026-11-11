package com.lumina.ae2_gvt.quantum;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelMode;
import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;

/** The quantum controllers' sums: which controllers are a slave's, and how many channels a network can still give. */
public final class QuantumChannels {
    public static final int INFINITE = -2, UNKNOWN = -1;
    /** what one face of a controller gives, before the channel mode's factor */
    public static final int PER_FACE = 32;

    private QuantumChannels() {
    }

    /** The slave controllers among {@code controllers}, and every quantum controller built onto one (it is part of that slave). */
    public static Set<ControllerBlockEntity> slaveStructures(Collection<ControllerBlockEntity> controllers) {
        Set<ControllerBlockEntity> slaves = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<ControllerBlockEntity> open = new ArrayDeque<>();
        for (ControllerBlockEntity c : controllers)
            if (c instanceof QuantumControllerBlockEntity q && q.isSlave() && slaves.add(c))
                open.add(c);
        while (!open.isEmpty()) {
            ControllerBlockEntity from = open.poll();
            for (ControllerBlockEntity c : controllers) {
                if (c instanceof QuantumControllerBlockEntity && !slaves.contains(c) && c.getLevel() == from.getLevel()
                        && c.getBlockPos().distManhattan(from.getBlockPos()) == 1) {
                    slaves.add(c);
                    open.add(c);
                }
            }
        }
        return slaves;
    }

    /**
     * The channels left, or {@link #INFINITE}: the master's quantum controllers, six faces of {@link #PER_FACE} each times the channel mode's
     * factor (1 to 4 in AE2's settings), less what is in use. The slaves use the same pool, they add none. With no controller
     * structure it is the ad hoc network's few.
     */
    public static int left(IGrid grid) {
        var pathing = grid.getPathingService();
        ChannelMode mode = pathing.getChannelMode();
        if (mode == ChannelMode.INFINITE)
            return INFINITE;
        long total;
        if (pathing.getControllerState() != ControllerState.CONTROLLER_ONLINE) {
            total = mode.getAdHocNetworkChannels();
        } else {
            java.util.List<ControllerBlockEntity> all = new java.util.ArrayList<>();
            for (IGridNode node : grid.getMachineNodes(ControllerBlockEntity.class))
                if (node.getOwner() instanceof ControllerBlockEntity c)
                    all.add(c);
            Set<ControllerBlockEntity> slaves = slaveStructures(all);
            long masters = all.stream().filter(c -> c instanceof QuantumControllerBlockEntity && !slaves.contains(c)).count();
            total = masters * 6L * PER_FACE * mode.getCableCapacityFactor();
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, total - pathing.getUsedChannels()));
    }
}
