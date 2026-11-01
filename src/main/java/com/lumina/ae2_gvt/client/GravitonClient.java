package com.lumina.ae2_gvt.client;

import java.util.List;
import java.util.function.Function;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;
import appeng.api.client.StorageCellModels;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.graviton.GravitonKey;
import com.lumina.ae2_gvt.graviton.GravitonKeyType;
import com.lumina.ae2_gvt.graviton.Gravitons;
import com.lumina.ae2_gvt.storage.FeEnergy;
import com.lumina.ae2_gvt.storage.FeKey;
import com.lumina.ae2_gvt.storage.FeKeyType;

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
            AEKeyRendering.register(GravitonKeyType.INSTANCE, GravitonKey.class, new Renderer<>(GravitonKey::displayStack));
            AEKeyRendering.register(FeKeyType.INSTANCE, FeKey.class, new Renderer<>(k -> new ItemStack(FeEnergy.ICON.get())));
            for (Gravitons.Tier t : Gravitons.Tier.values()) {
                StorageCellModels.registerModel(t.cell().get(),
                        ResourceLocation.fromNamespaceAndPath("ae2", "block/drive/cells/" + t.size + "_item_cell"));
            }
            StorageCellModels.registerModel(Gravitons.INFINITE_CELL.get(),
                    ResourceLocation.fromNamespaceAndPath("ae2", "block/drive/cells/256k_item_cell"));
            StorageCellModels.registerModel(com.lumina.ae2_gvt.items.Items.BLACK_HOLE_STORAGE_CELL.get(),
                    ResourceLocation.fromNamespaceAndPath("ae2", "block/drive/cells/256k_item_cell"));
        });
    }

    /** AE2's item handler, fed the key's display item. */
    private record Renderer<K extends AEKey>(Function<K, ItemStack> display) implements AEKeyRenderHandler<K> {
        private AEItemKey item(K key) {
            return AEItemKey.of(display.apply(key));
        }

        @Override
        public void drawInGui(Minecraft mc, GuiGraphics graphics, int x, int y, K key) {
            AEKeyRendering.drawInGui(mc, graphics, x, y, item(key));
        }

        @Override
        public void drawOnBlockFace(PoseStack pose, MultiBufferSource buffers, K key, float scale, int light, Level level) {
            AEKeyRendering.drawOnBlockFace(pose, buffers, item(key), scale, light, level);
        }

        @Override
        public Component getDisplayName(K key) {
            return key.getDisplayName();
        }

        @Override
        public List<Component> getTooltip(K key) {
            return List.of(key.getDisplayName());
        }
    }
}
