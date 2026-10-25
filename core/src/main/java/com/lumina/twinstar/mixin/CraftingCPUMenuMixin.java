package com.lumina.twinstar.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.core.network.clientbound.CraftingStatusPacket;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import com.lumina.twinstar.cpu.TwinStarCpu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(CraftingCPUMenu.class)
public abstract class CraftingCPUMenuMixin extends AEBaseMenu {
    @Unique
    private final Map<AEKey, Long> twinstar$serials = new HashMap<>();
    @Unique
    private long twinstar$nextSerial;

    @Unique
    private TwinStarCpu twinstar$cpu;

    public CraftingCPUMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "setCPU", at = @At("HEAD"))
    private void twinstar$setCPU(ICraftingCPU cpu, CallbackInfo ci) {
        twinstar$serials.clear();
        if (cpu instanceof TwinStarCpu c) {
            twinstar$cpu = c;
        } else {
            twinstar$cpu = null;
        }
    }

    @Inject(method = "cancelCrafting", at = @At("HEAD"), cancellable = true)
    private void twinstar$cancelCrafting(CallbackInfo ci) {
        if (isServerSide() && twinstar$cpu != null) {
            twinstar$cpu.cancelJob();
            ci.cancel();
        }
    }

    /** Pause button: the job keeps its capacity and still receives returns, but nothing runs. */
    @Inject(method = "toggleScheduling", at = @At("HEAD"), cancellable = true)
    private void twinstar$toggleScheduling(CallbackInfo ci) {
        if (isServerSide() && twinstar$cpu != null) {
            twinstar$cpu.toggleSuspended();
            ci.cancel();
        }
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void twinstar$broadcastChanges(CallbackInfo ci) {
        if (isServerSide() && twinstar$cpu != null) {
            var status = twinstar$cpu.createStatus(key -> twinstar$serials.computeIfAbsent(key, k -> twinstar$nextSerial++));
            sendPacketToClient(new CraftingStatusPacket(containerId, status));
        }
    }
}
