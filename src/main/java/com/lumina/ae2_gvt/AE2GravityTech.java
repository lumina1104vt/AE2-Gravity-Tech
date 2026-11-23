package com.lumina.ae2_gvt;

import java.util.List;

import com.lumina.ae2_gvt.blockentities.BlockEntities;
import com.lumina.ae2_gvt.blocks.Blocks;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.Items;
import com.lumina.ae2_gvt.items.armor.AntigravityArmor;
import com.lumina.ae2_gvt.items.armor.ArmorModules;
import com.lumina.ae2_gvt.menus.Menus;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredRegister;


@Mod(AE2GravityTech.MODID)
public class AE2GravityTech {
    public static final String MODID = "ae2_gvt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final CrystalLine GRAVITY = CrystalLine.register("gravity", MapColor.COLOR_BLACK, 8);
    public static final CrystalLine ANTIGRAVITY = CrystalLine.register("antigravity", MapColor.SNOW, 12);
    public static final CrystalLine PARADOX = CrystalLine.register("paradox", MapColor.COLOR_GRAY, 15);   // 15 is the maximum: light is stored in 4 bits

    static {
        com.lumina.ae2_gvt.hub.HubUnits.init();   // registers the hub units with BLOCKS / ITEMS
        com.lumina.ae2_gvt.hub.HubStructureBlocks.init();   // helix / pillar / ring blocks of the hexagram hub
        com.lumina.ae2_gvt.structure.FusionStructure.register();   // the fusion chamber's lumina-multiblock definition
        com.lumina.ae2_gvt.hub.HexagramHub.register();   // the hexagram hub's lumina-multiblock definition
        com.lumina.ae2_gvt.elevator.SpaceElevator.register();   // the space elevator: controller block + lumina-multiblock definition
        com.lumina.ae2_gvt.quantum.QuantumController.init();   // quantum controller
        com.lumina.ae2_gvt.graviton.Gravitons.init();   // graviton storage type and cells
        com.lumina.ae2_gvt.storage.FeCells.init();      // cells that keep FE
        com.lumina.ae2_gvt.storage.FeEnergy.init();     // FE on the network, without Applied Flux
    }

    // The mod's creative tab, placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ae2_gvt")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> GRAVITY.crystal().get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(Items.ANTI_SINGULARITY);
                output.accept(Items.PARADOX_QUANTUM_ENTANGLED_SINGULARITY);
                output.accept(Items.GRAVITY_WAVE_DETECTOR);
                output.accept(Items.ANNIHILATION_TNT);
                output.accept(Items.GRAVITY_FACTORY);
                output.accept(Items.CRYSTAL_GENERATOR);
                output.accept(Items.GRAVITON_GENERATOR);
                output.accept(Items.ACCRETION_GENERATOR);
                output.accept(Items.GRAVITY_CONSTRAINER);
                output.accept(Items.FUSION_CONTROLLER);
                output.accept(Items.ENERGY_PORT);
                output.accept(Items.CONTAINMENT_GLASS);
                output.accept(Items.MACHINE_FRAME);
                output.accept(Items.STRUCTURE_FRAME);
                com.lumina.ae2_gvt.hub.HubUnits.all().forEach(item -> output.accept(item.get()));
                com.lumina.ae2_gvt.hub.HubStructureBlocks.all().forEach(item -> output.accept(item.get()));
                com.lumina.ae2_gvt.elevator.SpaceElevator.items().forEach(item -> output.accept(item.get()));
                output.accept(com.lumina.ae2_gvt.terminal.TwinStarTerminal.ITEM.get());
                output.accept(com.lumina.ae2_gvt.terminal.TwinStarTerminal.WIRELESS_ITEM.get());
                output.accept(Items.MACHINE_ACCESS_INTERFACE);
                output.accept(Items.MACHINE_ACCESS_BLOCK);
                output.accept(Items.MACHINE_LINK_TOOL);
                output.accept(com.lumina.ae2_gvt.quantum.QuantumController.ITEM.get());
                com.lumina.ae2_gvt.graviton.Gravitons.all().forEach(item -> output.accept(item.get()));
                if (com.lumina.ae2_gvt.storage.FeCells.enabled())
                    com.lumina.ae2_gvt.storage.FeCells.all().forEach(item -> output.accept(item.get()));
                output.accept(Items.BLACK_HOLE_STORAGE_CELL);
                output.accept(Items.WHITE_HOLE_ENERGY_CELL);
                output.accept(Items.GRAVITON_COLLECTOR);
                for (CrystalLine line : List.of(GRAVITY, ANTIGRAVITY, PARADOX)) {
                    line.all().forEach(item -> output.accept(item.get()));
                }
                output.accept(Items.PARADOX_ALLOY_PLATE);
                output.accept(Items.ALMOST_FLAWLESS_BUDDING_PARADOX);
                output.accept(Items.FLAWED_BUDDING_PARADOX);
                output.accept(Items.CHIPPED_BUDDING_PARADOX);
                output.accept(Items.DAMAGED_BUDDING_PARADOX);
                output.accept(Items.PARADOX_CRYSTAL_SEED);
                output.accept(Items.CHAOS_MATTER);
                output.accept(com.lumina.ae2_gvt.hole.GravityHole.BLACK_HOLE_ITEM);
                output.accept(com.lumina.ae2_gvt.hole.GravityHole.WHITE_HOLE_ITEM);
                output.accept(Items.ANTIGRAVITY_ARMOR_HELMET);
                output.accept(Items.ANTIGRAVITY_ARMOR_CHESTPLATE);
                output.accept(Items.ANTIGRAVITY_ARMOR_LEGGINGS);
                output.accept(Items.ANTIGRAVITY_ARMOR_BOOTS);
                ArmorModules.all().forEach(item -> output.accept(item.get()));
            }).build());

    public AE2GravityTech() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        Blocks.register(modEventBus);

        ITEMS.register(modEventBus);
        Items.register(modEventBus);
        com.lumina.ae2_gvt.items.DispatchMarker.register();

        CREATIVE_MODE_TABS.register(modEventBus);
        Menus.register(modEventBus);
        com.lumina.ae2_gvt.recipes.Recipes.register(modEventBus);
        // dev check (-Dae2_gvt.loadMixinTargets=true): load the AE2 classes our mixins patch now, so a mixin that does not fit AE2 shows at start-up
        if (Boolean.getBoolean("ae2_gvt.loadMixinTargets"))
            modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
                for (String name : new String[] {"appeng.me.service.CraftingService", "appeng.me.Grid", "appeng.me.pathfinding.ControllerValidator",
                        "appeng.menu.me.crafting.CraftingCPUMenu", "appeng.menu.me.crafting.CraftAmountMenu",
                        "appeng.menu.me.crafting.CraftingStatusEntry", "appeng.menu.me.networktool.NetworkStatusMenu",
                        "appeng.me.service.helpers.NetworkCraftingProviders$ProviderState"}) {
                    try {
                        Class.forName(name);
                        LOGGER.info("mixin target loaded: {}", name);
                    } catch (Throwable t) {
                        LOGGER.error("mixin target failed: {}", name, t);
                    }
                }
            }));
        // AE2 upgrade cards our machines take (registered once the items exist)
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(
                com.lumina.ae2_gvt.storage.BlackHoleCell::registerHandler));
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(
                com.lumina.ae2_gvt.storage.FeEnergy::registerStrategies));
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(
                com.lumina.ae2_gvt.graviton.InfiniteGravitonCell::registerHandler));
        // the wireless terminal: what AE2 does for its own (energy card slots, linking to a network, charging)
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            var wireless = com.lumina.ae2_gvt.terminal.TwinStarTerminal.WIRELESS_ITEM.get();
            appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.ENERGY_CARD, wireless, 2,
                    appeng.core.localization.GuiText.WirelessTerminals.getTranslationKey());
            appeng.api.features.GridLinkables.register(wireless, appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER);
        }));
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            for (var piece : java.util.List.of(Items.ANTIGRAVITY_ARMOR_HELMET, Items.ANTIGRAVITY_ARMOR_CHESTPLATE,
                    Items.ANTIGRAVITY_ARMOR_LEGGINGS, Items.ANTIGRAVITY_ARMOR_BOOTS))
                appeng.api.features.GridLinkables.register(piece.get(), com.lumina.ae2_gvt.items.armor.ArmorNetwork.LINKABLE_HANDLER);
        }));
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() ->
                appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD,
                        com.lumina.ae2_gvt.blocks.Blocks.GRAVITY_FACTORY.get(),
                        com.lumina.ae2_gvt.blockentities.GravityFactoryBlockEntity.MAX_SPEED_CARDS)));
        // which machine a synthesis pattern is for, for the twin-star engine
        com.lumina.twinstar.api.PatternMachines.setReader(definition -> {
            return com.lumina.ae2_gvt.terminal.TwinStarTerminal.PATTERN_MACHINE.get(definition);
        });
        com.lumina.twinstar.api.PatternMachines.setSubMachineReader(definition -> {
            var subs = com.lumina.ae2_gvt.terminal.TwinStarTerminal.PATTERN_SUB_MACHINES.get(definition);
            return subs == null ? java.util.List.of() : subs;
        });
        com.lumina.twinstar.api.PatternMachines.setToolReader(definition -> {
            return com.lumina.ae2_gvt.terminal.TwinStarTerminal.PATTERN_TOOL.get(definition);
        });
        BlockEntities.register(modEventBus);
        AntigravityArmor.register(modEventBus);
        com.lumina.ae2_gvt.terminal.TwinStarTerminal.register(modEventBus);
        com.lumina.ae2_gvt.hole.GravityHole.register(modEventBus);
        com.lumina.ae2_gvt.hole.BlackHoleCrater.register(modEventBus);   // natural black holes
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(com.lumina.ae2_gvt.structure.FusionCommands::register);
        com.lumina.ae2_gvt.dev.DevChecks.init();
        com.lumina.ae2_gvt.rift.SpaceRift.register(modEventBus);   // the tear of an annihilation TNT, 破碎星空
        com.lumina.ae2_gvt.quantum.QuantumController.register(modEventBus);
        com.lumina.ae2_gvt.graviton.Gravitons.register(modEventBus);
        com.lumina.ae2_gvt.storage.FeEnergy.register(modEventBus);
        com.lumina.ae2_gvt.preview.TwinStarPreview.register(modEventBus);
        com.lumina.ae2_gvt.hub.HubStructureBlocks.register(modEventBus);   // hexagram hub controller's block entity

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> AE2GravityTechClient::init);
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(com.lumina.ae2_gvt.payload.ModNet::register));
    }
}
