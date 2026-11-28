package com.lumina.ae2_gvt.blockentities;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.helpers.externalstorage.GenericStackFluidStorage;
import appeng.helpers.externalstorage.GenericStackItemStorage;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.recipes.BorrowedRecipes;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 引力加工工廠: runs {@code ae2_gvt:gravity_processing} recipes (after those, the other mods' recipes named
 * by {@code ae2_gvt:factory_borrow}, see {@link BorrowedRecipes}) on its 12 input slots into its 12
 * output slots, on FE. A slot is an AE2 generic one and holds {@link #SLOT_UNITS} units of anything: that many
 * items, that many buckets of a fluid. A recipe is found when the inputs change; it runs while its results fit and
 * there is energy for the next tick (energy / time each tick), and stops (keeping its progress) when
 * there isn't. A recipe's gravitons / anti-gravitons are taken from the ME network (cabled from any
 * side) when it finishes, with whatever else the recipe takes from the network (lightning of AE2 Lightning
 * Tech, …); it waits on its last tick until the network has them. Pipes and hoppers put items into the inputs and take them from the outputs, from any
 * side; FE comes in from any side.
 * <p>
 * Parallel: each chaos matter in the catalyst slot doubles the copies of the recipe that run at once, as far as
 * the inputs, the outputs and the gravitons go; none is used up. A run of N costs N times the materials,
 * energy and gravitons and takes the same time. The energy buffer grows with it.
 * <p>
 * Upgrades: up to 4 AE2 acceleration cards, each one more run step a tick (5× with all four); a
 * recipe still takes the same energy in all.
 */
public class GravityFactoryBlockEntity extends AENetworkedBlockEntity implements InternalInventoryHost, IUpgradeableObject {
    public static final int MAX_SPEED_CARDS = 4;
    public static final long ENERGY_CAPACITY = 10_000_000, ENERGY_INPUT = 1_000_000;   // FE, FE/t, without chaos matter
    public static final int MAX_CHAOS_MATTER = 16;
    /** What a slot holds: items, or buckets and the like. */
    public static final long SLOT_UNITS = 262_144;

    private final ConfigInventory inputs = ConfigInventory.storage(12).allowOverstacking(true).changeListener(this::onSlotsChanged).build();
    private final ConfigInventory outputs = ConfigInventory.storage(12).allowOverstacking(true).changeListener(this::onSlotsChanged).build();
    private final AppEngInternalInventory catalyst = new AppEngInternalInventory(this, 1, MAX_CHAOS_MATTER, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.is(Items.CHAOS_MATTER.get());
        }
    });
    private final Buffer energy = new Buffer();
    private final IItemHandler pipes = new PipeHandler(new GenericStackItemStorage(inputs), new GenericStackItemStorage(outputs));
    private final IFluidHandler fluidPipes = new FluidPipes(new GenericStackFluidStorage(inputs),
            new GenericStackFluidStorage(outputs));
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(Blocks.GRAVITY_FACTORY.get(), MAX_SPEED_CARDS, this::setChanged);

    /** ticks run of the current recipe */
    private int progress;
    /** copies of the recipe in this run */
    private long runs = 1;
    private @Nullable RecipeHolder<GravityProcessingRecipe> recipe;
    /** look for a recipe again (the inputs or outputs changed) */
    private boolean recheck = true;

    public GravityFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GRAVITY_FACTORY.get(), pos, state);
        getMainNode().setVisualRepresentation(Blocks.GRAVITY_FACTORY.get());
        for (AEKeyType type : AEKeyTypes.getAll()) {
            inputs.setCapacity(type, type.getAmountPerUnit() * SLOT_UNITS);
            outputs.setCapacity(type, type.getAmountPerUnit() * SLOT_UNITS);
        }
    }

    public ConfigInventory getInputs() {
        return inputs;
    }

    public ConfigInventory getOutputs() {
        return outputs;
    }

    private void onSlotsChanged() {
        recheck = true;
        saveChanges();
    }

    public InternalInventory getCatalyst() {
        return catalyst;
    }

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public long energyStored() {
        return energy.stored;
    }

    public long energyCapacity() {
        return energy.capacity();
    }

    public IItemHandler getPipeHandler() {
        return pipes;
    }

    public IFluidHandler getFluidHandler() {
        return fluidPipes;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** Progress of the current recipe, 0 … 1000. */
    public int progressPermille() {
        return recipe == null || recipe.value().time() <= 0 ? 0 : Math.min(1000, progress * 1000 / recipe.value().time());
    }

    // ── processing ──

    public static void serverTick(Level level, BlockPos pos, BlockState state, GravityFactoryBlockEntity factory) {
        factory.tick(level);
    }

    private void tick(Level level) {
        if (recheck) {
            recheck = false;
            RecipeHolder<GravityProcessingRecipe> found = find(level);
            if (found != recipe) {
                recipe = found;
                progress = 0;
            }
        }
        if (recipe == null)
            return;
        drawPower();
        // one step per tick, one more for each acceleration card (5 with four)
        int steps = 1 + upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        for (int i = 0; i < steps && recipe != null; i++)
            if (!step(level, recipe.value()))
                break;
    }

    /** The most copies of a recipe the chaos matter allows at once. */
    public long parallel() {
        return 1L << Math.min(MAX_CHAOS_MATTER, catalyst.getStackInSlot(0).getCount());
    }

    /** Tops the buffer up from the ME network it is cabled to, as much as the buffer takes in a tick. */
    private void drawPower() {
        long room = Math.min(ENERGY_INPUT * parallel(), energy.capacity() - energy.stored);
        if (room <= 0 || !getMainNode().isActive() || getMainNode().getGrid() == null)
            return;
        double got = getMainNode().getGrid().getEnergyService().extractAEPower(PowerUnit.FE.convertTo(PowerUnit.AE, room),
                Actionable.MODULATE, PowerMultiplier.CONFIG);
        energy.add((long) PowerUnit.AE.convertTo(PowerUnit.FE, got));
    }

    /** One step of the recipe (its share of the energy); finishes it on the last. False when it can't go on now. */
    private boolean step(Level level, GravityProcessingRecipe r) {
        int time = Math.max(1, r.time());
        if (progress == 0) {
            runs = runsNow(r);
            if (runs == 0) {
                runs = 1;
                return false;
            }
        }
        // energy for this step: the recipe's share, the rest on the last step
        boolean last = progress == time - 1;
        long total = r.energy() * runs;
        long perStep = total / time + (last ? total % time : 0);
        // gravitons are taken on finishing, but a run only starts when they are there
        if (energy.stored < perStep || last && (!resultsFit(r, runs) || !payNetwork(r, runs, Actionable.SIMULATE)))
            return false;
        energy.use(perStep);
        progress++;
        setChanged();
        if (progress < time)
            return true;
        long[] taken = r.take(input(), runs);
        progress = 0;
        if (taken == null) {
            recheck = true;
            return false;
        }
        for (int i = 0; i < taken.length; i++)
            if (taken[i] > 0)
                inputs.extract(i, inputs.getKey(i), taken[i], Actionable.MODULATE);
        payNetwork(r, runs, Actionable.MODULATE);
        for (GenericStack out : r.outputs())
            add(outputs, out, runs);
        // the same recipe again while the inputs still hold it (the rest of this tick's steps go on)
        if (!r.matches(input(), level))
            recipe = null;
        recheck = true;
        return recipe != null;
    }

    /** The copies of the recipe that can run now: as many as the chaos matter allows and all else can bear. */
    private long runsNow(GravityProcessingRecipe r) {
        if (!canRun(r, 1))
            return 0;
        long can = 1, cannot = parallel() + 1;
        while (cannot - can > 1) {
            long n = can + (cannot - can) / 2;
            if (canRun(r, n))
                can = n;
            else
                cannot = n;
        }
        return can;
    }

    private boolean canRun(GravityProcessingRecipe r, long n) {
        return r.take(input(), n) != null && resultsFit(r, n) && payNetwork(r, n, Actionable.SIMULATE);
    }

    /**
     * Takes (or, simulated, checks for) what that many runs cost the network: gravitons, anti-gravitons and
     * the recipe's other network costs; true if all are there.
     */
    private boolean payNetwork(GravityProcessingRecipe r, long runs, Actionable mode) {
        List<GenericStack> costs = new ArrayList<>(r.networkCosts());
        if (r.gravitons() > 0)
            costs.add(new GenericStack(GravitonKey.GRAVITON, r.gravitons()));
        if (r.antigravitons() > 0)
            costs.add(new GenericStack(GravitonKey.ANTIGRAVITON, r.antigravitons()));
        if (costs.isEmpty())
            return true;
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive())
            return false;
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);
        for (GenericStack cost : costs) {
            long amount = cost.amount() * runs;
            if (storage.extract(cost.what(), amount, mode, source) < amount && mode == Actionable.SIMULATE)
                return false;
        }
        return true;
    }

    private @Nullable RecipeHolder<GravityProcessingRecipe> find(Level level) {
        GravityProcessingRecipe.Input input = input();
        if (inputs.isEmpty())
            return null;
        if (recipe != null && recipe.value().matches(input, level))
            return recipe;
        for (RecipeHolder<GravityProcessingRecipe> holder : BorrowedRecipes.own(level))
            if (holder.value().matches(input, level))
                return holder;
        for (RecipeHolder<GravityProcessingRecipe> holder : BorrowedRecipes.of(level))
            if (holder.value().matches(input, level))
                return holder;
        return null;
    }

    private GravityProcessingRecipe.Input input() {
        return new GravityProcessingRecipe.Input(inputs);
    }

    /** The results of that many runs fit into the outputs together. */
    private boolean resultsFit(GravityProcessingRecipe r, long runs) {
        ConfigInventory test = ConfigInventory.storage(outputs.size()).allowOverstacking(true).build();
        for (AEKeyType type : AEKeyTypes.getAll())
            test.setCapacity(type, outputs.getCapacity(type));
        for (int i = 0; i < outputs.size(); i++)
            test.setStack(i, outputs.getStack(i));
        for (GenericStack out : r.outputs())
            if (!add(test, out, runs))
                return false;
        return true;
    }

    /** That many runs' worth of a result into the slots; false if not all went in. */
    private boolean add(ConfigInventory slots, GenericStack result, long runs) {
        long amount = result.amount() * runs;
        return slots.insert(result.what(), amount, Actionable.MODULATE, IActionSource.ofMachine(this)) == amount;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        recheck = true;
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    @Override
    public void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        inputs.writeToChildTag(output, "input_stacks", registries);
        outputs.writeToChildTag(output, "output_stacks", registries);
        catalyst.writeToNBT(output, "catalyst", registries);
        upgrades.writeToNBT(output, "upgrades", registries);
        output.putLong("energy", energy.stored);
        output.putInt("progress", progress);
        output.putLong("runs", runs);
    }

    @Override
    public void loadTag(CompoundTag input, HolderLookup.Provider registries) {
        super.loadTag(input, registries);
        inputs.readFromChildTag(input, "input_stacks", registries);
        outputs.readFromChildTag(input, "output_stacks", registries);
        // factories saved with plain item slots
        loadOldSlots(input, "inputs", inputs, registries);
        loadOldSlots(input, "outputs", outputs, registries);
        catalyst.readFromNBT(input, "catalyst", registries);
        upgrades.readFromNBT(input, "upgrades", registries);
        energy.stored = input.getLong("energy");
        progress = input.getInt("progress");
        runs = Math.max(1, input.getLong("runs"));
        recheck = true;
    }

    private void loadOldSlots(CompoundTag input, String name, ConfigInventory slots, HolderLookup.Provider registries) {
        if (!input.contains(name))
            return;
        AppEngInternalInventory old = new AppEngInternalInventory(slots.size());
        old.readFromNBT(input, name, registries);
        for (int i = 0; i < old.size(); i++) {
            ItemStack stack = old.getStackInSlot(i);
            if (!stack.isEmpty())
                slots.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, IActionSource.ofMachine(this));
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ConfigInventory slots : new ConfigInventory[] {inputs, outputs}) {
            List<ItemStack> drops = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++)
                if (slots.getKey(i) instanceof AEItemKey item)
                    item.addDrops(slots.getAmount(i), drops, level, pos);
            drops.forEach(stack -> Block.popResource(level, pos, stack));
            slots.clear();
        }
        for (InternalInventory inv : new InternalInventory[] {catalyst, upgrades}) {
            for (ItemStack stack : inv) {
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
            inv.clear();
        }
    }

    /** FE in from cables, none out (the factory spends it itself). */
    private final class Buffer implements IEnergyStorage {
        long stored;

        long capacity() {
            return ENERGY_CAPACITY * parallel();
        }

        void add(long amount) {
            stored += amount;
            setChanged();
        }

        void use(long amount) {
            stored -= Math.min(amount, stored);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int got = (int) Math.min(toReceive, Math.min(ENERGY_INPUT * parallel(), Math.max(0, capacity() - stored)));
            if (got > 0 && !simulate)
                add(got);
            return got;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(stored, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(capacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    /** What fluid pipes see: filling goes into the inputs, draining comes from the outputs. */
    private record FluidPipes(IFluidHandler in, IFluidHandler out) implements IFluidHandler {
        @Override
        public int getTanks() {
            return in.getTanks() + out.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank < in.getTanks() ? in.getFluidInTank(tank) : out.getFluidInTank(tank - in.getTanks());
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank < in.getTanks() ? in.getTankCapacity(tank) : out.getTankCapacity(tank - in.getTanks());
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank < in.getTanks() && in.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return in.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return out.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return out.drain(maxDrain, action);
        }
    }

    /** What pipes see: the 12 inputs (put in only), then the 12 outputs (take out only). */
    private record PipeHandler(IItemHandler in, IItemHandler out) implements IItemHandler {
        @Override
        public int getSlots() {
            return in.getSlots() + out.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < in.getSlots() ? in.getStackInSlot(slot) : out.getStackInSlot(slot - in.getSlots());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < in.getSlots() ? in.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < in.getSlots() ? ItemStack.EMPTY : out.extractItem(slot - in.getSlots(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < in.getSlots() ? in.getSlotLimit(slot) : out.getSlotLimit(slot - in.getSlots());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < in.getSlots() && in.isItemValid(slot, stack);
        }
    }
}
