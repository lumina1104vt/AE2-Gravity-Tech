package com.lumina.ae2_gvt.integration;

import java.util.List;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.CrystalLine;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import com.lumina.ae2_gvt.hole.GravityHole;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * What happens in the world rather than in a machine, for JEI and EMI to show beside the recipes: things dropped
 * on a hole or caught together in an explosion, holes blown up, crystals growing on holes, the paradox seed.
 * The numbers here are those of the code that does it (HoleCollapse, GravityHoleBlockEntity, GravityHoleBlock,
 * ParadoxSeedItem); change them together.
 *
 * @param conditions what has to be there besides the inputs: a hole, an explosion (TNT stands for any)
 */
public record WorldInteraction(String name, List<ItemStack> inputs, List<ItemStack> conditions, List<ItemStack> outputs) {
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "world/" + name);
    }

    public Component text() {
        return Component.translatable("gui.ae2_gvt.world." + name);
    }

    public static List<WorldInteraction> all() {
        ItemStack explosion = stack(net.minecraft.world.item.Items.TNT, 1);
        ItemStack black = stack(GravityHole.BLACK_HOLE_ITEM.get(), 1), white = stack(GravityHole.WHITE_HOLE_ITEM.get(), 1);
        ItemStack chaos = stack(Items.CHAOS_MATTER.get(), 1), singularity = stack(AEItems.SINGULARITY, 1);
        ItemStack anti = stack(Items.ANTI_SINGULARITY.get(), 1);
        ItemStack gravityDust = stack(AE2GravityTech.GRAVITY.dust().get(), 64);
        return List.of(
                new WorldInteraction("anti_singularity", List.of(singularity), List.of(black), List.of(anti)),
                new WorldInteraction("black_hole", List.of(chaos, singularity, gravityDust), List.of(explosion), List.of(black)),
                new WorldInteraction("white_hole", List.of(chaos, anti, gravityDust), List.of(explosion), List.of(white)),
                new WorldInteraction("entangled_singularity",
                        List.of(anti, singularity, stack(AE2GravityTech.PARADOX.dust().get(), 1)), List.of(explosion),
                        List.of(stack(Items.PARADOX_QUANTUM_ENTANGLED_SINGULARITY.get(), 2))),
                new WorldInteraction("black_hole_blown", List.of(black), List.of(explosion),
                        List.of(chaos, GravitonKey.GRAVITON.displayStack().copyWithCount(128))),
                new WorldInteraction("white_hole_blown", List.of(white), List.of(explosion),
                        List.of(chaos, GravitonKey.ANTIGRAVITON.displayStack().copyWithCount(128))),
                new WorldInteraction("black_hole_growth", List.of(black), List.of(), growth(AE2GravityTech.GRAVITY, GravitonKey.GRAVITON)),
                new WorldInteraction("white_hole_growth", List.of(white), List.of(), growth(AE2GravityTech.ANTIGRAVITY, GravitonKey.ANTIGRAVITON)),
                new WorldInteraction("paradox_seed", List.of(stack(Items.PARADOX_CRYSTAL_SEED.get(), 1), stack(AEBlocks.FLUIX_BLOCK, 1)),
                        List.of(black, white), List.of(stack(Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX.get(), 1))),
                new WorldInteraction("budding_paradox", List.of(stack(Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX.get(), 1)), List.of(),
                        List.of(stack(AE2GravityTech.PARADOX.smallBud().get(), 1), stack(AE2GravityTech.PARADOX.mediumBud().get(), 1),
                                stack(AE2GravityTech.PARADOX.largeBud().get(), 1), stack(AE2GravityTech.PARADOX.cluster().get(), 1))));
    }

    private static List<ItemStack> growth(CrystalLine line, GravitonKey gravitons) {
        return List.of(stack(line.smallBud().get(), 1), stack(line.mediumBud().get(), 1), stack(line.largeBud().get(), 1),
                stack(line.cluster().get(), 1), gravitons.displayStack().copyWithCount(16));
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }
}
