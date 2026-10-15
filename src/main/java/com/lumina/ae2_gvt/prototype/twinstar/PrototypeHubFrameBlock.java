package com.lumina.ae2_gvt.prototype.twinstar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * PROTOTYPE - invisible stand-in for the hub's placeholder blocks while it is formed. On formation
 * the controller swaps them for this block (which keeps a full hitbox) and swaps them back when the
 * hub breaks apart. {@link #KIND} and {@link #FACING} remember exactly what was replaced, so
 * directional blocks (end rods, access points) come back facing the same way. The real hub will
 * use its own frame / unit blocks with a "formed" state instead of swapping.
 */
public class PrototypeHubFrameBlock extends Block {
    /** The placeholder blocks the formed hub hides. */
    public enum Kind implements StringRepresentable {
        SKY_STONE_BRICK("ae2", "sky_stone_brick"),
        MYSTERIOUS_CUBE("ae2", "mysterious_cube"),
        TINTED_GLASS("minecraft", "tinted_glass"),
        END_ROD("minecraft", "end_rod"),
        ACCESS_POINT("ae2", "wireless_access_point"),
        /** Only hidden on the sphere frame, never on the base (see TwinStarBlueprint). */
        PARADOX_BLOCK("ae2_gvt", "paradox_block"),
        /** Like paradox blocks: only hidden on the sphere (see TwinStarBlueprint). */
        STRUCTURE_FRAME("ae2_gvt", "structure_frame"),
        /** Anything else in the sphere; restored from the blueprint, drops nothing if broken. */
        OTHER("minecraft", "air"),
        // hub units in the sphere's centre: any mix is allowed there, so the stand-in remembers which
        BLANK_UNIT("ae2_gvt", "blank_unit"),
        BLACK_HOLE_CORE("ae2_gvt", "black_hole_core"),
        WHITE_HOLE_CORE("ae2_gvt", "white_hole_core"),
        SINGULAR_CRAFTING_UNIT("ae2_gvt", "singular_crafting_unit"),
        SINGULAR_COMPUTING_UNIT("ae2_gvt", "singular_computing_unit"),
        IMAGINARY_CRAFTING_UNIT("ae2_gvt", "imaginary_crafting_unit"),
        IMAGINARY_COMPUTING_UNIT("ae2_gvt", "imaginary_computing_unit"),
        PARADOX_CRAFTING_UNIT("ae2_gvt", "paradox_crafting_unit"),
        PARADOX_COMPUTING_UNIT("ae2_gvt", "paradox_computing_unit"),
        PLANCK_CRAFTING_UNIT("ae2_gvt", "planck_crafting_unit"),
        PLANCK_COMPUTING_UNIT("ae2_gvt", "planck_computing_unit");

        final Identifier id;

        Kind(String namespace, String path) {
            this.id = Identifier.fromNamespaceAndPath(namespace, path);
        }

        Block block() {
            return BuiltInRegistries.BLOCK.getValue(id);
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static Kind of(Block block) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            for (Kind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public PrototypeHubFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(KIND, Kind.SKY_STONE_BRICK).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // the drawn effect takes the placeholders' place
    }

    /** Whether the hub hides this block type. */
    static boolean hides(Block block) {
        return Kind.of(block) != null;
    }

    /** The AE2 / vanilla block this stand-in replaced. */
    static Block source(BlockState standIn) {
        return standIn.getValue(KIND).block();
    }

    /** Stand-in for {@code original}, or null if the hub doesn't hide that block. */
    static BlockState standInFor(Block frame, BlockState original) {
        Kind kind = Kind.of(original.getBlock());
        if (kind == null) {
            kind = Kind.OTHER;
        }
        Direction facing = facingOf(original);
        return frame.defaultBlockState().setValue(KIND, kind).setValue(FACING, facing != null ? facing : Direction.NORTH);
    }

    /**
     * {@code original} back, facing the way the stand-in recorded. The caller passes the block the
     * blueprint expects rather than trusting {@link #KIND}: stand-ins saved by an older prototype
     * build had a different property and load with the default kind.
     */
    static BlockState restoreAs(BlockState standIn, Block original) {
        BlockState state = original.defaultBlockState();
        Direction facing = standIn.getValue(FACING);
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals("facing") && p.getValueClass() == Direction.class) {
                state = withFacing(state, p, facing);
            }
        }
        return state;
    }

    // AE2 and vanilla each have their own "facing" property instance, so match it by name
    private static Direction facingOf(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals("facing") && p.getValueClass() == Direction.class) {
                return (Direction) state.getValue(p);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState withFacing(BlockState state, Property<?> p, Direction facing) {
        Property<T> prop = (Property<T>) p;
        return prop.getPossibleValues().contains(facing) ? state.setValue(prop, (T) facing) : state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // breaking a hidden block gives back the block it stands in for
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(KIND) != Kind.OTHER) {
            popResource(level, pos, new ItemStack(source(state)));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
