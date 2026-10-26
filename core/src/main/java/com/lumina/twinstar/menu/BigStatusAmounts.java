package com.lumina.twinstar.menu;

import com.lumina.twinstar.util.BigIntCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.math.BigInteger;

/**
 * Exact amounts of one CPU-status entry (AE2's entry only has longs): in the job's inventory, sent
 * to machines and not back yet, still to be made; whether more of it comes from an infinite source;
 * and what outputs that come with a probability did not give after all their runs (機率產物不足).
 */
public record BigStatusAmounts(BigInteger stored, BigInteger active, BigInteger pending, boolean infinite,
                               BigInteger chanceShort) {
    public void write(FriendlyByteBuf buf) {
        BigIntCodec.write(buf, stored);
        BigIntCodec.write(buf, active);
        BigIntCodec.write(buf, pending);
        buf.writeBoolean(infinite);
        BigIntCodec.write(buf, chanceShort);
    }

    public static BigStatusAmounts read(FriendlyByteBuf buf) {
        return new BigStatusAmounts(BigIntCodec.read(buf), BigIntCodec.read(buf),
                BigIntCodec.read(buf), buf.readBoolean(), BigIntCodec.read(buf));
    }
}
