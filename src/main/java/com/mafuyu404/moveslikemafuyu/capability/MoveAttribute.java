package com.mafuyu404.moveslikemafuyu.capability;

import com.mafuyu404.moveslikemafuyu.Config;

import java.util.Locale;

public enum MoveAttribute {
    CLIMB_JUMP_COOLDOWN("climbJumpCooldown", true),

    SWIMMING_BOOST_COOLDOWN("swimmingBoostCooldown", true),
    SWIMMING_BOOST_AIR_COST("swimmingBoostAirCost", true),
    SWIMMING_BOOST_STRENGTH("swimmingBoostStrength", false),

    ROLL_DOUBLE_PRESS_DELAY("rollDoublePressDelay", true),
    ROLL_AIR_TIMER("rollAirTimer", true),
    ROLL_DURATION("rollDuration", true),
    ROLL_SHIFT_START_TICK("rollShiftStartTick", true),
    ROLL_SHIFT_END_TICK("rollShiftEndTick", true),
    ROLL_COOLDOWN("rollCooldown", true),
    ROLL_ACTION_SPEED_MULTIPLIER("rollActionSpeedMultiplier", false),
    ROLL_SPEED_MULTIPLIER("rollSpeedMultiplier", false),
    ROLL_START_SPEED("rollStartSpeed", false),
    ROLL_PEAK_SPEED("rollPeakSpeed", false),
    ROLL_END_SPEED("rollEndSpeed", false),
    ROLL_AIR_VERTICAL_SPEED("rollAirVerticalSpeed", false),

    CRAW_DOUBLE_PRESS_DELAY("crawDoublePressDelay", true),
    LEAP_JUMP_TIMER("leapJumpTimer", true),
    LEAP_AUTO_CRAW_TICKS("leapAutoCrawTicks", true),
    LEAP_FORWARD_BOOST("leapForwardBoost", false),
    LEAP_VERTICAL_BOOST("leapVerticalBoost", false),
    CRAW_LEAP_FORWARD_BOOST("crawLeapForwardBoost", false),
    CRAW_LEAP_VERTICAL_BOOST("crawLeapVerticalBoost", false),

    SLIDE_DURATION("slideDuration", true),
    SLIDE_AIR_DURATION("slideAirDuration", true),
    DAP_TIMES("dapTimes", true),
    SLIDE_COOLDOWN("slideCooldown", true),
    SLIDE_START_BOOST("slideStartBoost", false),
    SLIDE_AIR_FORWARD_BOOST("slideAirForwardBoost", false),
    SLIDE_AIR_FALL_ACCELERATION("slideAirFallAcceleration", false),
    SLIDE_INITIAL_DAP_VERTICAL_BOOST("slideInitialDapVerticalBoost", false),
    SLIDE_DAP_VERTICAL_BOOST("slideDapVerticalBoost", false),
    SLIDE_DAP_MOTION_DECAY("slideDapMotionDecay", false),
    SLIDE_KNOCK_DELAY("slideKnockDelay", true);

    private final String key;
    private final boolean integer;

    MoveAttribute(String key, boolean integer) {
        this.key = key;
        this.integer = integer;
    }

    public String key() {
        return key;
    }

    public boolean isInteger() {
        return integer;
    }

    public double defaultValue() {
        return switch (this) {
            case CLIMB_JUMP_COOLDOWN -> Config.CLIMB_JUMP_COOLDOWN.get();
            case SWIMMING_BOOST_COOLDOWN -> Config.SWIMMING_BOOST_COOLDOWN.get();
            case SWIMMING_BOOST_AIR_COST -> Config.SWIMMING_BOOST_AIR_COST.get();
            case SWIMMING_BOOST_STRENGTH -> Config.SWIMMING_BOOST_STRENGTH.get();
            case ROLL_DOUBLE_PRESS_DELAY -> Config.ROLL_DOUBLE_PRESS_DELAY.get();
            case ROLL_AIR_TIMER -> Config.ROLL_AIR_TIMER.get();
            case ROLL_DURATION -> Config.ROLL_DURATION.get();
            case ROLL_SHIFT_START_TICK -> Config.ROLL_SHIFT_START_TICK.get();
            case ROLL_SHIFT_END_TICK -> Config.ROLL_SHIFT_END_TICK.get();
            case ROLL_COOLDOWN -> Config.ROLL_COOLDOWN.get();
            case ROLL_ACTION_SPEED_MULTIPLIER -> Config.ROLL_ACTION_SPEED_MULTIPLIER.get();
            case ROLL_SPEED_MULTIPLIER -> Config.ROLL_SPEED_MULTIPLIER.get();
            case ROLL_START_SPEED -> Config.ROLL_START_SPEED.get();
            case ROLL_PEAK_SPEED -> Config.ROLL_PEAK_SPEED.get();
            case ROLL_END_SPEED -> Config.ROLL_END_SPEED.get();
            case ROLL_AIR_VERTICAL_SPEED -> Config.ROLL_AIR_VERTICAL_SPEED.get();
            case CRAW_DOUBLE_PRESS_DELAY -> Config.CRAW_DOUBLE_PRESS_DELAY.get();
            case LEAP_JUMP_TIMER -> Config.LEAP_JUMP_TIMER.get();
            case LEAP_AUTO_CRAW_TICKS -> Config.LEAP_AUTO_CRAW_TICKS.get();
            case LEAP_FORWARD_BOOST -> Config.LEAP_FORWARD_BOOST.get();
            case LEAP_VERTICAL_BOOST -> Config.LEAP_VERTICAL_BOOST.get();
            case CRAW_LEAP_FORWARD_BOOST -> Config.CRAW_LEAP_FORWARD_BOOST.get();
            case CRAW_LEAP_VERTICAL_BOOST -> Config.CRAW_LEAP_VERTICAL_BOOST.get();
            case SLIDE_DURATION -> Config.SLIDE_DURATION.get();
            case SLIDE_AIR_DURATION -> Config.SLIDE_AIR_DURATION.get();
            case DAP_TIMES -> Config.DAP_TIMES.get();
            case SLIDE_COOLDOWN -> Config.SLIDE_COOLDOWN.get();
            case SLIDE_START_BOOST -> Config.SLIDE_START_BOOST.get();
            case SLIDE_AIR_FORWARD_BOOST -> Config.SLIDE_AIR_FORWARD_BOOST.get();
            case SLIDE_AIR_FALL_ACCELERATION -> Config.SLIDE_AIR_FALL_ACCELERATION.get();
            case SLIDE_INITIAL_DAP_VERTICAL_BOOST -> Config.SLIDE_INITIAL_DAP_VERTICAL_BOOST.get();
            case SLIDE_DAP_VERTICAL_BOOST -> Config.SLIDE_DAP_VERTICAL_BOOST.get();
            case SLIDE_DAP_MOTION_DECAY -> Config.SLIDE_DAP_MOTION_DECAY.get();
            case SLIDE_KNOCK_DELAY -> Config.SLIDE_KNOCK_DELAY.get();
        };
    }

    public static MoveAttribute byKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (MoveAttribute attribute : values()) {
            if (attribute.key.toLowerCase(Locale.ROOT).equals(normalized)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Unknown move attribute: " + key);
    }
}
