package com.lumina.bridge;

/** The six sides of a block; a version's adapter turns it to and from that version's direction class. */
public enum Face {
    DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);

    public final int dx;
    public final int dy;
    public final int dz;

    Face(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** Lower case name ("north"), as Minecraft names it; for messages. */
    public String getName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Face opposite() {
        return values()[ordinal() ^ 1];
    }
}
