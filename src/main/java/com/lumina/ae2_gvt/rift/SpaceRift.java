package com.lumina.ae2_gvt.rift;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;

import appeng.core.definitions.AEBlocks;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredBlock;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * The tear in space an 湮滅 TNT leaves at its centre: permanent, unbreakable and two-way. Walking into
 * it leads to 破碎星空 ({@link #STARFIELD}) straight "above" the same x / z, where a second tear is made
 * the first time; that one leads back. Each side has a small sky stone landing under the tear. An entity
 * that came through must step out of the tear before it can go back.
 */
public final class SpaceRift {
    public static final ResourceKey<Level> STARFIELD =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(AE2GravityTech.MODID, "shattered_starfield"));
    /** Height of the tear in 破碎星空. */
    private static final int STARFIELD_Y = 128;
    /** How far from the middle of the tear's block it catches: half width, half height. */
    private static final double CATCH_WIDE = 1.2, CATCH_TALL = 3.5;

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2GravityTech.MODID);
    private static final DeferredRegister<com.mojang.serialization.Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, AE2GravityTech.MODID);

    public static final DeferredBlock<RiftBlock> BLOCK = AE2GravityTech.BLOCKS.registerBlock("space_rift", RiftBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1, 3_600_000).noLootTable().noOcclusion()
                    .noCollission().pushReaction(PushReaction.BLOCK).lightLevel(s -> 12));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiftEntity>> ENTITY =
            BLOCK_ENTITY_TYPES.register("space_rift", () -> BlockEntityType.Builder.of(RiftEntity::new, BLOCK.get()).build(null));

    static {
        GENERATORS.register("shattered_starfield", () -> StarfieldGenerator.CODEC);
    }

    private SpaceRift() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
        GENERATORS.register(modBus);
    }

    /** Tears space at {@code pos}, leading to 破碎星空 (none inside 破碎星空 itself: it is torn already). */
    public static void open(ServerLevel level, BlockPos pos) {
        if (level.dimension() != STARFIELD)
            place(level, pos, STARFIELD, null);
    }

    private static void place(ServerLevel level, BlockPos pos, ResourceKey<Level> target, @Nullable BlockPos targetPos) {
        level.setBlock(pos, BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof RiftEntity rift)
            rift.link(target, targetPos);
        // the landing: entities arrive standing on it
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++) {
                p.set(pos.getX() + x, pos.getY() - 4, pos.getZ() + z);
                if (!level.isOutsideBuildHeight(p) && level.getBlockState(p).canBeReplaced())
                    level.setBlockAndUpdate(p, AEBlocks.SKY_STONE_BLOCK.block().defaultBlockState());
            }
    }

    public static final class RiftBlock extends Block implements EntityBlock {
        public RiftBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RiftEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() || type != ENTITY.get() ? null : (l, p, s, be) -> ((RiftEntity) be).serverTick();
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }

    public static final class RiftEntity extends BlockEntity {
        /** The tears loaded on the client, for {@code client.RiftRenderer} (weakly held). */
        public static final Set<RiftEntity> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

        private @Nullable ResourceKey<Level> target;
        private @Nullable BlockPos targetPos;

        public RiftEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }

        void link(ResourceKey<Level> target, @Nullable BlockPos targetPos) {
            this.target = target;
            this.targetPos = targetPos;
            setChanged();
        }

        void serverTick() {
            if (target == null || !(level instanceof ServerLevel server))
                return;
            Vec3 at = Vec3.atCenterOf(worldPosition);
            AABB area = new AABB(at, at).inflate(CATCH_WIDE, CATCH_TALL, CATCH_WIDE);
            for (Entity e : level.getEntities((Entity) null, area, Entity::isAlive)) {
                if (e.isOnPortalCooldown()) {
                    e.setPortalCooldown();     // stays on until it has stepped out
                    continue;
                }
                if (!e.isPassenger() && !e.isVehicle() && e.canChangeDimensions())
                    travel(server, e);
            }
        }

        private void travel(ServerLevel here, Entity entity) {
            ServerLevel there = here.getServer().getLevel(target);
            if (there == null)
                return;
            BlockPos to = targetPos != null ? targetPos : new BlockPos(worldPosition.getX(), STARFIELD_Y, worldPosition.getZ());
            there.getChunk(to);
            if (!there.getBlockState(to).is(BLOCK.get()))
                place(there, to, here.dimension(), worldPosition);
            if (targetPos == null)
                link(target, to);
            Vec3 arrive = new Vec3(to.getX() + 0.5, to.getY() - 3, to.getZ() + 0.5);
            Entity arrived = entity.changeDimension(there, new net.minecraftforge.common.util.ITeleporter() {
                @Override
                public Entity placeEntity(Entity e, ServerLevel current, ServerLevel destination, float yaw,
                                          java.util.function.Function<Boolean, Entity> reposition) {
                    Entity moved = reposition.apply(false);
                    moved.moveTo(arrive.x, arrive.y, arrive.z, e.getYRot(), e.getXRot());
                    return moved;
                }

                @Override
                public boolean isVanilla() {
                    return false;
                }
            });
            if (arrived != null)
                arrived.setPortalCooldown();
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level != null && level.isClientSide())
                CLIENT.add(this);
        }

        @Override
        public void setRemoved() {
            CLIENT.remove(this);
            super.setRemoved();
        }

        @Override
        public void onChunkUnloaded() {
            CLIENT.remove(this);
            super.onChunkUnloaded();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (target != null)
                tag.putString("target", target.location().toString());
            if (targetPos != null)
                tag.put("target_pos", NbtUtils.writeBlockPos(targetPos));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            target = tag.contains("target") ? ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("target"))) : null;
            targetPos = tag.contains("target_pos") ? NbtUtils.readBlockPos(tag.getCompound("target_pos")) : null;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
