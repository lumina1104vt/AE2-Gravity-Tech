package com.lumina.corelib.util;

import com.lumina.bridge.wire.WireBuf;

import java.math.BigInteger;

/**
 * Exact amounts on the wire: a length (a varint, the same as Minecraft's byte-array prefix) and the number's
 * two's-complement bytes, about 0.42 bytes per decimal digit (a decimal string would take 1). Bounded, so a bad
 * packet cannot make the other side allocate megabytes. Reads and writes through the bridge's {@link WireBuf},
 * so it knows no Minecraft version.
 */
public final class BigIntCodec {
    /** bytes of {@link AmountParser#MAX_EXTERNAL} (log2(10) / 8 ≈ 0.4153 per digit), plus sign room */
    public static final int MAX_BYTES = (int) ((AmountParser.MAX_EXPONENT + 4) * 0.4153) + 16;

    /**
     * Screen data from the server: materials and capacity can exceed the order (1e10000 × the
     * amount per item). Plans stop below the unlimited-capacity stand-in (2^65536 = 8 KiB).
     */
    public static final int SCREEN_MAX_BYTES = 16 * 1024;

    private BigIntCodec() {
    }

    /** Screen data (server → client). */
    public static void write(WireBuf buf, BigInteger v) {
        byte[] bytes = v.toByteArray();
        if (bytes.length > SCREEN_MAX_BYTES)
            throw new IllegalArgumentException("ByteArray with size " + bytes.length + " is bigger than allowed " + SCREEN_MAX_BYTES);
        buf.writeVarInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static BigInteger read(WireBuf buf) {
        int length = buf.readVarInt();
        if (length > SCREEN_MAX_BYTES)
            throw new IllegalArgumentException("ByteArray with size " + length + " is bigger than allowed " + SCREEN_MAX_BYTES);
        return new BigInteger(buf.readBytes(length));
    }
}
