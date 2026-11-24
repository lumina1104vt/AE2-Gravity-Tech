package com.lumina.ae2_gvt.hub;

import com.lumina.bridge.Face;
import com.lumina.bridge.Pos;
import com.lumina.bridge.Positions;
import com.lumina.twinstar.algorithm.execution.Ae2Adapters;
import com.lumina.twinstar.port.World;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.twinstar.api.IMachineAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Hub port: an ME cable connects here, on the hub's outer circle or in a wall of the fusion chamber. Its
 * grid node is joined to the controller's by a link of its own while the structure is formed (the
 * controller makes and drops the link), so the whole network reaches it without a cable run to the
 * middle. Takes no channel. On a fusion chamber it also stands for the controller ({@link #setMachine}),
 * so a pattern provider or pipe against it fills the controller's inputs, and one port of the chamber
 * is its machine access interface: the hub finds the machine without anything else to set up.
 */
public class HubPortBlockEntity extends AENetworkedBlockEntity implements IMachineAccess {
    private @Nullable FusionControllerBlockEntity machine;
    /** The chamber's one port that speaks for it to the hub: several would make it several machines. */
    private boolean access;

    public HubPortBlockEntity(BlockPos pos, BlockState state) {
        super(HubStructureBlocks.PORT_ENTITY.get(), pos, state);
        getMainNode().setVisualRepresentation(HubStructureBlocks.PORT.get());
    }

    /** The fusion controller reached through this port, or null. */
    public @Nullable FusionControllerBlockEntity getController() {
        return machine;
    }

    public void setMachine(@Nullable FusionControllerBlockEntity machine, boolean access) {
        this.access = access;
        if (this.machine != machine) {
            this.machine = machine;
            if (level != null)
                level.invalidateCapabilities(worldPosition);
        }
    }

    // ── IMachineAccess ──

    @Override
    public @Nullable Item getMachine() {
        return machine == null ? null : machine.getBlockState().getBlock().asItem();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public InsertMode getInsertMode() {
        return InsertMode.ANY;
    }

    @Override
    public @Nullable ServerLevel getMachineLevel() {
        return level instanceof ServerLevel server ? server : null;
    }

    @Override
    public @Nullable World getMachineWorld() {
        return Ae2Adapters.world(getMachineLevel());
    }

    @Override
    public Pos getMachinePos() {
        return Positions.of(machine == null ? worldPosition : machine.getBlockPos());
    }

    @Override
    public Face getMachineFace() {
        return Face.NORTH;
    }

    @Override
    public boolean isAccessActive() {
        return access && machine != null && !machine.isRemoved() && getMainNode().isActive();
    }
}
