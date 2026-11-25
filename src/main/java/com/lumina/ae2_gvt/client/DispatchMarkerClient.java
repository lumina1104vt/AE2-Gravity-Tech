package com.lumina.ae2_gvt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The dispatch marker is drawn as the vanilla chest: the game own renderer of the chest item, asked to draw it for the marker. */
public final class DispatchMarkerClient {
    private static BlockEntityWithoutLevelRenderer renderer;

    private DispatchMarkerClient() {
    }

    /** The marker renderer (made when first asked for: the game is up by then). */
    public static BlockEntityWithoutLevelRenderer renderer() {
        if (renderer == null)
            renderer = new Chest();
        return renderer;
    }

    private static final class Chest extends BlockEntityWithoutLevelRenderer {
        private final ItemStack chest = new ItemStack(Items.CHEST);

        Chest() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer().renderByItem(chest, context, pose, buffer, light, overlay);
        }
    }
}
