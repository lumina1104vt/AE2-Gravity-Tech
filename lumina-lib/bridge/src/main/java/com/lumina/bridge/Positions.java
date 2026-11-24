package com.lumina.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

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

    public static Place of(GlobalPos at) {
        return new Place(at.dimension().location().toString(), of(at.pos()));
    }

    /** Null when there is no place or the dimension name is not a valid name. */
    public static @Nullable GlobalPos toMc(@Nullable Place place) {
        if (place == null)
            return null;
        ResourceLocation dim = ResourceLocation.tryParse(place.dimension());
        return dim == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), toMc(place.pos()));
    }
}
