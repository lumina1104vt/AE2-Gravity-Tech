package com.lumina.ae2_gvt.prototype.fusion;

import net.minecraft.world.level.block.TransparentBlock;

/**
 * 約束玻璃: the fusion generator's walls; you can see the plasma through it. Connected like the
 * structure frame (fusion.client.ContainmentGlassModel); faces between two panes are skipped.
 */
public class ContainmentGlassBlock extends TransparentBlock {
    public ContainmentGlassBlock(Properties properties) {
        super(properties);
    }
}
