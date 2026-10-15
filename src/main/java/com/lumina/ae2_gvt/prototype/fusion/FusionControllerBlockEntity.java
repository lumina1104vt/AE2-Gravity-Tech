package com.lumina.ae2_gvt.prototype.fusion;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The fusion controller's state. Energy input heats the plasma, which loses heat in proportion
 * to its temperature, so it settles where the two balance: {@link #equilibrium()}. The player
 * sets the energy-input cap on the controller; recipes have a minimum temperature and only run
 * at or above it.
 * <p>
 * The chamber ({@link FusionStructure}) is checked once a second; {@link #chamber} is its low
 * corner while formed (synced, so the client draws the plasma at its centre).
 * <p>
 * Placeholder until energy and recipes exist: the cap is taken as fully supplied,
 * the output is made up, and one stand-in recipe needs {@link #PLACEHOLDER_NEED}.
 */
public class FusionControllerBlockEntity extends BlockEntity {
    /** A cold, unpowered chamber. */
    public static final double AMBIENT = 1500;
    /** Equilibrium Kelvin per FE/t of input. */
    public static final double KELVIN_PER_FE = 0.0005;
    /** Int max (~2.1e9 FE/t), settling near 1,075,000 K - well past the hottest colour (80,000 K at ~157M FE/t). */
    public static final long MAX_CAP = Integer.MAX_VALUE;
    /** Stand-in for the current recipe's minimum temperature. */
    public static final int PLACEHOLDER_NEED = 15_000;
    /** Fraction of the gap to the equilibrium closed per tick (heating / cooling rate). */
    private static final double RATE = 0.01;

    /** How often the chamber is checked, in ticks. */
    private static final int CHECK_INTERVAL = 20;
    /** Chambers whose controller was just removed, so its frames can be reset (see unformOnRemoval). */
    private static final Map<GlobalPos, BlockPos> LAST_CHAMBER = new ConcurrentHashMap<>();

    private double temperature = AMBIENT;
    private long energyCap = 0;
    private @Nullable BlockPos chamber;

    public FusionControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FusionGenerator.CONTROLLER_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionControllerBlockEntity be) {
        if (!level.isClientSide() && (level.getGameTime() + pos.asLong()) % CHECK_INTERVAL == 0) {
            be.checkChamber(level, pos, state);
            state = be.getBlockState();
        }
        double goal = state.getValue(FusionControllerBlock.FORMED) ? be.equilibrium() : AMBIENT;
        be.temperature += (goal - be.temperature) * RATE;
    }

    private void checkChamber(Level level, BlockPos pos, BlockState state) {
        BlockPos found = FusionStructure.find(level, pos, chamber);
        if (Objects.equals(found, chamber) && state.getValue(FusionControllerBlock.FORMED) == (found != null)) {
            return;
        }
        if (chamber != null && !chamber.equals(found)) {
            FusionStructure.setFormed(level, chamber, false);
        }
        chamber = found;
        if (found != null) {
            FusionStructure.setFormed(level, found, true);
            LAST_CHAMBER.put(GlobalPos.of(level.dimension(), pos.immutable()), found);
        } else {
            LAST_CHAMBER.remove(GlobalPos.of(level.dimension(), pos));
        }
        level.setBlock(pos, state.setValue(FusionControllerBlock.FORMED, found != null), Block.UPDATE_ALL);
        setChanged();
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Called when the controller block is removed: the frames of its chamber go back to unformed. */
    static void unformOnRemoval(Level level, BlockPos pos) {
        BlockPos chamber = LAST_CHAMBER.remove(GlobalPos.of(level.dimension(), pos));
        if (chamber != null) {
            FusionStructure.setFormed(level, chamber, false);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (chamber != null && level != null && !level.isClientSide()) {
            LAST_CHAMBER.put(GlobalPos.of(level.dimension(), worldPosition.immutable()), chamber);
        }
    }

    /** Low corner of the formed chamber, or null. */
    public @Nullable BlockPos chamber() {
        return chamber;
    }

    public double temperature() {
        return temperature;
    }

    public double equilibrium() {
        return AMBIENT + energyCap * KELVIN_PER_FE;
    }

    public long energyCap() {
        return energyCap;
    }

    public boolean formed() {
        return getBlockState().getValue(FusionControllerBlock.FORMED);
    }

    /** Energy actually taken in (placeholder: the whole cap while formed). */
    public long energyIn() {
        return formed() ? energyCap : 0;
    }

    /** Power produced (placeholder: more than goes in, once the recipe runs). */
    public long energyOut() {
        return formed() && temperature >= PLACEHOLDER_NEED ? energyCap * 8 / 5 : 0;
    }

    public void setEnergyCap(long cap) {
        energyCap = Math.max(0, Math.min(MAX_CAP, cap));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);   // clients simulate the approach
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("temperature", temperature);
        output.putLong("energyCap", energyCap);
        if (chamber != null) {
            output.putLong("chamber", chamber.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        temperature = input.getDoubleOr("temperature", AMBIENT);
        energyCap = input.getLongOr("energyCap", 0);
        chamber = input.getLong("chamber").map(BlockPos::of).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
