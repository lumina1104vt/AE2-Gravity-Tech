package com.lumina.corelib.util;

import com.lumina.bridge.wire.WireBuf;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BigIntCodecTest {
    /** A buffer over a byte array: the codec needs no Minecraft to be tested. */
    static final class Fake implements WireBuf {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data;
        int at;

        public void writeVarInt(int value) {
            while ((value & ~0x7F) != 0) {
                out.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            out.write(value);
        }

        public int readVarInt() {
            load();
            int value = 0;
            for (int shift = 0; ; shift += 7) {
                byte b = data[at++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0)
                    return value;
            }
        }

        public void writeBytes(byte[] bytes) {
            out.writeBytes(bytes);
        }

        public byte[] readBytes(int length) {
            load();
            byte[] r = Arrays.copyOfRange(data, at, at + length);
            at += length;
            return r;
        }

        private void load() {
            if (data == null)
                data = out.toByteArray();
        }
    }

    @Test
    void roundTrip() {
        for (BigInteger v : new BigInteger[]{BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(-1),
                BigInteger.TWO.pow(70), BigInteger.TWO.pow(70).negate(), AmountParser.MAX_EXTERNAL}) {
            Fake buf = new Fake();
            BigIntCodec.write(buf, v);
            assertEquals(v, BigIntCodec.read(buf));
            assertEquals(buf.data.length, buf.at);
        }
    }

    /** The bytes Minecraft's own byte-array codec wrote before (a varint length, then the bytes): the format must not change. */
    @Test
    void sameBytesAsBefore() {
        assertWire(BigInteger.valueOf(127), 0x01, 0x7F);
        assertWire(BigInteger.valueOf(-300), 0x02, 0xFE, 0xD4);
        // 188 bytes long: the length takes two varint bytes (0xBC 0x01)
        Fake buf = new Fake();
        BigIntCodec.write(buf, BigInteger.TWO.pow(1500));
        byte[] w = buf.out.toByteArray();
        assertEquals((byte) 0xBC, w[0]);
        assertEquals(0x01, w[1]);
        assertEquals(2 + 188, w.length);
    }

    private static void assertWire(BigInteger v, int... expected) {
        Fake buf = new Fake();
        BigIntCodec.write(buf, v);
        byte[] w = buf.out.toByteArray();
        assertEquals(expected.length, w.length);
        for (int i = 0; i < expected.length; i++)
            assertEquals((byte) expected[i], w[i]);
    }

    @Test
    void bigIntegersFitTheWireLimit() {
        assertTrue(AmountParser.MAX_EXTERNAL.toByteArray().length <= BigIntCodec.MAX_BYTES);
    }

    @Test
    void refusesWhatIsTooBig() {
        assertThrows(IllegalArgumentException.class,
                () -> BigIntCodec.write(new Fake(), BigInteger.TWO.pow(8 * BigIntCodec.SCREEN_MAX_BYTES)));
        Fake buf = new Fake();
        buf.writeVarInt(32768);
        assertThrows(IllegalArgumentException.class, () -> BigIntCodec.read(buf));
    }
}
