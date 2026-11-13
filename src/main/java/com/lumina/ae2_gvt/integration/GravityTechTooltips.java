package com.lumina.ae2_gvt.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity;
import com.lumina.ae2_gvt.blocks.FusionController;
import com.lumina.ae2_gvt.blocks.GeneratorBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What Jade, WTHIT and the like show of our generators and the fusion controller besides what AE2 adds (the device state): the
 * FE a generator makes, the controller's temperature and its FE. Loaded as a service (see META-INF/services), through AE2's own
 * tooltip registration, so it works with whichever of them is installed.
 */
public class GravityTechTooltips implements TooltipProvider {
    private static final ResourceLocation GENERATOR = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "generator");
    private static final ResourceLocation FUSION = ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion");

    @Override
    public void registerCommon(CommonRegistration registration) {
        registration.addBlockEntityData(GENERATOR, PowerGeneratorBlockEntity.class, (player, be, tag) -> {
            tag.putLong("output", be.lastOutput());
            tag.putInt("burn", be.burnTicks());
        });
        registration.addBlockEntityData(FUSION, FusionControllerBlockEntity.class, (player, be, tag) -> {
            tag.putDouble("temperature", be.temperature());
            tag.putLong("in", be.energyIn());
            tag.putLong("out", be.energyOut());
            tag.putLong("stored", be.storedFe());
            tag.putLong("stored_out", be.outFe());
            tag.putBoolean("formed", be.getBlockState().getValue(FusionController.FORMED));
        });
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(PowerGeneratorBlockEntity.class, GeneratorBlock.class, GENERATOR, (be, context, tooltip) -> {
            var data = context.serverData();
            if (!data.contains("output"))
                return;
            tooltip.addLine(data.getLong("output") > 0
                    ? Component.translatable("ae2_gvt.tooltip.generator.output", String.format("%,d", data.getLong("output")))
                    : Component.translatable("ae2_gvt.tooltip.generator.idle"));
        });
        registration.addBlockEntityBody(FusionControllerBlockEntity.class, FusionController.class, FUSION, (be, context, tooltip) -> {
            var data = context.serverData();
            if (!data.contains("temperature") || !data.getBoolean("formed"))
                return;
            tooltip.addLine(Component.translatable("ae2_gvt.tooltip.fusion.temperature", String.format("%,d", Math.round(data.getDouble("temperature")))));
            tooltip.addLine(Component.translatable("ae2_gvt.tooltip.fusion.in", String.format("%,d", data.getLong("in")),
                    String.format("%,d", data.getLong("stored"))));
            tooltip.addLine(Component.translatable("ae2_gvt.tooltip.fusion.out", String.format("%,d", data.getLong("out")),
                    String.format("%,d", data.getLong("stored_out"))));
        });
    }
}
