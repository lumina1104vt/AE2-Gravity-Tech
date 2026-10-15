package com.lumina.ae2_gvt.prototype.hole.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

/**
 * A copy of the world as drawn so far this frame, for the holes' gravitational lensing to bend
 * (a shader can't read the target it is drawing into). Registered with the texture manager under
 * {@link #ID} so a RenderType can bind it; resized with the window. Also used by the graviton
 * collector's space distortion.
 */
public final class SceneSnapshot extends AbstractTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "hole_scene");
    private static SceneSnapshot instance;

    private SceneSnapshot() {
    }

    /** Copies the main target's colour into the snapshot. */
    public static void capture(RenderTarget main) {
        if (instance == null) {
            instance = new SceneSnapshot();
            Minecraft.getInstance().getTextureManager().register(ID, instance);
        }
        instance.copyFrom(main);
    }

    private void copyFrom(RenderTarget main) {
        GpuTexture src = main.getColorTexture();
        if (src == null) {
            return;
        }
        int w = src.getWidth(0), h = src.getHeight(0);
        GpuDevice device = RenderSystem.getDevice();
        if (texture == null || texture.getWidth(0) != w || texture.getHeight(0) != h) {
            if (textureView != null) {
                textureView.close();
            }
            if (texture != null) {
                texture.close();
            }
            texture = device.createTexture(() -> "ae2_gvt hole scene", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    src.getFormat(), w, h, 1, 1);
            textureView = device.createTextureView(texture);
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
        device.createCommandEncoder().copyTextureToTexture(src, texture, 0, 0, 0, 0, 0, w, h);
    }
}
