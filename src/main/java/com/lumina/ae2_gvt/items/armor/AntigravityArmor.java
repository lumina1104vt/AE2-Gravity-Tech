package com.lumina.ae2_gvt.items.armor;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;

public class AntigravityArmor {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AE2GravityTech.MODID);

    public static final TagKey<Item> REPAIRS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "repairs_antigravity_armor"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = ARMOR_MATERIALS.register("antigravity",
            () -> new ArmorMaterial(
                    Util.make(new HashMap<>(), m -> {
                        m.put(ArmorItem.Type.HELMET, 6);
                        m.put(ArmorItem.Type.CHESTPLATE, 10);
                        m.put(ArmorItem.Type.LEGGINGS, 8);
                        m.put(ArmorItem.Type.BOOTS, 6);
                    }),
                    100, SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(REPAIRS),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "antigravity"))),
                    3f, 0.25f));

    public static DeferredItem<AntigravityArmorItem> register(String id, ArmorItem.Type type) {
        return Items.ITEMS.registerItem(id, p -> new AntigravityArmorItem(MATERIAL, type, p.fireResistant()));
    }

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }
}
