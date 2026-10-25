package com.lumina.twinstar.algorithm.cache;

import java.util.BitSet;
import java.util.PriorityQueue;

/**
 * Items waiting for the theory layer, popped lowest level first; each item is queued once.
 * Levels can rise while an item waits ({@code raiseLevel}), so a popped entry whose level is stale is
 * put back under its current level.
 */
final class DirtySet {
    private final PriorityQueue<Long> heap = new PriorityQueue<>();
    private final BitSet queued = new BitSet();

    void push(int item, int level) {
        if (queued.get(item))
            return;
        queued.set(item);
        heap.add(key(item, level));
    }

    boolean isEmpty() {
        return heap.isEmpty();
    }

    /** Pops the lowest-level item; {@code currentLevel[item]} is checked so stale entries re-sort. */
    int popMin(int[] currentLevel) {
        while (true) {
            long k = heap.poll();
            int item = (int) k;
            int level = (int) (k >>> 32);
            if (item < currentLevel.length && currentLevel[item] != level) {
                heap.add(key(item, currentLevel[item]));
                continue;
            }
            queued.clear(item);
            return item;
        }
    }

    void clear() {
        heap.clear();
        queued.clear();
    }

    private static long key(int item, int level) {
        return ((long) level << 32) | (item & 0xFFFFFFFFL);
    }
}
