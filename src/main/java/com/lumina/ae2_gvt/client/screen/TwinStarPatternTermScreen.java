package com.lumina.ae2_gvt.client.screen;

import java.util.Map;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * AE2's pattern encoding screen with our processing panel drawn over AE2's (input grid moved right,
 * machine slot at its top-left) and the mode tabs hidden. Slot and widget positions come from
 * assets/ae2/screens/terminals/twin_star_pattern_terminal.json. The amount dialog of an output slot
 * also sets its probability ({@link SetOutputAmountScreen}, shown in the slot's corner); the one of
 * an input slot its faces ({@link SetInputAmountScreen}).
 */
public class TwinStarPatternTermScreen extends PatternEncodingTermScreen<TwinStarPatternTermMenu> {
    private static final Blitter PANEL = Blitter
            .texture(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/twin_star_pattern_panel.png"))
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

    /**
     * Pick-item click on an output slot: our dialog with the probability; on an input slot: ours with
     * the faces; anything else as AE2 does it.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.hoveredSlot;
        if (minecraft.options.keyPickItem.matchesMouse(button) && slot != null) {
            Slot[] inputs = menu.getProcessingInputSlots();
            for (int i = 0; i < inputs.length; i++) {
                if (inputs[i] != slot) {
                    continue;
                }
                GenericStack stack = GenericStack.fromItemStack(slot.getItem());
                if (stack == null) {
                    break;
                }
                switchToScreen(new SetInputAmountScreen(this, stack, slot, i));
                return true;
            }
            Slot[] outputs = menu.getProcessingOutputSlots();
            for (int i = 0; i < outputs.length; i++) {
                if (outputs[i] != slot) {
                    continue;
                }
                GenericStack stack = GenericStack.fromItemStack(slot.getItem());
                if (stack == null) {
                    break;
                }
                switchToScreen(new SetOutputAmountScreen(this, stack, slot, i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** An output that comes only with a probability shows it in the slot's top-left corner. */
    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        Slot[] outputs = menu.getProcessingOutputSlots();
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] != slot) {
                continue;
            }
            double c = menu.getOutputChance(i);
            if (c < 1 && slot.hasItem()) {
                String text = SetOutputAmountScreen.percent(c) + "%";
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(slot.x, slot.y, 300);
                pose.scale(0.5f, 0.5f, 1);
                guiGraphics.drawString(font, text, 0, 0, 0xFFE070, true);
                pose.popPose();
            }
            return;
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
        public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
            // same spot AE2's ProcessingEncodingPanel draws its background
            PANEL.dest(bounds.getX() + 8, bounds.getY() + bounds.getHeight() - 165).blit(guiGraphics);
        }
    }
}
