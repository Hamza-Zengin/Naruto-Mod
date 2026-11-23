package com.sekwah.narutomod.config;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.gui.BarDesigns;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoConfig {

    public static final String SERVER = "server";
    public static final String CATEGORY_WEAPONS = "weapons";

    // Settings about players
    public static final String CATEGORY_PLAYER = "player";

    public static final String CLIENT = "client";

    public static final String CATEGORY_UI = "ui";
    public static final String ENERGY_BARS = "energy_bars";

    public static final ModConfigSpec MOD_CONFIG;

    private static final ModConfigSpec.DoubleValue CONFIG_STARTING_CHAKRA;
    public static float startingChakra = 100.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_STARTING_STAMINA;
    public static float startingStamina = 100.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_MAX_CHAKRA;
    public static float maxChakra = 1000.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_MAX_STAMINA;
    public static float maxStamina = 800.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_CHAKRA_REGEN_RATE;
    public static float chakraRegenRate = 0.05f;

    private static final ModConfigSpec.DoubleValue CONFIG_STAMINA_REGEN_RATE;
    public static float staminaRegenRate = 0.4f;

    private static final ModConfigSpec.BooleanValue CONFIG_KUNAI_BLOCK_DAMAGE;
    public static boolean kunaiBlockDamage = true;

    private static final ModConfigSpec.DoubleValue CONFIG_KUNAI_EXPLOSION_RADIUS;
    public static float kunaiExplosionRadius = 3.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_PAPERBOMB_EXPLOSION_RADIUS;
    public static float paperbombExplosionRadius = 4.0f;

    private static final ModConfigSpec.BooleanValue CONFIG_PAPERBOMB_BLOCK_DAMAGE;
    public static boolean paperbombBlockDamage = true;

    private static final ModConfigSpec.IntValue CONFIG_JUTSU_CAST_DELAY;
    public static int jutsuCastDelay = 20;

    private static final ModConfigSpec.IntValue CONFIG_CHAKRA_BAR_DESIGN;
    public static int chakraBarDesign = 0;

    private static final ModConfigSpec.BooleanValue CONFIG_AUTO_HIDE_HUD;
    public static boolean autoHideHud = true;

    private static final ModConfigSpec.IntValue CONFIG_HUD_FADE_DELAY_TICKS;
    public static int hudFadeDelayTicks = 60;

    private static final ModConfigSpec.DoubleValue CONFIG_CAMERA_SHAKE_MULTIPLIER;
    public static double cameraShakeMultiplier = 1.0d;

    private static final ModConfigSpec.DoubleValue CONFIG_WATER_WALK_SPEED;
    public static float waterWalkSpeed = 1.0f;

    private static final ModConfigSpec.DoubleValue CONFIG_KAWARIMI_MIN_DISTANCE;
    public static double kawarimiMinDistance = 5.0d;

    private static final ModConfigSpec.DoubleValue CONFIG_KAWARIMI_MAX_DISTANCE;
    public static double kawarimiMaxDistance = 14.0d;

    private static final ModConfigSpec.IntValue CONFIG_KAWARIMI_MIN_COOLDOWN;
    public static int kawarimiMinCooldownTicks = 60;

    private static final ModConfigSpec.IntValue CONFIG_KAWARIMI_MAX_COOLDOWN;
    public static int kawarimiMaxCooldownTicks = 200;

    private static final ModConfigSpec.IntValue CONFIG_KAWARIMI_MIN_CAST;
    public static int kawarimiMinCastTicks = 2;

    private static final ModConfigSpec.IntValue CONFIG_KAWARIMI_MAX_CAST;
    public static int kawarimiMaxCastTicks = 15;

    private static final ModConfigSpec.DoubleValue CONFIG_WATER_LIGHTNING_DMG_MULT;
    public static float waterLightningDamageMultiplier = 1.25f;

    private static final ModConfigSpec.IntValue CONFIG_WATER_LIGHTNING_SLOWNESS_TICKS;
    public static int waterLightningSlownessTicks = 40;

    private static final ModConfigSpec.DoubleValue CONFIG_WIND_FIRE_SPEED_MULT;
    public static float windFireSpeedMultiplier = 1.20f;

    private static final ModConfigSpec.DoubleValue CONFIG_WIND_FIRE_RADIUS_MULT;
    public static float windFireRadiusMultiplier = 1.20f;

    private static final ModConfigSpec.DoubleValue CONFIG_EARTH_LIGHTNING_DMG_MULT;
    public static float earthLightningDamageMultiplier = 0.50f;

    private static final ModConfigSpec.IntValue CONFIG_AMATERASU_DURATION_TICKS;
    public static int amaterasuDurationTicks = 200;

    private static final ModConfigSpec.BooleanValue CONFIG_JUTSU_WORLD_DAMAGE;
    public static boolean jutsuWorldDamage = true;

    private static final ModConfigSpec.BooleanValue CONFIG_AMATERASU_PLACE_FIRE;
    public static boolean amaterasuPlaceFire = true;

    static {
        ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();

        // ===========================================================
        // Server Settings
        // ===========================================================
        configBuilder.comment("Server based settings").push(SERVER);

        configBuilder.comment("Player settings").push(CATEGORY_PLAYER);

        configBuilder.comment("Stuff such as regen rates and maximum (will likely change with updates, e.g. stats system)").push(ENERGY_BARS);

        CONFIG_STARTING_CHAKRA = configBuilder.comment("Starting Base Chakra")
                .defineInRange("startingChakra", 100F, 1d, Integer.MAX_VALUE);

        CONFIG_STARTING_STAMINA = configBuilder.comment("Starting Base Stamina")
                .defineInRange("startingStamina", 100F, 1d, Integer.MAX_VALUE);

        CONFIG_MAX_CHAKRA = configBuilder.comment("Max Chakra")
                .defineInRange("maxChakra", 1000F, 0d, Integer.MAX_VALUE);

        CONFIG_MAX_STAMINA = configBuilder.comment("Max Stamina")
                .defineInRange("maxStamina", 800F, 0d, Integer.MAX_VALUE);

        CONFIG_CHAKRA_REGEN_RATE = configBuilder.comment("Chakra regeneration per tick")
                .defineInRange("chakraRegenRate", 0.05d, 0.001d, 100d);

        CONFIG_STAMINA_REGEN_RATE = configBuilder.comment("Stamina regeneration per tick")
                .defineInRange("staminaRegenRate", 0.4d, 0.001d, 100d);

        configBuilder.pop();

        CONFIG_WATER_WALK_SPEED = configBuilder.comment("Water walking horizontal speed multiplier")
                .defineInRange("waterWalkSpeed", 1.0d, 0.1d, 10.0d);

        CONFIG_KAWARIMI_MIN_DISTANCE = configBuilder.comment("Kawarimi minimum teleport distance at max mastery")
                .defineInRange("kawarimiMinDistance", 5.0d, 1.0d, 64.0d);

        CONFIG_KAWARIMI_MAX_DISTANCE = configBuilder.comment("Kawarimi maximum teleport distance at 0 mastery")
                .defineInRange("kawarimiMaxDistance", 14.0d, 1.0d, 64.0d);

        CONFIG_KAWARIMI_MIN_COOLDOWN = configBuilder.comment("Kawarimi minimum cooldown in ticks at max mastery")
                .defineInRange("kawarimiMinCooldownTicks", 60, 0, 2400);

        CONFIG_KAWARIMI_MAX_COOLDOWN = configBuilder.comment("Kawarimi maximum cooldown in ticks at 0 mastery")
                .defineInRange("kawarimiMaxCooldownTicks", 200, 0, 2400);

        CONFIG_KAWARIMI_MIN_CAST = configBuilder.comment("Kawarimi minimum cast delay in ticks at max mastery")
                .defineInRange("kawarimiMinCastTicks", 2, 0, 200);

        CONFIG_KAWARIMI_MAX_CAST = configBuilder.comment("Kawarimi maximum cast delay in ticks at 0 mastery")
                .defineInRange("kawarimiMaxCastTicks", 15, 0, 200);

        CONFIG_WATER_LIGHTNING_DMG_MULT = configBuilder.comment("Damage multiplier when Lightning hits a wet or WaterBullet-soaked target")
                .defineInRange("waterLightningDamageMultiplier", 1.25d, 1.0d, 5.0d);

        CONFIG_WATER_LIGHTNING_SLOWNESS_TICKS = configBuilder.comment("Slowness duration in ticks applied by Water + Lightning conductivity")
                .defineInRange("waterLightningSlownessTicks", 40, 0, 600);

        CONFIG_WIND_FIRE_SPEED_MULT = configBuilder.comment("Velocity multiplier when FireballJutsuEntity is boosted by Wind")
                .defineInRange("windFireSpeedMultiplier", 1.20d, 1.0d, 5.0d);

        CONFIG_WIND_FIRE_RADIUS_MULT = configBuilder.comment("Explosion radius multiplier when FireballJutsuEntity is boosted by Wind")
                .defineInRange("windFireRadiusMultiplier", 1.20d, 1.0d, 5.0d);

        CONFIG_EARTH_LIGHTNING_DMG_MULT = configBuilder.comment("Damage multiplier when Lightning hits a target shielded behind MudWallEntity")
                .defineInRange("earthLightningDamageMultiplier", 0.50d, 0.0d, 1.0d);

        CONFIG_AMATERASU_DURATION_TICKS = configBuilder.comment("Duration in ticks before Amaterasu black flame blocks self-extinguish (20 ticks = 1 second)")
                .defineInRange("amaterasuDurationTicks", 200, 20, 6000);

        CONFIG_JUTSU_WORLD_DAMAGE = configBuilder.comment("Whether jutsu abilities are allowed to place or break blocks in the world")
                .define("jutsuWorldDamage", true);

        CONFIG_AMATERASU_PLACE_FIRE = configBuilder.comment("Whether Amaterasu places temporary black flame blocks on the ground")
                .define("amaterasuPlaceFire", true);

        configBuilder.pop();

        configBuilder.comment("Variables for weapons").push(CATEGORY_WEAPONS);

        CONFIG_KUNAI_BLOCK_DAMAGE = configBuilder.comment("Explosive Kunai block damage")
                .define("kunaiExplosionBreakBlocks", true);

        CONFIG_KUNAI_EXPLOSION_RADIUS = configBuilder.comment("Explosive Kunai explosion radius")
                .defineInRange("kunaiExplosionRadius", 3d, 1d, 100d);

        CONFIG_PAPERBOMB_EXPLOSION_RADIUS = configBuilder.comment("Paper Bomb explosion radius")
                .defineInRange("paperBombExplosionRadius", 4.0F, 1d, 100d);

        CONFIG_PAPERBOMB_BLOCK_DAMAGE = configBuilder.comment("Paper Bomb block damage")
                .define("paperBombExplosionBreakBlocks", true);

        CONFIG_JUTSU_CAST_DELAY = configBuilder.comment("Jutsu activation delay (20 per second)")
                .defineInRange("jutsuActivateDelay", 15, 0, Integer.MAX_VALUE);

        configBuilder.pop();

        configBuilder.pop();

        // ===========================================================
        // Client Settings
        // ===========================================================
        configBuilder.comment("Client based settings").push(CLIENT);

        configBuilder.comment("Variables for UI").push(CATEGORY_UI);
        CONFIG_CHAKRA_BAR_DESIGN = configBuilder.comment("Design for the chakra bar")
                .defineInRange("chakraBarDesign", 0, 0, BarDesigns.BarInfo.values().length);

        CONFIG_AUTO_HIDE_HUD = configBuilder.comment("Automatically fade out the Chakra/Stamina HUD when full and out of combat")
                .define("autoHideHud", true);

        CONFIG_HUD_FADE_DELAY_TICKS = configBuilder.comment("Delay in ticks before the HUD fades out (20 ticks = 1 second)")
                .defineInRange("hudFadeDelayTicks", 60, 0, 1200);

        CONFIG_CAMERA_SHAKE_MULTIPLIER = configBuilder.comment("Camera shake intensity multiplier (0.0 disables screen shake)")
                .defineInRange("cameraShakeMultiplier", 1.0d, 0.0d, 2.0d);

        configBuilder.pop();

        configBuilder.pop();

        MOD_CONFIG = configBuilder.build();
    }

    public static void loadVariables() {
        startingChakra = CONFIG_STARTING_CHAKRA.get().floatValue();
        startingStamina = CONFIG_STARTING_STAMINA.get().floatValue();
        maxChakra = CONFIG_MAX_CHAKRA.get().floatValue();
        maxStamina = CONFIG_MAX_STAMINA.get().floatValue();
        chakraRegenRate = CONFIG_CHAKRA_REGEN_RATE.get().floatValue();
        staminaRegenRate = CONFIG_STAMINA_REGEN_RATE.get().floatValue();
        kunaiBlockDamage = CONFIG_KUNAI_BLOCK_DAMAGE.get();
        kunaiExplosionRadius = CONFIG_KUNAI_EXPLOSION_RADIUS.get().floatValue();
        paperbombExplosionRadius = CONFIG_PAPERBOMB_EXPLOSION_RADIUS.get().floatValue();
        paperbombBlockDamage = CONFIG_PAPERBOMB_BLOCK_DAMAGE.get();
        chakraBarDesign = CONFIG_CHAKRA_BAR_DESIGN.get();
        autoHideHud = CONFIG_AUTO_HIDE_HUD.get();
        hudFadeDelayTicks = CONFIG_HUD_FADE_DELAY_TICKS.get();
        cameraShakeMultiplier = CONFIG_CAMERA_SHAKE_MULTIPLIER.get();
        jutsuCastDelay = CONFIG_JUTSU_CAST_DELAY
                .get();
        waterWalkSpeed = CONFIG_WATER_WALK_SPEED.get().floatValue();
        kawarimiMinDistance = CONFIG_KAWARIMI_MIN_DISTANCE.get();
        kawarimiMaxDistance = CONFIG_KAWARIMI_MAX_DISTANCE.get();
        kawarimiMinCooldownTicks = CONFIG_KAWARIMI_MIN_COOLDOWN.get();
        kawarimiMaxCooldownTicks = CONFIG_KAWARIMI_MAX_COOLDOWN.get();
        kawarimiMinCastTicks = CONFIG_KAWARIMI_MIN_CAST.get();
        kawarimiMaxCastTicks = CONFIG_KAWARIMI_MAX_CAST.get();
        waterLightningDamageMultiplier = CONFIG_WATER_LIGHTNING_DMG_MULT.get().floatValue();
        waterLightningSlownessTicks = CONFIG_WATER_LIGHTNING_SLOWNESS_TICKS.get();
        windFireSpeedMultiplier = CONFIG_WIND_FIRE_SPEED_MULT.get().floatValue();
        windFireRadiusMultiplier = CONFIG_WIND_FIRE_RADIUS_MULT.get().floatValue();
        earthLightningDamageMultiplier = CONFIG_EARTH_LIGHTNING_DMG_MULT.get().floatValue();
        amaterasuDurationTicks = CONFIG_AMATERASU_DURATION_TICKS.get();
        jutsuWorldDamage = CONFIG_JUTSU_WORLD_DAMAGE.get();
        amaterasuPlaceFire = CONFIG_AMATERASU_PLACE_FIRE.get();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        loadVariables();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        loadVariables();
    }
}
