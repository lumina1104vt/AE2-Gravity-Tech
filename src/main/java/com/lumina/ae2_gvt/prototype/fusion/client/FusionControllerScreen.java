package com.lumina.ae2_gvt.prototype.fusion.client;

import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.fusion.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.prototype.fusion.FusionControllerMenu;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 核融合控制器 GUI, laid out like AE2's own screens. Background: assets/ae2_gvt/textures/gui/
 * fusion_controller.png (tools/fusion_gui_texture.py); positions: assets/ae2/screens/
 * fusion_controller.json. Almost no text: values are in tooltips.
 * <ul>
 *   <li>top: the energy-input cap, AE2's own number entry placed as on its level emitter</li>
 *   <li>temperature bar: the plasma's colour path on a log scale, lit up to the current
 *       temperature; white line = current, dashed = where the cap will settle it, triangle = what
 *       the reaction needs</li>
 *   <li>energy in / out: two bars that look the same (AE2's vibration-chamber bar, lying down,
 *       log scale); tooltips tell them apart</li>
 *   <li>a status lamp beside the title</li>
 * </ul>
 */
public class FusionControllerScreen extends AEBaseScreen<FusionControllerMenu> {
    private static final int TX0 = 12, TX1 = 163, TY0 = 89, TY1 = 99;
    private static final int[] BAR_X = {12, 90};
    private static final int BAR_Y = 110, BAR_W = 72, BAR_H = 6;
    private static final int LAMP_X = 162, LAMP_Y = 7;
    private static final double LO = 1500, HI = 80000;
    private static final int[] LAMP = {0xFF6E7386, 0xFFE6A01E, 0xFF3CC85A, 0xFFDC3232};
    private static final String[] STATUS = {"idle", "heating", "running", "too_cold"};
    private static final Blitter SEGMENT = Blitter.texture(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/fusion_controller.png")).src(176, 0, 18, BAR_H);

    private final NumberEntryWidget cap;

    public FusionControllerScreen(FusionControllerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        cap = widgets.addNumberEntryWidget("cap", NumberEntryType.UNITLESS);
        cap.setTextFieldStyle(style.getWidget("capInput"));
        cap.setMinValue(0);
        cap.setMaxValue(FusionControllerBlockEntity.MAX_CAP);
        cap.setLongValue(menu.energyCap);
        cap.setOnChange(() -> cap.getLongValue().ifPresent(menu::setEnergyCap));
    }

    /** Bar column for a temperature, on a log scale. */
    private static int tempX(double k) {
        k = Math.max(LO, Math.min(HI, k));
        return (int) Math.round(TX0 + Math.log(k / LO) / Math.log(HI / LO) * (TX1 - TX0));
    }

    @Override
    public void drawBG(GuiGraphicsExtractor g, int ox, int oy, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(g, ox, oy, mouseX, mouseY, partialTicks);
        int temp = menu.temperature;

        // temperature: the colour path, lit up to the current temperature
        for (int x = TX0; x <= TX1; x++) {
            double k = LO * Math.pow(HI / LO, (double) (x - TX0) / (TX1 - TX0));
            int c = FusionPlasmaRenderer.colour(k);
            if (k > temp) {
                c = mix(c, 0x28282f, 0.68);
            }
            g.fill(ox + x, oy + TY0, ox + x + 1, oy + TY1 + 1, 0xFF000000 | c);
        }
        int xt = tempX(temp);
        g.fill(ox + xt, oy + TY0 - 2, ox + xt + 1, oy + TY1 + 3, 0xFFFFFFFF);
        int xe = tempX(menu.equilibrium);
        for (int y = TY0; y <= TY1; y += 2) {
            g.fill(ox + xe, oy + y, ox + xe + 1, oy + y + 1, 0xFF14141A);
        }
        if (menu.need > 0) {
            int xn = tempX(menu.need);
            for (int i = 0; i < 4; i++) {
                g.fill(ox + xn - i, oy + TY1 + 2 + i, ox + xn + i + 1, oy + TY1 + 3 + i, 0xFF413F54);
            }
        }

        // energy in / out: same look, same log scale (1 FE/t .. 1.6x the largest cap)
        bar(g, ox + BAR_X[0], oy + BAR_Y, logFrac(menu.energyIn));
        bar(g, ox + BAR_X[1], oy + BAR_Y, logFrac(menu.energyOut));

        g.fill(ox + LAMP_X, oy + LAMP_Y, ox + LAMP_X + 5, oy + LAMP_Y + 5, LAMP[menu.status()]);
    }

    private static double logFrac(long fePerTick) {
        return fePerTick <= 1 ? 0 : Math.log(fePerTick) / Math.log(FusionControllerBlockEntity.MAX_CAP * 1.6);
    }

    /** AE2's bar segment, tiled from the left up to the fraction. */
    private static void bar(GuiGraphicsExtractor g, int x, int y, double frac) {
        int n = (int) Math.round(Math.max(0, Math.min(1, frac)) * BAR_W);
        for (int k = 0; k < n; k += 18) {
            int w = Math.min(18, n - k);
            SEGMENT.copy().srcWidth(w).dest(x + k, y, w, BAR_H).blit(g);
        }
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xff) + (((b >> 16) & 0xff) - ((a >> 16) & 0xff)) * t);
        int g = (int) Math.round(((a >> 8) & 0xff) + (((b >> 8) & 0xff) - ((a >> 8) & 0xff)) * t);
        int bl = (int) Math.round((a & 0xff) + ((b & 0xff) - (a & 0xff)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int x, int y) {
        int mx = x - leftPos, my = y - topPos;
        List<Component> lines = null;
        if (mx >= TX0 - 1 && mx <= TX1 + 1 && my >= TY0 - 2 && my <= TY1 + 6) {
            lines = List.of(Component.translatable("gui.ae2_gvt.fusion.temperature", menu.temperature),
                    Component.translatable("gui.ae2_gvt.fusion.equilibrium", menu.equilibrium),
                    Component.translatable("gui.ae2_gvt.fusion.need", menu.need));
        } else if (my >= BAR_Y - 1 && my <= BAR_Y + BAR_H && mx >= BAR_X[0] - 1 && mx <= BAR_X[0] + BAR_W) {
            lines = List.of(Component.translatable("gui.ae2_gvt.fusion.energy_in", menu.energyIn));
        } else if (my >= BAR_Y - 1 && my <= BAR_Y + BAR_H && mx >= BAR_X[1] - 1 && mx <= BAR_X[1] + BAR_W) {
            lines = List.of(Component.translatable("gui.ae2_gvt.fusion.energy_out", menu.energyOut));
        } else if (mx >= LAMP_X - 1 && mx <= LAMP_X + 5 && my >= LAMP_Y - 1 && my <= LAMP_Y + 5) {
            lines = List.of(Component.translatable("gui.ae2_gvt.fusion.status." + STATUS[menu.status()]));
        }
        if (lines != null) {
            drawTooltip(g, x, y, lines);
        } else {
            super.extractTooltip(g, x, y);
        }
    }
}
