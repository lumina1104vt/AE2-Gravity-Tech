package com.lumina.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** The 1.21.1 side of {@link Pos} and {@link Face}: Minecraft's own classes in and out. Same packing, so no maths. */
public final class Positions {
    private Positions() {
    }

    public static Pos of(BlockPos pos) {
        return new Pos(pos.asLong());
    }

    public static BlockPos toMc(Pos pos) {
        return BlockPos.of(pos.packed());
    }

    public static Face of(Direction d) {
        return Face.values()[d.get3DDataValue()];
    }

    public static Direction toMc(Face f) {
        return Direction.from3DDataValue(f.ordinal());
    }
}
