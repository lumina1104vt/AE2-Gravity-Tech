package com.lumina.ae2_gvt.menus;

import appeng.menu.implementations.MenuTypeBuilder;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.menus.host.ArmorMenuHost;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Menus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArmorModuleMenu>> ARMOR_MODULE_MENU = MENUS.register("armor_modules",
            () -> MenuTypeBuilder.create(ArmorModuleMenu::new, ArmorMenuHost.class)
                    .buildUnregistered(Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "armor_modules")));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
