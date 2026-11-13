package com.lumina.ae2_gvt.hole;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.lumina.ae2_gvt.recipes.HoleConversionRecipe;
import com.lumina.ae2_gvt.recipes.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A hole's nutrients: every dropped item that touches the hole is swallowed, one nutrient per item;
 * each crystal stage grown eats one ({@link GravityHoleBlock#randomTick}). Nothing is pulled in from
 * a distance.
 * <p>
 * What a hole turns an item into, and so does not eat, is up to the {@code ae2_gvt:hole_conversion} recipes (a
 * black hole turns an AE2 singularity into an anti-singularity).
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
        List<HoleConversionRecipe> conversions = null;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, hole)) {
            if (!item.isAlive())
                continue;
            ItemStack stack = item.getItem();
            // what the hole makes is left lying on it, not eaten; what it converts is turned and put back on top of it
            if (conversions == null)
                conversions = level.getRecipeManager().getAllRecipesFor(Recipes.HOLE_CONVERSION.get()).stream()
                        .map(RecipeHolder::value).filter(r -> r.hole().covers(isWhite())).toList();
            if (conversions.stream().anyMatch(r -> ItemStack.isSameItemSameComponents(r.result(), stack)))
                continue;
            HoleConversionRecipe conversion = conversions.stream().filter(r -> r.ingredient().test(stack)).findFirst().orElse(null);
            if (conversion != null) {
                item.discard();
                ItemStack made = conversion.result().copyWithCount(conversion.result().getCount() * stack.getCount());
                ItemEntity out = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5 + TOUCH + 0.05, p.getZ() + 0.5, made, 0, 0.1, 0);
                level.addFreshEntity(out);
                continue;
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
