package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mafuyu404.moveslikemafuyu.network.TagMessage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class RollEvent {
    private static final int DOUBLE_PRESS_DELAY = 250;
    private static final int AIR_ROLL_TIMER = 500;
    public static final int DURATION = 14;
    private static final int SHIFT_START_TICK = 3;
    private static final int SHIFT_END_TICK = 10;
    private static final int COOLDOWN = 0;
    private static final double ACTION_SPEED_MULTIPLIER = 1.2;
    private static final double SPEED_MULTIPLIER = 1.2;
    private static final double START_SPEED = 0.225;
    private static final double PEAK_SPEED = 0.35;
    private static final double END_SPEED = 0.16;
    private static final double AIR_ROLL_VERTICAL_SPEED = 0.19;

    private static double timer;
    private static int cooldown;
    private static float previousProgress;
    private static float progress;
    private static boolean forcingShift;
    private static boolean rollMovementEnabled;
    private static Vec3 rollDirection = new Vec3(0, 0, 1);
    private static Vec3 rollLocalDirection = new Vec3(0, 0, 1);
    private static long lastSprintPressTime;
    private static long lastJumpPressTime;

    @SubscribeEvent
    public static void rollAction(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!player.isLocalPlayer() || player.isSpectator()) return;

        Options options = Minecraft.getInstance().options;
        if (canRoll(player) && hasMovementInput(options)) {
            KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.roll");
        }

        previousProgress = progress;
        if (player.getTags().contains("roll")) {
            timer += ACTION_SPEED_MULTIPLIER;
            progress = (float) Math.min(1.0, timer / DURATION);
            if (!rollMovementEnabled && player.onGround()) {
                rollMovementEnabled = true;
            }
            if (rollMovementEnabled) applyRollMovement(player, progress);
            setRollShift(player, timer >= SHIFT_START_TICK && timer <= SHIFT_END_TICK);
            player.setSprinting(true);
            if (timer > DURATION || player.isInWater() || player.isFallFlying()) {
                cancelRoll(player);
            }
        } else {
            progress = 0;
            setRollShift(player, false);
        }

        if (cooldown > 0) cooldown--;
    }

    @SubscribeEvent
    public static void onAction(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator() || event.getAction() != InputConstants.PRESS) return;

        if (event.getKey() == options.keySprint.getKey().getValue()) {
            long currentTime = System.currentTimeMillis();
            if (canRoll(player) && hasMovementInput(options) && player.isSprinting()) {
                startRoll(player);
                lastSprintPressTime = 0;
            } else if (canRoll(player) && hasMovementInput(options) && currentTime - lastSprintPressTime < DOUBLE_PRESS_DELAY) {
                startRoll(player);
                lastSprintPressTime = 0;
            } else {
                lastSprintPressTime = currentTime;
            }
        }

        if (event.getKey() == options.keyJump.getKey().getValue()) {
            if (player.onGround()) {
                lastJumpPressTime = System.currentTimeMillis();
            }
        }
    }

    public static void startRoll(Player player) {
        if (!Config.enable("Roll") || player.getTags().contains("roll")) return;
        timer = 0;
        previousProgress = 0;
        progress = 0;
        forcingShift = false;
        long currentTime = System.currentTimeMillis();
        rollMovementEnabled = player.onGround() || canAirRoll(player, currentTime);
        rollDirection = horizontalDirection(player);
        rollLocalDirection = localInputDirection(Minecraft.getInstance().options);
        player.addTag("roll");
        player.setSprinting(true);
        NetworkHandler.CHANNEL.sendToServer(new TagMessage("roll", true));

        if (!player.onGround() && rollMovementEnabled) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, AIR_ROLL_VERTICAL_SPEED, 0));
        }
        if (rollMovementEnabled) applyRollMovement(player, 0.0f);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.6f, 1.2f);
    }

    public static void cancelRoll(Player player) {
        if (!player.getTags().contains("roll")) return;
        player.removeTag("roll");
        setRollShift(player, false);
        NetworkHandler.CHANNEL.sendToServer(new TagMessage("roll", false));
        cooldown = COOLDOWN;
        previousProgress = 0;
        progress = 0;
        rollMovementEnabled = false;
    }

    public static float getRollDegrees(float partialTick) {
        float value = previousProgress + (progress - previousProgress) * partialTick;
        value = value * value * (3.0f - 2.0f * value);
        return value * 360.0f;
    }

    public static Vec3 getRollAxis() {
        return new Vec3(rollLocalDirection.z, 0, rollLocalDirection.x);
    }

    public static boolean isRolling(Player player) {
        return player != null && player.getTags().contains("roll");
    }

    private static boolean canRoll(Player player) {
        return Config.enable("Roll")
                && cooldown <= 0
                && player.isLocalPlayer()
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getTags().contains("roll")
                && !player.getTags().contains("slide")
                && !player.getTags().contains("craw");
    }

    private static boolean canAirRoll(Player player, long currentTime) {
        return !player.onGround()
                && player.getDeltaMovement().y > 0
                && currentTime - lastJumpPressTime < AIR_ROLL_TIMER;
    }

    private static Vec3 horizontalDirection(Player player) {
        Options options = Minecraft.getInstance().options;
        Vec3 localDirection = localInputDirection(options);
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        if (look.lengthSqr() < 1.0E-6) look = new Vec3(0, 0, 1);
        look = look.normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        Vec3 direction = look.scale(localDirection.z).add(right.scale(localDirection.x));
        return direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
    }

    private static Vec3 localInputDirection(Options options) {
        double x = 0;
        double z = 0;
        if (options.keyUp.isDown()) z += 1;
        if (options.keyDown.isDown()) z -= 1;
        if (options.keyLeft.isDown()) x -= 1;
        if (options.keyRight.isDown()) x += 1;
        Vec3 direction = new Vec3(x, 0, z);
        return direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
    }

    private static boolean hasMovementInput(Options options) {
        return options.keyUp.isDown()
                || options.keyDown.isDown()
                || options.keyLeft.isDown()
                || options.keyRight.isDown();
    }

    private static void applyRollMovement(Player player, float progress) {
        if (rollDirection.lengthSqr() < 1.0E-6) rollDirection = horizontalDirection(player);
        Vec3 movement = player.getDeltaMovement();
        double speed = getRollSpeed(progress) * SPEED_MULTIPLIER;
        player.setDeltaMovement(rollDirection.x * speed, movement.y, rollDirection.z * speed);
    }

    private static double getRollSpeed(float progress) {
        if (progress < 0.25f) {
            return lerp(START_SPEED, PEAK_SPEED, progress / 0.25f);
        }
        if (progress > 0.75f) {
            return lerp(PEAK_SPEED, END_SPEED, (progress - 0.75f) / 0.25f);
        }
        return PEAK_SPEED;
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * Math.max(0.0, Math.min(1.0, delta));
    }

    private static void setRollShift(Player player, boolean shift) {
        if (forcingShift == shift) return;
        forcingShift = shift;
        Minecraft.getInstance().options.keyShift.setDown(shift);
        player.setShiftKeyDown(shift);
        NetworkHandler.CHANNEL.sendToServer(new TagMessage("roll_shift", shift));
    }
}
