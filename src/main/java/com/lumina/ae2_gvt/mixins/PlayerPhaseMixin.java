package com.lumina.ae2_gvt.mixins;

import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.ArmorEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The armor's phase module: a player resets {@code noPhysics} to "is a spectator" at the start of every tick, so it can only be
 * set again after that and before the movement of the same tick; flying with the module on, the wearer passes through blocks.
 */
@Mixin(Player.class)
public abstract class PlayerPhaseMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;tick()V"))
    private void ae2_gvt$phase(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.getAbilities().flying && ArmorEffects.has(self, Items.PHASE_MODULE))
            self.noPhysics = true;
    }
}
