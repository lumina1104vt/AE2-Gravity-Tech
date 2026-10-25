package com.lumina.ae2_gvt.client.model;

import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.StructureFrameBlock;

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
 * Formed structure frame, connected 8 ways ({@link ConnectedQuarterModel}): tiles from
 * structure_frame_formed (tools/frame_textures.py, tools/ctm_sheet.py), 4 grain variants. Faces
 * between two frames are culled as usual. Used by models/block/structure_frame_formed.json.
 */
public final class StructureFrameModel extends ConnectedQuarterModel {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "structure_frame_formed");
    private static final Material SHEET = new Material(InventoryMenu.BLOCK_ATLAS,
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "block/structure_frame_formed"));

    private StructureFrameModel(TextureAtlasSprite sheet) {
        super(sheet, RenderType.solid());
    }

    /**
     * The face continues onto the block at {@code pos}: it is a formed frame and its own face on the
     * same side is showing (not buried under another frame, as on a stepped surface).
     */
    @Override
    protected boolean connects(BlockAndTintGetter level, BlockPos pos, Direction side) {
        return StructureFrameBlock.isFormedFrame(level.getBlockState(pos))
                && !StructureFrameBlock.isFormedFrame(level.getBlockState(pos.relative(side)));
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                               ModelState state, ItemOverrides overrides) {
            return new StructureFrameModel(sprites.apply(SHEET));
        }
    }

    public static final IGeometryLoader<Geometry> LOADER = (JsonObject json, JsonDeserializationContext ctx) -> new Geometry();
}
