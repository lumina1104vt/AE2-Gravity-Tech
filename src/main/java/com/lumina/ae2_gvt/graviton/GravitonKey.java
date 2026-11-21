package com.lumina.ae2_gvt.graviton;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.lumina.ae2_gvt.AE2GravityTech;

/**
 * 引力子 / 反引力子 as something the ME network stores, beside items and fluids. There are just
 * the two keys; neither can exist as an item in the world, so nothing drops when a cell holding
 * them is lost.
 */
public final class GravitonKey extends AEKey {
    public enum Kind implements StringRepresentable {
        GRAVITON, ANTIGRAVITON;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final GravitonKey GRAVITON = new GravitonKey(Kind.GRAVITON);
    public static final GravitonKey ANTIGRAVITON = new GravitonKey(Kind.ANTIGRAVITON);

    private final Kind kind;
    private final ResourceLocation id;

    private GravitonKey(Kind kind) {
        this.kind = kind;
        this.id = new ResourceLocation(AE2GravityTech.MODID, kind.getSerializedName());
    }

    public static GravitonKey of(Kind kind) {
        return kind == Kind.GRAVITON ? GRAVITON : ANTIGRAVITON;
    }

    public Kind kind() {
        return kind;
    }

    /** The (display-only) item that draws this key in terminals. */
    public ItemStack displayStack() {
        return new ItemStack(kind == Kind.GRAVITON ? Gravitons.GRAVITON.get() : Gravitons.ANTIGRAVITON.get());
    }

    @Override
    public AEKeyType getType() {
        return GravitonKeyType.INSTANCE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    /** The same shape {@link GravitonKeyType#loadKeyFromTag} reads back. */
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", kind.getSerializedName());
        return tag;
    }

    @Override
    public Object getPrimaryKey() {
        return kind;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
    }

    public static GravitonKey fromPacket(FriendlyByteBuf buf) {
        return of(buf.readEnum(Kind.class));
    }

    @Override
    protected Component computeDisplayName() {
        return Component.translatable("item." + AE2GravityTech.MODID + "." + kind.getSerializedName());
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // not matter in the usual sense: nothing to drop
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
