package com.lumina.ae2_gvt.items.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Taking machines out of the machine access interface's list in bulk, in plain Java (no Minecraft types, so it is unit tested).
 * A hatch belongs to the machine before it in the list, so taking a machine out takes its hatches with it: left in, they would
 * quietly become the hatches of the machine before.
 */
public final class MachineRemoval {
    private MachineRemoval() {
    }

    /** Which machines go: the ones marked (the selected one when none is), the ones like the selected, or all. */
    public enum Scope {
        MARKED, SAME, ALL
    }

    /**
     * The places to remove, sorted and without repeats: every picked place that exists, and after each picked machine the hatches that
     * follow it up to the next machine. A picked hatch goes alone.
     */
    public static List<Integer> withHatches(List<Boolean> isMachine, Collection<Integer> picked) {
        TreeSet<Integer> out = new TreeSet<>();
        for (int p : picked) {
            if (p < 0 || p >= isMachine.size())
                continue;
            out.add(p);
            if (!isMachine.get(p))
                continue;
            for (int k = p + 1; k < isMachine.size() && !isMachine.get(k); k++)
                out.add(k);
        }
        return new ArrayList<>(out);
    }

    /** Removes the places in {@code places} (any order, repeats and places out of the list ignored) from {@code list}; how many went. */
    public static <T> int removeAt(List<T> list, Collection<Integer> places) {
        int removed = 0;
        for (int p : new TreeSet<>(places).descendingSet()) {
            if (p < 0 || p >= list.size())
                continue;
            list.remove(p);
            removed++;
        }
        return removed;
    }

    /**
     * Where the selection is after {@code removed} (sorted places) went out of a list that had {@code oldCount}: the same machine when it
     * stays, else the next one that stays (the last when it was at the end). Always inside the new list ({@code 0} when it is empty).
     */
    public static int selectionAfter(int selected, List<Integer> removed, int oldCount) {
        int newCount = oldCount - removed.size();
        if (newCount <= 0)
            return 0;
        int before = 0;
        for (int r : removed)
            if (r < selected)
                before++;
        return Math.max(0, Math.min(selected - before, newCount - 1));
    }

    /** The page that shows listed place {@code index}. */
    public static int pageOf(int index, int perPage) {
        return Math.max(0, index) / Math.max(1, perPage);
    }

    /** The marks that are still places in a list of {@code count} (marked places that moved with a removal are the caller's to clear). */
    public static TreeSet<Integer> validMarks(Collection<Integer> marks, int count) {
        TreeSet<Integer> out = new TreeSet<>();
        for (int m : marks)
            if (m >= 0 && m < count)
                out.add(m);
        return out;
    }
}
