package com.lumina.ae2_gvt.menus;

import appeng.menu.implementations.MenuTypeBuilder;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blockentities.FusionControllerBlockEntity;
import com.lumina.ae2_gvt.blockentities.GravityFactoryBlockEntity;
import com.lumina.ae2_gvt.blockentities.cpu.DummyTwinStarCPUBlockEntity;
import com.lumina.ae2_gvt.items.parts.MachineAccessHost;
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

    public static final DeferredHolder<MenuType<?>, MenuType<HubControllerMenu>> TWIN_STAR_HUB = MENUS.register("twin_star_hub",
            () -> MenuTypeBuilder.create(HubControllerMenu::new, com.lumina.ae2_gvt.hub.TwinStarHubBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "twin_star_hub")));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineAccessMenu>> MACHINE_ACCESS = MENUS.register("machine_access_interface",
            () -> MenuTypeBuilder.create(MachineAccessMenu::new, MachineAccessHost.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "machine_access_interface")));

    public static final DeferredHolder<MenuType<?>, MenuType<QuantumControllerMenu>> QUANTUM_CONTROLLER = MENUS.register("quantum_controller",
            () -> MenuTypeBuilder.create(QuantumControllerMenu::new, com.lumina.ae2_gvt.quantum.QuantumControllerBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "quantum_controller")));

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> CRYSTAL_GENERATOR = generatorMenu("crystal_generator");
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GRAVITON_GENERATOR = generatorMenu("graviton_generator");
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> ACCRETION_GENERATOR = generatorMenu("accretion_generator");

    private static DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> generatorMenu(String name) {
        return MENUS.register(name, () -> MenuTypeBuilder.create(GeneratorMenu::new, com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity.class)
                .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, name)));
    }

    public static DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> generator(com.lumina.ae2_gvt.blockentities.PowerGeneratorBlockEntity.Kind kind) {
        return switch (kind) {
            case CRYSTAL -> CRYSTAL_GENERATOR;
            case GRAVITON -> GRAVITON_GENERATOR;
            case ACCRETION -> ACCRETION_GENERATOR;
        };
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
