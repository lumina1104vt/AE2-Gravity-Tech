package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;
import com.lumina.ae2_gvt.terminal.TwinStarTerminal;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * AE2's pattern encoding screen with our processing panel drawn over AE2's (input grid moved right,
 * machine slot at its top-left) and the mode tabs hidden. Slot and widget positions come from
 * assets/ae2/screens/terminals/twin_star_pattern_terminal.json. The amount dialog of an output slot
 * also sets its probability ({@link SetOutputAmountScreen}, shown in the slot's corner); the one of
 * an input slot its faces ({@link SetInputAmountScreen}); the one of the machine slot, like the field
 * under it, opens the sub-machines ({@link SubMachineScreen}).
 */
public class TwinStarPatternTermScreen extends PatternEncodingTermScreen<TwinStarPatternTermMenu> {
    private static final Blitter PANEL = Blitter
            .texture(new ResourceLocation(AE2GravityTech.MODID, "textures/gui/twin_star_pattern_panel.png"))
            .src(0, 0, 124, 66);

    public TwinStarPatternTermScreen(TwinStarPatternTermMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        // added after AE2's own panels, so it is drawn on top of AE2's processing panel
        this.widgets.add("twinStarPanel", new PanelBackground());
        this.widgets.add("subMachines", new SubMachineButton());
        hideModeTabs();
    }

    /** For a dialog that swaps itself for its other layout. */
    void openDialog(appeng.client.gui.AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> dialog) {
        switchToScreen(dialog);
    }

    private void openSubMachines() {
        switchToScreen(new SubMachineScreen(this));
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
        if (minecraft.options.keyPickItem.matchesMouse(button) && slot == menu.getMachineSlot()) {
            openSubMachines();
            return true;
        }
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
        if (slot == menu.getToolSlot() && !slot.hasItem()) {
            // an empty tool slot: a faint hammer
            int x = slot.x + 3, y = slot.y + 2;
            guiGraphics.fill(x, y, x + 10, y + 4, 0xFF8B8BA6);
            guiGraphics.fill(x + 4, y + 4, x + 6, y + 12, 0xFF8B8BA6);
            return;
        }
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
        // they have their own dialog (MEStorageScreen's init is final, so here)
        setSlotsHidden(TwinStarTerminal.SUB_MACHINE_SLOTS, true);
    }

    /**
     * Under the machine slot: the first sub-machine (and how many more), in a slot frame like the
     * machine's. A click opens {@link SubMachineScreen}.
     */
    private final class SubMachineButton extends AbstractWidget implements ITooltip {
        private static final Blitter FRAME = Blitter
                .texture(new ResourceLocation(AE2GravityTech.MODID, "textures/gui/twin_star_pattern_panel.png"))
                .src(3, 6, 18, 18);

        SubMachineButton() {
            super(0, 0, 18, 18, Component.translatable("ae2_gvt.sub_machines.title"));
        }

        private List<ItemStack> subs() {
            List<ItemStack> out = new ArrayList<>();
            for (Slot s : menu.getSubMachineSlots()) {
                if (s.hasItem()) {
                    out.add(s.getItem());
                }
            }
            return out;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            FRAME.copy().dest(getX(), getY()).blit(g);
            List<ItemStack> subs = subs();
            if (subs.isEmpty()) {
                // an empty frame with a faint plus: there is something to set here
                int cx = getX() + 9, cy = getY() + 9;
                // two pixels thick and an even length: the frame is 18 wide, so one pixel could not be in the middle
                g.fill(cx - 4, cy - 1, cx + 4, cy + 1, 0xFF8B8BA6);
                g.fill(cx - 1, cy - 4, cx + 1, cy + 4, 0xFF8B8BA6);
            } else {
                g.renderItem(subs.get(0), getX() + 1, getY() + 1);
                if (subs.size() > 1) {
                    String more = "+" + (subs.size() - 1);
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(0, 0, 200);
                    g.drawString(font, more, getX() + 18 - font.width(more), getY() + 10, 0xFFFFFF, true);
                    pose.popPose();
                }
            }
            if (isHovered()) {
                g.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0x80FFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            openSubMachines();
        }

        @Override
        public List<Component> getTooltipMessage() {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("ae2_gvt.sub_machines.title"));
            List<ItemStack> subs = subs();
            if (subs.isEmpty()) {
                lines.add(Component.translatable("ae2_gvt.sub_machines.none").withStyle(ChatFormatting.GRAY));
            }
            for (int i = 0; i < subs.size(); i++) {
                lines.add(Component.literal((i + 1) + ". ").append(subs.get(i).getHoverName()).withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("ae2_gvt.sub_machines.click").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), width, height);
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return visible;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
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
