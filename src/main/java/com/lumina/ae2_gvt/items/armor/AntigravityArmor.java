package com.lumina.ae2_gvt.items.armor;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.reg.DeferredItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;

public class AntigravityArmor {
    public static final TagKey<Item> REPAIRS = TagKey.create(
            Registries.ITEM, new ResourceLocation(AE2GravityTech.MODID, "repairs_antigravity_armor"));

    /** 1.20.1: an armor material is a plain object (1.21 registers it). */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 100 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 6;
                case CHESTPLATE -> 10;
                case LEGGINGS -> 8;
                case BOOTS -> 6;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(REPAIRS);
        }

        @Override
        public String getName() {
            return AE2GravityTech.MODID + ":antigravity";
        }

        @Override
        public float getToughness() {
            return 3f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.25f;
        }
    };

    public static DeferredItem<AntigravityArmorItem> register(String id, ArmorItem.Type type) {
        return Items.ITEMS.registerItem(id, p -> new AntigravityArmorItem(MATERIAL, type, p.fireResistant()));
    }

    public static void register(IEventBus bus) {
    }
}
