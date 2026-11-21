package com.lumina.ae2_gvt.terminal;

import com.lumina.ae2_gvt.AE2GravityTech;

import java.util.List;

import com.lumina.ae2_gvt.components.ItemData;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.items.parts.PartItem;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import com.lumina.ae2_gvt.reg.DeferredHolder;
import com.lumina.ae2_gvt.reg.DeferredItem;
import com.lumina.ae2_gvt.reg.DeferredRegister;

/**
 * The Twin-Star Pattern Terminal: AE2's pattern encoding terminal with one extra slot, at the
 * top-left of the recipe grid, that marks the machine the recipe runs in, and under it the
 * sub-machines: blocks near it that some materials go into instead (an altar's pedestals). It has no mode tabs -
 * there is only the one combined pattern type.
 */
public final class TwinStarTerminal {
    public static final String ID = "twin_star_pattern_terminal";

    /** The machine slot. Registered here so it exists before AE2 validates our screen style. */
    public static final SlotSemantic MACHINE_SLOT = SlotSemantics.register("TWIN_STAR_MACHINE", false);
    /** The sub-machine slots, shown in their own dialog ({@code SubMachineScreen}). */
    public static final SlotSemantic SUB_MACHINE_SLOTS = SlotSemantics.register("TWIN_STAR_SUB_MACHINES", false);
    /** The tool slot, in the same dialog as the sub-machines. */
    public static final SlotSemantic TOOL_SLOT = SlotSemantics.register("TWIN_STAR_TOOL", false);
    /** How many sub-machines a pattern can name. */
    public static final int SUB_MACHINES = 9;

    /**
     * On a synthesis pattern: the machine its recipe runs in - which kind of machine the hub should
     * send it to.
     */
    public static final ItemData<Item> PATTERN_MACHINE = new ItemData<>("pattern_machine", ForgeRegistries.ITEMS.getCodec());

    /**
     * On a synthesis pattern with a machine: the tool that right-clicks the machine once every material of a
     * run is in.
     */
    public static final ItemData<Item> PATTERN_TOOL = new ItemData<>("pattern_tool", ForgeRegistries.ITEMS.getCodec());

    /**
     * On a synthesis pattern with a machine: the kinds of block near it that materials set to go to
     * sub-machines go into, in this order.
     */
    public static final ItemData<List<Item>> PATTERN_SUB_MACHINES = new ItemData<>("pattern_sub_machines",
            ForgeRegistries.ITEMS.getCodec().listOf());

    /**
     * 合成處理樣板: what the terminal encodes. Works like AE2's processing pattern (same data, same
     * decoding), plus the machine component; the hub sends it to that machine through a machine
     * access interface.
     */
    public static final DeferredItem<Item> PATTERN = AE2GravityTech.ITEMS.registerItem("synthesis_pattern", SynthesisPatternItem::new);

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredItem<PartItem<TwinStarPatternTerminalPart>> ITEM = AE2GravityTech.ITEMS.registerItem(ID,
            props -> new PartItem<>(props, TwinStarPatternTerminalPart.class, TwinStarPatternTerminalPart::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TwinStarPatternTermMenu>> MENU = MENUS.register(ID,
            () -> MenuTypeBuilder.<TwinStarPatternTermMenu, TwinStarPatternTerminalPart>create(TwinStarPatternTermMenu::new,
                    TwinStarPatternTerminalPart.class)
                    .build(ID));

    /** the wireless one: AE2's wireless terminal with this screen */
    public static final DeferredItem<WirelessTwinStarTerminalItem> WIRELESS_ITEM = AE2GravityTech.ITEMS.registerItem("wireless_" + ID,
            props -> new WirelessTwinStarTerminalItem(() -> appeng.core.AEConfig.instance().getWirelessTerminalBattery().getAsDouble(), props.stacksTo(1)));

    public static final DeferredHolder<MenuType<?>, MenuType<TwinStarPatternTermMenu>> WIRELESS_MENU = MENUS.register("wireless_" + ID,
            () -> MenuTypeBuilder.<TwinStarPatternTermMenu, WirelessTwinStarTerminalMenuHost>create(TwinStarPatternTermMenu::new,
                    WirelessTwinStarTerminalMenuHost.class)
                    .build("wireless_" + ID));

    private TwinStarTerminal() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
