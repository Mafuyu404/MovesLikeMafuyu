package com.mafuyu404.moveslikemafuyu;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final String CONFIG_PREFIX = MovesLikeMafuyu.MOD_ID + ".configuration.";
    private static final Map<String, ModConfigSpec.BooleanValue> BOOLEAN_VALUES = new LinkedHashMap<>();

    public static final ModConfigSpec.BooleanValue ENABLE_SLIDE;
    public static final ModConfigSpec.BooleanValue ENABLE_DAP;
    public static final ModConfigSpec.BooleanValue ENABLE_SLIDE_REPEAT;
    public static final ModConfigSpec.BooleanValue ENABLE_SLIDE_KNOCK;
    public static final ModConfigSpec.BooleanValue ENABLE_SLIDE_IGNORE_EDGE_PROTECTION;
    public static final ModConfigSpec.BooleanValue ENABLE_SLIDE_DOUBLE_TAP_TRIGGER;
    public static final ModConfigSpec.BooleanValue ENABLE_ROLL;
    public static final ModConfigSpec.BooleanValue ENABLE_AIR_ROLL;
    public static final ModConfigSpec.BooleanValue ENABLE_ROLL_INVULNERABILITY;
    public static final ModConfigSpec.BooleanValue ENABLE_ROLL_CAMERA;
    public static final ModConfigSpec.BooleanValue ENABLE_ROLL_IGNORE_EDGE_PROTECTION;
    public static final ModConfigSpec.BooleanValue ENABLE_SPRINT_ROLL_DOUBLE_TAP_TRIGGER;
    public static final ModConfigSpec.BooleanValue ENABLE_CLIMB;
    public static final ModConfigSpec.BooleanValue ENABLE_CLIMB_JUMP;
    public static final ModConfigSpec.BooleanValue ENABLE_FALLING_RESCUE;
    public static final ModConfigSpec.BooleanValue ENABLE_SHALLOW_SWIMMING;
    public static final ModConfigSpec.BooleanValue ENABLE_SWIMMING_BOOST;
    public static final ModConfigSpec.BooleanValue ENABLE_FREESTYLE;
    public static final ModConfigSpec.BooleanValue ENABLE_SWIMMING_PUSH;
    public static final ModConfigSpec.BooleanValue ENABLE_CRAW;
    public static final ModConfigSpec.BooleanValue ENABLE_CRAW_SLIDE;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAP;
    public static final ModConfigSpec.BooleanValue ENABLE_JUMP_CANCEL_CRAW;
    public static final ModConfigSpec.BooleanValue ENABLE_AUTO_DODGE;

    public static final ModConfigSpec.IntValue SLIDE_DURATION;
    public static final ModConfigSpec.IntValue SLIDE_AIR_DURATION;
    public static final ModConfigSpec.IntValue DAP_TIMES;
    public static final ModConfigSpec.IntValue SLIDE_COOLDOWN;
    public static final ModConfigSpec.IntValue CLIMB_JUMP_COOLDOWN;
    public static final ModConfigSpec.IntValue SWIMMING_BOOST_COOLDOWN;
    public static final ModConfigSpec.IntValue SWIMMING_BOOST_AIR_COST;
    public static final ModConfigSpec.DoubleValue SWIMMING_BOOST_STRENGTH;

    public static final ModConfigSpec.IntValue ROLL_DOUBLE_PRESS_DELAY;
    public static final ModConfigSpec.IntValue ROLL_AIR_TIMER;
    public static final ModConfigSpec.IntValue ROLL_DURATION;
    public static final ModConfigSpec.IntValue ROLL_SHIFT_START_TICK;
    public static final ModConfigSpec.IntValue ROLL_SHIFT_END_TICK;
    public static final ModConfigSpec.IntValue ROLL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ROLL_ACTION_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ROLL_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ROLL_START_SPEED;
    public static final ModConfigSpec.DoubleValue ROLL_PEAK_SPEED;
    public static final ModConfigSpec.DoubleValue ROLL_END_SPEED;
    public static final ModConfigSpec.DoubleValue ROLL_AIR_VERTICAL_SPEED;

    public static final ModConfigSpec.IntValue CRAW_DOUBLE_PRESS_DELAY;
    public static final ModConfigSpec.IntValue LEAP_JUMP_TIMER;
    public static final ModConfigSpec.IntValue LEAP_AUTO_CRAW_TICKS;
    public static final ModConfigSpec.DoubleValue LEAP_FORWARD_BOOST;
    public static final ModConfigSpec.DoubleValue LEAP_VERTICAL_BOOST;
    public static final ModConfigSpec.DoubleValue CRAW_LEAP_FORWARD_BOOST;
    public static final ModConfigSpec.DoubleValue CRAW_LEAP_VERTICAL_BOOST;

    public static final ModConfigSpec.DoubleValue SLIDE_START_BOOST;
    public static final ModConfigSpec.DoubleValue SLIDE_AIR_FORWARD_BOOST;
    public static final ModConfigSpec.DoubleValue SLIDE_AIR_FALL_ACCELERATION;
    public static final ModConfigSpec.DoubleValue SLIDE_INITIAL_DAP_VERTICAL_BOOST;
    public static final ModConfigSpec.DoubleValue SLIDE_DAP_VERTICAL_BOOST;
    public static final ModConfigSpec.DoubleValue SLIDE_DAP_MOTION_DECAY;
    public static final ModConfigSpec.IntValue SLIDE_KNOCK_DELAY;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SLIDE_KNOCK_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CLIMB_BLOCK_WHITELIST;

    public static final ModConfigSpec SPEC;
    static {
        pushCategory("slide");
        ENABLE_SLIDE = defineBoolean("enableSlide", true);
        ENABLE_DAP = defineBoolean("enableDap", true);
        ENABLE_SLIDE_REPEAT = defineBoolean("enableSlideRepeat", true);
        ENABLE_SLIDE_KNOCK = defineBoolean("enableSlideKnock", true);
        ENABLE_SLIDE_IGNORE_EDGE_PROTECTION = defineBoolean("enableSlideIgnoreEdgeProtection", true);
        ENABLE_SLIDE_DOUBLE_TAP_TRIGGER = defineBoolean("enableSlideDoubleTapTrigger", false);
        ENABLE_ROLL = defineBoolean("enableRoll", true);
        ENABLE_AIR_ROLL = defineBoolean("enableAirRoll", true);
        ENABLE_ROLL_INVULNERABILITY = defineBoolean("enableRollInvulnerability", true);
        ENABLE_ROLL_CAMERA = defineBoolean("enableRollCamera", true);
        ENABLE_ROLL_IGNORE_EDGE_PROTECTION = defineBoolean("enableRollIgnoreEdgeProtection", true);
        ENABLE_SPRINT_ROLL_DOUBLE_TAP_TRIGGER = defineBoolean("enableSprintRollDoubleTapTrigger", false);
        BUILDER.pop();

        pushCategory("climb");
        ENABLE_CLIMB = defineBoolean("enableClimb", true);
        ENABLE_CLIMB_JUMP = defineBoolean("enableClimbJump", true);
        ENABLE_FALLING_RESCUE = defineBoolean("enableFallingRescue", true);
        BUILDER.pop();

        pushCategory("climb_attributes");
        CLIMB_JUMP_COOLDOWN = defineInt("ClimbJumpCooldown", 60, 0, 20 * 60);
        BUILDER.pop();

        pushCategory("swimming");
        ENABLE_SHALLOW_SWIMMING = defineBoolean("enableShallowSwimming", true);
        ENABLE_SWIMMING_BOOST = defineBoolean("enableSwimmingBoost", true);
        ENABLE_FREESTYLE = defineBoolean("enableFreestyle", true);
        ENABLE_SWIMMING_PUSH = defineBoolean("enableSwimmingPush", true);
        BUILDER.pop();

        pushCategory("swimming_attributes");
        SWIMMING_BOOST_COOLDOWN = defineInt("SwimmingBoostCooldown", 60, 0, 20 * 60);
        SWIMMING_BOOST_AIR_COST = defineInt("SwimmingBoostAirCost", 30, 0, 300);
        SWIMMING_BOOST_STRENGTH = defineDouble("SwimmingBoostStrength", 0.4, 0, 5);
        BUILDER.pop();

        pushCategory("crawling");
        ENABLE_CRAW = defineBoolean("enableCraw", true);
        ENABLE_CRAW_SLIDE = defineBoolean("enableCrawSlide", true);
        ENABLE_LEAP = defineBoolean("enableLeap", true);
        ENABLE_JUMP_CANCEL_CRAW = defineBoolean("enableJumpCancelCraw", false);
        BUILDER.pop();

        pushCategory("auto_dodge");
        ENABLE_AUTO_DODGE = defineBoolean("enableAutoDodge", true);
        BUILDER.pop();

        pushCategory("roll_attributes");
        ROLL_DOUBLE_PRESS_DELAY = defineInt("RollDoublePressDelay", 250, 0, 2000);
        ROLL_AIR_TIMER = defineInt("RollAirTimer", 500, 0, 5000);
        ROLL_DURATION = defineInt("RollDuration", 14, 1, 200);
        ROLL_SHIFT_START_TICK = defineInt("RollShiftStartTick", 3, 0, 200);
        ROLL_SHIFT_END_TICK = defineInt("RollShiftEndTick", 10, 0, 200);
        ROLL_COOLDOWN = defineInt("RollCooldown", 0, 0, 20 * 60);
        ROLL_ACTION_SPEED_MULTIPLIER = defineDouble("RollActionSpeedMultiplier", 1.2, 0.1, 10);
        ROLL_SPEED_MULTIPLIER = defineDouble("RollSpeedMultiplier", 1.2, 0, 10);
        ROLL_START_SPEED = defineDouble("RollStartSpeed", 0.225, 0, 5);
        ROLL_PEAK_SPEED = defineDouble("RollPeakSpeed", 0.35, 0, 5);
        ROLL_END_SPEED = defineDouble("RollEndSpeed", 0.16, 0, 5);
        ROLL_AIR_VERTICAL_SPEED = defineDouble("RollAirVerticalSpeed", 0.19, -5, 5);
        BUILDER.pop();

        pushCategory("crawl_attributes");
        CRAW_DOUBLE_PRESS_DELAY = defineInt("CrawDoublePressDelay", 250, 0, 2000);
        LEAP_JUMP_TIMER = defineInt("LeapJumpTimer", 500, 0, 5000);
        LEAP_AUTO_CRAW_TICKS = defineInt("LeapAutoCrawTicks", 8, 0, 200);
        LEAP_FORWARD_BOOST = defineDouble("LeapForwardBoost", 0.35, 0, 5);
        LEAP_VERTICAL_BOOST = defineDouble("LeapVerticalBoost", 0.42, -5, 5);
        CRAW_LEAP_FORWARD_BOOST = defineDouble("CrawLeapForwardBoost", 0.25, 0, 5);
        CRAW_LEAP_VERTICAL_BOOST = defineDouble("CrawLeapVerticalBoost", 0.15, -5, 5);
        BUILDER.pop();

        pushCategory("slide_attributes");
        SLIDE_DURATION = defineInt("SlideDuration", 25, 0, 20 * 60);
        SLIDE_AIR_DURATION = defineInt("SlideAirDuration", 30, 0, 20 * 60);
        DAP_TIMES = defineInt("DapTimes", 2, 0, 64);
        SLIDE_COOLDOWN = defineInt("SlideCooldown", 60, 0, 20 * 60);
        SLIDE_START_BOOST = defineDouble("SlideStartBoost", 0.5, 0, 5);
        SLIDE_AIR_FORWARD_BOOST = defineDouble("SlideAirForwardBoost", 0.1, 0, 5);
        SLIDE_AIR_FALL_ACCELERATION = defineDouble("SlideAirFallAcceleration", -0.025, -5, 5);
        SLIDE_INITIAL_DAP_VERTICAL_BOOST = defineDouble("SlideInitialDapVerticalBoost", 0.5, -5, 5);
        SLIDE_DAP_VERTICAL_BOOST = defineDouble("SlideDapVerticalBoost", 0.7, -5, 5);
        SLIDE_DAP_MOTION_DECAY = defineDouble("SlideDapMotionDecay", 0.92, 0, 2);
        SLIDE_KNOCK_DELAY = defineInt("SlideKnockDelay", 500, 0, 10000);
        BUILDER.pop();

        pushCategory("lists");
        SLIDE_KNOCK_BLACKLIST = defineStringList("SlideKnockBlacklist", List.of("minecraft:item"));
        CLIMB_BLOCK_WHITELIST = defineStringList("ClimbBlockWhitelist", List.of("minecraft:stone"));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean enable(String key) {
        ModConfigSpec.BooleanValue value = BOOLEAN_VALUES.get("enable" + key);
        return value != null && value.get();
    }

    private static void pushCategory(String key) {
        BUILDER.translation(configKey("category." + key)).push(key);
    }

    private static String configKey(String key) {
        return CONFIG_PREFIX + key;
    }

    private static ModConfigSpec.BooleanValue defineBoolean(String key, boolean defaultValue) {
        ModConfigSpec.BooleanValue value = BUILDER.translation(configKey(key)).define(key, defaultValue);
        BOOLEAN_VALUES.put(key, value);
        return value;
    }

    private static ModConfigSpec.IntValue defineInt(String key, int defaultValue, int min, int max) {
        ModConfigSpec.IntValue value = BUILDER.translation(configKey(key)).defineInRange(key, defaultValue, min, max);
        return value;
    }

    private static ModConfigSpec.DoubleValue defineDouble(String key, double defaultValue, double min, double max) {
        return BUILDER.translation(configKey(key)).defineInRange(key, defaultValue, min, max);
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> defineStringList(String key, List<String> defaultValue) {
        return BUILDER.translation(configKey(key)).defineList(key, defaultValue, entry -> entry instanceof String);
    }
}
