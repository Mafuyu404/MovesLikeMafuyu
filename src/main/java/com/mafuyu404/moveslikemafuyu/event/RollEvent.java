package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.network.payload.TagPayload;
import com.mafuyu404.moveslikemafuyu.util.KeyInputHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class RollEvent {
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
    public static void rollAction(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;

        Options options = Minecraft.getInstance().options;
        if (canPromptRoll(player, options)) {
            KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.roll");
        }

        previousProgress = progress;
        if (player.getTags().contains("roll")) {
            if (!Config.enable("Roll")) {
                cancelRoll(player);
                return;
            }
            timer += MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_ACTION_SPEED_MULTIPLIER);
            progress = (float) Math.min(1.0, timer / MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DURATION));
            if (!rollMovementEnabled && player.onGround()) {
                rollMovementEnabled = true;
            }
            if (rollMovementEnabled) applyRollMovement(player, progress);
            setRollShift(player, timer >= MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_SHIFT_START_TICK)
                    && timer <= MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_SHIFT_END_TICK));
            player.setSprinting(true);
            if (timer > MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DURATION) || player.isInWater() || player.isFallFlying()) {
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
        if (player == null || player.isSpectator()) return;

        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player, options);
        }

        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
        }
    }

    @SubscribeEvent
    public static void onMouseAction(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;

        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player, options);
        }
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
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
        PacketDistributor.sendToServer(new TagPayload("roll", true));

        if (!player.onGround() && rollMovementEnabled) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_AIR_VERTICAL_SPEED), 0));
        }
        if (rollMovementEnabled) applyRollMovement(player, 0.0f);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.6f, 1.2f);
    }

    public static void cancelRoll(Player player) {
        if (!player.getTags().contains("roll")) return;
        player.removeTag("roll");
        setRollShift(player, false);
        PacketDistributor.sendToServer(new TagPayload("roll", false));
        cooldown = MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_COOLDOWN);
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
                && (player.onGround() || Config.enable("AirRoll"))
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getTags().contains("roll")
                && !player.getTags().contains("slide")
                && !player.getTags().contains("craw");
    }

    private static boolean canPromptRoll(Player player, Options options) {
        return canRoll(player) && hasMovementInput(options);
    }

    private static boolean canAirRoll(Player player, long currentTime) {
        return !player.onGround()
                && player.getDeltaMovement().y > 0
                && currentTime - lastJumpPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_AIR_TIMER);
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
        double speed = getRollSpeed(player, progress) * MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_SPEED_MULTIPLIER);
        player.setDeltaMovement(rollDirection.x * speed, movement.y, rollDirection.z * speed);
    }

    private static double getRollSpeed(Player player, float progress) {
        if (progress < 0.25f) {
            return lerp(MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_START_SPEED),
                    MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_PEAK_SPEED), progress / 0.25f);
        }
        if (progress > 0.75f) {
            return lerp(MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_PEAK_SPEED),
                    MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_END_SPEED), (progress - 0.75f) / 0.25f);
        }
        return MoveAttributeResolver.getDouble(player, MoveAttribute.ROLL_PEAK_SPEED);
    }

    private static double lerp(double start, double end, double delta) {
        return start + (end - start) * Math.max(0.0, Math.min(1.0, delta));
    }

    private static void setRollShift(Player player, boolean shift) {
        if (forcingShift == shift) return;
        forcingShift = shift;
        Minecraft.getInstance().options.keyShift.setDown(shift);
        player.setShiftKeyDown(shift);
        PacketDistributor.sendToServer(new TagPayload("roll_shift", shift));
    }

    private static void handleSprintPress(Player player, Options options) {
        long currentTime = System.currentTimeMillis();
        boolean sprintRollPressed = canRoll(player)
                && hasMovementInput(options)
                && player.isSprinting()
                && (!Config.enable("SprintRollDoubleTapTrigger")
                || currentTime - lastSprintPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DOUBLE_PRESS_DELAY));
        if (sprintRollPressed) {
            startRoll(player);
            lastSprintPressTime = 0;
        } else if (canRoll(player) && hasMovementInput(options) && currentTime - lastSprintPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DOUBLE_PRESS_DELAY)) {
            startRoll(player);
            lastSprintPressTime = 0;
        } else {
            lastSprintPressTime = currentTime;
        }
    }

    private static void handleJumpPress(Player player) {
        if (player.onGround() && player.isSprinting()) {
            lastJumpPressTime = System.currentTimeMillis();
        }
    }
}
