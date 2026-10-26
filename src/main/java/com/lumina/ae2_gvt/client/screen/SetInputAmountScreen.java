package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AE2's amount dialog of a processing input slot (same layout and behaviour as AE2's
 * {@code SetProcessingPatternAmountScreen}) with the machine faces this material goes in through
 * under it, laid out as an unfolded cube around the north face: used when the machine access
 * interface is set to "specified" (any number; tried down, up, north, south, west, east; none = the
 * interface's own face). Positions from assets/ae2/screens/ae2_gvt_input_amount.json.
 */
public class SetInputAmountScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/input_faces.png");
    private static final Blitter OFF = Blitter.texture(TEXTURE).src(0, 176, 24, 14);
    private static final Blitter ON = Blitter.texture(TEXTURE).src(24, 176, 24, 14);
    private static final int NET_X = 52, NET_Y = 100, STEP_X = 26, STEP_Y = 15;

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;
    private final int inputIndex;
    private int faces;
    private final List<FaceToggle> toggles = new ArrayList<>();

    public SetInputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int inputIndex) {
        super(parent, "/screens/ae2_gvt_input_amount.json");
        this.currentStack = currentStack;
        this.slot = slot;
        this.inputIndex = inputIndex;
        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        ItemStack icon = getMenu().getHost().getMainMenuIcon();
        widgets.add("back", new TabButton(Icon.BACK, icon.getHoverName(), btn -> returnToParent()));
        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        amount.setLongValue(currentStack.amount());
        amount.setMaxValue(getMaxAmount());
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);
        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
        faces = getMenu().getInputFaces(inputIndex);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
        toggles.clear();
        // unfolded cube: up over north, then west, north, east, south, then down under north
        add(Direction.UP, 1, 0);
        add(Direction.WEST, 0, 1);
        add(Direction.NORTH, 1, 1);
        add(Direction.EAST, 2, 1);
        add(Direction.SOUTH, 3, 1);
        add(Direction.DOWN, 1, 2);
    }

    private void add(Direction d, int col, int row) {
        FaceToggle t = new FaceToggle(leftPos + NET_X + col * STEP_X, topPos + NET_Y + row * STEP_Y, d);
        toggles.add(t);
        addRenderableWidget(t);
    }

    private boolean isOn(Direction d) {
        return (faces & (1 << d.get3DDataValue())) != 0;
    }

    private void confirm() {
        amount.getLongValue().ifPresent(value -> {
            value = Longs.constrainToRange(value, 0, getMaxAmount());
            GenericStack stack = value <= 0 ? null : new GenericStack(currentStack.what(), value);
            PacketDistributor.sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index,
                    GenericStack.wrapInItemStack(stack)));
            if (stack != null) {
                getMenu().setInputFaces(inputIndex, faces);
            }
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }

    /** One face of the cube: click to toggle; green when the material goes in through it. */
    private final class FaceToggle extends AbstractWidget {
        private final Direction face;

        FaceToggle(int x, int y, Direction face) {
            super(x, y, 24, 14, Component.translatable("ae2_gvt.face." + face.getName()));
            this.face = face;
            setTooltip(Tooltip.create(Component.translatable("ae2_gvt.face." + face.getName())
                    .append("\n").append(Component.translatable("ae2_gvt.pattern.faces.order"))));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean on = isOn(face);
            (on ? ON : OFF).copy().dest(getX(), getY()).blit(guiGraphics);
            if (isHovered()) {
                guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x30FFFFFF);
            }
            var font = SetInputAmountScreen.this.font;
            Component label = Component.translatable("ae2_gvt.face.short." + face.getName().toLowerCase(Locale.ROOT));
            guiGraphics.drawString(font, label, getX() + (width - font.width(label)) / 2, getY() + 3,
                    on ? 0xFFFFFF : 0x404040, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            faces ^= 1 << face.get3DDataValue();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }
}
