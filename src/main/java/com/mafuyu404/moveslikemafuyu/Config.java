package com.mafuyu404.moveslikemafuyu;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SLIDE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DAP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SLIDE_REPEAT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SLIDE_KNOCK;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_CLIMB;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_CLIMB_JUMP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_FALLING_RESCUE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SHALLOW_SWIMMING;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SWIMMING_BOOST;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_FREESTYLE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SWIMMING_PUSH;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_CRAW;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_CRAW_SLIDE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_LEAP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_JUMP_CANCEL_CRAW;
    public static final ForgeConfigSpec.ConfigValue<Integer> SLIDE_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> SLIDE_AIR_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> DAP_TIMES;
    public static final ForgeConfigSpec.ConfigValue<Integer> SLIDE_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> CLIMB_JUMP_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> SWIMMING_BOOST_COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> SWIMMING_BOOST_AIR_COST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>  SLIDE_KNOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>  CLIMB_BLOCK_WHITELIST;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec SPEC;

    public static CompoundTag ConfigCache = new CompoundTag();

    static {
        BUILDER.push("Slide Setting");
        ENABLE_SLIDE = BUILDER
                .comment("是否启用滑行：疾跑过程中按下潜行键即可贴地滑行。")
                .define("enableSlide", true);
        ENABLE_DAP = BUILDER
                .comment("是否启用打水漂：滑行到水面时向前跃起。")
                .define("enableDap", true);
        ENABLE_SLIDE_REPEAT = BUILDER
                .comment("是否启用滑行重置时间：滑行到空中时重置持续时间。")
                .define("enableSlideRepeat", true);
        ENABLE_SLIDE_KNOCK = BUILDER
                .comment("是否启用滑行撞飞实体：滑行过程中与实体碰撞会停下并撞飞对方。")
                .define("enableSlideKnock", true);
        BUILDER.pop();
        BUILDER.push("Climb Setting");
        ENABLE_CLIMB = BUILDER
                .comment("是否启用攀爬：跳起后可爬上两格高的障碍。")
                .define("enableClimb", true);
        ENABLE_CLIMB_JUMP = BUILDER
                .comment("是否启用攀爬跳：攀爬中并按着潜行键时按下跳跃键往上起跳。")
                .define("enableClimbJump", true);
        ENABLE_FALLING_RESCUE = BUILDER
                .comment("是否启用失足抢救：在失足后的短暂时间内按潜行键即可站稳。")
                .define("enableFallingRescue", true);
        BUILDER.pop();
        BUILDER.push("Swimming Setting");
        ENABLE_SHALLOW_SWIMMING = BUILDER
                .comment("是否启用浅水游泳：在一格深的水中按疾跑键进入游泳状态。")
                .define("enableShallowSwimming", true);
        ENABLE_SWIMMING_BOOST = BUILDER
                .comment("是否启用水中推进：在游泳状态下按疾跑键消耗氧气向前推进一段距离。")
                .define("enableSwimmingBoost", true);
        ENABLE_FREESTYLE = BUILDER
                .comment("是否启用自由泳：径直往上游到水面后进入可同时呼吸和游泳的状态。")
                .define("enableFreestyle", true);
        ENABLE_SWIMMING_PUSH = BUILDER
                .comment("是否启用水上漂：在自由泳状态下按跳跃向前跃出并进入滑行状态。")
                .define("enableSwimmingPush", true);
        BUILDER.pop();
        BUILDER.push("Craw Setting");
        ENABLE_CRAW = BUILDER
                .comment("是否启用爬行：双击潜行键进入爬行状态，单击退出。")
                .define("enableCraw", true);
        ENABLE_CRAW_SLIDE = BUILDER
                .comment("是否启用爬行中滑行：爬行状态中按下疾跑键触发滑行。")
                .define("enableCrawSlide", true);
        ENABLE_LEAP = BUILDER
                .comment("是否启用飞扑：疾跑跳跃后按下潜行键会触发飞扑并进入爬行状态。")
                .define("enableLeap", true);
        ENABLE_JUMP_CANCEL_CRAW = BUILDER
                .comment("是否启用跳跃取消爬行：跳跃后短时间内进入爬行状态从而触发飞扑。")
                .define("enableJumpCancelCraw", false);
        BUILDER.pop();
        BUILDER.push("Attribute Setting");
        SLIDE_DURATION = BUILDER
                .comment("滑行时间限制，默认为25， 一秒为20。")
                .define("SlideDuration", 25);
        SLIDE_AIR_DURATION = BUILDER
                .comment("空中滑行时间限制，默认为30，一秒是20。")
                .define("SlideAirDuration", 30);
        DAP_TIMES = BUILDER
                .comment("打水漂次数上限，默认为2。")
                .define("DapTimes", 2);
        SLIDE_COOLDOWN = BUILDER
                .comment("滑行的冷却，默认为60，一秒为20。")
                .define("SlideCooldown", 60);
        CLIMB_JUMP_COOLDOWN = BUILDER
                .comment("攀爬跳的冷却，默认为60，一秒为20。")
                .define("ClimbJumpCooldown", 60);
        SWIMMING_BOOST_COOLDOWN = BUILDER
                .comment("水中推进的冷却，默认为60，一秒为20。")
                .define("SwimmingBoostCooldown", 60);
        SWIMMING_BOOST_AIR_COST = BUILDER
                .comment("水中推进的氧气消耗，默认为30，氧气上限为300。")
                .define("SwimmingBoostAirCost", 30);
        BUILDER.pop();
        BUILDER.push("Blacklist Setting");
        SLIDE_KNOCK_BLACKLIST = BUILDER
                .comment("不会被滑行撞飞的实体。")
                .defineList("SlideKnockBlacklist",
                        List.of("minecraft:item"),
                        entry -> entry instanceof String
                );
        CLIMB_BLOCK_WHITELIST = BUILDER
                .comment("补充可以被攀爬的方块。")
                .defineList("ClimbBlockWhitelist",
                        List.of("minecraft:stone"),
                        entry -> entry instanceof String
                );
        SPEC = BUILDER.build();
    }

    public static void updateConfig(CompoundTag config) {
        ConfigCache = config;
    }

    public static CompoundTag getAllConfig() {
        CompoundTag config = new CompoundTag();
        config.putBoolean("enableSlide", ENABLE_SLIDE.get());
        config.putBoolean("enableDap", ENABLE_DAP.get());
        config.putBoolean("enableSlideRepeat", ENABLE_SLIDE_REPEAT.get());
        config.putBoolean("enableSlideKnock", ENABLE_SLIDE_KNOCK.get());
        config.putBoolean("enableClimb", ENABLE_CLIMB.get());
        config.putBoolean("enableClimbJump", ENABLE_CLIMB_JUMP.get());
        config.putBoolean("enableFallingRescue", ENABLE_FALLING_RESCUE.get());
        config.putBoolean("enableShallowSwimming", ENABLE_SHALLOW_SWIMMING.get());
        config.putBoolean("enableSwimmingBoost", ENABLE_SWIMMING_BOOST.get());
        config.putBoolean("enableFreestyle", ENABLE_FREESTYLE.get());
        config.putBoolean("enableSwimmingPush", ENABLE_SWIMMING_PUSH.get());
        config.putBoolean("enableCraw", ENABLE_CRAW.get());
        config.putBoolean("enableCrawSlide", ENABLE_CRAW_SLIDE.get());
        config.putBoolean("enableLeap", ENABLE_LEAP.get());
        config.putBoolean("enableJumpCancelCraw", ENABLE_JUMP_CANCEL_CRAW.get());
        config.putInt("SlideDuration", SLIDE_DURATION.get());
        config.putInt("SlideAirDuration", SLIDE_AIR_DURATION.get());
        config.putInt("DapTimes", DAP_TIMES.get());
        config.putInt("SlideCooldown", SLIDE_COOLDOWN.get());
        config.putInt("ClimbJumpCooldown", CLIMB_JUMP_COOLDOWN.get());
        config.putInt("SwimmingBoostCooldown", SWIMMING_BOOST_COOLDOWN.get());
        config.putInt("SwimmingBoostAirCost", SWIMMING_BOOST_AIR_COST.get());
        return config;
    }

    @SubscribeEvent
    public static void onConfigLoaded(FMLLoadCompleteEvent event) {
        updateConfig(getAllConfig());
    }

    public static boolean enable(String key) {
        return ConfigCache.getBoolean("enable" + key);
    }
}
