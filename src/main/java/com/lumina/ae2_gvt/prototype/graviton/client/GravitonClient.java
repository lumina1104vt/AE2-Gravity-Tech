package com.lumina.ae2_gvt.prototype.graviton.client;

import java.util.List;

import appeng.api.client.StorageCellModels;
import appeng.api.stacks.AEItemKey;
import appeng.client.api.AEKeyRenderer;
import appeng.client.api.AEKeyRendering;
import appeng.client.renderer.keytypes.ItemKeyRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InitializeClientRegistriesEvent;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.prototype.graviton.GravitonKey;
import com.lumina.ae2_gvt.prototype.graviton.GravitonKeyType;
import com.lumina.ae2_gvt.prototype.graviton.Gravitons;

/** Draws graviton keys in terminals (as their display items) and gives the cells AE2's drive models. */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class GravitonClient {
    private GravitonClient() {
    }

    @SubscribeEvent
    static void initRegistries(InitializeClientRegistriesEvent event) {
        AEKeyRendering.register(GravitonKeyType.INSTANCE, GravitonKey.class, new Renderer());
        for (Gravitons.Tier t : Gravitons.Tier.values()) {
            StorageCellModels.registerModel(t.cell().get(), Identifier.fromNamespaceAndPath("ae2", "block/drive_" + t.size + "_item_cell"));
        }
    }

    /** AE2's item renderer, fed the key's display item. */
    private static final class Renderer implements AEKeyRenderer<GravitonKey, ItemStackRenderState> {
        private ItemKeyRenderer items;

        private ItemKeyRenderer items() {
            if (items == null) {
                items = new ItemKeyRenderer();      // needs the item model resolver, so not at registration
            }
            return items;
        }

        private static AEItemKey item(GravitonKey key) {
            return AEItemKey.of(key.displayStack());
        }

        @Override
        public void drawInGui(Minecraft mc, GuiGraphicsExtractor graphics, int x, int y, GravitonKey key) {
            items().drawInGui(mc, graphics, x, y, item(key));
        }

        @Override
        public Class<ItemStackRenderState> stateClass() {
            return ItemStackRenderState.class;
        }

        @Override
        public ItemStackRenderState createState() {
            return new ItemStackRenderState();
        }

        @Override
        public void extract(ItemStackRenderState state, GravitonKey key, Level level, int seed) {
            items().extract(state, item(key), level, seed);
        }

        @Override
        public void submit(PoseStack pose, ItemStackRenderState state, SubmitNodeCollector collector, int light) {
            items().submit(pose, state, collector, light);
        }

        @Override
        public List<Component> getTooltip(GravitonKey key) {
            return List.of(key.getDisplayName());
        }
    }
}
