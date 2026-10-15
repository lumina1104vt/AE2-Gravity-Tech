package com.lumina.ae2_gvt.payload;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.menus.Menus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenArmorModulesPayload implements CustomPacketPayload {
    public static final OpenArmorModulesPayload INSTANCE = new OpenArmorModulesPayload();
    public static final Type<OpenArmorModulesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "open_armor_modules"));
    public static final StreamCodec<ByteBuf, OpenArmorModulesPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenArmorModulesPayload payload, IPayloadContext context) {
        Player player = context.player();
        for (ArmorType type : ArmorModules.PIECES) {
            if (ArmorModules.isPiece(player.getItemBySlot(type.getSlot()), type)) {
                MenuOpener.open(Menus.ARMOR_MODULE_MENU.get(), player, MenuLocators.forInventorySlot(type.getSlot().getIndex(36)));
                return;
            }
        }

        player.sendOverlayMessage(Component.translatable("gui.ae2_gvt.armor_modules.not_worn"));
    }
}
