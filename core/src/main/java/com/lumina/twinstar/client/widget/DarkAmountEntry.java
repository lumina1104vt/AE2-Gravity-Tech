package com.lumina.twinstar.client.widget;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import com.lumina.twinstar.util.AmountParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The amount entry of the twin-star order screen, laid out like AE2's (138x62: +1 +10 +100 +1000,
 * the field, -1 ... -1000; shift or ctrl for 1 16 32 64), in the dark style. The field takes any
 * amount up to the order limit, with unit suffixes (K M G ...) and scientific notation (1e10000).
 */
public class DarkAmountEntry implements ICompositeWidget {
    private static final long[] STEPS_1000 = {1, 10, 100, 1000};
    private static final long[] STEPS_64 = {1, 16, 32, 64};
    private static final int[][] BUTTONS = {{0, 22}, {28, 28}, {62, 32}, {100, 38}};    // x offset, width
    /** the field's frame inside the entry, between the button rows */
    private static final int FIELD_TOP = 22, FIELD_H = 18;

    private final Runnable onConfirm;
    private final Field field;
    private final List<DarkButton> buttons = new ArrayList<>(8);
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);
    private int amountPerUnit = 1;
    private boolean shiftSteps;

    public DarkAmountEntry(Runnable onConfirm) {
        this.onConfirm = onConfirm;
        this.field = new Field();
        field.setBordered(false);
        field.setMaxLength(AmountParser.MAX_EXPONENT + 64);     // every digit of the limit, plus separators
        field.setTextColor(DarkStyle.TEXT);
        field.setResponder(text -> validate());
        for (int i = 0; i < 8; i++) {
            int index = i;
            buttons.add(new DarkButton(Component.empty(), () -> addSteps(index)));
        }
        setStepLabels(false);
        field.setValue("1");
    }

    /** The field (for the screen's initial focus). */
    public EditBox field() {
        return field;
    }

    /** Base units per displayed unit (fluids: 1000 mB per B). */
    public void setAmountPerUnit(int amountPerUnit) {
        this.amountPerUnit = Math.max(1, amountPerUnit);
        validate();
    }

    public void setText(String text) {
        field.setValue(text);
        field.moveCursorToEnd(false);
        field.setHighlightPos(0);
    }

    /** The entered amount in base units; null if it is not a whole positive amount. */
    public @Nullable BigInteger exactValue() {
        BigDecimal v = AmountParser.parse(field.getValue());
        if (v == null)
            return null;
        BigDecimal base = v.multiply(BigDecimal.valueOf(amountPerUnit)).stripTrailingZeros();
        return base.scale() > 0 ? null : base.toBigInteger();
    }

    public boolean isValid() {
        BigInteger v = exactValue();
        return v != null && v.signum() > 0 && v.compareTo(AmountParser.MAX_EXTERNAL) <= 0;
    }

    private void validate() {
        field.setTextColor(isValid() ? DarkStyle.TEXT : DarkStyle.ERROR);
    }

    private long step(int index) {
        long s = (shiftSteps ? STEPS_64 : STEPS_1000)[index % 4];
        return index < 4 ? s : -s;
    }

    private void setStepLabels(boolean shift) {
        shiftSteps = shift;
        for (int i = 0; i < 8; i++) {
            long s = step(i);
            buttons.get(i).setMessage(Component.literal((s > 0 ? "+" : "") + s));
        }
    }

    /**
     * Adds a step in displayed units, as AE2's amount buttons do: kept between 1 and the order
     * limit, and from exactly 1 a step larger than 1 replaces it (1, +1000 gives 1000, not 1001).
     */
    private void addSteps(int index) {
        BigDecimal current = AmountParser.parse(field.getValue());
        if (current == null)
            current = BigDecimal.ZERO;
        long delta = step(index);
        BigDecimal next = current.add(BigDecimal.valueOf(delta));
        BigDecimal max = new BigDecimal(AmountParser.MAX);
        if (next.compareTo(BigDecimal.ONE) < 0)
            next = BigDecimal.ONE;
        else if (next.compareTo(max) > 0)
            next = max;
        else if (current.compareTo(BigDecimal.ONE) == 0 && delta > 1)
            next = next.subtract(BigDecimal.ONE);
        next = next.stripTrailingZeros();
        // the limit itself as 1e10000, not ten thousand zeros
        int exponent = -next.scale();
        setText(exponent >= 6 && next.precision() <= 20 ? next.unscaledValue() + "e" + exponent : next.toPlainString());
    }

    // ── ICompositeWidget ──

    @Override
    public void setPosition(Point position) {
        bounds = new Rect2i(position.getX(), position.getY(), bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public void setSize(int width, int height) {
        bounds = new Rect2i(bounds.getX(), bounds.getY(), width, height);
    }

    @Override
    public Rect2i getBounds() {
        return bounds;
    }

    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i screenBounds, AEBaseScreen<?> screen) {
        int left = screenBounds.getX() + bounds.getX();
        int top = screenBounds.getY() + bounds.getY();
        for (int i = 0; i < 8; i++) {
            DarkButton b = buttons.get(i);
            int[] slot = BUTTONS[i % 4];
            b.setRectangle(slot[1], 20, left + slot[0], top + (i < 4 ? 0 : 42));
            addWidget.accept(b);
        }
        // text inside the frame drawn by drawBackgroundLayer
        field.setRectangle(bounds.getWidth() - 10, 9, left + 5, top + FIELD_TOP + (FIELD_H - 8) / 2);
        addWidget.accept(field);
    }

    @Override
    public void updateBeforeRender() {
        boolean shift = Screen.hasShiftDown() || Screen.hasControlDown();
        if (shift != shiftSteps)
            setStepLabels(shift);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics g, Rect2i screenBounds, Point mouse) {
        int x = screenBounds.getX() + bounds.getX();
        int y = screenBounds.getY() + bounds.getY() + FIELD_TOP;
        int w = bounds.getWidth();
        g.fill(x, y, x + w, y + FIELD_H, field.isFocused() ? DarkStyle.HOVER_EDGE : DarkStyle.EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + FIELD_H - 1, DarkStyle.FIELD_FILL);
    }

    /** Enter confirms; while focused it keeps every key (the inventory key must not close the screen). */
    private final class Field extends EditBox {
        Field() {
            super(Minecraft.getInstance().font, 0, 0, 0, 0, Component.empty());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                onConfirm.run();
                return true;
            }
            if (super.keyPressed(keyCode, scanCode, modifiers))
                return true;
            return isFocused() && canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE;
        }
    }
}
