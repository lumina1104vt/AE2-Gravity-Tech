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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
