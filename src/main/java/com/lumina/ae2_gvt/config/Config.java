package com.lumina.ae2_gvt.config;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.lumina.twinstar.algorithm.execution.ExecutionSettings;
import net.minecraftforge.common.ForgeConfigSpec;

/** What the numbers of the generators and the antigravity armor are (the file is config/ae2_gvt-common.toml). */
@Mod.EventBusSubscriber(modid = com.lumina.ae2_gvt.AE2GravityTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---- generators ----
    public static final ForgeConfigSpec.LongValue CRYSTAL_OUTPUT;
    public static final ForgeConfigSpec.DoubleValue PARADOX_DUST_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DUST_BURN_TICKS;
    public static final ForgeConfigSpec.LongValue GRAVITON_OUTPUT;
    public static final ForgeConfigSpec.IntValue GRAVITON_TICKS;
    public static final ForgeConfigSpec.LongValue ACCRETION_OUTPUT;
    public static final ForgeConfigSpec.IntValue ACCRETION_PER_HOLE;

    // ---- armor: flight and the modules' sizes ----
    public static final ForgeConfigSpec.IntValue FLIGHT_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_BONUS, STRENGTH_BONUS, ATTACK_SPEED_BONUS, MOVE_SPEED_BONUS, SWIM_SPEED_BONUS,
            JUMP_BONUS, STEP_BONUS, LUCK_BONUS, REACH_BONUS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION, THORNS_DAMAGE, EVASION_CHANCE, UNDYING_HEALTH;
    public static final ForgeConfigSpec.IntValue UNDYING_COOLDOWN, MAGNET_RANGE, DASH_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue DASH_SPEED;
    public static final ForgeConfigSpec.DoubleValue STOCK_COST, RECHARGE_FE;
    public static final ForgeConfigSpec.IntValue PICK_CRAFT_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue ARMOR_LINK_RANGE;
    public static final ForgeConfigSpec.LongValue FUSION_OUTPUT_BUFFER;
    public static final ForgeConfigSpec.IntValue GUI_POLL_TICKS, TOOL_DELAY_TICKS, LINK_ALL_RADIUS, LINK_ALL_MAX, FORCED_IDLE_MINUTES, FORCED_MAX_JOBS;

    static {
        BUILDER.comment("Generators").push("generators");
        CRYSTAL_OUTPUT = BUILDER.comment("Crystal Dust Generator: FE/t burning gravity or anti-gravity dust")
                .defineInRange("crystalOutput", 2_000L, 0L, Long.MAX_VALUE);
        PARADOX_DUST_MULTIPLIER = BUILDER.comment("Crystal Dust Generator: paradox dust makes this many times the output")
                .defineInRange("paradoxDustMultiplier", 2.0, 0.0, 1000.0);
        DUST_BURN_TICKS = BUILDER.comment("Crystal Dust Generator: ticks one dust burns")
                .defineInRange("dustBurnTicks", 40, 1, 72_000);
        GRAVITON_OUTPUT = BUILDER.comment("Graviton Generator: FE/t while it has a graviton to burn")
                .defineInRange("gravitonOutput", 200_000L, 0L, Long.MAX_VALUE);
        GRAVITON_TICKS = BUILDER.comment("Graviton Generator: ticks one graviton or anti-graviton burns")
                .defineInRange("gravitonTicks", 20, 1, 72_000);
        ACCRETION_OUTPUT = BUILDER.comment("Accretion Generator: FE/t next to a white hole")
                .defineInRange("accretionOutput", 1_000_000L, 0L, Long.MAX_VALUE);
        ACCRETION_PER_HOLE = BUILDER.comment("Accretion Generator: how many one white hole feeds in full (more share it)")
                .defineInRange("accretionPerHole", 4, 1, 6);
        BUILDER.pop();

        BUILDER.comment("Twin-star machines").push("machines");
        GUI_POLL_TICKS = BUILDER.comment("How often (ticks) a machine that takes items only through a GUI (a forge, Immersive Engineering's alloy smelter) is looked into for its products")
                .defineInRange("guiPollTicks", 1, 1, 1200);
        TOOL_DELAY_TICKS = BUILDER.comment("How long (ticks) such a machine is left alone between its last material and the tool right-click")
                .defineInRange("toolDelayTicks", 1, 0, 200);
        FORCED_IDLE_MINUTES = BUILDER.comment("Forced orders (start although materials are missing): minutes a job that still lacks materials may make no progress before it is cancelled and gives everything back")
                .defineInRange("forcedIdleMinutes", 30, 1, 10_080);
        FORCED_MAX_JOBS = BUILDER.comment("Forced orders: the most that may wait for missing materials at once on one hub")
                .defineInRange("forcedMaxJobs", 4, 1, 64);
        LINK_ALL_RADIUS = BUILDER.comment("Machine Linker, Ctrl + right click: links every block of the same kind within this many blocks of the one clicked")
                .defineInRange("linkAllRadius", 16, 1, 64);
        LINK_ALL_MAX = BUILDER.comment("Machine Linker, Ctrl + right click: the most machines one click links")
                .defineInRange("linkAllMax", 64, 1, 512);
        BUILDER.pop();

        BUILDER.comment("Fusion generator").push("fusion");
        FUSION_OUTPUT_BUFFER = BUILDER.comment("How much of the energy a reaction gives back the energy ports hold for cables to draw (FE);",
                "what does not fit goes to the ME network")
                .defineInRange("outputBuffer", 1_000_000_000_000L, 0L, Long.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Antigravity armor").push("armor");
        FLIGHT_INTERVAL = BUILDER.comment("Flying: ticks between the anti-gravitons it takes from the linked network")
                .defineInRange("flightInterval", 5, 1, 72_000);
        MAX_HEALTH_BONUS = BUILDER.comment("Max health module: health added").defineInRange("maxHealthBonus", 8.0, 0.0, 1000.0);
        STRENGTH_BONUS = BUILDER.comment("Strength module: attack damage added").defineInRange("strengthBonus", 3.0, 0.0, 1000.0);
        ATTACK_SPEED_BONUS = BUILDER.comment("Attack speed module: attack speed added").defineInRange("attackSpeedBonus", 1.0, 0.0, 100.0);
        MOVE_SPEED_BONUS = BUILDER.comment("Move speed module: fraction added (0.3 = +30%)").defineInRange("moveSpeedBonus", 0.3, 0.0, 10.0);
        SWIM_SPEED_BONUS = BUILDER.comment("Swim speed module: fraction added").defineInRange("swimSpeedBonus", 0.5, 0.0, 10.0);
        JUMP_BONUS = BUILDER.comment("Jump height module: jump strength added (a jump is 0.42)").defineInRange("jumpBonus", 0.25, 0.0, 10.0);
        STEP_BONUS = BUILDER.comment("Step assist module: step height added (0.6 is a vanilla step)").defineInRange("stepBonus", 0.5, 0.0, 10.0);
        LUCK_BONUS = BUILDER.comment("Luck module: luck added").defineInRange("luckBonus", 2.0, 0.0, 100.0);
        REACH_BONUS = BUILDER.comment("Reach module: blocks added to the reach for blocks and entities").defineInRange("reachBonus", 3.0, 0.0, 100.0);
        DAMAGE_REDUCTION = BUILDER.comment("Damage reduction module: points taken off every hit").defineInRange("damageReduction", 3.0, 0.0, 1000.0);
        THORNS_DAMAGE = BUILDER.comment("Thorns module: damage done back to a melee attacker").defineInRange("thornsDamage", 4.0, 0.0, 1000.0);
        EVASION_CHANCE = BUILDER.comment("Evasion module: chance that a hit does nothing").defineInRange("evasionChance", 0.15, 0.0, 1.0);
        UNDYING_HEALTH = BUILDER.comment("Undying module: fraction of the health left after a deadly hit").defineInRange("undyingHealth", 0.5, 0.01, 1.0);
        UNDYING_COOLDOWN = BUILDER.comment("Undying module: ticks before it can save again").defineInRange("undyingCooldown", 2400, 0, 1_728_000);
        MAGNET_RANGE = BUILDER.comment("Magnet module: blocks in which it pulls items and experience").defineInRange("magnetRange", 8, 1, 64);
        DASH_SPEED = BUILDER.comment("Dash module: speed of the dash").defineInRange("dashSpeed", 1.8, 0.1, 20.0);
        DASH_COOLDOWN = BUILDER.comment("Dash module: ticks between dashes").defineInRange("dashCooldown", 30, 0, 72_000);
        STOCK_COST = BUILDER.comment("Auto stock and pick craft modules: AE taken from the network for every item brought")
                .defineInRange("stockCost", 0.2, 0.0, 10_000.0);
        RECHARGE_FE = BUILDER.comment("Recharge module: FE a carried item is charged by per second (2 FE = 1 AE from the network)")
                .defineInRange("rechargeFe", 20_000.0, 0.0, 1_000_000_000.0);
        PICK_CRAFT_AMOUNT = BUILDER.comment("Pick craft module: the amount the order screen starts with when the network has none")
                .defineInRange("pickCraftAmount", 1, 1, 100_000);
        ARMOR_LINK_RANGE = BUILDER.comment("Armor link: true = the wearer must be within the range of an access point of the network (as a wireless terminal);",
                "false = a linked piece reaches its network from anywhere, in any dimension, as long as the access point it is linked to is loaded")
                .define("armorLinkRange", false);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();



    /** The engine has no config: the mod hands it these when the file is loaded and when it is changed. */
    @SubscribeEvent
    public static void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC)
        {
            ExecutionSettings.set(GUI_POLL_TICKS.get(), TOOL_DELAY_TICKS.get());
            ExecutionSettings.setForced(FORCED_IDLE_MINUTES.get() * 60L, FORCED_MAX_JOBS.get());
        }
    }
}
