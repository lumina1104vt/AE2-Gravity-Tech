package com.lumina.ae2_gvt.client.model;

import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.ContainmentGlassBlock;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * Containment glass, connected 8 ways like the structure frame ({@link ConnectedQuarterModel}):
 * AE2's quartz glass with our frame only on the open sides, so a wall of it reads as one pane.
 * Tiles from containment_glass_tiles (tools/fusion_textures.py), AE2's four glint variants. Faces
 * between two glass blocks are skipped by the block itself. Used by
 * models/block/containment_glass_connected.json.
 */
public final class ContainmentGlassModel extends ConnectedQuarterModel {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "containment_glass");
    private static final Material SHEET = new Material(InventoryMenu.BLOCK_ATLAS,
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "block/containment_glass_tiles"));

    private ContainmentGlassModel(TextureAtlasSprite sheet) {
        super(sheet, RenderType.translucent());
    }

    /** The face continues onto the block at {@code pos}: it is glass too, and its own face on that side shows. */
    @Override
    protected boolean connects(BlockAndTintGetter level, BlockPos pos, Direction side) {
        return level.getBlockState(pos).getBlock() instanceof ContainmentGlassBlock
                && !(level.getBlockState(pos.relative(side)).getBlock() instanceof ContainmentGlassBlock);
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                               ModelState state, ItemOverrides overrides) {
            return new ContainmentGlassModel(sprites.apply(SHEET));
        }
    }

    public static final IGeometryLoader<Geometry> LOADER = (JsonObject json, JsonDeserializationContext ctx) -> new Geometry();
}
