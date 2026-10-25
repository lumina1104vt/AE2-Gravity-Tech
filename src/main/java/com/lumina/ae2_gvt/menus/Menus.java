package com.lumina.ae2_gvt.menus;

import appeng.menu.implementations.MenuTypeBuilder;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.blockentities.GravityFactoryBlockEntity;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.menus.host.ArmorMenuHost;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Menus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArmorModuleMenu>> ARMOR_MODULE_MENU = MENUS.register("armor_modules",
            () -> MenuTypeBuilder.create(ArmorModuleMenu::new, ArmorMenuHost.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "armor_modules")));
    public static final DeferredHolder<MenuType<?>, MenuType<GravityFactoryMenu>> GRAVITY_FACTORY = MENUS.register("gravity_processing_factory",
            () -> MenuTypeBuilder.create(GravityFactoryMenu::new, GravityFactoryBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "gravity_processing_factory")));
    public static final DeferredHolder<MenuType<?>, MenuType<FusionControllerMenu>> FUSION_CONTROLLER = MENUS.register("fusion_controller",
            () -> MenuTypeBuilder.create(FusionControllerMenu::new, FusionControllerBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "fusion_controller")));
    public static final DeferredHolder<MenuType<?>, MenuType<DummyCpuMenu>> DUMMY_CPU = MENUS.register("dummy_twinstar_cpu",
            () -> MenuTypeBuilder.create(DummyCpuMenu::new, DummyTwinStarCPUBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "dummy_twinstar_cpu")));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
