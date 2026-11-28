package com.lumina.ae2_gvt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** What a player sets for the look of the game on their own computer (the file is config/ae2_gvt-client.toml). */
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 0 low, 1 medium, 2 high. */
    public static final ModConfigSpec.IntValue HUB_DETAIL = BUILDER
            .comment("How detailed the inside of the Twin-Star Hub is drawn: 0 low, 1 medium, 2 high. A computer that is short of memory or slow",
                    "when looking at the hub should use 0; the two holes get coarser, and 0 also leaves out the data arcs.")
            .defineInRange("hubDetail", 2, 0, 2);

    /** 0 low, 1 medium, 2 high. */
    public static final ModConfigSpec.IntValue HOLE_DETAIL = BUILDER
            .comment("How detailed black and white holes are drawn: 0 low, 1 medium, 2 high. Every hole in range is drawn, but the effects are staged",
                    "by distance, since a far hole's warp is a few pixels: 2 bends the background within 64 blocks and adds small ripples within 32,",
                    "1 bends it within 40 blocks, 0 bends nothing (the hole is a plain sphere and disc with its rings). Farther holes keep the",
                    "rings. Lower also means a shorter drawing distance and a coarser body and disc.")
            .defineInRange("holeDetail", 2, 0, 2);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
