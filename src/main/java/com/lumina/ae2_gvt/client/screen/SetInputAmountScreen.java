package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AE2's amount dialog of a processing input slot (same layout and behaviour as AE2's
 * {@code SetProcessingPatternAmountScreen}) with six face toggles in the strip under the title: the
 * machine faces this material goes in through when the machine access interface is set to
 * "specified" (any number; tried in this order; none = the interface's own face).
 */
public class SetInputAmountScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    private static final int FACES_X = 40, FACES_Y = 16, FACE_W = 20, FACE_H = 12;

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;
    private final int inputIndex;
    private int faces;
    private final List<Button> faceButtons = new ArrayList<>();

    public SetInputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int inputIndex) {
        super(parent, "/screens/set_processing_pattern_amount.json");
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
        faceButtons.clear();
        Direction[] all = Direction.values();
        for (int i = 0; i < all.length; i++) {
            Direction d = all[i];
            Button b = Button.builder(label(d), btn -> {
                faces ^= 1 << d.get3DDataValue();
                btn.setMessage(label(d));
            }).bounds(leftPos + FACES_X + i * FACE_W, topPos + FACES_Y, FACE_W - 1, FACE_H)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("ae2_gvt.face." + d.getName())))
                    .build();
            faceButtons.add(b);
            addRenderableWidget(b);
        }
    }

    private Component label(Direction d) {
        boolean on = (faces & (1 << d.get3DDataValue())) != 0;
        return Component.translatable("ae2_gvt.face.short." + d.getName().toLowerCase(Locale.ROOT))
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        guiGraphics.drawString(font, Component.translatable("ae2_gvt.pattern.faces"), 8, FACES_Y + 2, 0x404040, false);
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
}
