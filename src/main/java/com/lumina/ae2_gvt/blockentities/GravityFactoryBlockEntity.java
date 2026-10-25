package com.lumina.ae2_gvt.blockentities;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GravityFactoryBlockEntity extends BlockEntity implements InternalInventoryHost {
    public static final int TANK_CAPACITY = 16_000;     // mB
    public static final long ENERGY_CAPACITY = 100_000; // AE

    private final AppEngInternalInventory inputs = new AppEngInternalInventory(this, 12);
    private final AppEngInternalInventory outputs = new AppEngInternalInventory(this, 12);

    private final AppEngInternalInventory catalyst = new AppEngInternalInventory(this, 1, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.is(Items.CHAOS_MATTER.get());
        }
    });

    long energy;
    public int progress;

    public GravityFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GRAVITY_FACTORY.get(), pos, state);
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
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        inputs.writeToNBT(output, "inputs", registries);
        outputs.writeToNBT(output, "outputs", registries);
        catalyst.writeToNBT(output, "catalyst", registries);
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        inputs.readFromNBT(input, "inputs", registries);
        outputs.readFromNBT(input, "outputs", registries);
        catalyst.readFromNBT(input, "catalyst", registries);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (InternalInventory inv : new InternalInventory[] {inputs, outputs, catalyst}) {
            for (ItemStack stack : inv) {
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
            inv.clear();
        }
    }
}
