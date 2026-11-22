package com.lumina.ae2_gvt.items.parts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * A row of Pulverizers along x at 0, 2, 4, ... and a Furnace in between at every odd step. Ctrl + right click on one
 * Pulverizer must pick the other Pulverizers around it and nothing else.
 */
class SameMachinesTest {
    private static String blockAt(int x, int y, int z) {
        if (y != 0 || z != 0 || x < 0 || x > 40)
            return "air";
        return x % 2 == 0 ? "pulverizer" : "furnace";
    }

    private static SameMachines.Probe pulverizer() {
        return (x, y, z) -> blockAt(x, y, z).equals("pulverizer");
    }

    private static Set<Integer> xs(SameMachines.Scan s) {
        Set<Integer> out = new HashSet<>();
        for (int[] c : s.cells())
            out.add(c[0]);
        return out;
    }

    @Test
    void picksTheSameBlockWithinTheRadiusOnly() {
        // radius 6 round x = 10: the pulverizers at 4, 6, 8, 10, 12, 14, 16; the furnaces are not picked
        var s = SameMachines.scan(10, 0, 0, 6, 64, pulverizer(), (x, y, z) -> false);
        assertEquals(Set.of(4, 6, 8, 10, 12, 14, 16), xs(s));
        assertEquals(7, s.total());
        assertFalse(s.cut());
    }

    @Test
    void theClickedOneIsFirstAndTheOthersGoNearestFirst() {
        var s = SameMachines.scan(10, 0, 0, 6, 64, pulverizer(), (x, y, z) -> false);
        assertArrayEquals(new int[]{10, 0, 0}, s.cells().get(0));
        int last = -1;
        for (int[] c : s.cells()) {
            int d = Math.abs(c[0] - 10);
            assertTrue(d >= last);
            last = d;
        }
    }

    @Test
    void theRadiusIsASphereNotACube() {
        // a block on the corner of the cube (3, 3, 3) is 5.2 away: out of radius 4, in radius 6
        SameMachines.Probe corner = (x, y, z) -> x == 3 && y == 3 && z == 3;
        assertTrue(SameMachines.scan(0, 0, 0, 4, 64, corner, (x, y, z) -> false).cells().isEmpty());
        assertEquals(1, SameMachines.scan(0, 0, 0, 6, 64, corner, (x, y, z) -> false).cells().size());
    }

    @Test
    void alreadyLinkedOnesAreSkipped() {
        var s = SameMachines.scan(10, 0, 0, 6, 64, pulverizer(), (x, y, z) -> x == 10 || x == 12);
        assertEquals(Set.of(4, 6, 8, 14, 16), xs(s));
    }

    @Test
    void theLimitKeepsTheNearestAndSaysItCut() {
        var s = SameMachines.scan(10, 0, 0, 20, 3, pulverizer(), (x, y, z) -> false);
        assertEquals(3, s.cells().size());
        assertEquals(Set.of(10, 8, 12), xs(s));      // the clicked one, then the two that are 2 blocks away
        assertEquals(16, s.total());                 // pulverizers at 0, 2 .. 30 are within 20 of x = 10
        assertTrue(s.cut());
    }

    @Test
    void nothingToPickGivesAnEmptyAnswer() {
        var s = SameMachines.scan(1000, 0, 0, 5, 64, pulverizer(), (x, y, z) -> false);
        assertTrue(s.cells().isEmpty());
        assertEquals(0, s.total());
    }

    @Test
    void radiusZeroIsJustTheClickedBlock() {
        var s = SameMachines.scan(10, 0, 0, 0, 64, pulverizer(), (x, y, z) -> false);
        assertEquals(1, s.cells().size());
    }

    @Test
    void sameKindIsTheSameBlockAmongMachinesNotHatches() {
        List<String> kinds = List.of("pulverizer", "pulverizer", "furnace", "pulverizer", "pulverizer", "furnace");
        // the 2nd pulverizer's hatch (4th entry) is a pulverizer block used as a hatch: not a machine, not counted
        List<Boolean> machine = List.of(true, true, true, false, true, true);
        assertEquals(List.of(0, 1, 4), SameMachines.indicesLike(kinds, machine, 0));
        assertEquals(List.of(2, 5), SameMachines.indicesLike(kinds, machine, 2));
        assertEquals(List.of(), SameMachines.indicesLike(kinds, machine, 3));     // a hatch has no optional machine to share
    }

    @Test
    void anUnloadedMachineIsNeitherSameNorCounted() {
        List<String> kinds = java.util.Arrays.asList("pulverizer", null, null);
        List<Boolean> machine = List.of(true, true, true);
        assertEquals(List.of(0), SameMachines.indicesLike(kinds, machine, 0));
        assertEquals(List.of(), SameMachines.indicesLike(kinds, machine, 1));
        assertEquals(List.of(), SameMachines.indicesLike(kinds, machine, 9));
    }
}
