package com.lumina.ae2_gvt.items.armor;

import java.util.List;

import com.lumina.ae2_gvt.AE2GravityTech;
import com.lumina.ae2_gvt.config.Config;
import com.lumina.ae2_gvt.items.Items;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * What the antigravity armor does. Built in, with the whole set on: flight and no fall damage (knockback resistance is in the
 * armor's material). The modules act while they are installed and switched on in any worn piece: potion effects and attribute
 * changes are kept up every few ticks, the rest is done where the damage or the breaking happens.
 */
@EventBusSubscriber(modid = AE2GravityTech.MODID)
public final class ArmorEffects {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String FLIGHT_GIVEN = "ae2_gvt_armor_flight", UNDYING_READY = "ae2_gvt_undying_ready";

    /** One attribute change a module makes: its size is the module's single level. */
    private record Change(DeferredItem<Item> module, Holder<Attribute> attribute, java.util.function.DoubleSupplier amount,
                          AttributeModifier.Operation operation) {
        ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(AE2GravityTech.MODID, "armor_" + module.getId().getPath());
        }
    }

    private static final List<Change> CHANGES = List.of(
            new Change(Items.MAX_HEALTH_MODULE, Attributes.MAX_HEALTH, Config.MAX_HEALTH_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.STRENGTH_MODULE, Attributes.ATTACK_DAMAGE, Config.STRENGTH_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.ATTACK_SPEED_MODULE, Attributes.ATTACK_SPEED, Config.ATTACK_SPEED_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.MOVE_SPEED_MODULE, Attributes.MOVEMENT_SPEED, Config.MOVE_SPEED_BONUS::get, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            new Change(Items.SWIM_SPEED_MODULE, NeoForgeMod.SWIM_SPEED, Config.SWIM_SPEED_BONUS::get, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            new Change(Items.JUMP_HEIGHT_MODULE, Attributes.JUMP_STRENGTH, Config.JUMP_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.STEP_ASSIST_MODULE, Attributes.STEP_HEIGHT, Config.STEP_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.LUCK_MODULE, Attributes.LUCK, Config.LUCK_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.REACH_MODULE, Attributes.BLOCK_INTERACTION_RANGE, Config.REACH_BONUS::get, AttributeModifier.Operation.ADD_VALUE),
            new Change(Items.REACH_MODULE, Attributes.ENTITY_INTERACTION_RANGE, Config.REACH_BONUS::get, AttributeModifier.Operation.ADD_VALUE));

    private ArmorEffects() {
    }

    /** Whether the armor piece is one of ours. */
    private static boolean isOurs(ItemStack stack) {
        return stack.getItem() instanceof AntigravityArmorItem;
    }

    /** All four pieces worn. */
    public static boolean fullSet(LivingEntity entity) {
        for (EquipmentSlot slot : SLOTS) {
            if (!isOurs(entity.getItemBySlot(slot)))
                return false;
        }
        return true;
    }

    /** The module is installed and switched on in any piece worn. */
    public static boolean has(LivingEntity entity, DeferredItem<Item> module) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!isOurs(stack))
                continue;
            for (var installed : ArmorModules.installed(stack)) {
                if (installed.enabled() && installed.module().value() == module.get())
                    return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        flight(player);
        if (player.tickCount % 2 == 0 && !player.isShiftKeyDown() && has(player, Items.MAGNET_MODULE))
            magnet(player);
        if (player.tickCount % 10 != 0)
            return;
        attributes(player);
        effects(player);
    }

    private static void flight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator())
            return;
        var abilities = player.getAbilities();
        boolean given = player.getPersistentData().getBoolean(FLIGHT_GIVEN);
        if (fullSet(player)) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.getPersistentData().putBoolean(FLIGHT_GIVEN, true);
                player.onUpdateAbilities();
            }
            // flying costs an anti-graviton from the linked network every 5 ticks; without one the wearer drops out of the air
            if (abilities.flying && player.tickCount % Config.FLIGHT_INTERVAL.get() == 0 && !payFlight(player)) {
                abilities.flying = false;
                player.onUpdateAbilities();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.ae2_gvt.armor_flight.no_fuel"), true);
            }
        } else if (given) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.getPersistentData().remove(FLIGHT_GIVEN);
            player.onUpdateAbilities();
        }
    }

    /** Magnet: items and experience within 8 blocks fly to the wearer (hold sneak to leave them). */
    private static void magnet(ServerPlayer player) {
        var box = player.getBoundingBox().inflate(Config.MAGNET_RANGE.get());
        var target = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (var item : player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (!item.hasPickUpDelay() && !item.getItem().isEmpty())
                pull(item, target);
        }
        for (var orb : player.level().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box))
            pull(orb, target);
    }

    private static void pull(net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.Vec3 target) {
        var d = target.subtract(entity.position());
        if (d.lengthSqr() < 0.04)
            return;
        entity.setDeltaMovement(d.normalize().scale(Math.min(0.5, 0.15 + d.length() * 0.05)));
        entity.hasImpulse = true;
    }

    private static boolean payFlight(ServerPlayer player) {
        var grid = ArmorNetwork.grid(player);
        if (grid == null)
            return false;
        return grid.getStorageService().getInventory().extract(com.lumina.ae2_gvt.graviton.GravitonKey.ANTIGRAVITON, 1,
                appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.ofPlayer(player)) > 0;
    }

    private static void attributes(ServerPlayer player) {
        for (Change c : CHANGES) {
            var instance = player.getAttribute(c.attribute());
            if (instance == null)
                continue;
            boolean want = has(player, c.module());
            boolean present = instance.hasModifier(c.id());
            if (want && !present)
                instance.addTransientModifier(new AttributeModifier(c.id(), c.amount().getAsDouble(), c.operation()));
            else if (!want && present) {
                instance.removeModifier(c.id());
                if (c.attribute() == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth())
                    player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void effects(ServerPlayer player) {
        refresh(player, Items.NIGHT_VISION_MODULE, MobEffects.NIGHT_VISION, 0, 400);
        refresh(player, Items.WATER_BREATHING_MODULE, MobEffects.WATER_BREATHING, 0, 400);
        refresh(player, Items.REGENERATION_MODULE, MobEffects.REGENERATION, 0, 60);
        refresh(player, Items.FIRE_RESISTANCE_MODULE, MobEffects.FIRE_RESISTANCE, 0, 60);
        if (has(player, Items.LAVA_IMMUNITY_MODULE)) {
            refresh(player, Items.LAVA_IMMUNITY_MODULE, MobEffects.FIRE_RESISTANCE, 0, 60);
            player.clearFire();
        }
        if (has(player, Items.SHIELD_MODULE) && player.getAbsorptionAmount() <= 0 && player.tickCount % 200 == 0)
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1, true, false, true));
        if (has(player, Items.CLEANSE_MODULE)) {
            for (var effect : List.copyOf(player.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    player.removeEffect(effect.getEffect());
            }
        }
        if (player.tickCount % 20 == 0) {
            ArmorNetworkEffects.tick(player);
            if (has(player, Items.SATURATION_MODULE) && player.getFoodData().getFoodLevel() < 20)
                player.getFoodData().eat(1, 0.5f);
            if (has(player, Items.AUTO_FEED_MODULE) && player.getFoodData().getFoodLevel() <= 14)
                eatSomething(player);
        }
    }

    /** Keeps a potion effect on without particles, topped up before it runs out. */
    private static void refresh(ServerPlayer player, DeferredItem<Item> module, Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier, int duration) {
        if (!has(player, module))
            return;
        var now = player.getEffect(effect);
        if (now == null || now.getDuration() < duration / 2 || now.getAmplifier() < amplifier)
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void eatSomething(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            var food = stack.get(DataComponents.FOOD);
            if (food != null && food.nutrition() > 0) {
                player.getFoodData().eat(food);
                stack.shrink(1);
                return;
            }
        }
    }

    /** Whole set on: no fall damage. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && fullSet(player))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        var source = event.getSource();
        // fire: the fire resistance module covers fire, the lava immunity module lava as well
        if (source.is(DamageTypeTags.IS_FIRE)) {
            boolean lava = source.is(DamageTypes.LAVA);
            if (has(player, Items.LAVA_IMMUNITY_MODULE) || (!lava && has(player, Items.FIRE_RESISTANCE_MODULE))) {
                event.setCanceled(true);
                return;
            }
        }
        // evasion: now and then a hit just misses
        if (has(player, Items.EVASION_MODULE) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && player.getRandom().nextFloat() < Config.EVASION_CHANCE.get()) {
            event.setCanceled(true);
            return;
        }
        if (has(player, Items.DAMAGE_REDUCTION_MODULE))
            event.setAmount((float) Math.max(0, event.getAmount() - Config.DAMAGE_REDUCTION.get()));
        // a deadly hit: once a minute the armor keeps its wearer alive
        if (has(player, Items.UNDYING_MODULE) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && event.getAmount() >= player.getHealth() + player.getAbsorptionAmount()
                && player.level().getGameTime() >= player.getPersistentData().getLong(UNDYING_READY)) {
            event.setCanceled(true);
            player.getPersistentData().putLong(UNDYING_READY, player.level().getGameTime() + Config.UNDYING_COOLDOWN.get());
            player.setHealth((float) (player.getMaxHealth() * Config.UNDYING_HEALTH.get()));
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        }
    }

    /** Thorns: a melee hit is hurt back. */
    @SubscribeEvent
    public static void onHurtPost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !has(player, Items.THORNS_MODULE))
            return;
        var source = event.getSource();
        if (source.is(DamageTypes.THORNS) || source.getDirectEntity() == null || source.getDirectEntity() != source.getEntity())
            return;
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player)
            attacker.hurt(player.damageSources().thorns(player), Config.THORNS_DAMAGE.get().floatValue());
    }

    /** Dig affinity: air and water no longer slow the breaking down. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!has(player, Items.DIG_AFFINITY_MODULE))
            return;
        float speed = event.getNewSpeed();
        if (!player.onGround())
            speed *= 5f;
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))
            speed /= (float) Math.max(0.01, Math.min(1, player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED)));
        event.setNewSpeed(speed);
    }
}
