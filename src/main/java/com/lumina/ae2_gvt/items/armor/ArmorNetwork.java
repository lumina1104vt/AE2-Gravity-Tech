package com.lumina.ae2_gvt.items.armor;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.util.Platform;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * The armor's way to an ME network: any piece is linked to a wireless access point as AE2 links its terminals (put the piece
 * into the access point's slot), and the network is reached as long as that access point is on (by config also the wearer within the
 * range of one of the network's access points). What the armor takes from the network costs the network power.
 */
public final class ArmorNetwork {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** AE2 links an item through this handler (the access point's GUI hands it the place of the access point). */
    public static final IGridLinkableHandler LINKABLE_HANDLER = new IGridLinkableHandler() {
        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof AntigravityArmorItem;
        }

        @Override
        public void link(ItemStack stack, GlobalPos pos) {
            stack.set(AEComponents.WIRELESS_LINK_TARGET, pos);
        }

        @Override
        public void unlink(ItemStack stack) {
            stack.remove(AEComponents.WIRELESS_LINK_TARGET);
        }
    };

    private ArmorNetwork() {
    }

    /** The network the wearer reaches now, or null. */
    public static @Nullable IGrid grid(ServerPlayer player) {
        var point = accessPoint(player);
        return point == null ? null : point.getGrid();
    }

    /** A wireless access point of the linked network that is on and has the wearer in range, or null. */
    public static @Nullable IWirelessAccessPoint accessPoint(ServerPlayer player) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof AntigravityArmorItem))
                continue;
            GlobalPos target = stack.get(AEComponents.WIRELESS_LINK_TARGET);
            if (target == null)
                continue;
            var level = player.server.getLevel(target.dimension());
            if (level == null)
                continue;
            if (!(Platform.getTickingBlockEntity(level, target.pos()) instanceof IWirelessAccessPoint linked))
                continue;
            IGrid grid = linked.getGrid();
            if (grid == null)
                continue;
            // the access point it is linked to is enough (the wearer need not be near it) unless the config asks for the range
            if (!com.lumina.ae2_gvt.config.Config.ARMOR_LINK_RANGE.get())
                return linked.isActive() ? linked : null;
            for (WirelessAccessPointBlockEntity point : grid.getMachines(WirelessAccessPointBlockEntity.class)) {
                if (inRange(point, player))
                    return point;
            }
        }
        return null;
    }

    private static boolean inRange(IWirelessAccessPoint point, ServerPlayer player) {
        if (!point.isActive())
            return false;
        var where = point.getLocation();
        if (where.getLevel() != player.level())
            return false;
        double range = point.getRange();
        double dx = where.getPos().getX() + 0.5 - player.getX(), dy = where.getPos().getY() + 0.5 - player.getY(),
                dz = where.getPos().getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    /** Takes AE from the network; false (and nothing taken) if it can't give that much. */
    public static boolean pay(IGrid grid, double ae) {
        var energy = grid.getEnergyService();
        if (energy.extractAEPower(ae, Actionable.SIMULATE, PowerMultiplier.CONFIG) < ae - 0.001)
            return false;
        energy.extractAEPower(ae, Actionable.MODULATE, PowerMultiplier.CONFIG);
        return true;
    }
}
