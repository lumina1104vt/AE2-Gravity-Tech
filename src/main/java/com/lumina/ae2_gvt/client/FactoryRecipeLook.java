package com.lumina.ae2_gvt.client;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
 * recipes fit on a page. The middle slot is the chaos matter's, as in the factory, and stays empty.
 */
public final class FactoryRecipeLook {
    public static final int WIDTH = 208, HEIGHT = 76;
    /** Item corners of the first input and output slot, as in the GUI. */
    public static final int INPUT_X = 8, OUTPUT_X = 129, SLOT_Y = 6, SLOT = 18, COLUMNS = 4, SLOTS = 12;
    private static final int WELL_X = 104, WELL_Y = 21, TEXT = 0xFF404040;
    public static final int LINE_Y = 63;
    private static final int ICON = 10;
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/gravity_processing_factory.png");

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
        drawCosts(g, first, r.networkCosts());
    }

    /**
     * The line under the slots: time, energy and gravitons, then what else the network pays (lightning, data
     * flow) as its icon and amount; smaller if it would not fit.
     */
    private static void drawCosts(GuiGraphics g, Component text, List<GenericStack> network) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        for (GenericStack cost : network)
            width += font.width("・") + ICON + font.width(" " + RecipeNumbers.compact(cost.amount()));
        float scale = Math.min(1f, (float) (WIDTH - 2 * INPUT_X) / width);
        g.pose().pushPose();
        g.pose().translate(INPUT_X, LINE_Y + (font.lineHeight - font.lineHeight * scale) / 2, 0);
        g.pose().scale(scale, scale, 1);
        int x = g.drawString(font, text, 0, 0, TEXT, false);
        for (GenericStack cost : network) {
            x = g.drawString(font, "・", x, 0, TEXT, false);
            g.pose().pushPose();
            g.pose().translate(x, -1, 0);
            g.pose().scale(ICON / 16f, ICON / 16f, 1);
            AEKeyRendering.drawInGui(Minecraft.getInstance(), g, 0, 0, cost.what());
            g.pose().popPose();
            x = g.drawString(font, " " + RecipeNumbers.compact(cost.amount()), x + ICON, 0, TEXT, false);
        }
        g.pose().popPose();
    }

    /** What to say over the cost line at x, y (from the recipe's corner); nothing elsewhere. */
    public static List<Component> tooltip(GravityProcessingRecipe r, double x, double y) {
        if (x < INPUT_X || x >= WIDTH - INPUT_X || y < LINE_Y - 1 || y >= LINE_Y + 10)
            return List.of();
        return networkTooltip(r);
    }

    public static List<Component> networkTooltip(GravityProcessingRecipe r) {
        List<Component> lines = new ArrayList<>();
        for (GenericStack cost : r.networkCosts())
            lines.add(Component.translatable("gui.ae2_gvt.factory.jei.network", cost.what().getDisplayName(),
                    NumberFormat.getInstance().format(cost.amount())));
        return lines;
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
