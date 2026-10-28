package com.lumina.ae2_gvt.prototype.hub;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hexagram hub controller's block entity. Holds nothing yet: it exists so the client can draw the
 * formed hub's sphere. The units it will hold (one black-hole slot, one white-hole slot) come later.
 */
public class TwinStarHubBlockEntity extends BlockEntity {
    public TwinStarHubBlockEntity(BlockPos pos, BlockState state) {
        super(HubStructureBlocks.CONTROLLER_ENTITY.get(), pos, state);
    }
}
