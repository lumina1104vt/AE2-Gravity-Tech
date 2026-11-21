package com.lumina.ae2_gvt.elevator;

import java.util.List;
import java.util.function.Supplier;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.lmb.api.BlockMatchers;
import com.lumina.lmb.api.FormedLooks;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.api.RoleBlocks;
import com.lumina.lmb.core.Definition;
import com.lumina.lmb.core.Instance;
import com.lumina.lmb.core.Match;
import com.lumina.lmb.core.Matcher;
import com.lumina.lmb.core.Pos;
import com.lumina.lmb.core.Role;
import com.lumina.lmb.core.RoleField;
import com.lumina.lmb.core.Shape;
import com.lumina.lmb.core.StructureListener;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import com.lumina.ae2_gvt.reg.DeferredBlock;
import com.lumina.ae2_gvt.reg.DeferredItem;

/**
 * The space elevator (太空電梯) for the lumina-multiblock lib: shape in {@link SpaceElevatorShape}, 320
 * tall (161×319×161 box), about 143,000 blocks. Built from AE2's decorative blocks so a player can make
 * them in bulk; only the controller is ours.
 * <ul>
 *   <li>shell (white): AE2 smooth quartz block</li>
 *   <li>frame (dark): AE2 smooth sky stone block</li>
 *   <li>deck (floors, ramp steps): AE2 sky stone brick, and its slab for the ramp's half steps</li>
 *   <li>conduit (cyan glow): AE2 vibrant quartz glass</li>
 *   <li>light (warm): ochre froglight</li>
 *   <li>cable (between the rings): AE2 sky stone brick wall</li>
 * </ul>
 * The controller sits on the core's face at the top of a ramp and faces out of it; the rest is the same
 * all round, so it may face any of the four sides (the lib turns the structure to match).
 */
public final class SpaceElevator {
    public static final String ID = AE2GravityTech.MODID + ":space_elevator";

    public static final DeferredBlock<SpaceElevatorBlock> BLOCK = AE2GravityTech.BLOCKS.registerBlock("space_elevator",
            SpaceElevatorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.2f, 11f)
                    .sound(SoundType.METAL).lightLevel(s -> s.getValue(SpaceElevatorBlock.FORMED) ? 12 : 0));
    public static final DeferredItem<BlockItem> ITEM = AE2GravityTech.ITEMS.registerSimpleBlockItem("space_elevator", BLOCK);

    /** The main block of each role (index = role id in {@link SpaceElevatorShape}); also what /gvt elevator build places. */
    static final String[] BLOCK_IDS = {null, "ae2:smooth_quartz_block", "ae2:smooth_sky_stone_block", "ae2:sky_stone_brick",
            "ae2:quartz_vibrant_glass", "minecraft:ochre_froglight", "ae2:sky_stone_brick_wall", null, "ae2:sky_stone_brick_slab"};

    private SpaceElevator() {
    }

    /** Called from mod construction: registers the controller block (static fields) and the definition. */
    public static void register() {
        @SuppressWarnings("unchecked")
        Role<BlockState>[] roles = new Role[SpaceElevatorShape.ROLE_NAMES.length];
        for (int i = 1; i < roles.length; i++) {
            Supplier<Block> block = i == SpaceElevatorShape.CTRL ? BLOCK::get : lookup(BLOCK_IDS[i]);
            roles[i] = Role.of(SpaceElevatorShape.ROLE_NAMES[i], matcher(block));
            if (i != SpaceElevatorShape.CTRL) RoleBlocks.prefer(roles[i], block);
        }
        int r = SpaceElevatorShape.R;
        RoleField.Builder<BlockState> b = RoleField.builder(SpaceElevatorShape.W, SpaceElevatorShape.H, SpaceElevatorShape.W);
        for (int y = 0; y < SpaceElevatorShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    int id = SpaceElevatorShape.role(x, y, z);
                    if (id != 0) b.set(x + r, y, z + r, roles[id]);
                }
            }
        }
        b.anchor(SpaceElevatorShape.CTRL_X + r, SpaceElevatorShape.CTRL_Y, SpaceElevatorShape.CTRL_Z + r);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(SpaceElevatorCommands::register);   // /gvt elevator build
        FormedLooks.set(ID, "controller", s -> s.hasProperty(SpaceElevatorBlock.FORMED) ? s.setValue(SpaceElevatorBlock.FORMED, true) : s);
        Multiblocks.register(Definition.builder(ID, Shape.fixed(b.build()))
                // the unturned controller faces south, out of the core's south face
                .facing(new int[]{0, 0, 1}, SpaceElevator::frontOf)
                .listener(new StructureListener<>() {
                    @Override
                    public void formed(Instance<BlockState> s) {
                        log("formed", s);
                    }

                    @Override
                    public void damaged(Instance<BlockState> s) {
                        log("damaged", s);
                    }

                    @Override
                    public void recovered(Instance<BlockState> s) {
                        log("recovered", s);
                    }

                    @Override
                    public void removed(Instance<BlockState> s) {
                        log("removed", s);
                    }
                })
                .build());
    }

    private static void log(String what, Instance<BlockState> s) {
        AE2GravityTech.LOGGER.info("space elevator {} at {}", what, Pos.str(s.anchor()));
    }

    private static int[] frontOf(BlockState state) {
        if (!state.hasProperty(SpaceElevatorBlock.FACING)) return null;
        Direction d = state.getValue(SpaceElevatorBlock.FACING);
        return new int[]{d.getStepX(), d.getStepY(), d.getStepZ()};
    }

    /** A block from another mod, looked up when first needed (registries are not filled at construction). */
    static Supplier<Block> lookup(String id) {
        ResourceLocation key = new ResourceLocation(id);
        Block[] cache = new Block[1];
        return () -> {
            if (cache[0] == null) cache[0] = BuiltInRegistries.BLOCK.get(key);
            return cache[0];
        };
    }

    private static Matcher<BlockState> matcher(Supplier<Block> block) {
        return s -> s.is(block.get()) ? Match.of(BlockMatchers.id(s)) : null;
    }

    /** Creative tab entries. */
    public static List<DeferredItem<BlockItem>> items() {
        return List.of(ITEM);
    }
}
