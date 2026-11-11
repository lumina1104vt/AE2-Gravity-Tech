package com.lumina.ae2_gvt.mixins;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.networktool.NetworkStatusScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.networktool.NetworkStatusMenu;
import com.lumina.ae2_gvt.client.screen.QuantumSettingsButton;
import com.lumina.ae2_gvt.quantum.QuantumChannels;
import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;
import com.lumina.ae2_gvt.quantum.QuantumStatusAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A quantum controller's network status: a button to its settings, and how many channels are left. */
@Mixin(NetworkStatusScreen.class)
public abstract class NetworkStatusScreenMixin extends AEBaseScreen<NetworkStatusMenu> {
    protected NetworkStatusScreenMixin(NetworkStatusMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2_gvt$button(NetworkStatusMenu menu, Inventory inv, Component title, ScreenStyle style, CallbackInfo ci) {
        if (menu.getTarget() instanceof QuantumControllerBlockEntity)
            addToLeftToolbar(new QuantumSettingsButton(() -> ((QuantumStatusAccess) menu).ae2_gvt$openSettings()));
    }

    @Inject(method = "drawFG", at = @At("TAIL"))
    private void ae2_gvt$channelsLeft(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        if (!(menu.getTarget() instanceof QuantumControllerBlockEntity))
            return;
        int left = ((QuantumStatusAccess) menu).ae2_gvt$channelsLeft();
        if (left == QuantumChannels.UNKNOWN)
            return;
        Component text = left == QuantumChannels.INFINITE ? Component.translatable("gui.ae2_gvt.quantum.channels_infinite")
                : Component.translatable("gui.ae2_gvt.quantum.channels_left", left);
        g.drawString(font, text, 187 - font.width(text), 6, 0x404040, false);
    }
}
