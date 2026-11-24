package com.lumina.ae2_gvt.blockentities;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.externalstorage.GenericStackFluidStorage;
import appeng.helpers.externalstorage.GenericStackItemStorage;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.blocks.FusionController;
import com.lumina.ae2_gvt.hub.HexagramHub;
import com.lumina.ae2_gvt.hub.HubPortBlockEntity;
import com.lumina.ae2_gvt.recipes.FusionRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;
import com.lumina.ae2_gvt.structure.FusionStructure;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.core.Instance;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import com.lumina.twinstar.algorithm.ae2.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The fusion controller: an ME device, reached only through structure ports in the chamber's walls (the
 * same port as the twin-star hub's: it links their nodes to its own while formed); a cable can't connect
 * to the controller itself. It takes its power from the network and what it makes goes straight into the network.
 * Its input slots hold anything AE2 can, as an ME interface's storage does: items, fluids, gravitons…
 * A pattern provider, pipe or fluid pipe against a port or the controller fills them, or a hand. And as
 * on an ME interface, each has a marked slot: mark something and how much, and the slot is kept at
 * exactly that from the network, so a reaction goes on for as long as the network has the materials.
 * <p>
 * Energy burnt heats the plasma, which loses heat in proportion to its temperature, so it settles where
 * the two balance. The player sets the cap on what is burnt per tick; reactions ({@link FusionRecipe})
 * have a minimum temperature and only run at or above it, and while one runs the chamber gives several
 * times the energy burnt back to the network.
 * <p>
 * The chamber ({@link FusionStructure}) is a structure of the multiblock lib; once a second the
 * controller asks whether it stands. {@link #chamber} is its low corner while formed (synced, so the
 * client draws the plasma at its centre).
 */
public class FusionControllerBlockEntity extends AENetworkBlockEntity {
    /** A cold, unpowered chamber. */
    public static final double AMBIENT = 1500;
    /** Equilibrium Kelvin per FE/t of input. */
    public static final double KELVIN_PER_FE = 0.0005;
    /** Long max FE/t: no cap in practice. A chamber settles at 1,500 K + 0.0005 K per FE/t - 1,000,000 K at about 2e9 FE/t, far past the hottest colour (80,000 K at ~157M FE/t). */
    public static final long MAX_CAP = Long.MAX_VALUE;
    /** FE held from the energy ports: a second of the cap, and never less than this. */
    private static final long MIN_FE_BUFFER = 1_000_000;
    private static final long FE_BUFFER_TICKS = 20;
    /** The energy ports of formed chambers. */
    private static final Map<GlobalPos, FusionControllerBlockEntity> ENERGY_PORTS = new ConcurrentHashMap<>();
    /** The temperature shown as the aim while no reaction is loaded: that of the coolest. */
    public static final int LOWEST_NEED = 15_000;
    /** What one slot may hold: this many of anything (items, buckets …), as much as anyone would mark. */
    private static final long SLOT_UNITS = 1L << 40;
    /** Fraction of the gap to the equilibrium closed per tick (heating / cooling rate). */
    private static final double RATE = 0.01;

    /** Products wait this long in the outputs for the hub, whose job needs to see them, before going to the network. */
    private static final int HUB_TAKES_TICKS = 40;
    /** What an output slot holds: items. */
    private static final long OUTPUT_SLOT_UNITS = 16_384;
    /** How often items move between the controller and the network, in ticks. */
    private static final int TRANSFER_INTERVAL = 1;
    /** How often the chamber is checked, in ticks. */
    private static final int CHECK_INTERVAL = 20;
    /** Chambers whose controller was just removed, so its frames can be reset (see unformOnRemoval). */
    private static final Map<GlobalPos, BlockPos> LAST_CHAMBER = new ConcurrentHashMap<>();

    private double temperature = AMBIENT;
    /**
     * Just loaded, the network is not up yet and gives no energy for a few seconds; the plasma does not cool in that time, or a
     * chamber at millions of kelvin would be near ambient again when the world is back (not saved).
     */
    private int loadGrace = 200;
    private long energyCap = 0;
    private @Nullable BlockPos chamber;

    private final ConfigInventory inputs = ConfigInventory.storage(12, this::onInputsChanged);
    private final ConfigInventory outputs = ConfigInventory.storage(20, this::saveChanges);
    /** What each input slot is kept stocked with from the network. */
    private final ConfigInventory marks = ConfigInventory.configStacks((appeng.api.storage.AEKeyFilter) null, inputs.size(), this::saveChanges, true);
    private final IItemHandler pipes = new PipeHandler(new GenericStackItemStorage(inputs), new GenericStackItemStorage(outputs));
    private final IFluidHandler fluidPipes = new GenericStackFluidStorage(inputs);
    /** A link to each hub port of the formed chamber. */
    private final Map<BlockPos, IGridConnection> links = new HashMap<>();
    private final IActionSource source = IActionSource.ofMachine(this);
    private long burning, giving;
    /** FE come in through the energy ports, burnt before the network's power. */
    private long storedFe;
    /** What a reaction gives back, kept for the energy ports to be drawn from; what does not fit goes to the network. */
    private long outFe;
    private Set<BlockPos> energyPorts = Set.of();
    private final IEnergyStorage feInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int taken = (int) Math.min(Math.max(amount, 0), Math.max(0, feCapacity() - storedFe));
            if (!simulate && taken > 0) {
                storedFe += taken;
                setChanged();
            }
            return taken;
        }

        /** Cables and pipes draw the energy the reactions gave back (not what was put in). */
        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int taken = (int) Math.min(Math.max(amount, 0), outFe);
            if (!simulate && taken > 0) {
                outFe -= taken;
                setChanged();
            }
            return taken;
        }

        /** What can be drawn: the energy given back. */
        @Override
        public int getEnergyStored() {
            return (int) Math.min(outFe, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(outCapacity(), Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private @Nullable RecipeHolder<FusionRecipe> reaction;
    private int progress;
    private boolean recheck = true;
    /** Products of the last moments, which wait for the hub: the rest goes on, so a long run never backs up. */
    private final Deque<Made> justMade = new ArrayDeque<>();
    /** Test only ({@code /gvt fusion free}): all the energy the cap lets it burn, for nothing; not saved. */
    private boolean free;

    public FusionControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.FUSION_CONTROLLER.get(), pos, state);
        getMainNode().setExposedOnSides(EnumSet.noneOf(Direction.class));
        getMainNode().setVisualRepresentation(Blocks.FUSION_CONTROLLER.get());
        for (AEKeyType type : AEKeyTypes.getAll()) {
            inputs.setCapacity(type, type.getAmountPerUnit() * SLOT_UNITS);
            outputs.setCapacity(type, type.getAmountPerUnit() * OUTPUT_SLOT_UNITS);
            marks.setCapacity(type, type.getAmountPerUnit() * SLOT_UNITS);
        }
    }

    private void onInputsChanged() {
        recheck = true;
        saveChanges();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionControllerBlockEntity be) {
        boolean check = (level.getGameTime() + pos.asLong()) % CHECK_INTERVAL == 0;
        if (!level.isClientSide() && check) {
            be.checkChamber((ServerLevel) level, pos, state);
            state = be.getBlockState();
        }
        boolean formed = state.getValue(FusionController.FORMED);
        if (!level.isClientSide()) {
            be.burn(formed);
            if (formed)
                be.react(level);
            be.giveBack();
            if (level.getGameTime() % TRANSFER_INTERVAL == 0) {
                be.deliver();
                be.stock();
            }
            if (check) {
                be.saveChanges();
                be.markForUpdate();     // what is really burnt: clients follow it
            }
        }
        double goal = formed ? AMBIENT + be.burning * KELVIN_PER_FE : AMBIENT;
        if (be.loadGrace > 0) {
            be.loadGrace--;
            if (goal < be.temperature)
                return;
        }
        be.temperature += (goal - be.temperature) * RATE;
    }

    /** What the energy ports can hold of what a reaction gave back: a second of it (at least a million), never above the config. */
    private long outCapacity() {
        long second = giving > Long.MAX_VALUE / FE_BUFFER_TICKS ? Long.MAX_VALUE : giving * FE_BUFFER_TICKS;
        return Math.min(Config.FUSION_OUTPUT_BUFFER.get(), Math.max(MIN_FE_BUFFER, second));
    }

    private long feCapacity() {
        long second = energyCap > MAX_CAP / FE_BUFFER_TICKS ? MAX_CAP : energyCap * FE_BUFFER_TICKS;
        return Math.max(MIN_FE_BUFFER, second);
    }

    /** The controller of the chamber the energy port at pos is built into, or null. */
    public static FusionControllerBlockEntity controllerOf(Level level, BlockPos pos) {
        return ENERGY_PORTS.get(GlobalPos.of(level.dimension(), pos));
    }

    /** What FE cables on the energy port at pos reach; null when it is not part of a formed chamber. */
    public static IEnergyStorage energyInput(Level level, BlockPos pos) {
        FusionControllerBlockEntity controller = ENERGY_PORTS.get(GlobalPos.of(level.dimension(), pos));
        return controller == null ? null : controller.feInput;
    }

    /** Burns FE from the energy ports, then what the network can give, up to the cap. */
    private void burn(boolean formed) {
        burning = 0;
        if (!formed) {
            giving = 0;
        } else if (free) {
            burning = energyCap;
        } else {
            burning = Math.min(energyCap, storedFe);
            storedFe -= burning;
            if (burning < energyCap && getMainNode().isActive() && getMainNode().getGrid() != null) {
                double wanted = PowerUnits.FE.convertTo(PowerUnits.AE, energyCap - burning);
                double got = getMainNode().getGrid().getEnergyService().extractAEPower(wanted, Actionable.MODULATE, PowerMultiplier.CONFIG);
                burning = Math.min(energyCap, burning + (long) PowerUnits.AE.convertTo(PowerUnits.FE, got));
            }
        }
    }

    private void react(Level level) {
        if (recheck) {
            recheck = false;
            FusionRecipe.Input input = new FusionRecipe.Input(inputs);
            if (reaction == null || !reaction.value().matches(input, level)) {
                reaction = RecipeHolder.allOf(level.getRecipeManager().getAllRecipesFor(Recipes.FUSION.get())).stream()
                        .filter(r -> r.value().matches(input, level)).findFirst().orElse(null);
                progress = 0;
            }
        }
        giving = 0;
        if (reaction == null)
            return;
        FusionRecipe r = reaction.value();
        if (temperature < r.temperature())
            return;
        giving = (long) (burning * r.power());
        if (progress < r.time() - 1) {
            progress++;
            return;
        }
        if (!resultsFit(r))
            return;
        long[] taken = r.take(new FusionRecipe.Input(inputs));
        progress = 0;
        recheck = true;
        if (taken == null)
            return;
        for (int i = 0; i < taken.length; i++)
            if (taken[i] > 0)
                inputs.extract(i, inputs.getKey(i), taken[i], Actionable.MODULATE);
        for (ItemStack out : r.results()) {
            add(outputs, out);
            justMade.addLast(new Made(level.getGameTime(), AEItemKey.of(out), out.getCount()));
        }
    }

    /** The energy a reaction gives goes to the network; what it can't hold is lost. */
    private void giveBack() {
        if (giving <= 0)
            return;
        long left = giving;
        // the network's power first, as far as it has room: a network that is full, or one that swallows without end (a creative
        // energy cell: its maximum is what it holds) takes nothing and the energy goes to the energy ports
        if (getMainNode().isActive() && getMainNode().getGrid() != null) {
            var energy = getMainNode().getGrid().getEnergyService();
            double room = energy.getMaxStoredPower() - energy.getStoredPower();
            if (room > 0) {
                double offered = Math.min(PowerUnits.FE.convertTo(PowerUnits.AE, left), room);
                double refused = energy.injectPower(offered, Actionable.MODULATE);
                left -= Math.min(left, (long) PowerUnits.AE.convertTo(PowerUnits.FE, offered - refused));
            }
        }
        // then the energy ports' buffer, for the cables that draw it (what it cannot hold is lost)
        if (left > 0)
            outFe += Math.min(left, Math.max(0, outCapacity() - outFe));
    }

    private record Made(long at, AEKey key, long amount) {
    }

    /** What was made goes into the network, but for what the hub may still come for; what doesn't fit waits in the outputs. */
    private void deliver() {
        long now = level.getGameTime();
        while (!justMade.isEmpty() && now - justMade.peekFirst().at() >= HUB_TAKES_TICKS)
            justMade.removeFirst();
        if (!getMainNode().isActive() || getMainNode().getGrid() == null)
            return;
        Map<AEKey, Long> keep = new HashMap<>();
        for (Made made : justMade)
            keep.merge(made.key(), made.amount(), Long::sum);
        var storage = getMainNode().getGrid().getStorageService().getInventory();
        for (int i = 0; i < outputs.size(); i++) {
            AEKey key = outputs.getKey(i);
            if (key == null)
                continue;
            long kept = Math.min(keep.getOrDefault(key, 0L), outputs.getAmount(i));
            keep.merge(key, -kept, Long::sum);
            long give = outputs.getAmount(i) - kept;
            if (give > 0) {
                long inserted = storage.insert(key, give, Actionable.MODULATE, source);
                if (inserted > 0)
                    outputs.extract(i, key, inserted, Actionable.MODULATE);
            }
        }
    }

    /** The marked slots: what isn't marked there, or is too much, goes back; what is short is fetched; as an ME interface does. */
    private void stock() {
        if (!getMainNode().isActive() || getMainNode().getGrid() == null)
            return;
        var storage = getMainNode().getGrid().getStorageService().getInventory();
        for (int i = 0; i < marks.size(); i++) {
            GenericStack marked = marks.getStack(i);
            if (marked == null)
                continue;
            AEKey mark = marked.what();
            AEKey held = inputs.getKey(i);
            if (held != null && !held.equals(mark)) {
                long back = storage.insert(held, inputs.getAmount(i), Actionable.MODULATE, source);
                inputs.extract(i, held, back, Actionable.MODULATE);
                if (inputs.getKey(i) != null)
                    continue;
            }
            long missing = marked.amount() - inputs.getAmount(i);
            if (missing < 0) {
                long back = storage.insert(mark, -missing, Actionable.MODULATE, source);
                inputs.extract(i, mark, back, Actionable.MODULATE);
            } else if (missing > 0) {
                long got = storage.extract(mark, missing, Actionable.MODULATE, source);
                if (got > 0)
                    inputs.insert(i, mark, got, Actionable.MODULATE);
            }
        }
    }

    private boolean resultsFit(FusionRecipe r) {
        ConfigInventory test = ConfigInventory.storage(outputs.size(), () -> {});
        for (AEKeyType type : AEKeyTypes.getAll())
            test.setCapacity(type, outputs.getCapacity(type));
        for (int i = 0; i < outputs.size(); i++)
            test.setStack(i, outputs.getStack(i));
        for (ItemStack out : r.results())
            if (!add(test, out))
                return false;
        return true;
    }

    /** A result into the slots; false when it doesn't all fit. */
    private boolean add(ConfigInventory slots, ItemStack result) {
        return slots.insert(AEItemKey.of(result), result.getCount(), Actionable.MODULATE, source) == result.getCount();
    }

    private void checkChamber(ServerLevel level, BlockPos pos, BlockState state) {
        Instance<BlockState> structure = Multiblocks.at(level, pos);
        boolean standing = structure != null && structure.isFormed();
        linkPorts(level, standing ? structure.members(HexagramHub.PORT_KIND) : Set.of());
        linkEnergyPorts(level, standing ? structure.members(FusionStructure.ENERGY_PORT_KIND) : Set.of());
        BlockPos found = null;
        if (standing) {
            int[] bounds = structure.placement().bounds();
            found = new BlockPos(bounds[0], bounds[1], bounds[2]);
        }
        if (Objects.equals(found, chamber) && state.getValue(FusionController.FORMED) == (found != null)) {
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
        level.setBlock(pos, state.setValue(FusionController.FORMED, found != null), Block.UPDATE_ALL);
        saveChanges();
        markForUpdate();
    }

    private void linkEnergyPorts(Level level, Set<Long> members) {
        Set<BlockPos> ports = new HashSet<>();
        for (long p : members)
            ports.add(BlockPos.of(p));
        if (ports.equals(energyPorts))
            return;
        for (BlockPos p : energyPorts) {
            if (!ports.contains(p)) {
                ENERGY_PORTS.remove(GlobalPos.of(level.dimension(), p));
            }
        }
        for (BlockPos p : ports) {
            if (!energyPorts.contains(p)) {
                ENERGY_PORTS.put(GlobalPos.of(level.dimension(), p), this);
            }
        }
        energyPorts = ports;
    }

    /** A link to each port of the formed chamber, none otherwise; as the hub does it. */
    private void linkPorts(Level level, Set<Long> members) {
        Set<BlockPos> ports = new HashSet<>();
        for (long p : members)
            ports.add(BlockPos.of(p));
        for (Iterator<Map.Entry<BlockPos, IGridConnection>> it = links.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, IGridConnection> e = it.next();
            IGridConnection link = e.getValue();
            boolean alive = link.a().getConnections().contains(link);
            if (!ports.contains(e.getKey()) || !alive) {
                if (alive)
                    link.destroy();
                if (level.getBlockEntity(e.getKey()) instanceof HubPortBlockEntity port)
                    port.setMachine(null, false);
                it.remove();
            }
        }
        IGridNode own = getMainNode().getNode();
        if (own == null)
            return;
        for (BlockPos p : ports) {
            if (links.containsKey(p) || !level.isLoaded(p) || !(level.getBlockEntity(p) instanceof HubPortBlockEntity port))
                continue;
            IGridNode other = port.getMainNode().getNode();
            if (other == null)
                continue;
            port.setMachine(this, false);
            try {
                links.put(p, GridHelper.createConnection(own, other));
            } catch (RuntimeException e) {
                // already joined (a cable between the same nodes): nothing to add
                AE2GravityTech.LOGGER.debug("fusion controller at {}: no link to port {}: {}", worldPosition, p, e.getMessage());
            }
        }
        // one port speaks for the chamber to the hub: one chamber, one machine
        BlockPos speaker = links.keySet().stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(null);
        for (BlockPos p : links.keySet())
            if (level.getBlockEntity(p) instanceof HubPortBlockEntity port)
                port.setMachine(this, p.equals(speaker));
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            for (BlockPos p : energyPorts)
                ENERGY_PORTS.remove(GlobalPos.of(level.dimension(), p));
            energyPorts = Set.of();
        }
        for (Map.Entry<BlockPos, IGridConnection> e : links.entrySet()) {
            if (e.getValue().a().getConnections().contains(e.getValue()))
                e.getValue().destroy();
            if (level != null && level.getBlockEntity(e.getKey()) instanceof HubPortBlockEntity port)
                port.setMachine(null, false);
        }
        links.clear();
        super.setRemoved();
    }

    /** Called when the controller block is removed: the frames of its chamber go back to unformed. */
    public static void unformOnRemoval(Level level, BlockPos pos) {
        BlockPos chamber = LAST_CHAMBER.remove(GlobalPos.of(level.dimension(), pos));
        if (chamber != null) {
            FusionStructure.setFormed(level, chamber, false);
        }
    }

    public void dropContents() {
        if (level == null)
            return;
        for (int i = 0; i < outputs.size(); i++)
            if (outputs.getKey(i) instanceof AEItemKey item)
                drop(item.toStack((int) Math.min(outputs.getAmount(i), Integer.MAX_VALUE)));
        // what isn't an item (fluids, gravitons) has nothing to fall as, and is lost, as from an ME interface
        for (int i = 0; i < inputs.size(); i++)
            if (inputs.getKey(i) instanceof AEItemKey item)
                drop(item.toStack((int) Math.min(inputs.getAmount(i), Integer.MAX_VALUE)));
    }

    private void drop(ItemStack stack) {
        while (!stack.isEmpty())
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    stack.split(stack.getMaxStackSize()));
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
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

    /** The controller nearest to {@code pos} within {@code reach} blocks, or null. */
    public static @Nullable FusionControllerBlockEntity nearest(Level level, BlockPos pos, int reach) {
        FusionControllerBlockEntity best = null;
        int chunks = (reach >> 4) + 1;
        for (int x = -chunks; x <= chunks; x++)
            for (int z = -chunks; z <= chunks; z++)
                for (BlockEntity be : level.getChunk((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z).getBlockEntities().values())
                    if (be instanceof FusionControllerBlockEntity c && c.worldPosition.closerThan(pos, reach)
                            && (best == null || c.worldPosition.distSqr(pos) < best.worldPosition.distSqr(pos)))
                        best = c;
        return best;
    }

    /** Test only: turns free energy on or off; true when on. */
    public boolean toggleFree() {
        free = !free;
        return free;
    }

    /** Test only: how it is doing, in a line. */
    public Component describe() {
        Component doing = reaction == null ? Component.translatable("message.ae2_gvt.fusion.no_reaction")
                : temperature < reaction.value().temperature()
                ? Component.translatable("message.ae2_gvt.fusion.reaction_cold", reaction.id().toString(), progress, reaction.value().time(), reaction.value().temperature())
                : Component.translatable("message.ae2_gvt.fusion.reaction", reaction.id().toString(), progress, reaction.value().time());
        return Component.translatable("message.ae2_gvt.fusion.info",
                Component.translatable(formed() ? "message.ae2_gvt.fusion.formed" : "message.ae2_gvt.fusion.not_formed"),
                Component.translatable(getMainNode().isActive() ? "message.ae2_gvt.fusion.online" : "message.ae2_gvt.fusion.offline"),
                (long) temperature, energyCap, burning, giving, doing);
    }

    public ConfigInventory getMarks() {
        return marks;
    }

    /** How much of what is marked in {@code slot} it is kept at. */
    public void setMarkAmount(int slot, long amount) {
        GenericStack mark = slot >= 0 && slot < marks.size() ? marks.getStack(slot) : null;
        if (mark != null && amount > 0)
            marks.setStack(slot, new GenericStack(mark.what(), amount));
    }

    public ConfigInventory getInputs() {
        return inputs;
    }

    public ConfigInventory getOutputs() {
        return outputs;
    }

    /** What item pipes see, and the hub through a port: the input slots, then the outputs (take out only). */
    public IItemHandler getPipeHandler() {
        return pipes;
    }

    /** What fluid pipes see: the input slots. */
    public IFluidHandler getFluidHandler() {
        return fluidPipes;
    }

    public int progress() {
        return progress;
    }

    /** How long the loaded reaction takes (1 with none). */
    public int reactionTime() {
        return reaction == null ? 1 : Math.max(1, reaction.value().time());
    }

    /** The temperature the loaded reaction needs. */
    public int need() {
        return reaction != null ? reaction.value().temperature() : LOWEST_NEED;
    }

    public long energyCap() {
        return energyCap;
    }

    public boolean formed() {
        return getBlockState().getValue(FusionController.FORMED);
    }

    /** Energy really burnt this tick. */
    /** FE waiting in the input buffer (from the energy ports). */
    public long storedFe() {
        return storedFe;
    }

    /** FE a reaction gave back, waiting for cables to draw it. */
    public long outFe() {
        return outFe;
    }

    public long energyIn() {
        return burning;
    }

    /** Energy given back this tick, while a reaction runs. */
    public long energyOut() {
        return giving;
    }

    public void setEnergyCap(long cap) {
        energyCap = Math.max(0, Math.min(MAX_CAP, cap));
        saveChanges();
        markForUpdate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("temperature", temperature);
        tag.putLong("energyCap", energyCap);
        tag.putLong("storedFe", storedFe);
        tag.putLong("outFe", outFe);
        if (chamber != null) {
            tag.putLong("chamber", chamber.asLong());
        }
        inputs.writeToChildTag(tag, "inputs");
        marks.writeToChildTag(tag, "marks");
        outputs.writeToChildTag(tag, "output_stacks");
        tag.putInt("progress", progress);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        temperature = tag.contains("temperature") ? tag.getDouble("temperature") : AMBIENT;
        energyCap = tag.getLong("energyCap");
        storedFe = tag.getLong("storedFe");
        outFe = tag.getLong("outFe");
        chamber = tag.contains("chamber") ? BlockPos.of(tag.getLong("chamber")) : null;
        inputs.readFromChildTag(tag, "inputs");
        marks.readFromChildTag(tag, "marks");
        outputs.readFromChildTag(tag, "output_stacks");
        // controllers saved with plain item slots
        if (tag.contains("outputs")) {
            AppEngInternalInventory old = new AppEngInternalInventory(outputs.size());
            old.readFromNBT(tag, "outputs");
            for (ItemStack stack : old)
                if (!stack.isEmpty())
                    add(outputs, stack);
        }
        progress = tag.getInt("progress");
        recheck = true;
    }

    /** The inputs as they are, then the outputs, refusing anything put in, which is how the hub tells outputs. */
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

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeDouble(temperature);
        data.writeLong(energyCap);
        data.writeLong(burning);
        data.writeLong(chamber == null ? Long.MIN_VALUE : chamber.asLong());
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        temperature = data.readDouble();
        energyCap = data.readLong();
        burning = data.readLong();
        long low = data.readLong();
        chamber = low == Long.MIN_VALUE ? null : BlockPos.of(low);
        return changed;
    }


    // ── capabilities (1.20.1: the block entity answers for them) ──

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,
                                                                            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
            return net.minecraftforge.common.util.LazyOptional.of(this::getPipeHandler).cast();
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)
            return net.minecraftforge.common.util.LazyOptional.of(this::getFluidHandler).cast();
        if (cap == appeng.capabilities.Capabilities.STORAGE || cap == appeng.capabilities.Capabilities.GENERIC_INTERNAL_INV)
            return net.minecraftforge.common.util.LazyOptional.of(this::getInputs).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return com.lumina.ae2_gvt.client.renderer.FusionPlasmaRenderer.renderBox(this);
    }
}
