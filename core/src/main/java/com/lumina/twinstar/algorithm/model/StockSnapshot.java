package com.lumina.twinstar.algorithm.model;

import java.util.BitSet;

/**
 * Network stock taken on the main thread; the planner (background thread) only reads this, never
 * AE2's storage.
 */
public final class StockSnapshot {
    private final AmountArray stock;
    private final BitSet infinite;

    public StockSnapshot(AmountArray stock, BitSet infinite) {
        this.stock = stock;
        this.infinite = infinite;
    }

    public static StockSnapshot empty() {
        return new StockSnapshot(new AmountArray(0), new BitSet());
    }

    public Amount stock(int item) {
        return stock.get(item);
    }

    public boolean isInfinite(int item) {
        return infinite.get(item);
    }

    public int size() {
        return stock.size();
    }

    /** A working copy sized for {@code items} item ids (the item index may have grown since). */
    public AmountArray copyStock(int items) {
        return stock.resized(Math.max(items, stock.size()));
    }
}
