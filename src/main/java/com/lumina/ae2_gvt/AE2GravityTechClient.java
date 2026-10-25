package com.lumina.ae2_gvt;

import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;
import com.lumina.ae2_gvt.prototype.terminal.TwinStarPatternTerminalPart;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AE2GravityTech.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public class AE2GravityTechClient {
    public AE2GravityTechClient(ModContainer container) {
        // AE2 19 bakes a part's models only if they are registered before its model loading (it reads
        // the @PartModels fields of each part class); the ae2/parts/*.json of newer AE2 is not read
        PartModels.registerModels(PartModelsHelper.createModels(MachineAccessInterface.class));
        PartModels.registerModels(PartModelsHelper.createModels(TwinStarPatternTerminalPart.class));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {

    }
}
