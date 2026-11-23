package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.lumina.ae2_gvt.client.screen.ArmorModuleScreen;
import com.lumina.ae2_gvt.client.screen.TwinStarPatternTermScreen;
import com.lumina.ae2_gvt.menus.Menus;
import com.lumina.ae2_gvt.payload.OpenArmorModulesPayload;
import com.lumina.ae2_gvt.terminal.TwinStarTerminal;

import com.lumina.twinstar.algorithm.ae2.PatternChances;
import com.lumina.twinstar.algorithm.ae2.PatternInputFaces;
import com.lumina.twinstar.storage.TwinstarContent;

import appeng.api.client.AEKeyRendering;
import appeng.init.client.InitScreens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The twin-star pattern terminal's screen and pattern tooltip, and the armor module screen with its
 * key. Mod-bus registrations here; the game-bus listeners (tooltip, key presses) in {@link Game}.
 */
@Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TwinStarTerminalClient {
    /** Opens the armor module screen (K by default) while a piece is worn. */
    public static final KeyMapping OPEN_ARMOR_MODULES = new KeyMapping("key.ae2_gvt.armor_modules", GLFW.GLFW_KEY_K,
            "key.categories.misc");

    private TwinStarTerminalClient() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InitScreens.register(TwinStarTerminal.MENU.get(), TwinStarPatternTermScreen::new,
                    "/screens/terminals/twin_star_pattern_terminal.json");
            InitScreens.register(TwinStarTerminal.WIRELESS_MENU.get(), TwinStarPatternTermScreen::new, "/screens/terminals/twin_star_pattern_terminal.json");
            InitScreens.register(Menus.ARMOR_MODULE_MENU.get(), ArmorModuleScreen::new, "/screens/armor_modules.json");
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ARMOR_MODULES);
    }

    @Mod.EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        /** Encoded patterns made in the terminal say which machine they are for. */
        @SubscribeEvent
        public static void patternTooltip(ItemTooltipEvent event) {
            Item machine = TwinStarTerminal.PATTERN_MACHINE.get(event.getItemStack());
            if (machine != null) {
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.machine", machine.getName(new ItemStack(machine)))
                        .withStyle(ChatFormatting.GRAY));
            }
            java.util.List<Item> subs = TwinStarTerminal.PATTERN_SUB_MACHINES.get(event.getItemStack());
            if (subs != null && !subs.isEmpty()) {
                Component names = net.minecraft.network.chat.ComponentUtils.formatList(subs, Component.literal(" → "),
                        h -> h.getName(new ItemStack(h)));
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.sub_machines", names).withStyle(ChatFormatting.GRAY));
            }
            Item tool = TwinStarTerminal.PATTERN_TOOL.get(event.getItemStack());
            if (tool != null) {
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.tool", tool.getName(new ItemStack(tool)))
                        .withStyle(ChatFormatting.GRAY));
            }
            PatternChances chances = TwinstarContent.PATTERN_CHANCES.get(event.getItemStack());
            if (chances != null) {
                for (PatternChances.Entry e : chances.entries()) {
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.output_chance", AEKeyRendering.getDisplayName(e.key()),
                            String.format(java.util.Locale.ROOT, "%.4g%%", e.chance() * 100)).withStyle(ChatFormatting.GRAY));
                }
            }
            var targets = TwinstarContent.PATTERN_INPUT_TARGETS.get(event.getItemStack());
            if (targets != null) {
                for (var e : targets.entries()) {
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_target", AEKeyRendering.getDisplayName(e.key()),
                            targetName(e.target(), TwinStarTerminal.PATTERN_SUB_MACHINES.has(event.getItemStack()), false))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            var ignoreNbt = TwinstarContent.PATTERN_IGNORE_NBT.get(event.getItemStack());
            if (ignoreNbt != null) {
                java.util.List<appeng.api.stacks.AEKey> all = new java.util.ArrayList<>(ignoreNbt.inputs());
                ignoreNbt.outputs().stream().filter(k -> !all.contains(k)).forEach(all::add);
                Component names = net.minecraft.network.chat.ComponentUtils.formatList(all, Component.literal("、"), AEKeyRendering::getDisplayName);
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.ignore_nbt.list", names).withStyle(ChatFormatting.GRAY));
            }
            PatternInputFaces faces = TwinstarContent.PATTERN_INPUT_FACES.get(event.getItemStack());
            if (faces != null) {
                for (PatternInputFaces.Entry e : faces.entries()) {
                    Component sides = net.minecraft.network.chat.ComponentUtils.formatList(PatternInputFaces.directions(e.faces()),
                            Component.literal("、"), d -> Component.translatable("ae2_gvt.face.short." + d.getSerializedName()));
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_faces", AEKeyRendering.getDisplayName(e.key()), sides)
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            var doneKeys = TwinstarContent.PATTERN_DISPATCH_DONE.get(event.getItemStack());
            if (doneKeys != null && !doneKeys.keys().isEmpty()) {
                Component names = net.minecraft.network.chat.ComponentUtils.formatList(doneKeys.keys(), Component.literal("、"), AEKeyRendering::getDisplayName);
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.done.list", names).withStyle(ChatFormatting.GRAY));
            }
            var outGuiSlots = TwinstarContent.PATTERN_OUTPUT_GUI_SLOTS.get(event.getItemStack());
            if (outGuiSlots != null) {
                for (var e : outGuiSlots.entries())
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.output_gui_slot", AEKeyRendering.getDisplayName(e.key()), e.slot())
                            .withStyle(ChatFormatting.GRAY));
            }
            var guiSlots = TwinstarContent.PATTERN_INPUT_GUI_SLOTS.get(event.getItemStack());
            if (guiSlots != null && guiSlots.slots().stream().anyMatch(g -> g > 0)) {
                // one number per material slot, in order (0 = the first slot that takes it)
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_gui_slots",
                        guiSlots.slots().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(" "))).withStyle(ChatFormatting.GRAY));
            }
            var roles = TwinstarContent.PATTERN_INPUT_ROLES.get(event.getItemStack());
            if (roles != null) {
                for (var e : roles.entries()) {
                    if (e.role() == com.lumina.twinstar.algorithm.ae2.PatternInputRoles.CATALYST)
                        event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_catalyst", AEKeyRendering.getDisplayName(e.key()))
                                .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        /**
         * Where a material goes, as the tooltip and the amount dialog say it; "default" says what it stands
         * for now ({@code hasSubMachines}: the pattern names sub-machines; {@code first}: it is its first material).
         */
        public static Component targetName(int target, boolean hasSubMachines, boolean first) {
            int kind = com.lumina.twinstar.algorithm.ae2.PatternInputTargets.kind(target);
            if (kind == com.lumina.twinstar.algorithm.ae2.PatternInputTargets.DEFAULT)
                return Component.translatable("ae2_gvt.pattern.target.default",
                        targetName(com.lumina.twinstar.algorithm.ae2.PatternInputTargets.resolve(target, hasSubMachines, first), hasSubMachines, first));
            String k = switch (kind) {
                case com.lumina.twinstar.algorithm.ae2.PatternInputTargets.SUB -> "sub";
                case com.lumina.twinstar.algorithm.ae2.PatternInputTargets.FRONT_BLOCK -> "front_block";
                case com.lumina.twinstar.algorithm.ae2.PatternInputTargets.FRONT_DROP -> "front_drop";
                default -> "machine";
            };
            return Component.translatable("ae2_gvt.pattern.target." + k,
                    com.lumina.twinstar.algorithm.ae2.PatternInputTargets.distance(target));
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END)
                return;
            while (OPEN_ARMOR_MODULES.consumeClick()) {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                    com.lumina.ae2_gvt.payload.ModNet.toServer(OpenArmorModulesPayload.INSTANCE);
                }
            }
        }
    }
}
