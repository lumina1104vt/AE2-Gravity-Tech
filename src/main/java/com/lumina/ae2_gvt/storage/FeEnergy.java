package com.lumina.ae2_gvt.storage;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.parts.automation.StackWorldBehaviors;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * FE on the ME network: Applied Flux's key when it is installed, else our own {@link FeKey} (with what it
 * takes to reach a machine's FE, which Applied Flux brings along for its key). Applied Flux is found through
 * AE2's key types, so nothing here needs it to compile or load.
 */
public final class FeEnergy {
    private static final String APPLIED_FLUX = "appflux";
    private static final DeferredRegister<AEKeyType> KEY_TYPES = DeferredRegister.create(AEKeyType.REGISTRY_KEY, AE2GravityTech.MODID);

    static {
        KEY_TYPES.register("fe", () -> FeKeyType.INSTANCE);
    }

    /** Display-only: how our key looks in terminals (not in the creative tab). */
    public static final DeferredItem<Item> ICON = AE2GravityTech.ITEMS.registerSimpleItem("fe_energy");

    private static AEKey key;

    private FeEnergy() {
    }

    public static void init() {
    }

    public static void register(IEventBus modEventBus) {
        KEY_TYPES.register(modEventBus);
    }

    /** During common setup. */
    public static void registerStrategies() {
        if (!ModList.get().isLoaded(APPLIED_FLUX))
            StackWorldBehaviors.registerExternalStorageStrategy(FeKeyType.INSTANCE, MachineFe::new);
    }

    /** The key FE goes by on this game's networks. */
    public static AEKey key() {
        if (key == null) {
            AEKey flux = ModList.get().isLoaded(APPLIED_FLUX) ? appliedFluxKey() : null;
            key = flux != null ? flux : FeKey.INSTANCE;
        }
        return key;
    }

    private static AEKey appliedFluxKey() {
        JsonObject fe = new JsonObject();
        fe.addProperty("type", "FE");
        for (AEKeyType type : AEKeyTypes.getAll()) {
            if (!type.getId().getNamespace().equals(APPLIED_FLUX))
                continue;
            var found = type.codec().decoder().parse(JsonOps.INSTANCE, fe);
            if (found.isSuccess())
                return found.getOrThrow();
            AE2GravityTech.LOGGER.warn("Applied Flux's FE key could not be read: {}", found);
        }
        return null;
    }

    /** Our key's way into a machine's FE. */
    private record MachineFe(ServerLevel level, BlockPos pos, Direction side) implements ExternalStorageStrategy {
        @Override
        public MEStorage createWrapper(boolean extractableOnly, Runnable callback) {
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            return energy == null ? null : new MachineStorage(energy, extractableOnly);
        }
    }

    private record MachineStorage(IEnergyStorage energy, boolean extractableOnly) implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (what != FeKey.INSTANCE || !energy.canReceive())
                return 0;
            return energy.receiveEnergy((int) Math.min(amount, Integer.MAX_VALUE), mode == Actionable.SIMULATE);
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (what != FeKey.INSTANCE || !energy.canExtract())
                return 0;
            return energy.extractEnergy((int) Math.min(amount, Integer.MAX_VALUE), mode == Actionable.SIMULATE);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            if (!extractableOnly || energy.canExtract())
                out.add(FeKey.INSTANCE, energy.getEnergyStored());
        }

        @Override
        public Component getDescription() {
            return Component.translatable("ae2_gvt.key_type.fe");
        }
    }
}
