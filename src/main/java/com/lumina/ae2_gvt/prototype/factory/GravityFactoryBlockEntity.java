package com.lumina.ae2_gvt.prototype.factory;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The factory's storage: 12 inputs, 12 outputs and the catalyst. Item inventories are real; progress
 * is a placeholder (always idle) until recipes and processing exist. The tank / energy fields are
 * left from the old GUI and no longer shown (the GUI is slots only).
 */
public class GravityFactoryBlockEntity extends BlockEntity implements InternalInventoryHost {
    public static final int TANK_CAPACITY = 16_000;     // mB
    public static final long ENERGY_CAPACITY = 100_000; // AE

    private final AppEngInternalInventory inputs = new AppEngInternalInventory(this, 12);
    private final AppEngInternalInventory outputs = new AppEngInternalInventory(this, 12);
    /** The catalyst is chaos matter; nothing else goes in. */
    private final AppEngInternalInventory catalyst = new AppEngInternalInventory(this, 1, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.is(Items.CHAOS_MATTER.get());
        }
    });

    // placeholders until processing exists
    int fluidIn, gasIn, fluidOut, gasOut;
    long energy;
    /** Progress of the current operation, 0-1000. */
    int progress;

    public GravityFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(GravityFactory.BLOCK_ENTITY.get(), pos, state);
    }

    public InternalInventory getInputs() {
        return inputs;
    }

    public InternalInventory getOutputs() {
        return outputs;
    }

    public InternalInventory getCatalyst() {
        return catalyst;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputs.writeToNBT(output, "inputs");
        outputs.writeToNBT(output, "outputs");
        catalyst.writeToNBT(output, "catalyst");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputs.readFromNBT(input, "inputs");
        outputs.readFromNBT(input, "outputs");
        catalyst.readFromNBT(input, "catalyst");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            for (InternalInventory inv : new InternalInventory[] {inputs, outputs, catalyst}) {
                for (ItemStack stack : inv) {
                    if (!stack.isEmpty()) {
                        Block.popResource(level, pos, stack);
                    }
                }
                inv.clear();
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
}
