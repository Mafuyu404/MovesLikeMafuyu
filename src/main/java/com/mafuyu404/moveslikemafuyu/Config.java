package com.mafuyu404.moveslikemafuyu;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Map<String, ForgeConfigSpec.BooleanValue> BOOLEAN_VALUES = new LinkedHashMap<>();

    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIDE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIDE_REPEAT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIDE_KNOCK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIDE_IGNORE_EDGE_PROTECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIDE_DOUBLE_TAP_TRIGGER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ROLL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AIR_ROLL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ROLL_INVULNERABILITY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ROLL_CAMERA;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ROLL_IGNORE_EDGE_PROTECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPRINT_ROLL_DOUBLE_TAP_TRIGGER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CLIMB;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CLIMB_JUMP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FALLING_RESCUE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHALLOW_SWIMMING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SWIMMING_BOOST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FREESTYLE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SWIMMING_PUSH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRAW;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRAW_SLIDE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LEAP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JUMP_CANCEL_CRAW;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_DODGE;

    public static final ForgeConfigSpec.IntValue SLIDE_DURATION;
    public static final ForgeConfigSpec.IntValue SLIDE_AIR_DURATION;
    public static final ForgeConfigSpec.IntValue DAP_TIMES;
    public static final ForgeConfigSpec.IntValue SLIDE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue CLIMB_JUMP_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SWIMMING_BOOST_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SWIMMING_BOOST_AIR_COST;
    public static final ForgeConfigSpec.DoubleValue SWIMMING_BOOST_STRENGTH;

    public static final ForgeConfigSpec.IntValue ROLL_DOUBLE_PRESS_DELAY;
    public static final ForgeConfigSpec.IntValue ROLL_AIR_TIMER;
    public static final ForgeConfigSpec.IntValue ROLL_DURATION;
    public static final ForgeConfigSpec.IntValue ROLL_SHIFT_START_TICK;
    public static final ForgeConfigSpec.IntValue ROLL_SHIFT_END_TICK;
    public static final ForgeConfigSpec.IntValue ROLL_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ROLL_ACTION_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ROLL_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ROLL_START_SPEED;
    public static final ForgeConfigSpec.DoubleValue ROLL_PEAK_SPEED;
    public static final ForgeConfigSpec.DoubleValue ROLL_END_SPEED;
    public static final ForgeConfigSpec.DoubleValue ROLL_AIR_VERTICAL_SPEED;

    public static final ForgeConfigSpec.IntValue CRAW_DOUBLE_PRESS_DELAY;
    public static final ForgeConfigSpec.IntValue LEAP_JUMP_TIMER;
    public static final ForgeConfigSpec.IntValue LEAP_AUTO_CRAW_TICKS;
    public static final ForgeConfigSpec.DoubleValue LEAP_FORWARD_BOOST;
    public static final ForgeConfigSpec.DoubleValue LEAP_VERTICAL_BOOST;
    public static final ForgeConfigSpec.DoubleValue CRAW_LEAP_FORWARD_BOOST;
    public static final ForgeConfigSpec.DoubleValue CRAW_LEAP_VERTICAL_BOOST;

    public static final ForgeConfigSpec.DoubleValue SLIDE_START_BOOST;
    public static final ForgeConfigSpec.DoubleValue SLIDE_AIR_FORWARD_BOOST;
    public static final ForgeConfigSpec.DoubleValue SLIDE_AIR_FALL_ACCELERATION;
    public static final ForgeConfigSpec.DoubleValue SLIDE_INITIAL_DAP_VERTICAL_BOOST;
    public static final ForgeConfigSpec.DoubleValue SLIDE_DAP_VERTICAL_BOOST;
    public static final ForgeConfigSpec.DoubleValue SLIDE_DAP_MOTION_DECAY;
    public static final ForgeConfigSpec.IntValue SLIDE_KNOCK_DELAY;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLIDE_KNOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLIMB_BLOCK_WHITELIST;

    public static final ForgeConfigSpec SPEC;
    static {
        BUILDER.push("slide");
        ENABLE_SLIDE = defineBoolean("enableSlide", true, "是否启用滑铲：疾跑时按潜行键触发。", "Enable slide: press sneak while sprinting to slide.");
        ENABLE_DAP = defineBoolean("enableDap", true, "是否启用打水漂：滑铲入水时向上弹起。", "Enable water dap while sliding over water.");
        ENABLE_SLIDE_REPEAT = defineBoolean("enableSlideRepeat", true, "是否在滑铲滞空时刷新滑铲持续时间。", "Refresh slide duration while sliding in the air.");
        ENABLE_SLIDE_KNOCK = defineBoolean("enableSlideKnock", true, "是否启用滑铲撞飞实体。", "Knock living entities hit during a slide.");
        ENABLE_SLIDE_IGNORE_EDGE_PROTECTION = defineBoolean("enableSlideIgnoreEdgeProtection", true, "滑铲时是否无视潜行的方块边缘保护。", "Ignore sneak edge protection while sliding.");
        ENABLE_SLIDE_DOUBLE_TAP_TRIGGER = defineBoolean("enableSlideDoubleTapTrigger", false, "是否需要双击潜行键才触发滑铲。", "Require double tapping sneak to start slide.");
        ENABLE_ROLL = defineBoolean("enableRoll", true, "是否启用翻滚总开关。", "Enable roll.");
        ENABLE_AIR_ROLL = defineBoolean("enableAirRoll", true, "是否允许在空中触发翻滚。", "Enable triggering roll while airborne.");
        ENABLE_ROLL_INVULNERABILITY = defineBoolean("enableRollInvulnerability", true, "翻滚时是否获得无敌帧。", "Enable invulnerability frames during roll.");
        ENABLE_ROLL_CAMERA = defineBoolean("enableRollCamera", true, "翻滚时是否旋转摄像机。", "Enable camera rotation during roll.");
        ENABLE_ROLL_IGNORE_EDGE_PROTECTION = defineBoolean("enableRollIgnoreEdgeProtection", true, "翻滚时是否无视潜行的方块边缘保护。", "Ignore sneak edge protection while rolling.");
        ENABLE_SPRINT_ROLL_DOUBLE_TAP_TRIGGER = defineBoolean("enableSprintRollDoubleTapTrigger", false, "疾跑中是否需要双击疾跑键才触发翻滚。", "Require double tapping sprint to roll while already sprinting.");
        BUILDER.pop();

        BUILDER.push("climb");
        ENABLE_CLIMB = defineBoolean("enableClimb", true, "是否启用攀爬。", "Enable wall climb.");
        ENABLE_CLIMB_JUMP = defineBoolean("enableClimbJump", true, "是否启用攀爬跳。", "Enable jumping from climbable surfaces while sneaking.");
        ENABLE_FALLING_RESCUE = defineBoolean("enableFallingRescue", true, "是否启用失足抢救。", "Enable falling rescue on climbable surfaces.");
        BUILDER.pop();

        BUILDER.push("swimming");
        ENABLE_SHALLOW_SWIMMING = defineBoolean("enableShallowSwimming", true, "是否启用浅水游泳。", "Enable sprint swimming in shallow water.");
        ENABLE_SWIMMING_BOOST = defineBoolean("enableSwimmingBoost", true, "是否启用水中推进。", "Enable sprint boost while swimming.");
        ENABLE_FREESTYLE = defineBoolean("enableFreestyle", true, "是否启用水面自由泳。", "Enable freestyle surface swimming.");
        ENABLE_SWIMMING_PUSH = defineBoolean("enableSwimmingPush", true, "是否允许从自由泳跳出并进入滑铲。", "Enable jumping from freestyle swimming into slide.");
        BUILDER.pop();

        BUILDER.push("crawling");
        ENABLE_CRAW = defineBoolean("enableCraw", true, "是否启用趴下。", "Enable crawl.");
        ENABLE_CRAW_SLIDE = defineBoolean("enableCrawSlide", true, "是否允许趴下时触发滑铲。", "Enable slide from crawl.");
        ENABLE_LEAP = defineBoolean("enableLeap", true, "是否启用飞扑。", "Enable leap into crawl.");
        ENABLE_JUMP_CANCEL_CRAW = defineBoolean("enableJumpCancelCraw", false, "是否允许跳跃取消趴下。", "Enable jump to cancel crawl.");
        BUILDER.pop();

        BUILDER.push("auto_dodge");
        ENABLE_AUTO_DODGE = defineBoolean("enableAutoDodge", true, "是否启用自动闪避。", "Automatically dodge incoming projectiles.");
        BUILDER.pop();

        BUILDER.push("attributes");
        SLIDE_DURATION = defineInt("SlideDuration", 25, 0, 20 * 60, "滑铲持续时间，单位为 tick。", "Slide duration in ticks.");
        SLIDE_AIR_DURATION = defineInt("SlideAirDuration", 30, 0, 20 * 60, "滑铲最大滞空时间，单位为 tick。", "Maximum slide air duration in ticks.");
        DAP_TIMES = defineInt("DapTimes", 2, 0, 64, "打水漂次数上限。", "Maximum water dap count.");
        SLIDE_COOLDOWN = defineInt("SlideCooldown", 60, 0, 20 * 60, "滑铲冷却时间，单位为 tick。", "Slide cooldown in ticks.");
        CLIMB_JUMP_COOLDOWN = defineInt("ClimbJumpCooldown", 60, 0, 20 * 60, "攀爬跳冷却时间，单位为 tick。", "Climb jump cooldown in ticks.");
        SWIMMING_BOOST_COOLDOWN = defineInt("SwimmingBoostCooldown", 60, 0, 20 * 60, "水中推进冷却时间，单位为 tick。", "Swimming boost cooldown in ticks.");
        SWIMMING_BOOST_AIR_COST = defineInt("SwimmingBoostAirCost", 30, 0, 300, "水中推进消耗的氧气值。", "Air supply consumed by swimming boost.");
        SWIMMING_BOOST_STRENGTH = defineDouble("SwimmingBoostStrength", 0.4, 0, 5, "水中推进强度。", "Swimming boost movement strength.");
        BUILDER.pop();

        BUILDER.push("roll_attributes");
        ROLL_DOUBLE_PRESS_DELAY = defineInt("RollDoublePressDelay", 250, 0, 2000, "疾跑键双击判定窗口，单位为毫秒。", "Maximum sprint-key double press interval in milliseconds.");
        ROLL_AIR_TIMER = defineInt("RollAirTimer", 500, 0, 5000, "空中强化翻滚的跳跃窗口，单位为毫秒。", "Jump window in milliseconds for boosted air roll.");
        ROLL_DURATION = defineInt("RollDuration", 14, 1, 200, "翻滚持续时间，单位为 tick，动作速度倍率会影响实际速度。", "Roll duration in ticks before action speed is applied.");
        ROLL_SHIFT_START_TICK = defineInt("RollShiftStartTick", 3, 0, 200, "翻滚中开始强制潜行的 tick。", "Roll tick where forced sneaking starts.");
        ROLL_SHIFT_END_TICK = defineInt("RollShiftEndTick", 10, 0, 200, "翻滚中结束强制潜行的 tick。", "Roll tick where forced sneaking ends.");
        ROLL_COOLDOWN = defineInt("RollCooldown", 0, 0, 20 * 60, "翻滚冷却时间，单位为 tick。", "Roll cooldown in ticks.");
        ROLL_ACTION_SPEED_MULTIPLIER = defineDouble("RollActionSpeedMultiplier", 1.2, 0.1, 10, "翻滚动作速度倍率。", "Roll animation speed multiplier.");
        ROLL_SPEED_MULTIPLIER = defineDouble("RollSpeedMultiplier", 1.2, 0, 10, "翻滚移动速度倍率。", "Roll movement speed multiplier.");
        ROLL_START_SPEED = defineDouble("RollStartSpeed", 0.225, 0, 5, "翻滚起步水平速度。", "Roll starting horizontal speed.");
        ROLL_PEAK_SPEED = defineDouble("RollPeakSpeed", 0.35, 0, 5, "翻滚峰值水平速度。", "Roll peak horizontal speed.");
        ROLL_END_SPEED = defineDouble("RollEndSpeed", 0.16, 0, 5, "翻滚结束水平速度。", "Roll ending horizontal speed.");
        ROLL_AIR_VERTICAL_SPEED = defineDouble("RollAirVerticalSpeed", 0.19, -5, 5, "强化空中翻滚追加的竖直速度。", "Vertical speed added by boosted air roll.");
        BUILDER.pop();

        BUILDER.push("crawl_attributes");
        CRAW_DOUBLE_PRESS_DELAY = defineInt("CrawDoublePressDelay", 250, 0, 2000, "潜行键双击判定窗口，单位为毫秒。", "Maximum sneak-key double press interval in milliseconds.");
        LEAP_JUMP_TIMER = defineInt("LeapJumpTimer", 500, 0, 5000, "飞扑的跳跃窗口，单位为毫秒。", "Jump window in milliseconds for leap.");
        LEAP_AUTO_CRAW_TICKS = defineInt("LeapAutoCrawTicks", 8, 0, 200, "飞扑后自动趴下持续时间，单位为 tick。", "Auto crawl duration after leap.");
        LEAP_FORWARD_BOOST = defineDouble("LeapForwardBoost", 0.35, 0, 5, "飞扑追加的水平速度。", "Forward speed added by leap.");
        LEAP_VERTICAL_BOOST = defineDouble("LeapVerticalBoost", 0.42, -5, 5, "飞扑追加的竖直速度。", "Vertical speed added by leap.");
        CRAW_LEAP_FORWARD_BOOST = defineDouble("CrawLeapForwardBoost", 0.25, 0, 5, "跳跃窗口内潜行飞扑追加的水平速度。", "Forward speed added by sneak leap from jump window.");
        CRAW_LEAP_VERTICAL_BOOST = defineDouble("CrawLeapVerticalBoost", 0.15, -5, 5, "跳跃窗口内潜行飞扑追加的竖直速度。", "Vertical speed added by sneak leap from jump window.");
        BUILDER.pop();

        BUILDER.push("slide_attributes");
        SLIDE_START_BOOST = defineDouble("SlideStartBoost", 0.5, 0, 5, "滑铲开始时追加的水平速度。", "Horizontal boost applied when slide starts.");
        SLIDE_AIR_FORWARD_BOOST = defineDouble("SlideAirForwardBoost", 0.1, 0, 5, "滑铲离地时追加的水平速度。", "Horizontal boost when slide leaves the ground.");
        SLIDE_AIR_FALL_ACCELERATION = defineDouble("SlideAirFallAcceleration", -0.025, -5, 5, "滑铲滞空时每 tick 追加的竖直速度。", "Vertical acceleration applied while sliding in air.");
        SLIDE_INITIAL_DAP_VERTICAL_BOOST = defineDouble("SlideInitialDapVerticalBoost", 0.5, -5, 5, "滑铲首次入水时追加的竖直速度。", "Initial vertical boost when sliding into water.");
        SLIDE_DAP_VERTICAL_BOOST = defineDouble("SlideDapVerticalBoost", 0.7, -5, 5, "刷新打水漂时追加的竖直速度。", "Vertical boost for refreshed water dap.");
        SLIDE_DAP_MOTION_DECAY = defineDouble("SlideDapMotionDecay", 0.92, 0, 2, "连续打水漂速度强度衰减倍率。", "Multiplier applied to repeated dap boost strength.");
        SLIDE_KNOCK_DELAY = defineInt("SlideKnockDelay", 500, 0, 10000, "滑铲撞击检测间隔，单位为毫秒。", "Delay between slide knock checks in milliseconds.");
        BUILDER.pop();

        BUILDER.push("lists");
        SLIDE_KNOCK_BLACKLIST = defineStringList("SlideKnockBlacklist", List.of("minecraft:item"), "不会被滑铲撞飞的实体 ID 列表。", "Entities ignored by slide knock.");
        CLIMB_BLOCK_WHITELIST = defineStringList("ClimbBlockWhitelist", List.of("minecraft:stone"), "额外允许攀爬的方块 ID 列表。", "Extra blocks that can be climbed.");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean enable(String key) {
        ForgeConfigSpec.BooleanValue value = BOOLEAN_VALUES.get("enable" + key);
        return value != null && value.get();
    }

    private static ForgeConfigSpec.BooleanValue defineBoolean(String key, boolean defaultValue, String... comment) {
        ForgeConfigSpec.BooleanValue value = BUILDER.comment(comment).define(key, defaultValue);
        BOOLEAN_VALUES.put(key, value);
        return value;
    }

    private static ForgeConfigSpec.IntValue defineInt(String key, int defaultValue, int min, int max, String... comment) {
        ForgeConfigSpec.IntValue value = BUILDER.comment(comment).defineInRange(key, defaultValue, min, max);
        return value;
    }

    private static ForgeConfigSpec.DoubleValue defineDouble(String key, double defaultValue, double min, double max, String... comment) {
        return BUILDER.comment(comment).defineInRange(key, defaultValue, min, max);
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> defineStringList(String key, List<String> defaultValue, String... comment) {
        return BUILDER.comment(comment).defineList(key, defaultValue, entry -> entry instanceof String);
    }
}
