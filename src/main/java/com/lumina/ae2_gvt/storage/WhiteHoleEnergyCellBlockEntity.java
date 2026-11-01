package com.lumina.ae2_gvt.storage;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lumina.ae2_gvt.blockentities.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 白洞能源元件, the black hole disk's other half: an energy cell that never runs out. The network's power comes
 * from it, and in the terminal it is a stack of FE ({@link FeEnergy}) that does not go down, which the
 * twin-star engine takes for an infinite source and machine access interfaces hand to their machines.
 * It gives; it takes nothing in.
 */
public class WhiteHoleEnergyCellBlockEntity extends AENetworkedBlockEntity implements IStorageProvider, IAEPowerStorage {
    /** What AE2 is told it holds: it wants a number, any large one. */
    private static final double SHOWN_POWER = 1e15;

    private final Storage storage = new Storage();

    public WhiteHoleEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.WHITE_HOLE_ENERGY_CELL.get(), pos, state);
        getMainNode().setIdlePowerUsage(0).addService(IStorageProvider.class, this).addService(IAEPowerStorage.class, this);
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        mounts.mount(storage);
    }

    private final class Storage implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            return 0;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            return what.equals(FeEnergy.key()) ? Math.max(amount, 0) : 0;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            AEKey fe = FeEnergy.key();
            out.set(fe, Long.MAX_VALUE);
        }

        @Override
        public Component getDescription() {
            return getBlockState().getBlock().getName();
        }
    }

    @Override
    public double injectAEPower(double amount, Actionable mode) {
        return amount;      // takes none: all of it comes back
    }

    @Override
    public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        return amount;
    }

    @Override
    public double getAEMaxPower() {
        return SHOWN_POWER;
    }

    @Override
    public double getAECurrentPower() {
        return SHOWN_POWER;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ;
    }
}
