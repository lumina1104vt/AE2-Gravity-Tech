/*
 * PROTOTYPE - Twin-Star Hub GUI, hosted by the prototype controller. Delete with the prototype package.
 */
package com.lumina.ae2_gvt.prototype.twinstar.gui;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration for the Twin-Star Hub's GUI. */
public final class TwinStarHub {
    public static final String ID = "twin_star_hub";

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TwinStarHubMenu>> MENU = MENUS.register(ID,
            () -> MenuTypeBuilder.<TwinStarHubMenu, TwinStarHubHost>create(TwinStarHubMenu::new, TwinStarHubHost.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, ID)));

    private TwinStarHub() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
