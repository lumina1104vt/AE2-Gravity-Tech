package com.lumina.ae2_gvt.hub;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Twin-Star Hub for the lumina-multiblock lib: since 2026-11-27 the black / white hole computing core
 * (shape and materials in {@link HubShape}, 235 × 121 × 235), which replaced the hexagram of 2026-10-27.
 * The id and the class name stay the hexagram's, so saves and the fusion controller's port kind carry on.
 * <ul>
 *   <li>controller: the tip of the lower anchor; its screen faces out and the structure turns with it;</li>
 *   <li>slabs count only in their own half: a lower slab where an upper one belongs (or a double slab) is
 *       wrong;</li>
 *   <li>ME cables come in through hub ports, which may stand in for any pillar of the lower anchor (at least
 *       one);</li>
 *   <li>the black and white holes and their light are drawn by the client, not built.</li>
 * </ul>
 */
public final class HexagramHub {
    public static final String ID = AE2GravityTech.MODID + ":hexagram_hub";
    /** What the lib calls the hub ports among the members (their block id): the controller links to these. */
    public static final String PORT_KIND = AE2GravityTech.MODID + ":hub_port";

    /** Pillar cells up to this height (the lower anchor) may hold a port instead. */
    private static final int PORT_TOP = HubShape.CY - 32;
    private static final String CONTROLLER = "controller", PORT_ROLE = "rim";
    /** Ends the name of a role that wants a slab in one half. */
    private static final String UPPER = "#top", LOWER = "#bottom";

    /** Role name → the block it is named after in messages. */
    private static final Map<String, Supplier<Block>> ROLE_BLOCKS = new HashMap<>();

    private HexagramHub() {
    }

    /** Called from mod construction: registers the definition, the formed looks and the commands. */
    public static void register() {
        @SuppressWarnings("unchecked")
        Role<BlockState>[] roles = new Role[HubShape.BLOCK_IDS.length];
        Map<String, Role<BlockState>> byName = new HashMap<>();   // one role per block
        for (int i = 1; i < roles.length; i++) {
            Supplier<Block> block = block(i);
            String name = i == HubShape.CTRL ? CONTROLLER : HubShape.BLOCK_IDS[i];
            ROLE_BLOCKS.put(name, block);
            if (isSlab(i)) {
                roles[i] = slabRole(byName, block, name, SlabType.BOTTOM);
                continue;
            }
            roles[i] = byName.computeIfAbsent(name, n -> {
                Role<BlockState> role = Role.of(n, matcher(block));
                if (!n.equals(CONTROLLER)) RoleBlocks.prefer(role, block);
                return role;
            });
        }
        Role<BlockState> portPlace = Role.of(PORT_ROLE, matcher(() -> HubStructureBlocks.PILLAR.get().getBlock(),
                () -> HubStructureBlocks.PORT_BLOCK.get()));
        RoleBlocks.prefer(portPlace, () -> HubStructureBlocks.PILLAR.get().getBlock());
        ROLE_BLOCKS.put(PORT_ROLE, () -> HubStructureBlocks.PILLAR.get().getBlock());

        int r = HubShape.R;
        RoleField.Builder<BlockState> b = RoleField.builder(HubShape.W, HubShape.H, HubShape.W);
        for (int y = 0; y < HubShape.H; y++) {
            for (int z = -r; z <= r; z++) {
                for (int x = -r; x <= r; x++) {
                    int cell = HubShape.cell(x, y, z);
                    if (cell == 0) continue;
                    int i = cell & HubShape.ROLE_MASK;
                    Role<BlockState> role = isPortPlace(cell, y) ? portPlace
                            : isSlab(i) && (cell & HubShape.TOP) != 0 ? slabRole(byName, block(i), HubShape.BLOCK_IDS[i], SlabType.TOP)
                            : roles[i];
                    b.set(x + r, y, z + r, role);
                }
            }
        }
        b.anchor(r, 0, r);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HexagramHubCommands::register);   // /gvt hub build
        formedLooks();
        Multiblocks.register(Definition.builder(ID, Shape.fixed(b.build()))
                // the unturned controller faces south (+z); the lib turns the structure with it
                .facing(new int[]{0, 0, 1}, HexagramHub::frontOf)
                .hatchKinds(PORT_KIND)
                .limit(PORT_KIND, 1, Integer.MAX_VALUE)
                .listener(new StructureListener<>() {
                    @Override
                    public void formed(Instance<BlockState> s) {
                        log("formed", s);
                    }

                    @Override
                    public void damaged(Instance<BlockState> s) {
                        log("damaged", s);
                        logProblems(s);
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

    private static boolean isSlab(int role) {
        return role != HubShape.CTRL && HubShape.BLOCK_IDS[role].endsWith("_slab");
    }

    /** The role "this slab in this half": one per block and half. */
    private static Role<BlockState> slabRole(Map<String, Role<BlockState>> byName, Supplier<Block> block, String id, SlabType half) {
        String name = id + (half == SlabType.TOP ? UPPER : LOWER);
        return byName.computeIfAbsent(name, n -> {
            ROLE_BLOCKS.put(n, block);
            Role<BlockState> role = Role.of(n, s -> s.is(block.get()) && s.getValue(SlabBlock.TYPE) == half
                    ? Match.of(BlockMatchers.id(s)) : null);
            RoleBlocks.place(role, () -> block.get().defaultBlockState().setValue(SlabBlock.TYPE, half));
            return role;
        });
    }

    static boolean isPortPlace(int cell, int y) {
        return y <= PORT_TOP && HubShape.BLOCK_IDS[cell & HubShape.ROLE_MASK] != null
                && HubShape.BLOCK_IDS[cell & HubShape.ROLE_MASK].equals("ae2_gvt:hub_pillar");
    }

    /** The state /gvt hub build places for a cell of {@link HubShape} (not the controller). */
    static BlockState stateOf(int cell) {
        BlockState state = block(cell & HubShape.ROLE_MASK).get().defaultBlockState();
        if ((cell & HubShape.TOP) != 0) state = state.setValue(SlabBlock.TYPE, SlabType.TOP);
        return state;
    }

    /** What a role is called in the controller's messages: the block it wants. */
    static Component roleName(String role) {
        Supplier<Block> block = ROLE_BLOCKS.get(role);
        if (block == null) return Component.literal(role);
        Component name = block.get().getName();
        if (role.endsWith(UPPER)) return Component.translatable("message.ae2_gvt.hexagram_hub.slab_top", name);
        if (role.endsWith(LOWER)) return Component.translatable("message.ae2_gvt.hexagram_hub.slab_bottom", name);
        return name;
    }

    /** Blocks of other mods are looked up when first needed (registries are not filled at construction). */
    static Supplier<Block> block(int role) {
        if (role == HubShape.CTRL) return HubStructureBlocks.CONTROLLER_BLOCK::get;
        ResourceLocation key = ResourceLocation.parse(HubShape.BLOCK_IDS[role]);
        return () -> BuiltInRegistries.BLOCK.get(key);
    }

    /** Matches by block, looked up when tested (the lib's own matchers need the block at registration). */
    @SafeVarargs
    private static Matcher<BlockState> matcher(Supplier<? extends Block>... blocks) {
        return s -> Arrays.stream(blocks).anyMatch(b -> s.is(b.get())) ? Match.of(BlockMatchers.id(s)) : null;
    }

    private static int[] frontOf(BlockState state) {
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return null;
        Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return new int[]{d.getStepX(), d.getStepY(), d.getStepZ()};
    }

    /** Joined crystals and plates, lit veins and edges; ports keep their own look. */
    private static void formedLooks() {
        FormedLooks.set(ID, CONTROLLER, s -> s.hasProperty(TwinStarHubBlock.FORMED) ? s.setValue(TwinStarHubBlock.FORMED, true) : s);
        FormedLooks.set(ID, "ae2_gvt:gravity_block", s -> HubStructureBlocks.GRAVITY_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "ae2_gvt:antigravity_block", s -> HubStructureBlocks.ANTIGRAVITY_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "ae2_gvt:paradox_block", s -> HubStructureBlocks.PARADOX_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "ae2_gvt:machine_frame", s -> HubStructureBlocks.FRAME_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "ae2_gvt:structure_frame", s -> HubStructureBlocks.STRUCTURE_FRAME_JOINED.get().defaultBlockState());
        lit("ae2_gvt:hub_helix_black", HubLookBlock.FORMED);
        lit("ae2_gvt:hub_helix_white", HubLookBlock.FORMED);
        lit("ae2_gvt:hub_ring", HubRingBlock.FORMED);
        lit("ae2_gvt:hub_pillar", HubPillarBlock.FORMED);
        lit(PORT_ROLE, HubPillarBlock.FORMED);   // a port has no such property and stays as it is
    }

    private static void lit(String role, BooleanProperty formed) {
        FormedLooks.set(ID, role, s -> s.hasProperty(formed) ? s.setValue(formed, true) : s);
    }

    private static void log(String what, Instance<BlockState> s) {
        AE2GravityTech.LOGGER.info("twin-star hub {} at {}", what, Pos.str(s.anchor()));
    }

    /** Why it broke: the first few wrong cells and what is in them now (the listener has no level, so find it). */
    private static void logProblems(Instance<BlockState> s) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            var engine = Multiblocks.engine(level);
            if (engine == null || engine.at(s.anchor()) != s) {
                continue;
            }
            for (com.lumina.lmb.core.Problem p : engine.problems(s, 5)) {
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(p.pos());
                AE2GravityTech.LOGGER.info("  {} {} at {} ({}): now {}", p.type(), p.role(), Pos.str(p.pos()), p.detail(),
                        level.isLoaded(pos) ? level.getBlockState(pos) : "unloaded");
            }
        }
    }
}
