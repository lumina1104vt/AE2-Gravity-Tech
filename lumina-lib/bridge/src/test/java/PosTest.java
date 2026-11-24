import com.lumina.bridge.Face;
import com.lumina.bridge.Pos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosTest {
    @Test
    void roundTrip() {
        for (int[] p : new int[][]{{0, 0, 0}, {1, 2, 3}, {-1, -64, -1}, {30_000_000, 319, -30_000_000}, {-30_000_000, -2048, 29_999_999}}) {
            Pos pos = Pos.of(p[0], p[1], p[2]);
            assertEquals(p[0], pos.x());
            assertEquals(p[1], pos.y());
            assertEquals(p[2], pos.z());
        }
    }

    @Test
    void relative() {
        Pos pos = Pos.of(10, 64, -5).relative(Face.NORTH, 3);
        assertEquals(10, pos.x());
        assertEquals(64, pos.y());
        assertEquals(-8, pos.z());
        assertEquals(Face.UP, Face.DOWN.opposite());
        assertEquals(Face.WEST, Face.EAST.opposite());
    }

    @Test
    void distances() {
        Pos a = Pos.of(0, 0, 0), b = Pos.of(3, -4, 12);
        assertEquals(19, a.distManhattan(b));
        assertEquals(169.0, a.distSqr(b));
        assertEquals("3, -4, 12", b.toShortString());
        assertEquals(b, a.offset(3, -4, 12));
    }
}
