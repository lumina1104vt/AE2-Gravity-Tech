package com.lumina.ae2_gvt.client.model;

import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.lumina.ae2_gvt.AE2GravityTech;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

/**
 * A block as the formed hexagram hub draws it: joined to its neighbours of the same block
 * ({@link ConnectedQuarterModel}, one 17-tile sheet), so the crystal wedges and seams and the machine
 * frames of the floor and ceiling each read as one surface. Only a look - the world holds the real blocks,
 * and those are what it joins to. Model JSON:
 * {"loader": "ae2_gvt:joined_look", "sheet": "ae2_gvt:block/gravity_block_joined", "block": "ae2_gvt:gravity_block"}
 * (sheets from tools/crystal_joined_textures.py and tools/hub_structure_textures.py).
 */
public final class JoinedLookModel extends ConnectedQuarterModel {
    public static final ResourceLocation ID = new ResourceLocation(AE2GravityTech.MODID, "joined_look");

    private final Block block;

    private JoinedLookModel(TextureAtlasSprite sheet, Block block) {
        super(sheet, RenderType.solid(), 1);
        this.block = block;
    }

    @Override
    protected boolean connects(BlockAndTintGetter level, BlockPos pos, Direction side) {
        return level.getBlockState(pos).is(block) && !level.getBlockState(pos.relative(side)).is(block);
    }

    public record Geometry(ResourceLocation sheet, ResourceLocation block) implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                               ModelState state, ItemOverrides overrides, ResourceLocation modelLocation) {
            return new JoinedLookModel(sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, sheet)), BuiltInRegistries.BLOCK.get(block));
        }
    }

    public static final IGeometryLoader<Geometry> LOADER = (JsonObject json, JsonDeserializationContext ctx) -> new Geometry(
            new ResourceLocation(json.get("sheet").getAsString()), new ResourceLocation(json.get("block").getAsString()));
}
