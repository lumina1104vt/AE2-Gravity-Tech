package com.lumina.ae2_gvt.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The dispatch marker item: a stack of it in a player's hands (a command, a leak) vanishes at once. */
public class DispatchMarkerItem extends Item {
    public DispatchMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player)
            stack.setCount(0);
    }

    /** A marker made from a product is called by that product: that is what it stands for. */
    @Override
    public Component getName(ItemStack stack) {
        net.minecraft.core.HolderLookup.Provider registries = DispatchMarker.registries();
        appeng.api.stacks.AEKey origin = registries == null ? null : DispatchMarker.originOf(stack, registries);
        return origin != null ? origin.getDisplayName() : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appeng.api.stacks.AEKey origin = context.registries() == null ? null : DispatchMarker.originOf(stack, context.registries());
        if (origin instanceof appeng.api.stacks.AEItemKey item) {
            // what it stands for shows as itself: the product's own description. Only that: the id, the mod name, the tags and
            // the rest that other mods add to every tooltip are added to the marker's own tooltip by them, once
            ItemStack product = item.toStack();
            product.getItem().appendHoverText(product, context, tooltip, flag);
        } else if (origin == null) {
            tooltip.add(Component.translatable("item.ae2_gvt.dispatch_marker.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
