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
    private static final int DURATION = 14;
    private static final int SHIFT_START_TICK = 3;
    private static final int SHIFT_END_TICK = 10;
    private static final int COOLDOWN = 0;
    private static final double ACTION_SPEED_MULTIPLIER = 1.1;
    private static final double SPEED_MULTIPLIER = 1.2;
    private static final double START_SPEED = 0.225;
    private static final double PEAK_SPEED = 0.35;
    private static final double END_SPEED = 0.16;

    private static double timer;
    private static int cooldown;
    private static float previousProgress;
    private static float progress;
    private static boolean forcingShift;
    private static Vec3 rollDirection = new Vec3(0, 0, 1);

    @SubscribeEvent
    public static void rollAction(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!player.isLocalPlayer() || player.isSpectator()) return;

        Options options = Minecraft.getInstance().options;
        if (canRoll(player)) {
            KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.roll");
        }

        previousProgress = progress;
        if (player.getTags().contains("roll")) {
            timer += ACTION_SPEED_MULTIPLIER;
            progress = (float) Math.min(1.0, timer / DURATION);
            applyRollMovement(player, progress);
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

        if (event.getKey() == options.keySprint.getKey().getValue() && canRoll(player)) {
            startRoll(player);
        }
    }

    public static void startRoll(Player player) {
        if (!Config.enable("Roll") || player.getTags().contains("roll")) return;
        timer = 0;
        previousProgress = 0;
        progress = 0;
        forcingShift = false;
        rollDirection = horizontalDirection(player);
        player.addTag("roll");
        player.setSprinting(true);
        NetworkHandler.CHANNEL.sendToServer(new TagMessage("roll", true));

        applyRollMovement(player, 0.0f);
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
    }

    public static float getRollDegrees(float partialTick) {
        float value = previousProgress + (progress - previousProgress) * partialTick;
        value = value * value * (3.0f - 2.0f * value);
        return value * 360.0f;
    }

    public static boolean isRolling(Player player) {
        return player != null && player.getTags().contains("roll");
    }

    private static boolean canRoll(Player player) {
        return Config.enable("Roll")
                && cooldown <= 0
                && player.isLocalPlayer()
                && player.isSprinting()
                && player.onGround()
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getTags().contains("roll")
                && !player.getTags().contains("slide")
                && !player.getTags().contains("craw");
    }

    private static Vec3 horizontalDirection(Player player) {
        Vec3 direction = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        return direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
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
