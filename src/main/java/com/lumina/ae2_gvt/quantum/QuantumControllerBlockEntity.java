package com.lumina.ae2_gvt.quantum;

import appeng.api.networking.IGridConnection;
import appeng.blockentity.networking.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AE2's controller with a role and a frequency. Controllers on one frequency link up (see {@link QuantumLinks}): one master, any
 * number of slaves. A slave's network joins the master's, and the slave's controllers give channels to the whole of it without
 * counting as a second controller structure.
 */
public class QuantumControllerBlockEntity extends ControllerBlockEntity {
    public static final int OFF = 0, MASTER = 1, SLAVE = 2;
    /** a frequency is a name the player types */
    public static final int MAX_FREQUENCY_LENGTH = 32;

    /** read by the pathing mixin and written when the screen asks: volatile for the gap between */
    private volatile int role = OFF;
    private String frequency = "";
    /** what the link is doing (QuantumLinks.NONE ...), and for a master how many slaves are on it */
    int linkState;
    int slaves;
    IGridConnection link;

    public QuantumControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getRole() {
        return role;
    }

    public String getFrequency() {
        return frequency;
    }

    public int getLinkState() {
        return linkState;
    }

    public int getSlaves() {
        return slaves;
    }

    public boolean isSlave() {
        return role == SLAVE;
    }

    public void setRole(int role) {
        this.role = Math.floorMod(role, 3);
        changed();
    }

    public void setFrequency(String frequency) {
        String f = frequency.strip();
        this.frequency = f.length() > MAX_FREQUENCY_LENGTH ? f.substring(0, MAX_FREQUENCY_LENGTH) : f;
        changed();
    }

    private void changed() {
        saveChanges();
        QuantumLinks.dirty();
    }

    @Override
    public void onReady() {
        super.onReady();
        if (level != null && !level.isClientSide())
            QuantumLinks.add(this);
    }

    @Override
    public void onChunkUnloaded() {
        QuantumLinks.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        QuantumLinks.remove(this);
        super.setRemoved();
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        data.putInt("QuantumRole", role);
        data.putString("QuantumFrequency", frequency);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        role = data.contains("QuantumRole") ? Math.floorMod(data.getInt("QuantumRole"), 3) : OFF;
        if (data.contains("QuantumFrequency", net.minecraft.nbt.Tag.TAG_STRING))
            frequency = data.getString("QuantumFrequency");
        else if (data.contains("QuantumFrequency"))      // saved as a number by an earlier version
            frequency = Integer.toString(data.getInt("QuantumFrequency"));
        else
            frequency = "";
    }
}
