package com.lumina.ae2_gvt.client;

import java.util.List;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;
import appeng.api.client.StorageCellModels;
import appeng.api.stacks.AEItemKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.graviton.GravitonKey;
import com.lumina.ae2_gvt.prototype.graviton.GravitonKeyType;
import com.lumina.ae2_gvt.prototype.graviton.Gravitons;

/**
 * Draws graviton keys in terminals and on storage monitors (as their display items, through AE2's
 * own item handler) and gives the cells AE2's drive models. Set up before the first model bake, so
 * the drive picks the cell models up.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class GravitonClient {
    private GravitonClient() {
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AEKeyRendering.register(GravitonKeyType.INSTANCE, GravitonKey.class, new Renderer());
            for (Gravitons.Tier t : Gravitons.Tier.values()) {
                StorageCellModels.registerModel(t.cell().get(),
                        ResourceLocation.fromNamespaceAndPath("ae2", "block/drive/cells/" + t.size + "_item_cell"));
            }
        });
    }

    /** AE2's item handler, fed the key's display item. */
    private static final class Renderer implements AEKeyRenderHandler<GravitonKey> {
        private static AEItemKey item(GravitonKey key) {
            return AEItemKey.of(key.displayStack());
        }

        @Override
        public void drawInGui(Minecraft mc, GuiGraphics graphics, int x, int y, GravitonKey key) {
            AEKeyRendering.drawInGui(mc, graphics, x, y, item(key));
        }

        @Override
        public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, GravitonKey key, float scale, int light, Level level) {
            AEKeyRendering.drawOnBlockFace(pose, buffers, item(key), scale, light, level);
        }

        @Override
        public Component getDisplayName(GravitonKey key) {
            return key.getDisplayName();
        }

        @Override
        public List<Component> getTooltip(GravitonKey key) {
            return List.of(key.getDisplayName());
        }
    }
}
