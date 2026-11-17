package com.lumina.ae2_gvt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** What a player sets for the look of the game on their own computer (the file is config/ae2_gvt-client.toml). */
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 0 low, 1 medium, 2 high. */
    public static final ModConfigSpec.IntValue HUB_DETAIL = BUILDER
            .comment("How detailed the inside of the Twin-Star Hub is drawn: 0 low, 1 medium, 2 high. A computer that is short of memory or slow",
                    "when looking at the hub should use 0; it is the sphere's mesh and the tails of the two holes that get coarser.")
            .defineInRange("hubDetail", 2, 0, 2);

    /** 0 low, 1 medium, 2 high. */
    public static final ModConfigSpec.IntValue HOLE_DETAIL = BUILDER
            .comment("How detailed black and white holes are drawn: 0 low, 1 medium, 2 high. What costs most is copying the frame to bend it:",
                    "2 bends the background and adds the small ripples (two half-size copies per hole), 1 bends it with one copy and no ripples,",
                    "0 copies nothing - the hole is a plain sphere and disc with its rings, the background is not bent. All three also draw fewer",
                    "holes at once, from nearer, with a coarser body and disc the lower they are.")
            .defineInRange("holeDetail", 2, 0, 2);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
