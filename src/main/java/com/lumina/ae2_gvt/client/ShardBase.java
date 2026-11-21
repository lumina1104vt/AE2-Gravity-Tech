package com.lumina.ae2_gvt.client;

import net.minecraft.client.renderer.RenderStateShard;

/**
 * 1.20.1 keeps the render state shards (cull, depth, blending...) protected; classes that build render types
 * extend this to reach them (Forge 1.21 opens them up, so 1.21 needs no such base).
 */
abstract class ShardBase extends RenderStateShard {
    protected ShardBase() {
        super("ae2_gvt_shard_base", () -> {
        }, () -> {
        });
    }
}
