package com.lumina.ae2_gvt.prototype.quantum;

import appeng.block.networking.ControllerBlock;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 量子控制器. For now a plain AE2 controller with our own look (its block entity is AE2's
 * {@link appeng.blockentity.networking.ControllerBlockEntity}, so it gives channels, powers up,
 * conflicts and forms structures exactly like AE2's). Planned: a frequency, linking every
 * controller on it like the quantum link chamber - not built yet.
 * <p>
 * AE2's controller picks its look (single / column / inside) by looking for AE2 controllers next to
 * it only; this does the same counting ours and AE2's alike, so mixed structures join up.
 */
public class QuantumControllerBlock extends ControllerBlock {
    public QuantumControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withType(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return withType(super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random), level, pos);
    }

    /** AE2's rule: a line through it on one axis = column, on two or more = inside (a / b checkered). */
    private static BlockState withType(BlockState state, LevelReader level, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        boolean xx = isController(level, x - 1, y, z) && isController(level, x + 1, y, z);
        boolean yy = isController(level, x, y - 1, z) && isController(level, x, y + 1, z);
        boolean zz = isController(level, x, y, z - 1) && isController(level, x, y, z + 1);
        ControllerRenderType type = ControllerRenderType.block;
        if (xx && !yy && !zz) {
            type = ControllerRenderType.column_x;
        } else if (!xx && yy && !zz) {
            type = ControllerRenderType.column_y;
        } else if (!xx && !yy && zz) {
            type = ControllerRenderType.column_z;
        } else if ((xx ? 1 : 0) + (yy ? 1 : 0) + (zz ? 1 : 0) >= 2) {
            type = (Math.abs(x) + Math.abs(y) + Math.abs(z)) % 2 == 0 ? ControllerRenderType.inside_a : ControllerRenderType.inside_b;
        }
        return state.setValue(CONTROLLER_TYPE, type);
    }

    private static boolean isController(LevelReader level, int x, int y, int z) {
        BlockState s = level.getBlockState(new BlockPos(x, y, z));
        return s.getBlock() instanceof QuantumControllerBlock || s.is(AEBlocks.CONTROLLER.block());
    }
}
