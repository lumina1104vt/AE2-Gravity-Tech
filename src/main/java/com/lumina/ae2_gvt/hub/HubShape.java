package com.lumina.ae2_gvt.hub;

/**
 * Shape of the Twin-Star Hub ({@link HexagramHub}), the black / white hole computing core: a port of the user's three.js draft ("方塊化黑白洞計算中樞",
 * 2026-11-27). The draft's layers and sizes; its rings are redrawn symmetric (see ring()). What the draft only renders (the holes, disks,
 * jets, streams, packets) is drawn by the client's HexagramHubRenderer, not built.
 * <p>
 * Materials follow the theme, not the draft's colours: the black hole's half is gravity crystal and
 * gravity-doped sky stone, the white hole's half antigravity crystal and antigravity-doped sky stone, and
 * where they meet (the L1 core, the seam of the rings, the anchors) is paradox.
 * <p>
 * Coordinates: x and z from -117 to 117 round the L1 core (x runs black hole → white hole), y from 0 (the
 * lower anchor's tip, where the controller is) to 120. Each cell is a role id, plus {@link #TOP} for a
 * slab in the upper half; 0 = not part of the structure.
 */
public final class HubShape {
    public static final int R = 117, W = 2 * R + 1, CY = 60, H = 2 * CY + 1;
    public static final int TOP = 64, ROLE_MASK = 63;

    // solid materials: role 1..6 the full block, role + SOLIDS the slab
    private static final int FRAME = 1, DECK = 2, BLACK = 3, WHITE = 4, SEAM = 5, PILLAR = 6;
    private static final int SOLIDS = 6;
    // crystals (never slabs)
    private static final int GRAVITY = 13, ANTIGRAVITY = 14, PARADOX = 15;
    public static final int CTRL = 16;

    /**
     * Block of each role (index = role id); null = the controller. Full blocks are our own; we have no
     * slabs, so those are AE2's of the nearest shade.
     */
    public static final String[] BLOCK_IDS = {null,
            // neutral dark, neutral light, black half, white half, where they meet, vertical columns only
            "ae2_gvt:structure_frame", "ae2_gvt:machine_frame", "ae2_gvt:hub_helix_black",
            "ae2_gvt:hub_helix_white", "ae2_gvt:hub_ring", "ae2_gvt:hub_pillar",
            // their slabs (the pillar has none)
            "ae2:sky_stone_brick_slab", "ae2:quartz_slab", "ae2:smooth_sky_stone_slab",
            "ae2:smooth_quartz_slab", "ae2:fluix_slab", "ae2:quartz_pillar_slab",
            "ae2_gvt:gravity_block", "ae2_gvt:antigravity_block", "ae2_gvt:paradox_block",
            null};

    /** What a half is built from: the black hole's (x < 0), the white hole's (x > 0), or the meeting of both. */
    private record Theme(int rock, int crystal, int frame) {
    }

    private static final Theme BLACK_SIDE = new Theme(BLACK, GRAVITY, FRAME);
    private static final Theme WHITE_SIDE = new Theme(WHITE, ANTIGRAVITY, DECK);
    private static final Theme MIDDLE = new Theme(SEAM, PARADOX, FRAME);

    /** The theme at x, with a seam of {@code seam} blocks either side of the middle. */
    private static Theme side(int x, int seam) {
        return x < -seam ? BLACK_SIDE : x > seam ? WHITE_SIDE : MIDDLE;
    }

    private static final int BH_X = -28, WH_X = 28;
    /** The four compute rings, inside out: radius, tilt about x, tilt about z. */
    private static final double[][] RINGS = {{47, 0.08, -0.05}, {59, 0.40, 0.22}, {72, -0.32, 0.34}, {86, 0.04, 0.03}};
    private static byte[] grid;

    private HubShape() {
    }

    /** Cell at (x, y, z): role id, possibly with {@link #TOP}; 0 outside the structure or the box. */
    public static int cell(int x, int y, int z) {
        if (x < -R || x > R || z < -R || z > R || y < 0 || y >= H) return 0;
        return grid()[idx(x, y, z)];
    }

    /**
     * The core block of ring {@code ring} (0 = innermost) in the direction {@code angle}, as {x, y, z} from the
     * L1 core: where the renderer ends its data arcs.
     */
    public static int[] ringPoint(int ring, double angle) {
        double radius = RINGS[ring][0];
        int x = Gen.sround(StrictMath.cos(angle) * radius), z = Gen.sround(StrictMath.sin(angle) * radius);
        return new int[]{x, ringHeight(RINGS[ring], x, z), z};
    }

    /** How high the tilt lifts a ring's column at (x, z). */
    private static int ringHeight(double[] ring, double x, double z) {
        double alongX = Math.sin(ring[2]) * Math.cos(ring[1]), alongZ = -Math.sin(ring[1]);
        return Gen.sround(ring[0] * (x * alongX + z * alongZ) / Math.sqrt(x * x + z * z));
    }

    private static int idx(int x, int y, int z) {
        return (y * W + (z + R)) * W + (x + R);
    }

    private static synchronized byte[] grid() {
        if (grid == null) grid = new Gen().run();
        return grid;
    }

    // ---- the generator; y here is the draft's (0 = the L1 core) ----

    private static final class Gen {
        private static final int BOTTOM = 1, UPPER = 2, FULL = 3;
        final byte[] mat = new byte[W * W * H];
        final byte[] halves = new byte[W * W * H];

        static int round(double v) {
            return (int) Math.floor(v + 0.5);   // JS Math.round
        }

        static boolean glows(int m) {
            return m >= GRAVITY && m <= PARADOX;
        }

        /** Places a block of the draft; where two meet, glow beats solid and bottom + top make a full block. */
        void set(int x, int y, int z, int m, int shape) {
            if (x < -R || x > R || z < -R || z > R || y < -CY || y > CY) return;
            int i = idx(x, y + CY, z);
            int old = mat[i];
            if (old == 0 || (glows(m) && !glows(old)) || (glows(m) == glows(old) && (shape == FULL || halves[i] != FULL))) {
                mat[i] = (byte) m;
            }
            halves[i] |= (byte) shape;
        }

        byte[] run() {
            l1Core();
            shell(BH_X, 14.2, BLACK_SIDE);
            shell(WH_X, 12.8, WHITE_SIDE);
            ring(RINGS[0], 40);
            ring(RINGS[1], 40);
            ring(RINGS[2], 56);
            ring(RINGS[3], 56);
            spine();
            spires();
            anchors();

            byte[] g = new byte[W * W * H];
            for (int i = 0; i < g.length; i++) {
                int m = mat[i];
                if (m == 0) continue;
                if (m <= SOLIDS && halves[i] != FULL) {
                    g[i] = (byte) ((m + SOLIDS) | (halves[i] == UPPER ? TOP : 0));
                } else {
                    g[i] = (byte) m;
                }
            }
            g[idx(0, 0, 0)] = CTRL;
            return g;
        }

        void l1Core() {
            for (int x = -6; x <= 6; x++) {
                for (int y = -6; y <= 6; y++) {
                    for (int z = -6; z <= 6; z++) {
                        int m = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (m <= 2) set(x, y, z, PARADOX, FULL);
                        if (m == 7 && (Math.abs(x) <= 1 || Math.abs(y) <= 1 || Math.abs(z) <= 1)) {
                            boolean glow = x == 0 || z == 0;
                            set(x, y, z, glow ? PARADOX : SEAM, y > 4 ? BOTTOM : y < -4 ? UPPER : FULL);
                        }
                    }
                }
            }
        }

        /** The Penrose shell round a hole and the collector belt round its equator. */
        void shell(int cx, double radius, Theme theme) {
            int rInt = (int) Math.ceil(radius) + 1;
            int toCentre = cx < 0 ? 1 : -1;
            for (int x = -rInt; x <= rInt; x++) {
                for (int y = -rInt; y <= rInt; y++) {
                    for (int z = -rInt; z <= rInt; z++) {
                        double d = Math.sqrt(x * x + y * y + z * z);
                        if (d < radius - 0.65 || d > radius + 0.55) continue;
                        if (Math.abs(y) > radius * 0.82) continue;      // polar jet vents
                        if (Math.abs(y) <= 2) continue;                 // disk channel
                        if (x * toCentre > radius * 0.68 && Math.abs(y) < radius * 0.42) continue;   // bridge opening
                        double ang = StrictMath.atan2(z, x);
                        boolean lat = Math.abs(y) == 3 || Math.abs(y) == 7 || Math.abs(y) == 11;
                        boolean lon = Math.abs(StrictMath.sin(ang * 4)) < 0.14;
                        if (!lat && !lon && ((x + y + z) & 1) == 0) continue;
                        boolean node = lat && lon;
                        int shape = FULL;
                        if (Math.abs(y) == 3) shape = y > 0 ? UPPER : BOTTOM;
                        else if (Math.abs(y) >= Math.floor(radius * 0.75)) shape = y > 0 ? BOTTOM : UPPER;
                        set(cx + x, y, z, node ? theme.crystal : lat ? theme.frame : theme.rock, shape);
                    }
                }
            }
            int beltR = round(radius * 1.24);
            for (int x = -beltR - 1; x <= beltR + 1; x++) {
                for (int z = -beltR - 1; z <= beltR + 1; z++) {
                    if (Math.abs(Math.sqrt(x * x + z * z) - beltR) > 0.5 || x * toCentre > beltR * 0.85) continue;
                    set(cx + x, 0, z, theme.frame, FULL);
                    set(cx + x, 1, z, theme.rock, BOTTOM);
                    set(cx + x, -1, z, theme.rock, UPPER);
                }
            }
            for (int i = 0; i < 16; i++) {
                double a = i / 16.0 * Math.PI * 2;
                int bx = sround(StrictMath.cos(a) * beltR), bz = sround(StrictMath.sin(a) * beltR);
                if (bx * toCentre <= beltR * 0.85) set(cx + bx, 0, bz, theme.crystal, FULL);
            }
        }

        /** JS-free rounding that is the same on both sides of zero, so mirrored places come out mirrored. */
        static int sround(double v) {
            return (int) Math.signum(v) * round(Math.abs(v) + 1e-7);   // nudged: sin and cos miss .5 by a hair
        }

        /**
         * One compute ring. Unlike the draft (which turns the finished ring and rounds every block on its
         * own, so no two details come out alike), the ring is drawn flat, mirror- and 90°-symmetric in plan,
         * and the tilt only raises each column: the cross-section is the same everywhere and upper and lower
         * halves mirror each other. {@code towers} = 8 × an odd number: that many blade towers, a radiator fin
         * at every other one, none of them on a diagonal.
         */
        void ring(double[] ring, int towers) {
            double radius = ring[0];
            int reach = (int) radius + 3;
            for (int x = -reach; x <= reach; x++) {
                for (int z = -reach; z <= reach; z++) {
                    double d = Math.sqrt(x * x + z * z), off = d - radius;
                    if (Math.abs(off) > 2.5) continue;
                    int h = ringHeight(ring, x, z);
                    Theme theme = side(x, 3);
                    if (Math.abs(off) <= 0.5) {
                        set(x, h, z, theme.frame, FULL);
                        set(x, h + 1, z, theme.rock, BOTTOM);
                        set(x, h - 1, z, theme.rock, UPPER);
                    } else if (Math.abs(off + 2) <= 0.5) {
                        set(x, h, z, theme.crystal, FULL);
                    } else if (Math.abs(off - 2) <= 0.5) {
                        set(x, h, z, theme.rock, FULL);
                    }
                }
            }
            for (int i = 0; i < towers; i++) {
                double a = (double) i / towers * Math.PI * 2;
                double c = StrictMath.cos(a), sn = StrictMath.sin(a);
                int px = sround(c * radius), pz = sround(sn * radius);
                int h = ringHeight(ring, c, sn);
                Theme theme = side(px, 3);
                for (int ty = -3; ty <= 3; ty++) {
                    boolean cap = Math.abs(ty) == 3;
                    boolean lit = ty == 0 || Math.abs(ty) == 2;
                    set(px, h + ty, pz, lit ? theme.crystal : theme.rock, cap ? (ty > 0 ? BOTTOM : UPPER) : FULL);
                }
                if (i % 2 != 0) continue;
                // the fin runs straight along the nearer axis, 4 long from just outside the rail
                boolean alongTheX = Math.abs(c) >= Math.abs(sn);
                int dx = alongTheX ? (int) Math.signum(c) : 0, dz = alongTheX ? 0 : (int) Math.signum(sn);
                int fx = px, fz = pz;
                while (Math.sqrt(fx * fx + fz * fz) <= radius + 2.5) {
                    fx += dx;
                    fz += dz;
                }
                for (int f = 0; f < 4; f++, fx += dx, fz += dz) {
                    set(fx, h + 1, fz, theme.rock, BOTTOM);
                    set(fx, h - 1, fz, theme.rock, UPPER);
                }
            }
        }

        /** The truss along x from hole to hole. */
        void spine() {
            for (int x = -45; x <= 45; x++) {
                if (Math.abs(x - BH_X) < 12 || Math.abs(x - WH_X) < 11) continue;
                Theme theme = side(x, 0);
                set(x, 4, 0, theme.rock, BOTTOM);
                set(x, -4, 0, theme.rock, UPPER);
                set(x, 0, 4, theme.frame, FULL);
                set(x, 0, -4, theme.frame, FULL);
                if (x % 5 == 0) {
                    for (int d = -3; d <= 3; d++) {
                        set(x, d, 4, theme.rock, FULL);
                        set(x, d, -4, theme.rock, FULL);
                        set(x, 4, d, theme.rock, BOTTOM);
                        set(x, -4, d, theme.rock, UPPER);
                    }
                    set(x, 4, 0, theme.crystal, UPPER);
                    set(x, -4, 0, theme.crystal, BOTTOM);
                }
            }
        }

        /** Eight relay spires: a hub at distance 87 and a shaft pointing outwards. */
        void spires() {
            for (int i = 0; i < 8; i++) {
                double a = i / 8.0 * Math.PI * 2;
                double c = StrictMath.cos(a), s = StrictMath.sin(a);
                int px = sround(c * 87), pz = sround(s * 87);
                Theme theme = side(px, 0);
                Local put = (lx, ly, lz, m, shape) ->
                        set(px + sround(lx * c - lz * s), round(ly), pz + sround(lx * s + lz * c), m, shape);
                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            int m = Math.abs(x) + Math.abs(y) + Math.abs(z);
                            if (m <= 4 && m >= 2) {
                                set(px + x, y, pz + z, m == 4 ? theme.rock : theme.frame,
                                        Math.abs(y) == 3 ? (y > 0 ? BOTTOM : UPPER) : FULL);
                            }
                        }
                    }
                }
                for (double sx = 4; sx <= 28; sx += 0.5) {
                    int thick = sx < 12 ? 2 : sx < 21 ? 1 : 0;
                    for (int y = -thick; y <= thick; y++) {
                        for (int z = -thick; z <= thick; z++) {
                            if (Math.abs(y) + Math.abs(z) > thick) continue;
                            boolean edge = thick > 0 && Math.abs(y) == thick;
                            put.at(sx, y, z, theme.frame, edge ? (y > 0 ? BOTTOM : UPPER) : FULL);
                        }
                    }
                    if (sx == 9 || sx == 16 || sx == 22) {
                        int cr = sx == 9 ? 3 : 2;
                        for (int y = -cr; y <= cr; y++) {
                            for (int z = -cr; z <= cr; z++) {
                                if (Math.abs(y) == cr || Math.abs(z) == cr) {
                                    put.at(sx, y, z, theme.crystal, Math.abs(y) == cr ? (y > 0 ? BOTTOM : UPPER) : FULL);
                                }
                            }
                        }
                    }
                }
                put.at(29, 0, 0, theme.crystal, FULL);
                put.at(30, 0, 0, theme.rock, BOTTOM);
            }
        }

        private interface Local {
            void at(double lx, double ly, double lz, int m, int shape);
        }

        /**
         * The two polar anchor towers, y ±32..±60, with their crown rings. The shafts are the hub pillar's
         * place: unbroken columns along y.
         */
        void anchors() {
            for (int dir = -1; dir <= 1; dir += 2) {
                int base = dir * 32;
                for (int y = 0; y <= 28; y++) {
                    int r = y < 8 ? 2 : y < 18 ? 1 : 0;
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.abs(x) + Math.abs(z) <= r) set(x, base + y * dir, z, PILLAR, FULL);
                        }
                    }
                }
                crown(base + 6 * dir, 7, PARADOX, FULL);
                crown(base + 14 * dir, 11, SEAM, dir > 0 ? BOTTOM : UPPER);
            }
        }

        void crown(int y, int radius, int m, int shape) {
            for (int x = -radius - 1; x <= radius + 1; x++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    if (Math.abs(Math.sqrt(x * x + z * z) - radius) <= 0.5) set(x, y, z, m, shape);
                }
            }
        }
    }
}
