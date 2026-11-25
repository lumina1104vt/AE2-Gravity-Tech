package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** The dispatch marker is drawn as the vanilla chest: the game own renderer of the chest item, asked to draw it for the marker. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class DispatchMarkerClient {
    private DispatchMarkerClient() {
    }

    /** The registries of the world the client is in, or null outside one. */
    public static net.minecraft.core.HolderLookup.@org.jetbrains.annotations.Nullable Provider registries() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }

    @SubscribeEvent
    static void extensions(RegisterClientExtensionsEvent event) {
        BlockEntityWithoutLevelRenderer renderer = new Chest();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, Items.DISPATCH_MARKER.get());
    }

    private static final class Chest extends BlockEntityWithoutLevelRenderer {
        private final ItemStack chest = new ItemStack(net.minecraft.world.item.Items.CHEST);

        Chest() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer().renderByItem(chest, context, pose, buffer, light, overlay);
        }
    }
}
