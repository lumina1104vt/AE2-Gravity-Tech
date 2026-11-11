package com.lumina.ae2_gvt.client;

import com.lumina.ae2_gvt.AE2GravityTech;
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
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The twin-star pattern terminal's screen and pattern tooltip, and the armor module screen with its
 * key. Mod-bus registrations here; the game-bus listeners (tooltip, key presses) in {@link Game}.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
public final class TwinStarTerminalClient {
    /** Opens the armor module screen (K by default) while a piece is worn. */
    public static final KeyMapping OPEN_ARMOR_MODULES = new KeyMapping("key.ae2_gvt.armor_modules", GLFW.GLFW_KEY_K,
            "key.categories.misc");

    private TwinStarTerminalClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, TwinStarTerminal.MENU.get(), TwinStarPatternTermScreen::new,
                "/screens/terminals/twin_star_pattern_terminal.json");
        InitScreens.register(event, Menus.ARMOR_MODULE_MENU.get(), ArmorModuleScreen::new, "/screens/armor_modules.json");
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ARMOR_MODULES);
    }

    @EventBusSubscriber(modid = AE2GravityTech.MODID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        /** Encoded patterns made in the terminal say which machine they are for. */
        @SubscribeEvent
        static void patternTooltip(ItemTooltipEvent event) {
            Holder<Item> machine = event.getItemStack().get(TwinStarTerminal.PATTERN_MACHINE.get());
            if (machine != null) {
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.machine", machine.value().getName(new ItemStack(machine)))
                        .withStyle(ChatFormatting.GRAY));
            }
            java.util.List<Holder<Item>> subs = event.getItemStack().get(TwinStarTerminal.PATTERN_SUB_MACHINES.get());
            if (subs != null && !subs.isEmpty()) {
                Component names = net.minecraft.network.chat.ComponentUtils.formatList(subs, Component.literal(" → "),
                        h -> h.value().getName(new ItemStack(h)));
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.sub_machines", names).withStyle(ChatFormatting.GRAY));
            }
            PatternChances chances = event.getItemStack().get(TwinstarContent.PATTERN_CHANCES.get());
            if (chances != null) {
                for (PatternChances.Entry e : chances.entries()) {
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.output_chance", AEKeyRendering.getDisplayName(e.key()),
                            String.format(java.util.Locale.ROOT, "%.4g%%", e.chance() * 100)).withStyle(ChatFormatting.GRAY));
                }
            }
            var targets = event.getItemStack().get(TwinstarContent.PATTERN_INPUT_TARGETS.get());
            if (targets != null) {
                for (var e : targets.entries()) {
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_target", AEKeyRendering.getDisplayName(e.key()),
                            targetName(e.target(), event.getItemStack().has(TwinStarTerminal.PATTERN_SUB_MACHINES.get()), false))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            var ignoreNbt = event.getItemStack().get(TwinstarContent.PATTERN_IGNORE_NBT.get());
            if (ignoreNbt != null) {
                java.util.List<appeng.api.stacks.AEKey> all = new java.util.ArrayList<>(ignoreNbt.inputs());
                ignoreNbt.outputs().stream().filter(k -> !all.contains(k)).forEach(all::add);
                Component names = net.minecraft.network.chat.ComponentUtils.formatList(all, Component.literal("、"), AEKeyRendering::getDisplayName);
                event.getToolTip().add(Component.translatable("ae2_gvt.pattern.ignore_nbt.list", names).withStyle(ChatFormatting.GRAY));
            }
            PatternInputFaces faces = event.getItemStack().get(TwinstarContent.PATTERN_INPUT_FACES.get());
            if (faces != null) {
                for (PatternInputFaces.Entry e : faces.entries()) {
                    Component sides = net.minecraft.network.chat.ComponentUtils.formatList(PatternInputFaces.directions(e.faces()),
                            Component.literal("、"), d -> Component.translatable("ae2_gvt.face.short." + d.getSerializedName()));
                    event.getToolTip().add(Component.translatable("ae2_gvt.pattern.input_faces", AEKeyRendering.getDisplayName(e.key()), sides)
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
        static void tick(ClientTickEvent.Post event) {
            while (OPEN_ARMOR_MODULES.consumeClick()) {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                    PacketDistributor.sendToServer(OpenArmorModulesPayload.INSTANCE);
                }
            }
        }
    }
}
