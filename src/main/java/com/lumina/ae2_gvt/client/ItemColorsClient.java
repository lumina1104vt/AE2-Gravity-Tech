package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.prototype.graviton.Gravitons;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarTerminal;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import appeng.items.storage.BasicStorageCell;
import net.minecraft.client.color.item.ItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Item tints, which 1.21.1 takes from code rather than the item model: the graviton cells' status
 * LED (AE2's own cell colour) and the fluix tones on our cable part items (as AE2 colours its
 * uncoloured parts). Made opaque like AE2 does, see {@link #opaque}.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class ItemColorsClient {
    private ItemColorsClient() {
    }

    @SubscribeEvent
    static void register(RegisterColorHandlersEvent.Item event) {
        for (Gravitons.Tier t : Gravitons.Tier.values()) {
            event.register(opaque(BasicStorageCell::getColor), t.cell().get());
        }
        event.register(opaque(new StaticItemColor(AEColor.TRANSPARENT)), Items.MACHINE_ACCESS_INTERFACE.get(), TwinStarTerminal.ITEM.get());
    }

    /**
     * 1.21.1 reads an item tint as ARGB; AE2's colours are plain RGB (alpha 0), so they would draw the
     * tinted layers fully transparent. AE2 wraps its own registrations the same way.
     */
    private static ItemColor opaque(ItemColor color) {
        return (stack, tintIndex) -> 0xFF000000 | color.getColor(stack, tintIndex);
    }
}
