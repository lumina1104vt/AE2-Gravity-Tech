package com.lumina.bridge.key;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyTest {
    private static Key item(String id, Object handle) {
        return new Key(Key.Kind.ITEM, id, "", 1, 64, handle, () -> id, () -> "t", (x, y) -> false);
    }

    @Test
    void equalityFollowsTheHandle() {
        Key a = item("minecraft:stick", "stick");
        Key b = item("minecraft:stick", new String("stick"));
        Key c = item("minecraft:stick", "stick with a tag");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);                      // same id, different components
    }

    @Test
    void fieldsAreKept() {
        Key water = new Key(Key.Kind.FLUID, "minecraft:water", "B", 1000, 1, "water", () -> "Water", () -> "Fluids", (x, y) -> false);
        assertEquals(Key.Kind.FLUID, water.kind());
        assertEquals("minecraft:water", water.id());
        assertEquals("B", water.unit());
        assertEquals(1000, water.amountPerUnit());
        assertEquals(1, water.stackLimit());
        assertEquals("FLUID:minecraft:water", water.toString());
    }

    @Test
    void handleOnlyGivesTheRightType() {
        Key k = item("minecraft:stick", "stick");
        assertEquals("stick", k.handle(String.class));
        assertNull(k.handle(Integer.class));
    }
}
