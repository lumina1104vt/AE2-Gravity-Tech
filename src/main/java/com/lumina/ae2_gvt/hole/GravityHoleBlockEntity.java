package com.lumina.ae2_gvt.hole;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import appeng.core.definitions.AEItems;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A hole's nutrients: every dropped item that touches the hole is swallowed, one nutrient per item;
 * each crystal stage grown eats one ({@link GravityHoleBlock#randomTick}). Nothing is pulled in from
 * a distance.
 * <p>
 * A black hole does not eat an AE2 singularity: it turns it into as many anti-singularities, left on
 * top of the hole (and never eats those).
 */
public class GravityHoleBlockEntity extends BlockEntity {
    /** The holes loaded on the client, for {@code client.HoleRenderer} (one set per client, weakly held). */
    public static final Set<GravityHoleBlockEntity> CLIENT_HOLES = Collections.newSetFromMap(new WeakHashMap<>());
    /** an item touching the drawn hole, with a little room for one resting on it */
    private static final double TOUCH = GravityHole.RADIUS + 0.05;

    private long nutrients;

    public GravityHoleBlockEntity(BlockPos pos, BlockState state) {
        super(GravityHole.BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isWhite() {
        return getBlockState().getBlock() instanceof GravityHoleBlock b && b.isWhite();
    }

    public long nutrients() {
        return nutrients;
    }

    void eat() {
        if (nutrients > 0) {
            nutrients--;
            setChanged();
        }
    }

    void serverTick() {
        BlockPos p = getBlockPos();
        AABB hole = new AABB(p.getX() + 0.5 - TOUCH, p.getY() + 0.5 - TOUCH, p.getZ() + 0.5 - TOUCH,
                p.getX() + 0.5 + TOUCH, p.getY() + 0.5 + TOUCH, p.getZ() + 0.5 + TOUCH);
        boolean ate = false;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, hole)) {
            if (!item.isAlive())
                continue;
            ItemStack stack = item.getItem();
            if (!isWhite()) {
                // its own make: left lying on the hole, not eaten
                if (stack.is(Items.ANTI_SINGULARITY.get()))
                    continue;
                // a singularity is turned inside out and put back on top of the hole
                if (stack.is(AEItems.SINGULARITY.asItem())) {
                    item.discard();
                    ItemEntity anti = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5 + TOUCH + 0.05, p.getZ() + 0.5,
                            new ItemStack(Items.ANTI_SINGULARITY.get(), stack.getCount()), 0, 0.1, 0);
                    level.addFreshEntity(anti);
                    continue;
                }
            }
            nutrients += stack.getCount();
            item.discard();
            ate = true;
        }
        if (ate)
            setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide())
            CLIENT_HOLES.add(this);
    }

    @Override
    public void setRemoved() {
        CLIENT_HOLES.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        CLIENT_HOLES.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("nutrients", nutrients);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        nutrients = tag.getLong("nutrients");
    }
}
