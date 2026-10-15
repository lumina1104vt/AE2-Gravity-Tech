package com.lumina.ae2_gvt.client.screen;

import java.util.Map;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTermMenu;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * AE2's pattern encoding screen with our processing panel drawn over AE2's (input grid moved right,
 * machine slot at its top-left) and the mode tabs hidden. Slot and widget positions come from
 * assets/ae2/screens/terminals/twin_star_pattern_terminal.json.
 */
public class TwinStarPatternTermScreen extends PatternEncodingTermScreen<TwinStarPatternTermMenu> {
    private static final Blitter PANEL = Blitter
            .texture(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/twin_star_pattern_panel.png"))
            .src(0, 0, 124, 66);

    public TwinStarPatternTermScreen(TwinStarPatternTermMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        // added after AE2's own panels, so it is drawn on top of AE2's processing panel
        this.widgets.add("twinStarPanel", new PanelBackground());
        hideModeTabs();
    }

    // AE2 keeps its mode tabs in a private map; there is no mode to switch here, so hide them
    private void hideModeTabs() {
        try {
            var field = PatternEncodingTermScreen.class.getDeclaredField("modeTabButtons");
            field.setAccessible(true);
            for (Object tab : ((Map<?, ?>) field.get(this)).values()) {
                ((TabButton) tab).visible = false;
            }
        } catch (ReflectiveOperationException e) {
            AE2GravityTech.LOGGER.warn("Could not hide the pattern terminal mode tabs", e);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        hideModeTabs();
    }

    private static final class PanelBackground implements ICompositeWidget {
        private int x, y;

        @Override
        public void setPosition(Point position) {
            x = position.getX();
            y = position.getY();
        }

        @Override
        public void setSize(int width, int height) {
        }

        @Override
        public Rect2i getBounds() {
            return new Rect2i(x, y, 124, 66);
        }

        @Override
        public void drawBackgroundLayer(GuiGraphicsExtractor guiGraphics, Rect2i bounds, Point mouse) {
            // same spot AE2's ProcessingEncodingPanel draws its background
            PANEL.dest(bounds.getX() + 8, bounds.getY() + bounds.getHeight() - 165).blit(guiGraphics);
        }
    }
}
