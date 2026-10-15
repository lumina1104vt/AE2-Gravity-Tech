package com.lumina.ae2_gvt.items.parts;

import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;

public class MachineAccessInterface extends AEBasePart {
    public MachineAccessInterface(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setFlags().setIdlePowerUsage(10);
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
