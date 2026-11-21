package com.lumina.ae2_gvt.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnits;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.blocks.GeneratorBlock;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import com.lumina.ae2_gvt.hole.GravityHoleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * 發電機: puts FE into the ME network it is cabled to (what the network can't take is lost). Three kinds:
 * <ul>
 * <li>結晶粉發電機 burns gravity, anti-gravity (20 s) or paradox (60 s) crystal dust, from its own slot (by hand or a hopper)
 *     or from the network if that has power;</li>
 * <li>引力子發電機 takes a graviton or anti-graviton from the network each second;</li>
 * <li>吸積發電機 burns nothing: it must touch a white hole, and a white hole feeds four of them at most.</li>
 * </ul>
 */
public class PowerGeneratorBlockEntity extends AENetworkBlockEntity implements InternalInventoryHost {
    public enum Kind {
        CRYSTAL(() -> Config.CRYSTAL_OUTPUT.get()),
        GRAVITON(() -> Config.GRAVITON_OUTPUT.get()),
        ACCRETION(() -> Config.ACCRETION_OUTPUT.get());

        /** FE per tick while running. */
        private final java.util.function.Supplier<Long> output;

        Kind(java.util.function.Supplier<Long> output) {
            this.output = output;
        }

        public long output() {
            return output.get();
        }
    }

    /** What the graviton generator takes from the network. */
    public enum Take {
        ANY, GRAVITON, ANTIGRAVITON
    }

    /** How many accretion generators one white hole can feed. */

    private final AppEngInternalInventory fuel = new AppEngInternalInventory(this, 1, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return dustTicks(stack.getItem()) > 0;
        }
    });
    /** FE per tick it made last tick, for the GUI */
    private long lastOutput;
    /** white holes it touches (accretion), for the GUI */
    private int holes;
    private Take take = Take.ANY;
    /** FE per tick of the dust that burns now */
    private long burnPower = Kind.CRYSTAL.output();
    /** ticks the current fuel still burns */
    private int burn;

    public PowerGeneratorBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode().setVisualRepresentation(state.getBlock());
        getMainNode().setIdlePowerUsage(0);      // a generator draws nothing from the network
    }

    public Kind kind() {
        return ((GeneratorBlock) getBlockState().getBlock()).kind();
    }

    public AppEngInternalInventory getFuel() {
        return fuel;
    }

    public boolean acceptsFuel(ItemStack stack) {
        return dustTicks(stack.getItem()) > 0;
    }

    public Take take() {
        return take;
    }

    public void setTake(Take take) {
        this.take = take;
        setChanged();
    }

    public int burnTicks() {
        return burn;
    }

    public long lastOutput() {
        return lastOutput;
    }

    public int holes() {
        return holes;
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    /** FE per tick of a dust while it burns: gravity and anti-gravity the same, paradox twice. */
    private static long dustPower(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(AE2GravityTech.MODID) && id.getPath().equals("paradox_dust")
                ? Math.round(Config.PARADOX_DUST_MULTIPLIER.get() * Kind.CRYSTAL.output()) : Kind.CRYSTAL.output();
    }

    private static int dustTicks(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals(AE2GravityTech.MODID))
            return 0;
        return switch (id.getPath()) {
            case "gravity_dust", "antigravity_dust" -> Config.DUST_BURN_TICKS.get();
            case "paradox_dust" -> Config.DUST_BURN_TICKS.get();
            default -> 0;
        };
    }

    public static void serverTick(PowerGeneratorBlockEntity be) {
        be.lastOutput = 0;
        var grid = be.getMainNode().getGrid();
        if (grid == null)
            return;
        double factor = 1;
        switch (be.kind()) {
            case CRYSTAL -> {
                if (be.burn <= 0)
                    be.takeDust();
                if (be.burn <= 0)
                    return;
                be.burn--;
            }
            case GRAVITON -> {
                if (be.burn <= 0)
                    be.takeGraviton();
                if (be.burn <= 0)
                    return;
                be.burn--;
            }
            case ACCRETION -> {
                factor = be.holeShare();
                if (factor <= 0)
                    return;
            }
        }
        double fe = (be.kind() == Kind.CRYSTAL ? be.burnPower : be.kind().output()) * factor;
        be.lastOutput = Math.round(fe);
        grid.getEnergyService().injectPower(PowerUnits.FE.convertTo(PowerUnits.AE, fe), Actionable.MODULATE);
    }

    private void takeDust() {
        ItemStack stack = fuel.getStackInSlot(0);
        if (!stack.isEmpty()) {
            burn = dustTicks(stack.getItem());
            burnPower = dustPower(stack.getItem());
            fuel.extractItem(0, 1, false);
            return;
        }
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive())
            return;
        var storage = grid.getStorageService().getInventory();
        for (String name : new String[]{"gravity_dust", "antigravity_dust", "paradox_dust"}) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(AE2GravityTech.MODID, name));
            if (storage.extract(AEItemKey.of(item), 1, Actionable.MODULATE, IActionSource.ofMachine(this)) > 0) {
                burn = dustTicks(item);
                burnPower = dustPower(item);
                return;
            }
        }
    }

    private void takeGraviton() {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive())
            return;
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);
        boolean got = take != Take.ANTIGRAVITON && storage.extract(GravitonKey.GRAVITON, 1, Actionable.MODULATE, source) > 0;
        if (!got)
            got = take != Take.GRAVITON && storage.extract(GravitonKey.ANTIGRAVITON, 1, Actionable.MODULATE, source) > 0;
        if (got)
            burn = Config.GRAVITON_TICKS.get();
    }

    /** 0 with no white hole touching; 1, or less when its hole is shared by more than four generators. */
    private double holeShare() {
        Level level = getLevel();
        double best = 0;
        holes = 0;
        for (Direction d : Direction.values()) {
            BlockPos hole = worldPosition.relative(d);
            if (!(level.getBlockState(hole).getBlock() instanceof GravityHoleBlock h) || !h.isWhite())
                continue;
            holes++;
            int n = 0;
            for (Direction e : Direction.values()) {
                if (level.getBlockState(hole.relative(e)).is(Blocks.ACCRETION_GENERATOR.get()))
                    n++;
            }
            best = Math.max(best, Math.min(1.0, (double) Config.ACCRETION_PER_HOLE.get() / n));
        }
        return best;
    }

    @Override
    public void saveAdditional(CompoundTag output) {
        super.saveAdditional(output);
        fuel.writeToNBT(output, "fuel");
        output.putInt("burn", burn);
        output.putLong("burn_power", burnPower);
        output.putInt("take", take.ordinal());
    }

    @Override
    public void loadTag(CompoundTag input) {
        super.loadTag(input);
        fuel.readFromNBT(input, "fuel");
        burn = input.getInt("burn");
        burnPower = input.contains("burn_power") ? input.getLong("burn_power") : Kind.CRYSTAL.output();
        take = Take.values()[Math.max(0, Math.min(Take.values().length - 1, input.getInt("take")))];
    }

    // ── capabilities (1.20.1: the block entity answers for them) ──

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
                                                                            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && getType() == BlockEntities.CRYSTAL_GENERATOR.get())
            return net.minecraftforge.common.util.LazyOptional.of(() -> getFuel().toItemHandler()).cast();
        return super.getCapability(cap, side);
    }
}
