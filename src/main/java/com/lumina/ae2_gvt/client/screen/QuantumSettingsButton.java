package com.lumina.ae2_gvt.client.screen;

import java.util.List;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** On a quantum controller's network status screen: opens the settings page (the role and the frequency). */
public class QuantumSettingsButton extends IconButton {
    public QuantumSettingsButton(Runnable action) {
        super(btn -> action.run());
    }

    @Override
    protected Icon getIcon() {
        return Icon.COG;
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(Component.translatable("gui.ae2_gvt.quantum.settings"),
                Component.translatable("gui.ae2_gvt.quantum.settings_hint").withStyle(ChatFormatting.GRAY));
    }
}
