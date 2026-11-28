package com.lumina.ae2_gvt.client;

import java.lang.reflect.Method;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.compat.CrystalScience;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** The seeds' models follow how far they have grown, as AE2 Crystal Science's do ({@code ae2cs:age}). */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CrystalSeedClient {
    private CrystalSeedClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (CrystalScience.loaded())
            event.enqueueWork(() -> CrystalScience.seeds().forEach(seed -> register(seed.get())));
    }

    private static void register(Item seed) {
        try {
            Method progress = seed.getClass().getMethod("getGrowProcess", ItemStack.class);
            ItemProperties.register(seed, ResourceLocation.fromNamespaceAndPath(CrystalScience.MODID, "age"),
                    (stack, level, entity, id) -> {
                        try {
                            return (float) progress.invoke(stack.getItem(), stack);
                        } catch (ReflectiveOperationException e) {
                            return 0f;
                        }
                    });
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("AE2 Crystal Science's seed item has changed", e);
        }
    }
}
