package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.screen.GravityFactoryScreen;
import com.lumina.ae2_gvt.recipes.GravityProcessingRecipe;
import com.lumina.lmb.compat.RecipeNumbers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * A 引力加工工廠 recipe drawn as the factory's own GUI, for JEI and EMI: its panel cut down to the 4 × 3
 * inputs, the gravity well and the 4 × 3 outputs, with a line of costs under them. Low enough that two
 * recipes fit on a page.
 */
public final class FactoryRecipeLook {
    public static final int WIDTH = 208, HEIGHT = 76;
    /** Item corners of the first input and output slot, as in the GUI. */
    public static final int INPUT_X = 8, OUTPUT_X = 129, SLOT_Y = 6, SLOT = 18, COLUMNS = 4, SLOTS = 12;
    private static final int WELL_X = 104, WELL_Y = 21, TEXT = 0xFF404040;
    private static final ResourceLocation GUI = new ResourceLocation(AE2GravityTech.MODID, "textures/gui/gravity_processing_factory.png");

    private FactoryRecipeLook() {
    }

    /** Everything but the items, from the recipe's top left corner. */
    public static void draw(GuiGraphics g, GravityProcessingRecipe r) {
        // the GUI panel: its top edge, the rows with the slots, a plain stretch for the text, its bottom edge
        g.blit(GUI, 0, 0, 0, 0, WIDTH, 3);
        g.blit(GUI, 0, 3, 0, 16, WIDTH, 59);
        g.blit(GUI, 0, 62, WIDTH, HEIGHT - 4 - 62, 0, 80, WIDTH, 1, 256, 256);
        g.blit(GUI, 0, HEIGHT - 4, 0, 174, WIDTH, 4);
        // the well works through the recipe's time over and over, as it does while the factory runs it
        long period = Math.max(1000L, r.time() * 50L);
        GravityFactoryScreen.drawArrows(g, WELL_X, WELL_Y, (System.currentTimeMillis() % period) / (float) period);

        MutableComponent first = Component.translatable("gui.ae2_gvt.factory.jei.info",
                String.format("%.1f", r.time() / 20.0), RecipeNumbers.compact(r.energy()));
        if (r.gravitons() > 0)
            first.append("・").append(Component.translatable("item.ae2_gvt.graviton"))
                    .append(" " + RecipeNumbers.compact(r.gravitons()));
        if (r.antigravitons() > 0)
            first.append("・").append(Component.translatable("item.ae2_gvt.antigraviton"))
                    .append(" " + RecipeNumbers.compact(r.antigravitons()));
        RecipeNumbers.drawLine(g, first, INPUT_X, 63, WIDTH - 2 * INPUT_X, TEXT);
    }

    /** The corner mark of an item that is kept: a small dark wedge at the top left of its slot (x, y: the item's). */
    public static void drawKept(GuiGraphics g, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (int row = 0; row < 5; row++) {
            g.fill(x - 1, y - 1 + row, x + 4 - row, y + row, 0xFF2B2E45);
            g.fill(x + 4 - row, y - 1 + row, x + 5 - row, y + row, 0xFFFFFFFF);
        }
        g.pose().popPose();
    }
}
