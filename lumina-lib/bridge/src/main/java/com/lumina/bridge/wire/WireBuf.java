package com.lumina.bridge.wire;

/**
 * A byte stream the mods read and write packets with, without knowing which Minecraft version's buffer is under it.
 * {@link Wire#of} wraps a version's buffer; the mods above only see this.
 */
public interface WireBuf {
    /** A length-style integer: 7 bits per byte, low bits first (the same as Minecraft's varint). */
    void writeVarInt(int value);

    int readVarInt();

    void writeBytes(byte[] bytes);

    byte[] readBytes(int length);
}
