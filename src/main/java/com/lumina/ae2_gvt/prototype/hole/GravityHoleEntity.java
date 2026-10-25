package com.lumina.ae2_gvt.prototype.hole;

import com.lumina.ae2_gvt.AE2GravityTech;

import com.lumina.ae2_gvt.items.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

/**
 * A black hole or a white hole (one class, two entity types). It never moves, can't be picked up,
 * and swallows dropped items that actually touch it - nothing is pulled in from a distance. What
 * it swallows becomes nutrients for growing crystal clusters (not yet). An explosion destroys it, leaving chaos matter.
 * Natural and artificial holes are the same size. Drawn by client.HoleRenderer (lensing, disc).
 */
public class GravityHoleEntity extends Entity {
    private long nutrients;

    public GravityHoleEntity(EntityType<? extends GravityHoleEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public boolean isWhite() {
        return getType() == GravityHole.WHITE_HOLE.get();
    }

    public long nutrients() {
        return nutrients;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel) {
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox())) {
                nutrients += item.getItem().getCount();
                item.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.is(DamageTypeTags.IS_EXPLOSION)) {
            // both kinds leave the same thing; TODO also release gravitons once their AEKey exists
            discard();
            spawnAtLocation(new ItemStack(Items.CHAOS_MATTER.get()));
            return true;
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        nutrients = tag.getLong("nutrients");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("nutrients", nutrients);
    }
}
