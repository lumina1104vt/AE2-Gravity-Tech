package com.lumina.ae2_gvt.client.screen;

import org.jetbrains.annotations.Nullable;

import com.lumina.ae2_gvt.client.SequencePattern;
import com.lumina.twinstar.algorithm.ae2.PatternInputGuiSlots;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * What the sequence pattern button asks first: the GUI slot of the machine the recipe's first slot goes into. The
 * number is kept for that machine; the dialog closes with the recipe screen behind it, back to the pattern terminal.
 */
public class SequenceStartScreen extends Screen {
    private static final int WIDTH = 176, HEIGHT = 92;
    private static final int PANEL = 0xFFCBCCD4, EDGE = 0xFF413F54, TEXT = 0xFF404040;

    private final @Nullable Screen parent;
    private final SequencePattern.Request request;
    private int start;
    private EditBox box;

    public SequenceStartScreen(@Nullable Screen parent, SequencePattern.Request request, int start) {
        super(Component.translatable("ae2_gvt.sequence_dialog.title"));
        this.parent = parent;
        this.request = request;
        this.start = Math.max(1, start);
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
        box = new EditBox(font, left + 60, top + 36, 56, 14, Component.empty());
        box.setMaxLength(2);
        box.setFilter(s -> s.chars().allMatch(Character::isDigit));
        box.setValue(String.valueOf(start));
        box.setTooltip(Tooltip.create(Component.translatable("ae2_gvt.sequence_start.hint")));
        addRenderableWidget(box);
        addRenderableWidget(Button.builder(Component.literal("-"), b -> move(-1)).bounds(left + 40, top + 35, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> move(1)).bounds(left + 120, top + 35, 16, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("ae2_gvt.sequence_dialog.confirm"), b -> confirm())
                .bounds(left + 16, top + 62, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ae2_gvt.sequence_dialog.cancel"), b -> onClose())
                .bounds(left + 90, top + 62, 70, 20).build());
        setInitialFocus(box);
    }

    private int typed() {
        try {
            return Math.max(1, Math.min(PatternInputGuiSlots.MAX, Integer.parseInt(box.getValue())));
        } catch (NumberFormatException e) {
            return start;
        }
    }

    private void move(int by) {
        start = Math.max(1, Math.min(PatternInputGuiSlots.MAX, typed() + by));
        box.setValue(String.valueOf(start));
    }

    private void confirm() {
        if (!SequencePattern.fill(request, typed())) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 257 || key == 335) {     // enter
            confirm();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) {
            parent.render(g, -1, -1, partialTick);    // the recipe screen stays behind it
        }
        int left = (width - WIDTH) / 2, top = (height - HEIGHT) / 2;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, EDGE);
        g.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        g.drawString(font, title, left + 8, top + 7, TEXT, false);
        Component machine = request.machine().isEmpty() ? Component.translatable("ae2_gvt.sequence_dialog.no_machine")
                : request.machine().getHoverName();
        g.drawString(font, Component.translatable("ae2_gvt.sequence_dialog.machine", machine), left + 8, top + 20, TEXT, false);
        g.drawString(font, Component.translatable("ae2_gvt.sequence_dialog.start"), left + 8, top + 39, TEXT, false);
        super.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }
}
