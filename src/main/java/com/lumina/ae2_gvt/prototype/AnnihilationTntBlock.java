package com.lumina.ae2_gvt.prototype;

import com.lumina.ae2_gvt.AE2GravityTech;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Annihilation TNT. For now it explodes like vanilla TNT (the real, nuke-level explosion is still
 * to be designed); this class only makes the lit TNT keep our look - vanilla's priming code always
 * spawns a {@link PrimedTnt} that renders as plain TNT.
 */
public class AnnihilationTntBlock extends TntBlock {
    public AnnihilationTntBlock(Properties properties) {
        super(properties);
    }

    // every way of lighting it (flint and steel, fire, redstone, burning projectiles, breaking it
    // while unstable) goes through here - same as vanilla's prime(), plus our block state
    @Override
    public boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTnt tnt = primed(level, pos, igniter);
            level.addFreshEntity(tnt);
            level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
            return true;
        }
        return false;
    }

    // set off by another explosion: vanilla's shortened random fuse, plus our block state
    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTnt tnt = primed(level, pos, explosion.getIndirectSourceEntity());
            int fuse = tnt.getFuse();
            tnt.setFuse((short) (level.getRandom().nextInt(fuse / 4) + fuse / 8));
            level.addFreshEntity(tnt);
        }
    }

    private PrimedTnt primed(Level level, BlockPos pos, @Nullable LivingEntity source) {
        PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, source);
        tnt.setBlockState(defaultBlockState());
        return tnt;
    }
}
