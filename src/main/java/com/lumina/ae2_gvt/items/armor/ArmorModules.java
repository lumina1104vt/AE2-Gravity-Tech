package com.lumina.ae2_gvt.items.armor;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.lumina.ae2_gvt.components.DataComponents;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.data.InstalledModule;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import oshi.util.tuples.Pair;

import java.util.*;

public class ArmorModules {
    public static final Set<ArmorType> HEAD = EnumSet.of(ArmorType.HELMET);
    public static final Set<ArmorType> CHEST = EnumSet.of(ArmorType.CHESTPLATE);
    public static final Set<ArmorType> LEGS = EnumSet.of(ArmorType.LEGGINGS);
    public static final Set<ArmorType> FEET = EnumSet.of(ArmorType.BOOTS);

    public static final List<ArmorType> PIECES = List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS);

    public static final Set<Class<? extends IArmorItem>> ANTIGRAVITY = Set.of(AntigravityArmorItem.class);

    public static final SlotSemantic INSTALL_SLOT = SlotSemantics.register("AE2GVT_ARMOR_MODULE_INSTALL", false);
    private static final Map<ResourceKey<Item>, Pair<Set<Class<? extends IArmorItem>>, Set<ArmorType>>> MODULES = new LinkedHashMap<>();
    private static final List<DeferredItem<Item>> RAW_MODULES = new ArrayList<>();

    public static DeferredItem<Item> module(String id, Set<ArmorType> armorTypes, Set<Class<? extends IArmorItem>> armors) {
        var item = Items.ITEMS.registerSimpleItem("armor_module_" + id);
        MODULES.put(item.getKey(), new Pair<>(armors, armorTypes));
        RAW_MODULES.add(item);
        return item;
    }

    public static boolean fits(Item module, ArmorType piece, Class<? extends IArmorItem> armor) {
        var valid = MODULES.get(module.builtInRegistryHolder().key());
        if (valid == null)
            return false;
        return valid.getA().contains(armor) && valid.getB().contains(piece);
    }

    public static boolean isPiece(ItemStack stack, ArmorType type) {
        return stack.getItem() instanceof IArmorItem i && i.armorType() == type;
    }

    public static Set<InstalledModule> installed(ItemStack stack) {
        return new TreeSet<>(stack.getOrDefault(DataComponents.INSTALLED_MODULES, List.of()));
    }

    public static void setInstalled(ItemStack stack, Set<InstalledModule> modules) {
        if (modules.isEmpty())
            stack.remove(DataComponents.INSTALLED_MODULES);
        else
            stack.set(DataComponents.INSTALLED_MODULES, List.copyOf(modules));
    }

    public static List<DeferredItem<Item>> all() {
        var result = new ArrayList<DeferredItem<Item>>();
        result.add(Items.BASE_MODULE);
        result.addAll(RAW_MODULES);
        return result;
    }
}
