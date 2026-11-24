package com.lumina.bridge;

/**
 * A block position as one {@code long} (x 26 bits, z 26 bits, y 12 bits: Minecraft's own packing), so the engine
 * can pass and compare positions without making objects. Each version's adapter turns it to and from its
 * own position class.
 */
public record Pos(long packed) {
    private static final int XZ_BITS = 26;
    private static final int Y_BITS = 12;
    private static final long XZ_MASK = (1L << XZ_BITS) - 1;
    private static final long Y_MASK = (1L << Y_BITS) - 1;

    public static Pos of(int x, int y, int z) {
        return new Pos(((x & XZ_MASK) << (XZ_BITS + Y_BITS)) | ((z & XZ_MASK) << Y_BITS) | (y & Y_MASK));
    }

    public int x() {
        return (int) (packed << (64 - XZ_BITS - XZ_BITS - Y_BITS) >> (64 - XZ_BITS));
    }

    public int y() {
        return (int) (packed << (64 - Y_BITS) >> (64 - Y_BITS));
    }

    public int z() {
        return (int) (packed << (64 - XZ_BITS - Y_BITS) >> (64 - XZ_BITS));
    }

    public Pos offset(int dx, int dy, int dz) {
        return of(x() + dx, y() + dy, z() + dz);
    }

    /** |dx| + |dy| + |dz| to the other position. */
    public int distManhattan(Pos o) {
        return Math.abs(x() - o.x()) + Math.abs(y() - o.y()) + Math.abs(z() - o.z());
    }

    /** Squared straight distance to the other position. */
    public double distSqr(Pos o) {
        double dx = x() - o.x(), dy = y() - o.y(), dz = z() - o.z();
        return dx * dx + dy * dy + dz * dz;
    }

    /** "x, y, z", for messages. */
    public String toShortString() {
        return x() + ", " + y() + ", " + z();
    }

    public Pos relative(Face face, int distance) {
        return of(x() + face.dx * distance, y() + face.dy * distance, z() + face.dz * distance);
    }
}
