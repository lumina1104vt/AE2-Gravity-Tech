package com.lumina.bridge.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataTest {
    @Test
    void missingNamesGiveNothing() {
        Data d = new Data();
        assertEquals(0, d.getInt("x"));
        assertEquals(0L, d.getLong("x"));
        assertEquals("", d.getString("x"));
        assertFalse(d.getBoolean("x"));
        assertEquals(0, d.getIntArray("x").length);
        assertEquals(0, d.getList("x").size());
        assertEquals(0, d.getData("x").size());
        assertNull(d.getUUID("x"));
    }

    @Test
    void valuesComeBack() {
        UUID id = UUID.randomUUID();
        Data d = new Data().putInt("i", 7).putLong("l", 1L << 40).putDouble("d", 0.25).putBoolean("b", true)
                .putString("s", "hi").putIntArray("a", new int[]{1, 2, 3}).putUUID("u", id)
                .put("sub", new Data().putInt("k", 9));
        assertEquals(7, d.getInt("i"));
        assertEquals(1L << 40, d.getLong("l"));
        assertEquals(0.25, d.getDouble("d"));
        assertTrue(d.getBoolean("b"));
        assertEquals("hi", d.getString("s"));
        assertArrayEquals(new int[]{1, 2, 3}, d.getIntArray("a"));
        assertEquals(id, d.getUUID("u"));
        assertEquals(9, d.getData("sub").getInt("k"));
        assertEquals(9L, d.getData("sub").getLong("k"));    // a number reads as any number, as in Minecraft
    }

    @Test
    void listsKeepCompoundsAndStrings() {
        DataList l = new DataList().add(new Data().putInt("a", 1)).addString("x").add(new Data().putInt("a", 2));
        int sum = 0;
        for (Data d : l)
            sum += d.getInt("a");
        assertEquals(3, sum);
        assertEquals(java.util.List.of("x"), l.strings());
        assertEquals(3, l.size());
    }

    @Test
    void equalityLooksInside() {
        Data a = new Data().putIntArray("a", new int[]{1}).put("l", new DataList().addString("q"));
        Data b = new Data().putIntArray("a", new int[]{1}).put("l", new DataList().addString("q"));
        assertEquals(a, b);
        assertNotEquals(a, new Data().putIntArray("a", new int[]{2}));
    }
}
