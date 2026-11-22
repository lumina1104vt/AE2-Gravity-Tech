package com.lumina.ae2_gvt.items.parts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * "The same machine" for the machine access interface, in plain Java (no Minecraft types, so it is unit tested):
 * <ul>
 * <li>{@link #scan}: Ctrl + right click with the 機器連結器 on a Pulverizer links every Pulverizer within a
 * radius of it in one go. This picks which cells those are: the cells within the radius (a sphere) that are the
 * same block and are not linked yet, nearest first, at most {@code max} of them.</li>
 * <li>{@link #indicesLike}: setting the optional machine of one listed machine for all the machines of the same
 * block in the interface's list.</li>
 * </ul>
 */
public final class SameMachines {
    private SameMachines() {
    }

    /** A yes/no question about a cell. */
    @FunctionalInterface
    public interface Probe {
        boolean test(int x, int y, int z);
    }

    /** The cells picked (nearest first) and how many matching cells there were before the {@code max} cut. */
    public record Scan(List<int[]> cells, int total) {
        public boolean cut() {
            return total > cells.size();
        }
    }

    /**
     * The cells within {@code radius} blocks (a sphere) of {@code (cx, cy, cz)} for which {@code same} is true and
     * {@code skip} is false, nearest first (the centre first of all), at most {@code max} of them.
     */
    public static Scan scan(int cx, int cy, int cz, int radius, int max, Probe same, Probe skip) {
        int r = Math.max(0, radius);
        long r2 = (long) r * r;
        List<int[]> found = new ArrayList<>();      // x, y, z, squared distance
        for (int dy = -r; dy <= r; dy++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    long d2 = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (d2 > r2)
                        continue;
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (same.test(x, y, z) && !skip.test(x, y, z))
                        found.add(new int[]{x, y, z, (int) d2});
                }
        // nearest first; the same distance goes by y, x, z so the order never depends on how the search ran
        found.sort(Comparator.<int[]>comparingInt(c -> c[3]).thenComparingInt(c -> c[1]).thenComparingInt(c -> c[0])
                .thenComparingInt(c -> c[2]));
        int total = found.size();
        List<int[]> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(Math.max(0, max), total); i++) {
            int[] c = found.get(i);
            picked.add(new int[]{c[0], c[1], c[2]});
        }
        return new Scan(picked, total);
    }

    /**
     * The places in the list of the machines that are the same as the one at {@code index}: the same non-null key
     * (the block it is), and a machine (a hatch is not one). {@code index} itself is in the answer. Empty when the one at
     * {@code index} is not a machine or has no key (its chunk is not loaded).
     */
    public static <K> List<Integer> indicesLike(List<K> keys, List<Boolean> isMachine, int index) {
        List<Integer> out = new ArrayList<>();
        if (index < 0 || index >= keys.size() || keys.get(index) == null || !isMachine.get(index))
            return out;
        for (int i = 0; i < keys.size(); i++)
            if (isMachine.get(i) && Objects.equals(keys.get(i), keys.get(index)))
                out.add(i);
        return out;
    }
}
