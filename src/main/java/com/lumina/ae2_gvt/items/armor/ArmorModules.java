package com.lumina.ae2_gvt.items.armor;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.lumina.ae2_gvt.components.DataComponents;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.data.InstalledModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import oshi.util.tuples.Pair;

import java.util.*;

public class ArmorModules {
    public static final Set<ArmorItem.Type> HEAD = EnumSet.of(ArmorItem.Type.HELMET);
    public static final Set<ArmorItem.Type> CHEST = EnumSet.of(ArmorItem.Type.CHESTPLATE);
    public static final Set<ArmorItem.Type> LEGS = EnumSet.of(ArmorItem.Type.LEGGINGS);
    public static final Set<ArmorItem.Type> FEET = EnumSet.of(ArmorItem.Type.BOOTS);

    public static final List<ArmorItem.Type> PIECES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);

    public static final Set<Class<? extends IArmorItem>> ANTIGRAVITY = Set.of(AntigravityArmorItem.class);

    public static final SlotSemantic INSTALL_SLOT = SlotSemantics.register("AE2GVT_ARMOR_MODULE_INSTALL", false);
    private static final Map<ResourceKey<Item>, Pair<Set<Class<? extends IArmorItem>>, Set<ArmorItem.Type>>> MODULES = new LinkedHashMap<>();
    private static final List<DeferredItem<Item>> RAW_MODULES = new ArrayList<>();

    public static DeferredItem<Item> module(String id, Set<ArmorItem.Type> armorTypes, Set<Class<? extends IArmorItem>> armors) {
        var item = Items.ITEMS.registerSimpleItem("armor_module_" + id);
        MODULES.put(item.getKey(), new Pair<>(armors, armorTypes));
        RAW_MODULES.add(item);
        return item;
    }

    public static boolean fits(Item module, ArmorItem.Type piece, Class<? extends IArmorItem> armor) {
        var valid = MODULES.get(BuiltInRegistries.ITEM.getResourceKey(module).orElse(null));
        if (valid == null)
            return false;
        return valid.getA().contains(armor) && valid.getB().contains(piece);
    }

    public static boolean isPiece(ItemStack stack, ArmorItem.Type type) {
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
        result.add(Items.PROPULSION_CORE);
        result.add(Items.LIFE_CORE);
        result.add(Items.IMPACT_CRYSTAL);
        result.add(Items.SENSOR_ARRAY);
        result.add(Items.NETWORK_NODE);
        result.addAll(RAW_MODULES);
        return result;
    }
}
