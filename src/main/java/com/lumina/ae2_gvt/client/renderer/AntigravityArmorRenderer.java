package com.lumina.ae2_gvt.client.renderer;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.armor.AntigravityArmorItem;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The anti-gravity armor on its wearer (GeckoLib 4): model geo/item/antigravity_armor.geo.json,
 * texture textures/item/antigravity_armor.png, glowing parts from antigravity_armor_glowmask.png.
 */
public class AntigravityArmorRenderer extends GeoArmorRenderer<AntigravityArmorItem> {
    public AntigravityArmorRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "antigravity_armor")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
