package com.lumina.bridge.wire;

import io.netty.buffer.ByteBuf;

/** Wraps and unwraps the version's buffer (Netty's, which Minecraft's own buffer extends) as a {@link WireBuf}. */
public final class Wire {
    private Wire() {
    }

    public static WireBuf of(ByteBuf buf) {
        return new NettyWire(buf);
    }

    private record NettyWire(ByteBuf buf) implements WireBuf {
        public void writeVarInt(int value) {
            while ((value & ~0x7F) != 0) {
                buf.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            buf.writeByte(value);
        }

        public int readVarInt() {
            int value = 0;
            for (int shift = 0; shift < 35; shift += 7) {
                byte b = buf.readByte();
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0)
                    return value;
            }
            throw new IllegalArgumentException("VarInt is too big");
        }

        public void writeBytes(byte[] bytes) {
            buf.writeBytes(bytes);
        }

        public byte[] readBytes(int length) {
            byte[] out = new byte[length];
            buf.readBytes(out);
            return out;
        }
    }
}
