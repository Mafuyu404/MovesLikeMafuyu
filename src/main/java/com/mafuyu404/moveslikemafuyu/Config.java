package com.mafuyu404.moveslikemafuyu;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
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
        BUILDER.push("slide");
        ENABLE_SLIDE = defineBoolean("enableSlide", true, "Enable slide: press sneak while sprinting to slide.");
        ENABLE_DAP = defineBoolean("enableDap", true, "Enable water dap while sliding over water.");
        ENABLE_SLIDE_REPEAT = defineBoolean("enableSlideRepeat", true, "Refresh slide duration while sliding in the air.");
        ENABLE_SLIDE_KNOCK = defineBoolean("enableSlideKnock", true, "Knock living entities hit during a slide.");
        ENABLE_SLIDE_IGNORE_EDGE_PROTECTION = defineBoolean("enableSlideIgnoreEdgeProtection", true, "Ignore sneak edge protection while sliding.");
        ENABLE_SLIDE_DOUBLE_TAP_TRIGGER = defineBoolean("enableSlideDoubleTapTrigger", false, "Require double tapping sneak to start slide.");
        ENABLE_ROLL = defineBoolean("enableRoll", true, "Enable roll.");
        ENABLE_AIR_ROLL = defineBoolean("enableAirRoll", true, "Enable triggering roll while airborne.");
        ENABLE_ROLL_INVULNERABILITY = defineBoolean("enableRollInvulnerability", true, "Enable invulnerability frames during roll.");
        ENABLE_ROLL_CAMERA = defineBoolean("enableRollCamera", true, "Enable camera rotation during roll.");
        ENABLE_ROLL_IGNORE_EDGE_PROTECTION = defineBoolean("enableRollIgnoreEdgeProtection", true, "Ignore sneak edge protection while rolling.");
        ENABLE_SPRINT_ROLL_DOUBLE_TAP_TRIGGER = defineBoolean("enableSprintRollDoubleTapTrigger", false, "Require double tapping sprint to roll while already sprinting.");
        BUILDER.pop();

        BUILDER.push("climb");
        ENABLE_CLIMB = defineBoolean("enableClimb", true, "Enable wall climb.");
        ENABLE_CLIMB_JUMP = defineBoolean("enableClimbJump", true, "Enable jumping from climbable surfaces while sneaking.");
        ENABLE_FALLING_RESCUE = defineBoolean("enableFallingRescue", true, "Enable falling rescue on climbable surfaces.");
        BUILDER.pop();

        BUILDER.push("climb_attributes");
        CLIMB_JUMP_COOLDOWN = defineInt("ClimbJumpCooldown", 60, 0, 20 * 60, "Climb jump cooldown in ticks.");
        BUILDER.pop();

        BUILDER.push("swimming");
        ENABLE_SHALLOW_SWIMMING = defineBoolean("enableShallowSwimming", true, "Enable sprint swimming in shallow water.");
        ENABLE_SWIMMING_BOOST = defineBoolean("enableSwimmingBoost", true, "Enable sprint boost while swimming.");
        ENABLE_FREESTYLE = defineBoolean("enableFreestyle", true, "Enable freestyle surface swimming.");
        ENABLE_SWIMMING_PUSH = defineBoolean("enableSwimmingPush", true, "Enable jumping from freestyle swimming into slide.");
        BUILDER.pop();

        BUILDER.push("swimming_attributes");
        SWIMMING_BOOST_COOLDOWN = defineInt("SwimmingBoostCooldown", 60, 0, 20 * 60, "Swimming boost cooldown in ticks.");
        SWIMMING_BOOST_AIR_COST = defineInt("SwimmingBoostAirCost", 30, 0, 300, "Air supply consumed by swimming boost.");
        SWIMMING_BOOST_STRENGTH = defineDouble("SwimmingBoostStrength", 0.4, 0, 5, "Swimming boost movement strength.");
        BUILDER.pop();

        BUILDER.push("crawling");
        ENABLE_CRAW = defineBoolean("enableCraw", true, "Enable crawl.");
        ENABLE_CRAW_SLIDE = defineBoolean("enableCrawSlide", true, "Enable slide from crawl.");
        ENABLE_LEAP = defineBoolean("enableLeap", true, "Enable leap into crawl.");
        ENABLE_JUMP_CANCEL_CRAW = defineBoolean("enableJumpCancelCraw", false, "Enable jump to cancel crawl.");
        BUILDER.pop();

        BUILDER.push("auto_dodge");
        ENABLE_AUTO_DODGE = defineBoolean("enableAutoDodge", true, "Automatically dodge incoming projectiles.");
        BUILDER.pop();

        BUILDER.push("roll_attributes");
        ROLL_DOUBLE_PRESS_DELAY = defineInt("RollDoublePressDelay", 250, 0, 2000, "Maximum sprint-key double press interval in milliseconds.");
        ROLL_AIR_TIMER = defineInt("RollAirTimer", 500, 0, 5000, "Jump window in milliseconds for boosted air roll.");
        ROLL_DURATION = defineInt("RollDuration", 14, 1, 200, "Roll duration in ticks before action speed is applied.");
        ROLL_SHIFT_START_TICK = defineInt("RollShiftStartTick", 3, 0, 200, "Roll tick where forced sneaking starts.");
        ROLL_SHIFT_END_TICK = defineInt("RollShiftEndTick", 10, 0, 200, "Roll tick where forced sneaking ends.");
        ROLL_COOLDOWN = defineInt("RollCooldown", 0, 0, 20 * 60, "Roll cooldown in ticks.");
        ROLL_ACTION_SPEED_MULTIPLIER = defineDouble("RollActionSpeedMultiplier", 1.2, 0.1, 10, "Roll animation speed multiplier.");
        ROLL_SPEED_MULTIPLIER = defineDouble("RollSpeedMultiplier", 1.2, 0, 10, "Roll movement speed multiplier.");
        ROLL_START_SPEED = defineDouble("RollStartSpeed", 0.225, 0, 5, "Roll starting horizontal speed.");
        ROLL_PEAK_SPEED = defineDouble("RollPeakSpeed", 0.35, 0, 5, "Roll peak horizontal speed.");
        ROLL_END_SPEED = defineDouble("RollEndSpeed", 0.16, 0, 5, "Roll ending horizontal speed.");
        ROLL_AIR_VERTICAL_SPEED = defineDouble("RollAirVerticalSpeed", 0.19, -5, 5, "Vertical speed added by boosted air roll.");
        BUILDER.pop();

        BUILDER.push("crawl_attributes");
        CRAW_DOUBLE_PRESS_DELAY = defineInt("CrawDoublePressDelay", 250, 0, 2000, "Maximum sneak-key double press interval in milliseconds.");
        LEAP_JUMP_TIMER = defineInt("LeapJumpTimer", 500, 0, 5000, "Jump window in milliseconds for leap.");
        LEAP_AUTO_CRAW_TICKS = defineInt("LeapAutoCrawTicks", 8, 0, 200, "Auto crawl duration after leap.");
        LEAP_FORWARD_BOOST = defineDouble("LeapForwardBoost", 0.35, 0, 5, "Forward speed added by leap.");
        LEAP_VERTICAL_BOOST = defineDouble("LeapVerticalBoost", 0.42, -5, 5, "Vertical speed added by leap.");
        CRAW_LEAP_FORWARD_BOOST = defineDouble("CrawLeapForwardBoost", 0.25, 0, 5, "Forward speed added by sneak leap from jump window.");
        CRAW_LEAP_VERTICAL_BOOST = defineDouble("CrawLeapVerticalBoost", 0.15, -5, 5, "Vertical speed added by sneak leap from jump window.");
        BUILDER.pop();

        BUILDER.push("slide_attributes");
        SLIDE_DURATION = defineInt("SlideDuration", 25, 0, 20 * 60, "Slide duration in ticks.");
        SLIDE_AIR_DURATION = defineInt("SlideAirDuration", 30, 0, 20 * 60, "Maximum slide air duration in ticks.");
        DAP_TIMES = defineInt("DapTimes", 2, 0, 64, "Maximum water dap count.");
        SLIDE_COOLDOWN = defineInt("SlideCooldown", 60, 0, 20 * 60, "Slide cooldown in ticks.");
        SLIDE_START_BOOST = defineDouble("SlideStartBoost", 0.5, 0, 5, "Horizontal boost applied when slide starts.");
        SLIDE_AIR_FORWARD_BOOST = defineDouble("SlideAirForwardBoost", 0.1, 0, 5, "Horizontal boost when slide leaves the ground.");
        SLIDE_AIR_FALL_ACCELERATION = defineDouble("SlideAirFallAcceleration", -0.025, -5, 5, "Vertical acceleration applied while sliding in air.");
        SLIDE_INITIAL_DAP_VERTICAL_BOOST = defineDouble("SlideInitialDapVerticalBoost", 0.5, -5, 5, "Initial vertical boost when sliding into water.");
        SLIDE_DAP_VERTICAL_BOOST = defineDouble("SlideDapVerticalBoost", 0.7, -5, 5, "Vertical boost for refreshed water dap.");
        SLIDE_DAP_MOTION_DECAY = defineDouble("SlideDapMotionDecay", 0.92, 0, 2, "Multiplier applied to repeated dap boost strength.");
        SLIDE_KNOCK_DELAY = defineInt("SlideKnockDelay", 500, 0, 10000, "Delay between slide knock checks in milliseconds.");
        BUILDER.pop();

        BUILDER.push("lists");
        SLIDE_KNOCK_BLACKLIST = defineStringList("SlideKnockBlacklist", List.of("minecraft:item"), "Entities ignored by slide knock.");
        CLIMB_BLOCK_WHITELIST = defineStringList("ClimbBlockWhitelist", List.of("minecraft:stone"), "Extra blocks that can be climbed.");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean enable(String key) {
        ModConfigSpec.BooleanValue value = BOOLEAN_VALUES.get("enable" + key);
        return value != null && value.get();
    }

    private static ModConfigSpec.BooleanValue defineBoolean(String key, boolean defaultValue, String... comment) {
        ModConfigSpec.BooleanValue value = BUILDER.comment(comment).define(key, defaultValue);
        BOOLEAN_VALUES.put(key, value);
        return value;
    }

    private static ModConfigSpec.IntValue defineInt(String key, int defaultValue, int min, int max, String... comment) {
        ModConfigSpec.IntValue value = BUILDER.comment(comment).defineInRange(key, defaultValue, min, max);
        return value;
    }

    private static ModConfigSpec.DoubleValue defineDouble(String key, double defaultValue, double min, double max, String... comment) {
        return BUILDER.comment(comment).defineInRange(key, defaultValue, min, max);
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> defineStringList(String key, List<String> defaultValue, String... comment) {
        return BUILDER.comment(comment).defineList(key, defaultValue, entry -> entry instanceof String);
    }
}
