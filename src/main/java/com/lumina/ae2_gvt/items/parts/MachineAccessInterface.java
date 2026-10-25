package com.lumina.ae2_gvt.items.parts;

import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraft.resources.ResourceLocation;

public class MachineAccessInterface extends AEBasePart {
    public MachineAccessInterface(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setFlags().setIdlePowerUsage(10);
    }

    private static final ResourceLocation MODEL_BASE =
            ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "part/machine_access_interface_base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_has_channel"));

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    public void getBoxes(IPartCollisionHelper iPartCollisionHelper) {
        iPartCollisionHelper.addBox(2, 2, 14, 14, 14, 16);
        iPartCollisionHelper.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 4;
    }
}
