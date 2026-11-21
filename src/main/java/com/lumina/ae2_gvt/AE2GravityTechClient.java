package com.lumina.ae2_gvt;

import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;
import com.lumina.ae2_gvt.terminal.TwinStarPatternTerminalPart;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;

/** The client side's own start (called from AE2GravityTech on a client only). */
public final class AE2GravityTechClient {
    private AE2GravityTechClient() {
    }

    public static void init() {
        com.lumina.ae2_gvt.client.DevChecksClient.init();
        // a part's models are baked only if they are registered before AE2's model loading (it reads the @PartModels fields of each part class)
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, com.lumina.ae2_gvt.config.ClientConfig.SPEC);
        PartModels.registerModels(PartModelsHelper.createModels(MachineAccessInterface.class));
        PartModels.registerModels(PartModelsHelper.createModels(TwinStarPatternTerminalPart.class));
    }
}
