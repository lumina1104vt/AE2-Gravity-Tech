package com.lumina.ae2_gvt.hole;

import appeng.decorative.solid.BuddingCertusQuartzBlock;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.CrystalLine;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.lumina.ae2_gvt.blockentities.GravitonCollector;
import com.lumina.ae2_gvt.graviton.GravitonKey;

/**
 * A black or white hole as a block. Nothing of it is drawn as a block: {@code client.HoleRenderer}
 * draws the hole (lensing, disc) in the middle of the cell. Its solid part is the hole itself (what
 * an item lands on and is swallowed by); towards its neighbours it counts as a full block, so crystal
 * buds can stand on all six faces.
 * <p>
 * Growth: as AE2's budding certus (a random tick, then one in five, picks a face and grows the bud
 * there a stage), but only while the hole has nutrients - each stage eats one - and it never decays.
 * Each stage lets {@link #GRAVITONS_PER_STAGE} gravitons go, caught in full by every 引力子收集器 near.
 */
public class GravityHoleBlock extends Block implements EntityBlock {
    /** Gravitons let go by each bud stage grown (anti-gravitons on a white hole). */
    public static final int GRAVITONS_PER_STAGE = 16;
    /** Gravitons let go when the hole is blown up (anti-gravitons on a white hole). */
    public static final int GRAVITONS_ON_EXPLOSION = 128;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final double LO = 0.5 - GravityHole.RADIUS, HI = 0.5 + GravityHole.RADIUS;
    private static final VoxelShape HOLE = Shapes.box(LO, LO, LO, HI, HI, HI);

    private final boolean white;

    public GravityHoleBlock(boolean white, Properties properties) {
        super(properties);
        this.white = white;
    }

    public boolean isWhite() {
        return white;
    }

    /** Black hole: gravity crystal; white hole: anti-gravity crystal. */
    private CrystalLine line() {
        return white ? AE2GravityTech.ANTIGRAVITY : AE2GravityTech.GRAVITY;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityHoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != GravityHole.BLOCK_ENTITY.get() ? null
                : (l, p, s, be) -> ((GravityHoleBlockEntity) be).serverTick();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HOLE;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();     // buds need a sturdy face to stand on
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Blown up: everyone near sees a big ripple run out from it, and its gravitons go as on growing. */
    @Override
    public void wasExploded(Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!(level instanceof ServerLevel server))
            return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server, null, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 160, new com.lumina.ae2_gvt.payload.HoleBurstPayload(pos, 48));
        GravitonCollector.release(level, pos, white ? GravitonKey.ANTIGRAVITON : GravitonKey.GRAVITON, GRAVITONS_ON_EXPLOSION);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0 || !(level.getBlockEntity(pos) instanceof GravityHoleBlockEntity hole) || hole.nutrients() <= 0)
            return;
        Direction direction = Util.getRandom(DIRECTIONS, random);
        BlockPos at = pos.relative(direction);
        BlockState there = level.getBlockState(at);
        CrystalLine line = line();
        Block next = null;
        if (BuddingCertusQuartzBlock.canClusterGrowAtState(there))
            next = line.smallBud().get().getBlock();
        else if (grows(there, line.smallBud().get().getBlock(), direction))
            next = line.mediumBud().get().getBlock();
        else if (grows(there, line.mediumBud().get().getBlock(), direction))
            next = line.largeBud().get().getBlock();
        else if (grows(there, line.largeBud().get().getBlock(), direction))
            next = line.cluster().get().getBlock();
        if (next == null)
            return;
        level.setBlockAndUpdate(at, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, direction)
                .setValue(AmethystClusterBlock.WATERLOGGED, there.getFluidState().getType() == Fluids.WATER));
        hole.eat();
        // the stage grown lets gravitons (white: anti-gravitons) go; every networked collector near catches them all, else they are lost
        GravitonCollector.release(level, pos, white ? GravitonKey.ANTIGRAVITON : GravitonKey.GRAVITON, GRAVITONS_PER_STAGE);
    }

    private static boolean grows(BlockState there, Block stage, Direction direction) {
        return there.is(stage) && there.getValue(AmethystClusterBlock.FACING) == direction;
    }
}
