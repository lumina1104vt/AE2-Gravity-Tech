package com.lumina.bridge;

/** A position in a named dimension ("minecraft:overworld"): to keep a block for later, or to tell worlds apart. */
public record Place(String dimension, Pos pos) {
}
