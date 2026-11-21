package com.lumina.ae2_gvt.payload;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.menus.Menus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: the key that opens the armor's module screen. */
public class OpenArmorModulesPayload {
    public static final OpenArmorModulesPayload INSTANCE = new OpenArmorModulesPayload();

    public void write(FriendlyByteBuf buf) {
    }

    public static OpenArmorModulesPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(OpenArmorModulesPayload payload, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;
            for (ArmorItem.Type type : ArmorModules.PIECES) {
                if (ArmorModules.isPiece(player.getItemBySlot(type.getSlot()), type)) {
                    MenuOpener.open(Menus.ARMOR_MODULE_MENU.get(), player, MenuLocators.forInventorySlot(type.getSlot().getIndex(36)));
                    return;
                }
            }
            player.displayClientMessage(Component.translatable("gui.ae2_gvt.armor_modules.not_worn"), true);
        });
        ctx.setPacketHandled(true);
    }
}
