package com.lumina.ae2_gvt.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.client.TwinStarTerminalClient;
import com.lumina.twinstar.algorithm.ae2.PatternInputGuiSlots;
import com.lumina.twinstar.algorithm.ae2.PatternInputRoles;
import com.lumina.twinstar.algorithm.ae2.PatternInputTargets;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTermMenu;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AE2Button;
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
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AE2's amount dialog of a processing input slot (same layout and behaviour as AE2's
 * {@code SetProcessingPatternAmountScreen}) with the machine faces this material goes in through
 * under it, laid out as an unfolded cube around the north face: used when the machine access
 * interface is set to "specified" (any number; tried down, up, north, south, west, east; none = the
 * interface's own face); under that where it goes: into the machine, into its sub-machines, or placed or
 * dropped in front of its opening, so many blocks out. Positions from assets/ae2/screens/ae2_gvt_input_amount.json.
 */
public class SetInputAmountScreen extends AESubScreen<TwinStarPatternTermMenu, TwinStarPatternTermScreen> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/input_faces.png");
    private static final Blitter OFF = Blitter.texture(TEXTURE).src(0, 232, 24, 14);
    private static final Blitter ON = Blitter.texture(TEXTURE).src(24, 232, 24, 14);
    private static final int NET_X = 52, NET_Y = 118, STEP_X = 26, STEP_Y = 15;

    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Slot slot;
    private final int inputIndex;
    private int faces;
    /** where the material goes (PatternInputTargets) */
    private int target;
    private final boolean advanced;
    /** what the material is besides used up (PatternInputRoles) */
    private int role;
    /** the slot of the machine's GUI the material goes into (0 = the first that takes it) */
    private int guiSlot;
    private final @Nullable AE2Button guiLess, guiMore;
    private final @Nullable AE2Button targetButton, distanceLess, distanceMore, roleButton;
    private final List<FaceToggle> toggles = new ArrayList<>();

    public SetInputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int inputIndex) {
        this(parent, currentStack, slot, inputIndex, parent.getMenu().getInputFaces(inputIndex), parent.getMenu().getInputTarget(inputIndex),
                parent.getMenu().getInputRole(inputIndex), parent.getMenu().getInputGuiSlot(inputIndex), false);
    }

    /** {@code faces} and {@code target}: as set so far (the menu may not have them back from the server yet). */
    private SetInputAmountScreen(TwinStarPatternTermScreen parent, GenericStack currentStack, Slot slot, int inputIndex, int faces, int target,
                                 int role, int guiSlot, boolean advanced) {
        super(parent, advanced ? "/screens/ae2_gvt_input_amount_advanced.json" : "/screens/ae2_gvt_input_amount.json");
        this.advanced = advanced;
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
        this.faces = faces;
        this.target = target;
        this.role = role;
        this.guiSlot = guiSlot;
        widgets.addButton("advanced", Component.translatable(advanced ? "ae2_gvt.pattern.advanced.hide" : "ae2_gvt.pattern.advanced.show"),
                this::toggleAdvanced);
        if (advanced) {
            targetButton = widgets.addButton("target", Component.empty(), this::nextTarget);
            distanceLess = widgets.addButton("distanceLess", Component.literal("-"), () -> moveDistance(-1));
            distanceMore = widgets.addButton("distanceMore", Component.literal("+"), () -> moveDistance(1));
            showTarget();
            roleButton = widgets.addButton("role", Component.empty(), this::nextRole);
            showRole();
            guiLess = widgets.addButton("guiSlotLess", Component.literal("-"), () -> moveGuiSlot(-1));
            guiMore = widgets.addButton("guiSlotMore", Component.literal("+"), () -> moveGuiSlot(1));
            Tooltip guiHint = Tooltip.create(Component.translatable("ae2_gvt.pattern.gui_slot.hint"));
            guiLess.setTooltip(guiHint);
            guiMore.setTooltip(guiHint);
            IgnoreNbt.add(widgets, getMenu(), inputIndex, false);
        } else {
            targetButton = distanceLess = distanceMore = roleButton = guiLess = guiMore = null;
        }
    }

    /** The same dialog in its other layout, with the amount typed so far. */
    private void toggleAdvanced() {
        long value = amount.getLongValue().orElse(currentStack.amount());
        GenericStack stack = new GenericStack(currentStack.what(), Math.max(1, value));
        TwinStarPatternTermScreen parent = getParent();
        returnToParent();
        parent.openDialog(new SetInputAmountScreen(parent, stack, slot, inputIndex, faces, target, role, guiSlot, !advanced));
    }

    /** The GUI slot up or down; 0 (the first that takes it) is the bottom. */
    private void moveGuiSlot(int by) {
        guiSlot = Math.max(0, Math.min(PatternInputGuiSlots.MAX, guiSlot + by));
        getMenu().setInputGuiSlot(inputIndex, guiSlot);
    }

    /** The switch: used up, or a catalyst. */
    private void nextRole() {
        role = PatternInputRoles.next(role);
        getMenu().setInputRole(inputIndex, role);
        showRole();
    }

    private void showRole() {
        if (roleButton == null) {
            return;
        }
        String name = role == PatternInputRoles.CATALYST ? "catalyst" : "normal";
        roleButton.setMessage(Component.translatable("ae2_gvt.pattern.role." + name));
        roleButton.setTooltip(Tooltip.create(Component.translatable("ae2_gvt.pattern.role.hint." + name)));
    }

    /** The next place: by default, machine, sub-machines, placed in front, dropped in front. */
    private void nextTarget() {
        int kind = PatternInputTargets.next(PatternInputTargets.kind(target));
        target = PatternInputTargets.of(kind, PatternInputTargets.distance(target));
        sendTarget();
    }

    private void moveDistance(int by) {
        if (!PatternInputTargets.isFront(target)) {
            return;
        }
        int d = Math.max(1, Math.min(PatternInputTargets.MAX_DISTANCE, PatternInputTargets.distance(target) + by));
        target = PatternInputTargets.of(PatternInputTargets.kind(target), d);
        sendTarget();
    }

    /** Kept at once, like the faces: leaving by the back tab or Esc must not lose it. */
    private void sendTarget() {
        getMenu().setInputTarget(inputIndex, target);
        showTarget();
    }

    private void showTarget() {
        if (targetButton == null || distanceLess == null || distanceMore == null) {
            return;
        }
        boolean subs = getMenu().getSubMachineSlots().stream().anyMatch(Slot::hasItem);
        // the first material is the first filled input slot, as the pattern lists them
        int firstFilled = -1;
        Slot[] ins = getMenu().getProcessingInputSlots();
        for (int i = 0; i < ins.length && firstFilled < 0; i++) {
            if (ins[i].hasItem()) {
                firstFilled = i;
            }
        }
        targetButton.setMessage(TwinStarTerminalClient.Game.targetName(target, subs, firstFilled == inputIndex));
        targetButton.setTooltip(Tooltip.create(Component.translatable("ae2_gvt.pattern.target.hint."
                + switch (PatternInputTargets.kind(target)) {
                    case PatternInputTargets.DEFAULT -> "default";
                    case PatternInputTargets.SUB -> "sub";
                    case PatternInputTargets.FRONT_BLOCK, PatternInputTargets.FRONT_DROP -> "front";
                    default -> "machine";
                })));
        boolean front = PatternInputTargets.isFront(target);
        distanceLess.visible = front;
        distanceMore.visible = front;
    }

    /** The distance between its buttons, for the front targets. */
    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        if (advanced) {
            Component g = Component.literal("GUI");
            guiGraphics.drawString(font, g, 8, 121, 0x404040, false);
            String n = guiSlot == 0 ? "-" : String.valueOf(guiSlot);
            guiGraphics.drawString(font, n, 51 - font.width(n) / 2, 121, 0x404040, false);
        }
        if (advanced && PatternInputTargets.isFront(target)) {
            Component d = Component.translatable("ae2_gvt.pattern.target.distance", PatternInputTargets.distance(target));
            guiGraphics.drawString(font, d, 82 - font.width(d) / 2, 188, 0x404040, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
        toggles.clear();
        if (!advanced) {
            return;
        }
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
                getMenu().setInputTarget(inputIndex, target);
                getMenu().setInputRole(inputIndex, role);
                getMenu().setInputGuiSlot(inputIndex, guiSlot);
            }
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }

    /** One face of the cube: click to toggle; lit when the material goes in through it. */
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
            // kept at once: leaving by the back tab or Esc must not lose it, as it did when only Set sent it
            getMenu().setInputFaces(inputIndex, faces);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }
}
