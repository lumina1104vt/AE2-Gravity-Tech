package com.lumina.ae2_gvt.mixins;

import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.networktool.NetworkStatusMenu;
import com.lumina.ae2_gvt.menus.Menus;
import com.lumina.ae2_gvt.quantum.QuantumChannels;
import com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity;
import com.lumina.ae2_gvt.quantum.QuantumStatusAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE2's network status screen is the main screen of a controller. For a quantum controller it also tells how many channels are
 * left, and has a way to its settings page.
 */
@Mixin(value = NetworkStatusMenu.class, remap = false)
public abstract class NetworkStatusMenuMixin extends AEBaseMenu implements QuantumStatusAccess {
    @Unique
    private static final String ae2_gvt$OPEN_SETTINGS = "openQuantumSettings";

    @Unique
    @GuiSync(90)
    public int ae2_gvt$left = QuantumChannels.UNKNOWN;

    protected NetworkStatusMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lappeng/blockentity/networking/ControllerBlockEntity;)V", at = @At("TAIL"))
    private void ae2_gvt$register(int id, Inventory inv, ControllerBlockEntity host, CallbackInfo ci) {
        if (host instanceof QuantumControllerBlockEntity)
            registerClientAction(ae2_gvt$OPEN_SETTINGS, this::ae2_gvt$openSettings);
    }

    // vanilla's method: named here, SRG (m_38946_) in a production AE2 (this mixin does not remap AE2's class)
    @Inject(method = {"broadcastChanges", "m_38946_"}, at = @At("HEAD"))
    private void ae2_gvt$channels(CallbackInfo ci) {
        if (isServerSide() && getTarget() instanceof QuantumControllerBlockEntity be) {
            var grid = be.getMainNode().getGrid();
            ae2_gvt$left = grid == null ? QuantumChannels.UNKNOWN : QuantumChannels.left(grid);
        }
    }

    @Override
    public int ae2_gvt$channelsLeft() {
        return getTarget() instanceof QuantumControllerBlockEntity ? ae2_gvt$left : QuantumChannels.UNKNOWN;
    }

    @Override
    public void ae2_gvt$openSettings() {
        if (!(getTarget() instanceof QuantumControllerBlockEntity be))
            return;
        if (isClientSide())
            sendClientAction(ae2_gvt$OPEN_SETTINGS);
        else
            MenuOpener.open(Menus.QUANTUM_CONTROLLER.get(), getPlayer(), MenuLocators.forBlockEntity(be));
    }
}
