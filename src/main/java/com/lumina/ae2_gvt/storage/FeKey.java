package com.lumina.ae2_gvt.storage;

import java.util.List;

import com.mojang.serialization.MapCodec;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** FE as something the ME network holds, when Applied Flux is not there to provide it ({@link FeEnergy}). */
public final class FeKey extends AEKey {
    public static final FeKey INSTANCE = new FeKey();
    public static final MapCodec<FeKey> MAP_CODEC = MapCodec.unit(INSTANCE);

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fe");

    private FeKey() {
    }

    @Override
    public AEKeyType getType() {
        return FeKeyType.INSTANCE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public CompoundTag toTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    @Override
    public Object getPrimaryKey() {
        return ID;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf buf) {
    }

    @Override
    protected Component computeDisplayName() {
        return Component.translatable("ae2_gvt.key_type.fe");
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // energy: nothing to drop
    }

    @Override
    public <T> T get(DataComponentType<T> type) {
        return null;
    }

    @Override
    public boolean hasComponents() {
        return false;
    }

    @Override
    public String toString() {
        return ID.toString();
    }
}
