package com.lumina.ae2_gvt;

import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

public record CrystalLine(
    DeferredItem<Item> crystal,
    DeferredItem<Item> dust,
    DeferredItem<Item> circuit,
    DeferredItem<Item> processor,
    DeferredItem<Item> alloyDust,
    DeferredItem<Item> alloyIngot,
    DeferredItem<BlockItem> block,
    DeferredItem<BlockItem> smallBud,
    DeferredItem<BlockItem> mediumBud,
    DeferredItem<BlockItem> largeBud,
    DeferredItem<BlockItem> cluster
) {
    public static CrystalLine register(String prefix, MapColor color, int blockLight) {
        var crystal = Items.ITEMS.registerSimpleItem(prefix + "_crystal");
        var dust = Items.ITEMS.registerSimpleItem(prefix + "_dust");
        var circuit = Items.ITEMS.registerSimpleItem(prefix + "_circuit");
        var processor = Items.ITEMS.registerSimpleItem(prefix + "_processor");
        var alloyDust = Items.ITEMS.registerSimpleItem(prefix + "_alloy_dust");
        var alloyIngot = Items.ITEMS.registerSimpleItem(prefix + "_alloy_ingot");
        var block = Blocks.BLOCKS.registerSimpleBlock(prefix + "_block", BlockBehaviour.Properties.of().mapColor(color).lightLevel(state -> blockLight));
        var blockItem = Items.ITEMS.registerSimpleBlockItem(block);
        var smallBud = Blocks.BLOCKS.registerBlock("small_" + prefix + "_bud",
                p -> new AmethystClusterBlock(3, 4, p),
                BlockBehaviour.Properties.of().mapColor(color).noOcclusion().strength(1.5f));
        var smallBudItem = Items.ITEMS.registerSimpleBlockItem(smallBud);
        var mediumBud = Blocks.BLOCKS.registerBlock("medium_" + prefix + "_bud",
                p -> new AmethystClusterBlock(4, 3, p),
                BlockBehaviour.Properties.of().mapColor(color).noOcclusion().strength(1.5f));
        var mediumBudItem = Items.ITEMS.registerSimpleBlockItem(mediumBud);
        var largeBud = Blocks.BLOCKS.registerBlock("large_" + prefix + "_bud",
                p -> new AmethystClusterBlock(5, 3, p),
                BlockBehaviour.Properties.of().mapColor(color).noOcclusion().strength(1.5f));
        var largeBudItem = Items.ITEMS.registerSimpleBlockItem(largeBud);
        var cluster = Blocks.BLOCKS.registerBlock(prefix + "_cluster",
                p -> new AmethystClusterBlock(6, 3, p),
                BlockBehaviour.Properties.of().mapColor(color).noOcclusion().strength(1.5f));
        var clusterItem = Items.ITEMS.registerSimpleBlockItem(cluster);
        return new CrystalLine(crystal, dust, circuit, processor, alloyDust, alloyIngot, blockItem, smallBudItem, mediumBudItem, largeBudItem, clusterItem);
    }

    public List<DeferredItem<? extends Item>> all() {
        return List.of(crystal, dust, circuit, processor, alloyDust, alloyIngot, block, smallBud, mediumBud, largeBud, cluster);
    }
}
