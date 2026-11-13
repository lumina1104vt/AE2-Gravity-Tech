package com.lumina.ae2_gvt.items;

import appeng.items.parts.PartItem;
import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.items.armor.AntigravityArmor;
import com.lumina.ae2_gvt.items.armor.AntigravityArmorItem;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.items.parts.MachineAccessInterface;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2GravityTech.MODID);

    public static final DeferredItem<Item> ANTI_SINGULARITY =
            ITEMS.registerSimpleItem("anti_singularity");
    public static final DeferredItem<Item> CHAOS_MATTER =
            ITEMS.registerSimpleItem("chaos_matter");

    public static final DeferredItem<Item> PARADOX_ALLOY_PLATE =
            ITEMS.registerSimpleItem("paradox_alloy_plate");
    public static final DeferredItem<Item> PARADOX_QUANTUM_ENTANGLED_SINGULARITY =
            ITEMS.registerSimpleItem("paradox_quantum_entangled_singularity");

    public static final DeferredItem<ParadoxSeedItem> PARADOX_CRYSTAL_SEED = ITEMS.registerItem("paradox_crystal_seed", ParadoxSeedItem::new);

    public static final DeferredItem<GravityWaveDetectorItem> GRAVITY_WAVE_DETECTOR = ITEMS.registerItem("gravity_wave_detector",
            GravityWaveDetectorItem::new, new Item.Properties().stacksTo(1));

    // block item
    public static final DeferredItem<BlockItem> GRAVITY_CONSTRAINER = ITEMS.registerSimpleBlockItem("gravity_constrainer", Blocks.GRAVITY_CONSTRAINER);
    public static final DeferredItem<BlockItem> ANNIHILATION_TNT = ITEMS.registerSimpleBlockItem("annihilation_tnt", Blocks.ANNIHILATION_TNT);
    public static final DeferredItem<BlockItem> MACHINE_FRAME = ITEMS.registerSimpleBlockItem("machine_frame", Blocks.MACHINE_FRAME);
    public static final DeferredItem<BlockItem> STRUCTURE_FRAME = ITEMS.registerSimpleBlockItem("structure_frame", Blocks.STRUCTURE_FRAME);

    // AE2 part
    public static final DeferredItem<PartItem<MachineAccessInterface>> MACHINE_ACCESS_INTERFACE = ITEMS.registerItem(
            "machine_access_interface", p -> new PartItem<>(p, MachineAccessInterface.class, MachineAccessInterface::new));

    public static final DeferredItem<BlockItem> MACHINE_ACCESS_BLOCK = ITEMS.registerSimpleBlockItem("machine_access_block", Blocks.MACHINE_ACCESS_BLOCK);
    public static final DeferredItem<com.lumina.ae2_gvt.items.tools.MachineLinkTool> MACHINE_LINK_TOOL =
            ITEMS.registerItem("machine_link_tool", com.lumina.ae2_gvt.items.tools.MachineLinkTool::new);

    // Armor
    public static final DeferredItem<AntigravityArmorItem> ANTIGRAVITY_ARMOR_HELMET = AntigravityArmor.register("antigravity_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<AntigravityArmorItem> ANTIGRAVITY_ARMOR_CHESTPLATE = AntigravityArmor.register("antigravity_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<AntigravityArmorItem> ANTIGRAVITY_ARMOR_LEGGINGS = AntigravityArmor.register("antigravity_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<AntigravityArmorItem> ANTIGRAVITY_ARMOR_BOOTS = AntigravityArmor.register("antigravity_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<Item> BASE_MODULE = ITEMS.registerSimpleItem("armor_module_base");

    public static final DeferredItem<Item> PROPULSION_CORE = ITEMS.registerSimpleItem("propulsion_core");
    public static final DeferredItem<Item> SENSOR_CORE = ITEMS.registerSimpleItem("sensor_core");
    public static final DeferredItem<Item> LIFE_CORE = ITEMS.registerSimpleItem("life_core");
    public static final DeferredItem<Item> IMPACT_CORE = ITEMS.registerSimpleItem("impact_core");
    public static final DeferredItem<Item> NETWORK_CORE = ITEMS.registerSimpleItem("network_core");

    public static final DeferredItem<Item> MOVE_SPEED_MODULE = ArmorModules.module("move_speed", ArmorModules.LEGS, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> JUMP_HEIGHT_MODULE = ArmorModules.module("jump_height", ArmorModules.FEET, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> STEP_ASSIST_MODULE = ArmorModules.module("step_assist", ArmorModules.FEET, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> SWIM_SPEED_MODULE = ArmorModules.module("swim_speed", ArmorModules.LEGS, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> FLIGHT_DRIFT_MODULE = ArmorModules.module("flight_drift", ArmorModules.FEET, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> PHASE_MODULE = ArmorModules.module("phase", ArmorModules.LEGS, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> DASH_MODULE = ArmorModules.module("dash", ArmorModules.FEET, ArmorModules.ANTIGRAVITY);

    public static final DeferredItem<Item> MAGNET_MODULE = ArmorModules.module("magnet", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> AUTO_STOCK_MODULE = ArmorModules.module("auto_stock", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> WORKBENCH_MODULE = ArmorModules.module("portable_workbench", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> REACH_MODULE = ArmorModules.module("reach", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> PICK_CRAFT_MODULE = ArmorModules.module("pick_craft", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> RECHARGE_MODULE = ArmorModules.module("recharge", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);

    public static final DeferredItem<Item> FIRE_RESISTANCE_MODULE = ArmorModules.module("fire_resistance", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> LAVA_IMMUNITY_MODULE = ArmorModules.module("lava_immunity", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> WATER_BREATHING_MODULE = ArmorModules.module("water_breathing", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> MAX_HEALTH_MODULE = ArmorModules.module("max_health", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> EVASION_MODULE = ArmorModules.module("evasion", ArmorModules.LEGS, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> REGENERATION_MODULE = ArmorModules.module("regeneration", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> STRENGTH_MODULE = ArmorModules.module("strength", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> ATTACK_SPEED_MODULE = ArmorModules.module("attack_speed", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> DAMAGE_REDUCTION_MODULE = ArmorModules.module("damage_reduction", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> THORNS_MODULE = ArmorModules.module("thorns", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> UNDYING_MODULE = ArmorModules.module("undying", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> SHIELD_MODULE = ArmorModules.module("shield", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);

    public static final DeferredItem<Item> LUCK_MODULE = ArmorModules.module("luck", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> NIGHT_VISION_MODULE = ArmorModules.module("night_vision", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> AUTO_FEED_MODULE = ArmorModules.module("auto_feed", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> SATURATION_MODULE = ArmorModules.module("saturation", ArmorModules.HEAD, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> CLEANSE_MODULE = ArmorModules.module("cleanse", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);
    public static final DeferredItem<Item> DIG_AFFINITY_MODULE = ArmorModules.module("dig_affinity", ArmorModules.CHEST, ArmorModules.ANTIGRAVITY);

    // budding
    public static final DeferredItem<BlockItem> ALMOST_FLAWLESS_BUDDING_PARADOX = ITEMS.registerSimpleBlockItem("almost_flawless_budding_paradox", Blocks.ALMOST_FLAWLESS_BUDDING_PARADOX);
    public static final DeferredItem<BlockItem> FLAWED_BUDDING_PARADOX = ITEMS.registerSimpleBlockItem("flawed_budding_paradox", Blocks.FLAWED_BUDDING_PARADOX);
    public static final DeferredItem<BlockItem> CHIPPED_BUDDING_PARADOX = ITEMS.registerSimpleBlockItem("chipped_budding_paradox", Blocks.CHIPPED_BUDDING_PARADOX);
    public static final DeferredItem<BlockItem> DAMAGED_BUDDING_PARADOX = ITEMS.registerSimpleBlockItem("damaged_budding_paradox", Blocks.DAMAGED_BUDDING_PARADOX);

    // machine blocks
    public static final DeferredItem<BlockItem> GRAVITON_COLLECTOR = ITEMS.registerSimpleBlockItem("graviton_collector", Blocks.GRAVITON_COLLECTOR);
    public static final DeferredItem<BlockItem> CRYSTAL_GENERATOR = ITEMS.registerSimpleBlockItem("crystal_generator", Blocks.CRYSTAL_GENERATOR);
    public static final DeferredItem<BlockItem> GRAVITON_GENERATOR = ITEMS.registerSimpleBlockItem("graviton_generator", Blocks.GRAVITON_GENERATOR);
    public static final DeferredItem<BlockItem> ACCRETION_GENERATOR = ITEMS.registerSimpleBlockItem("accretion_generator", Blocks.ACCRETION_GENERATOR);
    public static final DeferredItem<BlockItem> GRAVITY_FACTORY = ITEMS.registerSimpleBlockItem("gravity_processing_factory", Blocks.GRAVITY_FACTORY);
    public static final DeferredItem<BlockItem> FUSION_CONTROLLER = ITEMS.registerSimpleBlockItem("fusion_controller", Blocks.FUSION_CONTROLLER);
    public static final DeferredItem<BlockItem> ENERGY_PORT = ITEMS.registerSimpleBlockItem("energy_port", Blocks.ENERGY_PORT);
    public static final DeferredItem<BlockItem> CONTAINMENT_GLASS = ITEMS.registerSimpleBlockItem("containment_glass", Blocks.CONTAINMENT_GLASS);

    public static final DeferredItem<com.lumina.ae2_gvt.storage.BlackHoleCell> BLACK_HOLE_STORAGE_CELL =
            ITEMS.registerItem("black_hole_storage_cell", com.lumina.ae2_gvt.storage.BlackHoleCell::new);

    public static final DeferredItem<BlockItem> WHITE_HOLE_ENERGY_CELL = ITEMS.registerSimpleBlockItem("white_hole_energy_cell", Blocks.WHITE_HOLE_ENERGY_CELL);

    // dummy
    public static final DeferredItem<BlockItem> TWINSTAR_CPU = ITEMS.registerSimpleBlockItem("dummy_twinstar_cpu", Blocks.TWINSTAR_CPU);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
