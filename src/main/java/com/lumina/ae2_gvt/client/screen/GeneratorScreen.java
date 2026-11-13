package com.lumina.ae2_gvt.client.screen;

import java.util.List;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity.Kind;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.menus.GeneratorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** A generator's screen: its name, what it makes, and what it burns (texts from assets/ae2/screens/generator_*.json). */
public class GeneratorScreen extends AEBaseScreen<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        if (menu.kind() == Kind.GRAVITON)
            addToLeftToolbar(new TakeButton());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent(TEXT_ID_DIALOG_TITLE, Component.translatable(switch (menu.kind()) {
            case CRYSTAL -> "block.ae2_gvt.crystal_generator";
            case GRAVITON -> "block.ae2_gvt.graviton_generator";
            case ACCRETION -> "block.ae2_gvt.accretion_generator";
        }));
        boolean running = menu.output > 0;
        setTextContent("output", running ? Component.translatable("gui.ae2_gvt.generator.output", String.format("%,d", menu.output))
                : Component.translatable("gui.ae2_gvt.generator.idle"));
        switch (menu.kind()) {
            case CRYSTAL -> setTextContent("line", Component.translatable("gui.ae2_gvt.generator.burn", String.format("%.1f", menu.burn / 20.0)));
            case ACCRETION -> setTextContent("line", Component.translatable("gui.ae2_gvt.generator.holes", menu.holes));
            default -> {
            }
        }
    }

    /**
     * What the graviton generator takes, as one of AE2's toolbar buttons (the left toolbar, the settings' place): a graviton, an
     * anti-graviton, or both small side by side; the name is in the tooltip.
     */
    private final class TakeButton extends IconButton {
        TakeButton() {
            super(btn -> menu.cycleTake());
        }

        @Override
        protected Icon getIcon() {
            return null;    // the background only; the items are drawn on it
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (!visible)
                return;
            int y = getY() + (isHovered() ? 1 : 0);
            ItemStack graviton = Gravitons.GRAVITON.get().getDefaultInstance(), anti = Gravitons.ANTIGRAVITON.get().getDefaultInstance();
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            if (menu.take == 0) {
                small(g, graviton, getX(), y);
                small(g, anti, getX() + 8, y + 8);
            } else {
                g.renderItem(menu.take == 1 ? graviton : anti, getX(), y);
            }
            g.pose().popPose();
        }

        /** An item at half the size in a corner of the button. */
        private void small(GuiGraphics g, ItemStack stack, int x, int y) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.ae2_gvt.generator.take"),
                    Component.translatable("gui.ae2_gvt.generator.take." + menu.take).withStyle(ChatFormatting.WHITE));
        }
    }
}
