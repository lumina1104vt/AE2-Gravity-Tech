package com.lumina.ae2_gvt.items.parts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * A list of M(achine) and h(atch) entries, e.g. "M h h M M h": the hatches follow the machine they belong to. Each entry here is its
 * own letter, so after a removal the ones left can be told apart and compared with what they should be.
 */
class MachineRemovalTest {
    private static final List<Boolean> SHAPE = List.of(true, false, false, true, true, false);   // M h h M M h

    @Test
    void aPickedMachineTakesItsHatchesAndNoOthers() {
        assertEquals(List.of(0, 1, 2), MachineRemoval.withHatches(SHAPE, Set.of(0)));
        assertEquals(List.of(3), MachineRemoval.withHatches(SHAPE, Set.of(3)));          // no hatch of its own
        assertEquals(List.of(4, 5), MachineRemoval.withHatches(SHAPE, Set.of(4)));
    }

    @Test
    void aPickedHatchGoesAlone() {
        assertEquals(List.of(2), MachineRemoval.withHatches(SHAPE, Set.of(2)));
    }

    @Test
    void repeatsAndPlacesOutsideAreIgnoredAndTheAnswerIsSorted() {
        assertEquals(List.of(3, 4, 5), MachineRemoval.withHatches(SHAPE, List.of(4, 3, 4, 99, -1)));
        assertEquals(List.of(), MachineRemoval.withHatches(SHAPE, List.of()));
    }

    @Test
    void removingKeepsTheOthersInTheirOrderWithTheirOwnSettings() {
        // entries and their settings are one object each, so they leave together; what is left keeps its order
        List<String> entries = new ArrayList<>(List.of("A", "a1", "a2", "B", "C", "c1"));
        int n = MachineRemoval.removeAt(entries, MachineRemoval.withHatches(SHAPE, Set.of(0, 4)));
        assertEquals(5, n);                              // A a1 a2 + C c1
        assertEquals(List.of("B"), entries);
    }

    @Test
    void removeAtCountsOnlyWhatWasThere() {
        List<String> entries = new ArrayList<>(List.of("x", "y", "z"));
        assertEquals(2, MachineRemoval.removeAt(entries, List.of(2, 0, 2, 7)));
        assertEquals(List.of("y"), entries);
    }

    @Test
    void selectionStaysOnTheSameMachineWhenItStays() {
        // 6 entries, 0..2 go: the one that was 4 is now 1
        assertEquals(1, MachineRemoval.selectionAfter(4, List.of(0, 1, 2), 6));
        assertEquals(0, MachineRemoval.selectionAfter(3, List.of(0, 1, 2), 6));
    }

    @Test
    void selectionOfARemovedMachineGoesToTheNextOneThatStays() {
        assertEquals(1, MachineRemoval.selectionAfter(1, List.of(1, 2), 5));       // old 1 gone: the old 3 is now 1
        assertEquals(2, MachineRemoval.selectionAfter(4, List.of(3, 4), 5));       // the last ones gone: the last one left
    }

    @Test
    void selectionIsNeverOutsideTheList() {
        assertEquals(0, MachineRemoval.selectionAfter(5, List.of(0, 1, 2, 3, 4, 5), 6));    // all gone
        for (int sel = 0; sel < 6; sel++) {
            List<Integer> gone = List.of(0, 2, 5);
            int s = MachineRemoval.selectionAfter(sel, gone, 6);
            assertTrue(s >= 0 && s < 3, "selected " + sel + " -> " + s);
        }
    }

    @Test
    void removingTheSameKindTakesTheirHatchesAndLeavesOtherKindsAndTheirHatches() {
        // P h P F f P : pulverizers at 0, 2 (no hatch) and 5; a furnace at 3 with a hatch
        List<String> kinds = List.of("P", "P", "P", "F", "F", "P");
        List<Boolean> shape = List.of(true, false, true, true, false, true);
        var same = SameMachines.indicesLike(kinds, shape, 2);
        assertEquals(List.of(0, 2, 5), same);
        var gone = MachineRemoval.withHatches(shape, same);
        assertEquals(List.of(0, 1, 2, 5), gone);
        List<String> left = new ArrayList<>(List.of("P0", "p-hatch", "P2", "F3", "f-hatch", "P5"));
        assertEquals(4, MachineRemoval.removeAt(left, gone));
        assertEquals(List.of("F3", "f-hatch"), left);       // the furnace keeps its own hatch
        assertEquals(0, MachineRemoval.selectionAfter(2, gone, 6));
    }

    @Test
    void pageFollowsTheSelectionAndMarksAreCheckedAgainstTheNewSize() {
        assertEquals(0, MachineRemoval.pageOf(8, 9));
        assertEquals(1, MachineRemoval.pageOf(9, 9));
        assertEquals(Set.of(0, 2), MachineRemoval.validMarks(List.of(0, 2, 3, 40, -1), 3));
    }
}
