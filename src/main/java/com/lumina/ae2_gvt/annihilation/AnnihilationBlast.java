package com.lumina.ae2_gvt.annihilation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import appeng.core.definitions.AEBlocks;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.payload.HoleBurstPayload;
import com.lumina.ae2_gvt.hole.GravityHoleBlock;
import com.lumina.ae2_gvt.rift.SpaceRift;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What an 湮滅 TNT does instead of exploding: everything in a sphere of {@link #RADIUS} blocks is
 * annihilated, bedrock included, from the centre outward over a few seconds (a sphere that size is
 * over a million blocks, too many for one tick). Nothing drops. A hole caught in it goes as if blown up,
 * but leaves no chaos matter. The crater's wall turns to sky stone in patches, and a permanent tear in
 * space is left at the centre ({@link SpaceRift}).
 * <p>
 * Whatever lives inside is struck once as the blast reaches it (a totem saves a player, armour does not; a mob that survives the hit
 * is removed all the same); a
 * shockwave hurts out to twice the radius. A blast in progress is not saved: a restart leaves the crater
 * unfinished.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID)
public final class AnnihilationBlast {
    public static final int RADIUS = 64;
    /** How deep the patches of sky stone reach into the crater wall. */
    private static final int WALL = 4;
    /** Blocks looked at per tick, for all blasts together. */
    private static final int BUDGET = 60_000;
    private static final float KILL = 1_000_000, SHOCKWAVE = 40;
    public static final ResourceKey<DamageType> DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "annihilation"));

    private static final List<Blast> BLASTS = new ArrayList<>();
    /** Offsets per whole radius 0 … RADIUS + WALL, packed; built on the first blast. */
    private static IntArrayList[] shells;

    private AnnihilationBlast() {
    }

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Start event) {
        if (event.getLevel() instanceof ServerLevel level && event.getExplosion().getDirectSourceEntity() instanceof PrimedTnt tnt
                && tnt.getBlockState().is(Blocks.ANNIHILATION_TNT.get())) {
            event.setCanceled(true);
            start(level, tnt.blockPosition());
        }
    }

    private static void start(ServerLevel level, BlockPos centre) {
        BLASTS.add(new Blast(level, centre));
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 32, 0.5f);
        PacketDistributor.sendToPlayersNear(level, null, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5,
                RADIUS * 6, new HoleBurstPayload(centre, RADIUS * 2.5f));
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (BLASTS.isEmpty())
            return;
        int budget = BUDGET / BLASTS.size();
        for (Iterator<Blast> it = BLASTS.iterator(); it.hasNext(); )
            if (it.next().step(budget))
                it.remove();
    }

    private static int pack(int x, int y, int z) {
        return (x + 128) << 16 | (y + 128) << 8 | (z + 128);
    }

    private static IntArrayList[] shells() {
        if (shells == null) {
            int reach = RADIUS + WALL;
            IntArrayList[] built = new IntArrayList[reach + 1];
            for (int i = 0; i <= reach; i++)
                built[i] = new IntArrayList();
            for (int x = -reach; x <= reach; x++)
                for (int y = -reach; y <= reach; y++)
                    for (int z = -reach; z <= reach; z++) {
                        int r = Mth.ceil(Math.sqrt(x * x + y * y + z * z));
                        if (r <= reach)
                            built[r].add(pack(x, y, z));
                    }
            shells = built;
        }
        return shells;
    }

    private static final class Blast {
        final ServerLevel level;
        final BlockPos centre;
        final Vec3 at;
        final Set<UUID> hit = new HashSet<>();
        final SimplexNoise patches;
        int radius, index;

        Blast(ServerLevel level, BlockPos centre) {
            this.level = level;
            this.centre = centre;
            this.at = Vec3.atCenterOf(centre);
            this.patches = new SimplexNoise(RandomSource.create(centre.asLong()));
        }

        /** Runs on; true when done. */
        boolean step(int budget) {
            IntArrayList[] all = shells();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            while (budget > 0 && radius < all.length) {
                IntArrayList shell = all[radius];
                for (; index < shell.size() && budget > 0; index++, budget--) {
                    int o = shell.getInt(index);
                    p.set(centre.getX() + (o >> 16 & 255) - 128, centre.getY() + (o >> 8 & 255) - 128, centre.getZ() + (o & 255) - 128);
                    if (!level.isOutsideBuildHeight(p))
                        block(p, radius - RADIUS);
                }
                if (index >= shell.size()) {
                    radius++;
                    index = 0;
                }
            }
            entities(Math.min(radius, RADIUS));
            if (radius < all.length)
                return false;
            SpaceRift.open(level, centre);
            return true;
        }

        /** {@code wall}: how deep into the wall (blocks), 0 or less inside the sphere. */
        private void block(BlockPos.MutableBlockPos p, int wall) {
            BlockState state = level.getBlockState(p);
            if (wall > 0) {
                // patches, thinning out with depth
                if (state.isSolid() && state.getFluidState().isEmpty() && patches.getValue(p.getX() / 6.0, p.getY() / 6.0, p.getZ() / 6.0) > wall * 0.25 - 0.2)
                    level.setBlock(p, AEBlocks.SKY_STONE_BLOCK.block().defaultBlockState(), Block.UPDATE_CLIENTS);
                return;
            }
            if (state.isAir())
                return;
            // no drops: not even what a block lets fall when it goes
            if (state.hasBlockEntity() && level.getBlockEntity(p) instanceof Clearable contents)
                contents.clearContent();
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (state.getBlock() instanceof GravityHoleBlock hole)
                hole.wasExploded(level, p.immutable(), null);
        }

        private void entities(int reached) {
            double far = RADIUS * 2.0;
            DamageSource annihilation = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
            for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(far), Entity::isAlive)) {
                double d = e.position().distanceTo(at);
                if (e instanceof LivingEntity living) {
                    if (d <= reached && hit.add(e.getUUID())) {
                        living.hurt(annihilation, KILL);
                        // something that shrugs every hit off (a mob whose hurt does nothing) is annihilated all the same; a player
                        // is left to the totem and creative mode
                        if (living.isAlive() && !(living instanceof Player))
                            living.kill();
                    }
                    else if (d > RADIUS && d <= far && radius > RADIUS && hit.add(e.getUUID()))
                        living.hurt(level.damageSources().explosion(null), SHOCKWAVE * (float) (1 - (d - RADIUS) / RADIUS));
                } else if (d <= reached && !(e instanceof Player)) {
                    e.discard();
                }
            }
        }
    }
}
