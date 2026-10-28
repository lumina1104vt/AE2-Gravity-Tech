package com.lumina.ae2_gvt.prototype.hub;

import java.util.Arrays;
import java.util.function.Supplier;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.lmb.api.BlockMatchers;
import com.lumina.lmb.api.FormedLooks;
import com.lumina.lmb.api.Multiblocks;
import com.lumina.lmb.core.Definition;
import com.lumina.lmb.core.Instance;
import com.lumina.lmb.core.Match;
import com.lumina.lmb.core.Matcher;
import com.lumina.lmb.core.Pos;
import com.lumina.lmb.core.Role;
import com.lumina.lmb.core.RoleField;
import com.lumina.lmb.core.Shape;
import com.lumina.lmb.core.StructureListener;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hexagram Twin-Star Hub (design 2026-10-27, tools/hub_structure_block_options.gif and the
 * hub*.png concept sheets), described for the lumina-multiblock lib. About 63×71 wide and 61 tall:
 * <ul>
 *   <li>floor: a hexagram in a circle, one block thick, rising 6 towards the centre where the
 *       controller stands, one block above it. Circle, star outline and centre hexagon: machine frame. Tips of the
 *       "black" triangle (N, SE, SW) and the gap half next to them: gravity crystal block; tips of
 *       the "white" triangle and the gap half next to them: anti-gravity crystal block; the seam
 *       through each gap: paradox crystal block;</li>
 *   <li>ceiling: the same pattern, sagging 6 towards the centre;</li>
 *   <li>a flat layer of the same pattern at the very bottom and the very top, closing the space under
 *       the dome and over the ceiling (that space itself stays "don't care");</li>
 *   <li>six hollow cylinders (5 across) of hub pillars, centred on the block nearest each star tip, floor to ceiling,
 *       capped at the bottom (rim height) and the top (ceiling rim height);</li>
 *   <li>a black and a white helix strand (radius 19.5, 1.5 turns, 3 thick) round the sphere;</li>
 *   <li>three perpendicular 3-wide rings of hub ring blocks (radius 16), floating, 7 clear of floor and ceiling;</li>
 *   <li>the sphere (radius 15) must stay empty: it is drawn, not built.</li>
 * </ul>
 * Everything else in the box (under the floor, inside the cylinders, the gaps) is "don't care".
 * Turning it 90° gives the only other distinct orientation; mirrors are not allowed (the helix has a hand).
 */
public final class HexagramHub {
    public static final String ID = AE2GravityTech.MODID + ":hexagram_hub";

    // ---- geometry, in blocks, relative to the star's centre (x east, z south) ----
    private static final double STAR = 29.93;           // tip radius (29.93, not 30: keeps black and white the same size)
    private static final double RING_IN = 30.0, RING_OUT = 31.6, SEAM = 0.9;
    private static final double BASE = 31.0;            // floor rim radius for the dome
    private static final int DOME = 6;                  // floor rise / ceiling sag at the centre
    private static final int SPHERE = 15, RING = SPHERE + 1, GAP = 7;
    private static final int FLOOR_TOP = DOME;          // floor surface height at the centre
    private static final int CONTROLLER_LEVEL = FLOOR_TOP + 1; // the controller stands on the floor's apex
    private static final int CENTRE = FLOOR_TOP + 1 + GAP + RING;          // 30
    private static final int CEILING_RIM = CENTRE + RING + 1 + GAP + DOME; // 60
    private static final double CYLINDER = 2, HELIX = 19.5, HELIX_TURNS = 1.5, HELIX_HALF = 1.3;
    private static final double[] TIP_ANGLES = {-90, 30, 150, 90, 210, 330};
    private static final int HALF_X = 31, HALF_Z = 35;   // box half-sizes (cylinders on the N/S tips stick out past the circle)

    private HexagramHub() {
    }

    /** Called from mod construction: registers the definition and the controller's formed look. */
    public static void register() {
        Role<BlockState> controller = Role.of("controller", block(HubStructureBlocks.CONTROLLER_BLOCK));
        Role<BlockState> frame = Role.of("frame", block(Blocks.MACHINE_FRAME));
        Role<BlockState> gravity = Role.of("gravity_crystal", block(() -> AE2GravityTech.GRAVITY.block().get().getBlock()));
        Role<BlockState> antigravity = Role.of("antigravity_crystal", block(() -> AE2GravityTech.ANTIGRAVITY.block().get().getBlock()));
        Role<BlockState> paradox = Role.of("paradox_crystal", block(() -> AE2GravityTech.PARADOX.block().get().getBlock()));
        Role<BlockState> pillar = Role.of("pillar", block(() -> HubStructureBlocks.PILLAR.get().getBlock()));
        Role<BlockState> ring = Role.of("ring", block(() -> HubStructureBlocks.RING.get().getBlock()));
        Role<BlockState> helixBlack = Role.of("helix_black", block(() -> HubStructureBlocks.HELIX_BLACK.get().getBlock()));
        Role<BlockState> helixWhite = Role.of("helix_white", block(() -> HubStructureBlocks.HELIX_WHITE.get().getBlock()));
        Role<BlockState> sphere = Role.of("sphere", BlockMatchers.air());

        java.util.Map<Cell, Role<BlockState>> roles = new java.util.EnumMap<>(Cell.class);
        roles.put(Cell.CONTROLLER, controller);
        roles.put(Cell.FRAME, frame);
        roles.put(Cell.GRAVITY, gravity);
        roles.put(Cell.ANTIGRAVITY, antigravity);
        roles.put(Cell.PARADOX, paradox);
        roles.put(Cell.PILLAR, pillar);
        roles.put(Cell.RING, ring);
        roles.put(Cell.HELIX_BLACK, helixBlack);
        roles.put(Cell.HELIX_WHITE, helixWhite);
        roles.put(Cell.SPHERE, sphere);
        RoleField.Builder<BlockState> b = RoleField.builder(2 * HALF_X + 1, CEILING_RIM + 1, 2 * HALF_Z + 1);
        for (int x = -HALF_X; x <= HALF_X; x++) {
            for (int z = -HALF_Z; z <= HALF_Z; z++) {
                for (int y = 0; y <= CEILING_RIM; y++) {
                    Cell cell = cell(x, y, z);
                    b.set(x + HALF_X, y, z + HALF_Z, cell == null || cell == Cell.CLEAR ? null : roles.get(cell));
                }
            }
        }
        b.anchor(HALF_X, CONTROLLER_LEVEL, HALF_Z);
        RoleField<BlockState> field = b.build();

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HexagramHubCommands::register);   // /gvt hub build
        // crystals: drawn joined to their neighbours of the same kind (the world keeps the real blocks)
        FormedLooks.set(ID, "gravity_crystal", s -> HubStructureBlocks.GRAVITY_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "antigravity_crystal", s -> HubStructureBlocks.ANTIGRAVITY_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "paradox_crystal", s -> HubStructureBlocks.PARADOX_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "frame", s -> HubStructureBlocks.FRAME_JOINED.get().defaultBlockState());
        FormedLooks.set(ID, "ring", s -> s.hasProperty(HubRingBlock.FORMED) ? s.setValue(HubRingBlock.FORMED, true) : s);
        FormedLooks.set(ID, "helix_black", s -> s.hasProperty(HubLookBlock.FORMED) ? s.setValue(HubLookBlock.FORMED, true) : s);
        FormedLooks.set(ID, "helix_white", s -> s.hasProperty(HubLookBlock.FORMED) ? s.setValue(HubLookBlock.FORMED, true) : s);
        FormedLooks.set(ID, "pillar", s -> s.hasProperty(HubPillarBlock.FORMED) ? s.setValue(HubPillarBlock.FORMED, true) : s);
        FormedLooks.set(ID, "controller", s -> s.hasProperty(TwinStarHubBlock.FORMED) ? s.setValue(TwinStarHubBlock.FORMED, true) : s);
        Multiblocks.register(Definition.builder(ID, Shape.fixed(field))
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

    private static void log(String what, Instance<BlockState> s) {
        AE2GravityTech.LOGGER.info("hexagram hub {} at {}", what, Pos.str(s.anchor()));
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

    /** Matches by block, looked up when tested (the lib's own matchers need the block at registration). */
    @SafeVarargs
    private static Matcher<BlockState> block(Supplier<? extends Block>... blocks) {
        return s -> Arrays.stream(blocks).anyMatch(b -> s.is(b.get())) ? Match.of(BlockMatchers.id(s)) : null;
    }

    // ---- what goes where ----

    /** What a cell of the box holds. CLEAR: not part of the structure, but empty space a build should clear. */
    public enum Cell { CONTROLLER, FRAME, GRAVITY, ANTIGRAVITY, PARADOX, PILLAR, RING, HELIX_BLACK, HELIX_WHITE, SPHERE, CLEAR }

    /** Box offsets: x in [-HALF_X, HALF_X], z in [-HALF_Z, HALF_Z], y in [0, CEILING_RIM] with 0 = the floor's rim. */
    public static final int MIN_X = -HALF_X, MAX_X = HALF_X, MIN_Z = -HALF_Z, MAX_Z = HALF_Z, MAX_Y = CEILING_RIM;
    /** Height of the controller above the floor's rim. */
    public static final int CONTROLLER_Y = CONTROLLER_LEVEL;
    /** The drawn sphere: its centre's height above the controller, and its radius. */
    public static final int SPHERE_ABOVE_CONTROLLER = CENTRE - CONTROLLER_LEVEL, SPHERE_RADIUS = SPHERE;

    /**
     * The cell at (x, y, z) relative to the star's centre at rim height, in the unturned orientation;
     * null = don't care and left alone (under the floor, above the ceiling).
     */
    public static Cell cell(int x, int y, int z) {
        if (x == 0 && z == 0 && y == CONTROLLER_LEVEL) {
            return Cell.CONTROLLER;
        }
        double r = Math.hypot(x, z);
        Plan plan = plan(x, z);
        int floor = floorY(r), ceiling = CEILING_RIM - floorY(r);
        // the domed surfaces, plus a flat layer at the very bottom and top closing the space under the
        // dome and over the ceiling: each in that column's own block
        boolean surface = plan != null && (y == floor || y == ceiling || y == 0 || y == CEILING_RIM);
        if (!surface && (y == 0 || y == CEILING_RIM) && inCylinder(x, z)) {
            return Cell.PILLAR;   // the cylinders' caps
        }
        if (surface) {
            return switch (plan) {
                case FRAME -> Cell.FRAME;
                case GRAVITY -> Cell.GRAVITY;
                case ANTIGRAVITY -> Cell.ANTIGRAVITY;
                case PARADOX -> Cell.PARADOX;
            };
        }
        if (plan != null && (y < floor || y > ceiling)) {
            return null;
        }
        if (Math.sqrt(x * x + (y - CENTRE) * (y - CENTRE) + z * z) < SPHERE) {
            return Cell.SPHERE;
        }
        if (isRing(x, y, z)) {
            return Cell.RING;
        }
        if (isCylinderWall(x, z)) {
            return Cell.PILLAR;
        }
        int strand = helixStrand(x, y, z, r, floor, ceiling);
        return strand == 0 ? Cell.HELIX_BLACK : strand == 1 ? Cell.HELIX_WHITE : Cell.CLEAR;
    }

    /** Direction across the width of the ring a ring cell belongs to (the horizontal ring wins at crossings). */
    public static net.minecraft.core.Direction.Axis ringAxis(int x, int y, int z) {
        int dy = y - CENTRE;
        if (Math.abs(dy) <= 1 && Math.abs(Math.hypot(x, z) - RING) < 0.6) {
            return net.minecraft.core.Direction.Axis.Y;
        }
        return Math.abs(z) <= 1 && Math.abs(Math.hypot(x, dy) - RING) < 0.6 ? net.minecraft.core.Direction.Axis.Z
                : net.minecraft.core.Direction.Axis.X;
    }

    // ---- floor plan ----
    private enum Plan { FRAME, GRAVITY, ANTIGRAVITY, PARADOX }

    private enum Region { CIRCLE, HEX, BLACK_TIP, WHITE_TIP, GAP }

    /** What the floor (and ceiling) holds at this column; null outside the circle. */
    static Plan plan(int x, int z) {
        Region here = region(x, z);
        if (here == null) {
            return null;
        }
        if (x == 0 && z == 0 || here == Region.CIRCLE) {
            return Plan.FRAME;
        }
        if (here != Region.GAP) {   // star cells touching a non-star cell are the outline
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (!isStar(region(x + d[0], z + d[1]))) {
                    return Plan.FRAME;
                }
            }
            return switch (here) {
                case BLACK_TIP -> Plan.GRAVITY;
                case WHITE_TIP -> Plan.ANTIGRAVITY;
                default -> Plan.FRAME;
            };
        }
        // a gap: split by the ray through the star's concave corner (0°, 60°, ...)
        double r = Math.hypot(x, z);
        double angle = ((Math.toDegrees(Math.atan2(z, x)) % 360) + 360) % 360;
        double corner = Math.round(angle / 60) * 60;
        double off = ((angle - corner + 540) % 360) - 180;
        if (Math.abs(r * Math.sin(Math.toRadians(off))) <= SEAM) {
            return Plan.PARADOX;
        }
        long nearTip = Math.round(((corner + (off > 0 ? 30 : -30)) % 360 + 360) % 360);
        return nearTip == 270 || nearTip == 30 || nearTip == 150 ? Plan.GRAVITY : Plan.ANTIGRAVITY;
    }

    private static boolean isStar(Region region) {
        return region == Region.HEX || region == Region.BLACK_TIP || region == Region.WHITE_TIP;
    }

    private static Region region(int x, int z) {
        double r = Math.hypot(x, z);
        if (r > RING_OUT) {
            return null;
        }
        if (r >= RING_IN) {
            return Region.CIRCLE;
        }
        boolean black = inTriangle(x, z, -90), white = inTriangle(x, z, 90);
        if (black && white) {
            return Region.HEX;
        }
        return black ? Region.BLACK_TIP : white ? Region.WHITE_TIP : Region.GAP;
    }

    /** Inside the triangle with a tip at {@code tipAngle} (-90 = north) and the star's tip radius. */
    private static boolean inTriangle(double px, double pz, double tipAngle) {
        double[] vx = new double[3], vz = new double[3];
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(tipAngle + 120 * i);
            vx[i] = STAR * Math.cos(a);
            vz[i] = STAR * Math.sin(a);
        }
        boolean neg = false, pos = false;
        for (int i = 0; i < 3; i++) {
            int j = (i + 1) % 3;
            double s = (px - vx[j]) * (vz[i] - vz[j]) - (vx[i] - vx[j]) * (pz - vz[j]);
            neg |= s < 0;
            pos |= s > 0;
        }
        return !(neg && pos);
    }

    // ---- heights and solids ----
    static int floorY(double r) {
        return (int) Math.round(DOME * Math.max(0, 1 - (r / BASE) * (r / BASE)));
    }

    private static boolean isRing(int x, int y, int z) {
        int dy = y - CENTRE;
        return Math.abs(dy) <= 1 && Math.abs(Math.hypot(x, z) - RING) < 0.6
                || Math.abs(z) <= 1 && Math.abs(Math.hypot(x, dy) - RING) < 0.6
                || Math.abs(x) <= 1 && Math.abs(Math.hypot(z, dy) - RING) < 0.6;
    }

    /** Within a cylinder's outline, wall included. */
    private static boolean inCylinder(int x, int z) {
        for (double a : TIP_ANGLES) {
            if (Math.hypot(x - tipX(a), z - tipZ(a)) < CYLINDER + 0.55) {
                return true;
            }
        }
        return false;
    }

    /** The 5-across ring: .###. / #...# / #...# / #...# / .###. round the tip's block. */
    private static boolean isCylinderWall(int x, int z) {
        for (double a : TIP_ANGLES) {
            if (Math.abs(Math.hypot(x - tipX(a), z - tipZ(a)) - CYLINDER) < 0.55) {
                return true;
            }
        }
        return false;
    }

    /** A cylinder's centre: the block nearest the star tip, so every cylinder has the same symmetric outline. */
    private static long tipX(double angle) {
        return Math.round(STAR * Math.cos(Math.toRadians(angle)));
    }

    private static long tipZ(double angle) {
        return Math.round(STAR * Math.sin(Math.toRadians(angle)));
    }

    /** 0 = black strand, 1 = white strand, -1 = none. Each strand climbs from the floor to the ceiling. */
    private static int helixStrand(int x, int y, int z, double r, int floor, int ceiling) {
        if (Math.abs(r - HELIX) > HELIX_HALF || y <= floor || y >= ceiling) {
            return -1;
        }
        double a = Math.atan2(z, x);
        double base = 2 * Math.PI * HELIX_TURNS * (y - floor) / (ceiling - floor);
        for (int strand = 0; strand < 2; strand++) {
            double da = ((a - base - strand * Math.PI + Math.PI) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI) - Math.PI;
            if (Math.abs(da * HELIX) <= HELIX_HALF + 0.6) {
                return strand;
            }
        }
        return -1;
    }
}
