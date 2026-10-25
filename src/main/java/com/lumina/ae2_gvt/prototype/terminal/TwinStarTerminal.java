package com.lumina.ae2_gvt.prototype.terminal;

import com.lumina.ae2_gvt.AE2GravityTech;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.items.parts.PartItem;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Twin-Star Pattern Terminal: AE2's pattern encoding terminal with one extra slot, at the
 * top-left of the recipe grid, that marks the machine the recipe runs in. It has no mode tabs -
 * there is only the one combined pattern type.
 */
public final class TwinStarTerminal {
    public static final String ID = "twin_star_pattern_terminal";

    /** The machine slot. Registered here so it exists before AE2 validates our screen style. */
    public static final SlotSemantic MACHINE_SLOT = SlotSemantics.register("TWIN_STAR_MACHINE", false);

    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            AE2GravityTech.MODID);

    /**
     * On a synthesis pattern: the machine its recipe runs in - which kind of machine the hub should
     * send it to.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Holder<Item>>> PATTERN_MACHINE = COMPONENTS
            .registerComponentType("pattern_machine", builder -> builder.persistent(BuiltInRegistries.ITEM.holderByNameCodec())
                    .networkSynchronized(ByteBufCodecs.holderRegistry(Registries.ITEM)));

    /**
     * 合成處理樣板: what the terminal encodes. Works like AE2's processing pattern (same data, same
     * decoding), plus the machine component; the hub sends it to that machine through a machine
     * access interface.
     */
    public static final DeferredItem<Item> PATTERN = AE2GravityTech.ITEMS.registerItem("synthesis_pattern",
            props -> PatternDetailsHelper.encodedPatternItemBuilder(AEProcessingPattern::new)
                    .invalidPatternTooltip(AEProcessingPattern::getInvalidPatternTooltip)
                    .itemProperties(props)
                    .build());

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2GravityTech.MODID);

    public static final DeferredItem<PartItem<TwinStarPatternTerminalPart>> ITEM = AE2GravityTech.ITEMS.registerItem(ID,
            props -> new PartItem<>(props, TwinStarPatternTerminalPart.class, TwinStarPatternTerminalPart::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TwinStarPatternTermMenu>> MENU = MENUS.register(ID,
            () -> MenuTypeBuilder.<TwinStarPatternTermMenu, TwinStarPatternTerminalPart>create(TwinStarPatternTermMenu::new,
                    TwinStarPatternTerminalPart.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, ID)));

    private TwinStarTerminal() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }
}
