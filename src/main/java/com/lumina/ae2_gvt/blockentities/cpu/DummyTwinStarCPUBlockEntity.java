package com.lumina.ae2_gvt.blockentities.cpu;

import java.math.BigInteger;

import com.lumina.ae2_gvt.blockentities.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Test hub: capacity and parallel set by hand in its GUI (0 = unlimited), so the engine can be tried
 * without building the hexagram hub (creative / debugging).
 */
public class DummyTwinStarCPUBlockEntity extends TwinStarHostBlockEntity {
    /** 0 = unlimited; any size. */
    private BigInteger capacity = BigInteger.ZERO;
    /** 0 = unlimited; any size. */
    private BigInteger parallel = BigInteger.ZERO;

    public DummyTwinStarCPUBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntities.TWINSTAR_CPU.get(), pos, blockState);
    }

    public BigInteger getCapacity() {
        return capacity;
    }

    public void setCapacity(BigInteger capacity) {
        this.capacity = capacity.max(BigInteger.ZERO);
        saveChanges();
    }

    public BigInteger getParallel() {
        return parallel;
    }

    public void setParallel(BigInteger parallel) {
        this.parallel = parallel.max(BigInteger.ZERO);
        saveChanges();
    }

    @Override
    public @Nullable BigInteger getHostParallelism() {
        return parallel.signum() == 0 ? null : parallel;
    }

    @Override
    public @Nullable BigInteger getHostCapacity() {
        return capacity.signum() == 0 ? null : capacity;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putString("capacity", capacity.toString());
        data.putString("parallel", parallel.toString());
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        capacity = readAmount(data, "capacity");
        parallel = readAmount(data, "parallel");
    }
}
