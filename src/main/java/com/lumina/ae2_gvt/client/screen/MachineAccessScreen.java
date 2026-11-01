package com.lumina.ae2_gvt.client.screen;

import java.util.List;
import java.util.Locale;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.menus.MachineAccessMenu;
import com.lumina.twinstar.service.TwinStarService;

import appeng.api.client.AEKeyRendering;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machine access interface screen: the machine it sits on and the one it also stands for, what that
 * machine is doing (the output, a bar of the run going in, a status line), and on the left toolbar
 * blocking and how materials go in. Positions from assets/ae2/screens/machine_access_interface.json.
 */
public class MachineAccessScreen extends AEBaseScreen<MachineAccessMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "textures/gui/machine_access_interface.png");
    private static final int TEXT = 0x404040, MUTED = 0x808080, BUSY = 0x2E7D32, ERROR = 0xC03030;
    private static final int BAR_X = 8, BAR_Y = 103, BAR_W = 160, BAR_H = 6;

    public MachineAccessScreen(MachineAccessMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        addToLeftToolbar(new BlockingButton());
        addToLeftToolbar(new InsertModeButton());
        addToLeftToolbar(new GroupButton());
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        drawMachines(guiGraphics);
        drawStatus(guiGraphics);
    }

    /** The block it sits on (read from the client's world) and the name of the one it also stands for. */
    private void drawMachines(GuiGraphics g) {
        var part = menu.getPart();
        Level level = part.getLevel();
        var host = part.getBlockEntity();
        Component attached = Component.translatable("gui.ae2_gvt.machine_access.no_block");
        if (level != null && host != null) {
            BlockPos pos = host.getBlockPos().relative(part.getSide());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                ItemStack icon = new ItemStack(state.getBlock());
                if (!icon.is(Items.AIR))
                    g.renderItem(icon, 8, 28);
                attached = state.getBlock().getName();
            }
        }
        g.drawString(font, fit(attached, 138), 30, 33, TEXT, false);
        ItemStack also = menu.getSlots(MachineAccessMenu.MACHINE).get(0).getItem();
        Component name = also.isEmpty() ? Component.translatable("gui.ae2_gvt.machine_access.not_set")
                : also.getHoverName();
        g.drawString(font, fit(name, 138), 30, 65, also.isEmpty() ? MUTED : TEXT, false);
    }

    private void drawStatus(GuiGraphics g) {
        TwinStarService.MachineState state = menu.state;
        boolean busy = state == TwinStarService.MachineState.FEEDING || state == TwinStarService.MachineState.WORKING;
        if (busy && menu.output != null) {
            AEKeyRendering.drawInGui(minecraft, g, 8, 84, menu.output.what());
            String count = state == TwinStarService.MachineState.FEEDING
                    ? Component.translatable("gui.ae2_gvt.machine_access.progress", menu.fed, menu.total).getString()
                    : Component.translatable("gui.ae2_gvt.machine_access.runs", menu.runsIn).getString();
            int countX = 168 - font.width(count);
            g.drawString(font, count, countX, 88, TEXT, false);
            g.drawString(font, fit(AEKeyRendering.getDisplayName(menu.output.what()), countX - 32), 28, 88, TEXT, false);
        }
        if (state == TwinStarService.MachineState.FEEDING && menu.total > 0) {
            int w = (int) Math.round(BAR_W * Math.min(1.0, (double) menu.fed / menu.total));
            g.fill(BAR_X, BAR_Y, BAR_X + w, BAR_Y + BAR_H, 0xFF4CAF50);
        }
        String key = switch (state) {
            case WORKING -> menu.chance ? "working_chance" : "working";
            default -> state.name().toLowerCase(Locale.ROOT);
        };
        int color = switch (state) {
            case INACTIVE, NO_HUB, NO_PATTERN -> ERROR;
            case FEEDING, WORKING -> BUSY;
            default -> MUTED;
        };
        g.drawString(font, fit(Component.translatable("gui.ae2_gvt.machine_access.state." + key), 160), 8, 113, color, false);
    }

    /** The text cut to {@code width} pixels, with an ellipsis when cut. */
    private String fit(Component text, int width) {
        String s = text.getString();
        if (font.width(s) <= width)
            return s;
        return font.plainSubstrByWidth(s, width - font.width("…")) + "…";
    }

    /** AE2's blocking mode icons. */
    private final class BlockingButton extends IconButton {
        BlockingButton() {
            super(btn -> menu.toggleBlocking());
        }

        @Override
        protected Icon getIcon() {
            return menu.blocking ? Icon.BLOCKING_MODE_YES : Icon.BLOCKING_MODE_NO;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.blocking"),
                    Component.translatable(menu.blocking ? "gui.ae2_gvt.machine_access.blocking_on"
                            : "gui.ae2_gvt.machine_access.blocking_off").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The pairing number on the button: a machine spread over several blocks. */
    private final class GroupButton extends IconButton {
        GroupButton() {
            super(btn -> menu.cycleGroup());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the number is drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            String number = menu.group == 0 ? "-" : Integer.toString(menu.group);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.drawString(font, number, getX() + (16 - font.width(number)) / 2, getY() + 5 + (isHovered() ? 1 : 0), 0xFFFFFFFF, true);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.group"),
                    Component.translatable(menu.group == 0 ? "gui.ae2_gvt.machine_access.group_off" : "gui.ae2_gvt.machine_access.group_on", menu.group)
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    /** Our own icons (16x16 at the right of the background texture): default, specified, any, output only. */
    private final class InsertModeButton extends IconButton {
        InsertModeButton() {
            super(btn -> menu.cycleInsertMode());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the icon is ours
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            int u = 176 + 16 * menu.getInsertMode().ordinal();
            Blitter.texture(TEXTURE).src(u, 0, 16, 16).dest(getX(), getY() + 1 + (isHovered() ? 1 : 0)).zOffset(3).blit(g);
        }

        @Override
        public List<Component> getTooltipMessage() {
            String mode = menu.getInsertMode().name().toLowerCase(Locale.ROOT);
            return List.of(Component.translatable("gui.ae2_gvt.machine_access.insert"),
                    Component.translatable("gui.ae2_gvt.machine_access.insert." + mode).withStyle(ChatFormatting.GRAY));
        }
    }
}
