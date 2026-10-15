package com.lumina.ae2_gvt.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class GravitonCollector extends BlockEntity {
    private static final Set<GravitonCollector> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    public GravitonCollector(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntities.GRAVITON_COLLECTOR.get(), worldPosition, blockState);
    }

    public static Set<GravitonCollector> loadedOnClient() {
        return CLIENT;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CLIENT.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT.remove(this);
    }
}
