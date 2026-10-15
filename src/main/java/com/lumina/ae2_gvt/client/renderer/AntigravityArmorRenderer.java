package com.lumina.ae2_gvt.client.renderer;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.armor.AntigravityArmorItem;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class AntigravityArmorRenderer<R extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<AntigravityArmorItem, R> {
    public AntigravityArmorRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "antigravity_armor")));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
