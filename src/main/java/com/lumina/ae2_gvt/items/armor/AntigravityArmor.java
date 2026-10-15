package com.lumina.ae2_gvt.items.armor;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

public class AntigravityArmor {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "antigravity"));
    public static final TagKey<Item> REPAIRS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(AE2GravityTech.MODID, "repairs_antigravity_armor"));
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(1,
            Map.of(ArmorType.HELMET, 6, ArmorType.CHESTPLATE, 10, ArmorType.LEGGINGS, 8, ArmorType.BOOTS, 6),
            100, SoundEvents.ARMOR_EQUIP_NETHERITE, 3f, 0.25f, REPAIRS, ASSET);

    public static DeferredItem<AntigravityArmorItem> register(String id, ArmorType type) {
        return Items.ITEMS.registerItem(id, p -> new AntigravityArmorItem(p, type),
                p -> p.humanoidArmor(MATERIAL, type).fireResistant()
                        .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                        .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE, true)));
    }
}
