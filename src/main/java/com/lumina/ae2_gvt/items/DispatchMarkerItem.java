package com.lumina.ae2_gvt.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.lumina.ae2_gvt.client.DispatchMarkerClient.renderer();
            }
        });
    }

    /** A marker made from a product is called by that product: that is what it stands for. */
    @Override
    public Component getName(ItemStack stack) {
        appeng.api.stacks.AEKey origin = DispatchMarker.originOf(stack);
        return origin != null ? origin.getDisplayName() : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appeng.api.stacks.AEKey origin = DispatchMarker.originOf(stack);
        if (origin instanceof appeng.api.stacks.AEItemKey item) {
            // what it stands for shows as itself: the product's own tooltip (its name is already the marker's)
            List<Component> lines = item.toStack().getTooltipLines(null, flag);
            tooltip.addAll(lines.subList(Math.min(1, lines.size()), lines.size()));
        } else if (origin == null) {
            tooltip.add(Component.translatable("item.ae2_gvt.dispatch_marker.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
