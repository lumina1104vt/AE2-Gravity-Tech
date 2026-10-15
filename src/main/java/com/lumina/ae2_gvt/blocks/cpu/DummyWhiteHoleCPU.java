package com.lumina.ae2_gvt.blocks.cpu;

import appeng.block.crafting.AbstractCraftingUnitBlock;
import appeng.block.crafting.ICraftingUnitType;
import com.lumina.ae2_gvt.blockentities.cpu.DummyWhiteHoleCPUBlockEntity;

public class DummyWhiteHoleCPU extends AbstractCraftingUnitBlock<DummyWhiteHoleCPUBlockEntity> {
    public DummyWhiteHoleCPU(Properties props) {
        super(props, ICraftingUnitType);
    }
}
