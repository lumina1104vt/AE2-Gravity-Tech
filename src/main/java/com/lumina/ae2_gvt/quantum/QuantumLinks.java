package com.lumina.ae2_gvt.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.lumina.ae2_gvt.AE2GravityTech;

/**
 * Links quantum controllers of one frequency: a slave gets a grid connection to the master, so the two networks are one (any
 * distance, any dimension, as long as both are loaded). Looked over once a second, and at once after a change.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID)
public final class QuantumLinks {
    /** what a controller's link is doing (QuantumControllerBlockEntity#getLinkState) */
    public static final int NONE = 0, MASTER_OK = 1, SLAVE_OK = 2, NO_MASTER = 3, TWO_MASTERS = 4, FAILED = 5;

    /** the screen's list: lines apart by LINE, a line's name and counts apart by FIELD */
    public static final char LINE = (char) 10, FIELD = (char) 1;
    private static final Set<QuantumControllerBlockEntity> ALL = ConcurrentHashMap.newKeySet();
    private static volatile boolean dirty = true, repathNext;
    private static int ticks;

    private QuantumLinks() {
    }

    static void add(QuantumControllerBlockEntity be) {
        ALL.add(be);
        dirty = true;
    }

    static void remove(QuantumControllerBlockEntity be) {
        if (ALL.remove(be)) {
            unlink(be);
            dirty = true;
        }
    }

    /** a role or frequency was changed by hand: look again now, and start the pathing over (a slave counts as a controller or not) */
    static void dirty() {
        dirty = true;
        repathNext = true;
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        if (!dirty && ++ticks % 20 != 0)
            return;
        dirty = false;
        reconcile();
    }

    /**
     * Every frequency in use by the controllers that are loaded, for the screen's list: a line each (LINE apart), name, masters
     * and slaves (FIELD apart), with the separators kept out of the names.
     */
    public static String summary() {
        Map<String, int[]> counts = new java.util.TreeMap<>();
        for (QuantumControllerBlockEntity be : ALL) {
            if (be.getFrequency().isEmpty())
                continue;
            int[] c = counts.computeIfAbsent(be.getFrequency(), f -> new int[2]);
            if (be.getRole() == QuantumControllerBlockEntity.MASTER)
                c[0]++;
            else if (be.getRole() == QuantumControllerBlockEntity.SLAVE)
                c[1]++;
        }
        StringBuilder out = new StringBuilder();
        int rows = 0;
        for (var e : counts.entrySet()) {
            if (rows++ >= 100)
                break;
            out.append(e.getKey().replace(LINE, ' ').replace(FIELD, ' ')).append(FIELD).append(e.getValue()[0]).append(FIELD)
                    .append(e.getValue()[1]).append(LINE);
        }
        return out.toString();
    }

    private static void unlink(QuantumControllerBlockEntity be) {
        if (be.link != null) {
            try {
                be.link.destroy();
            } catch (RuntimeException ignored) {
            }
            be.link = null;
        }
    }

    private static void reconcile() {
        boolean changed = repathNext;
        repathNext = false;
        Map<String, List<QuantumControllerBlockEntity>> byFrequency = new HashMap<>();
        for (QuantumControllerBlockEntity be : ALL) {
            if (be.getRole() == QuantumControllerBlockEntity.OFF || be.getFrequency().isEmpty()) {
                changed |= be.link != null;
                unlink(be);
                be.linkState = NONE;
                be.slaves = 0;
            } else {
                byFrequency.computeIfAbsent(be.getFrequency(), f -> new ArrayList<>()).add(be);
            }
        }
        for (List<QuantumControllerBlockEntity> group : byFrequency.values()) {
            List<QuantumControllerBlockEntity> masters = new ArrayList<>(), slaves = new ArrayList<>();
            for (QuantumControllerBlockEntity be : group)
                (be.getRole() == QuantumControllerBlockEntity.MASTER ? masters : slaves).add(be);
            QuantumControllerBlockEntity master = masters.size() == 1 ? masters.get(0) : null;
            for (QuantumControllerBlockEntity m : masters) {
                changed |= m.link != null;
                unlink(m);
                m.linkState = master != null ? MASTER_OK : TWO_MASTERS;
                m.slaves = master != null ? slaves.size() : 0;
            }
            for (QuantumControllerBlockEntity s : slaves) {
                s.slaves = 0;
                if (master == null) {
                    changed |= s.link != null;
                    unlink(s);
                    s.linkState = masters.isEmpty() ? NO_MASTER : TWO_MASTERS;
                    continue;
                }
                IGridNode a = s.getMainNode().getNode(), b = master.getMainNode().getNode();
                if (a == null || b == null) {
                    s.linkState = FAILED;
                    continue;
                }
                if (s.link != null && (s.link.a() == b || s.link.b() == b) && (s.link.a() == a || s.link.b() == a)) {
                    s.linkState = SLAVE_OK;
                    continue;
                }
                unlink(s);
                try {
                    s.link = GridHelper.createConnection(a, b);
                    s.linkState = SLAVE_OK;
                    changed = true;
                } catch (RuntimeException e) {     // already one network (a cable between them), or a loop
                    s.linkState = a.getGrid() != null && a.getGrid() == b.getGrid() ? SLAVE_OK : FAILED;
                }
            }
        }
        if (changed) {      // what counts as a controller has moved: the pathing starts over
            for (QuantumControllerBlockEntity be : ALL)
                be.getMainNode().ifPresent(grid -> grid.getPathingService().repath());
        }
    }
}
