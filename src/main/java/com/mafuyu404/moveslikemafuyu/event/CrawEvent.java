package com.mafuyu404.moveslikemafuyu.event;

import net.neoforged.fml.common.EventBusSubscriber;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import com.mafuyu404.moveslikemafuyu.network.CrawlPacket;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mafuyu404.moveslikemafuyu.util.KeyInputHelper;
import com.mafuyu404.moveslikemafuyu.util.PoseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class CrawEvent {
    private static long lastShiftPressTime;
    private static long lastJumpPressTime;
    private static int autoCrawReleaseTicks;

    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;

        if (canLeap(player))
            KeyPrompts.show(options.keyShift.getKey().toString(), "smartkeyprompts.moveslikemafuyu.leap");
        if (canCrawSlide(player))
            KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.slide");

        if (Config.enable("Craw") && player.entityTags().contains("craw")
                && !player.isSpectator() && !player.entityTags().contains("slide")) {
            options.keyShift.setDown(false);
            PoseHelper.forcePose(player, Pose.SWIMMING);
        }
        if (player.entityTags().contains("auto_craw")) {
            if (AutoDodgeEvent.canStandSafely(player)) {
                cancelCraw(player);
            } else if (autoCrawReleaseTicks > 0) {
                autoCrawReleaseTicks--;
            } else {
                autoCrawReleaseTicks = 2;
            }
        }
    }

    @SubscribeEvent
    public static void onAction(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;

        if (KeyInputHelper.isPress(event, options.keyShift)) {
            handleShiftPress(player);
        }

        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player, options);
        }

        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player);
        }
    }

    @SubscribeEvent
    public static void onMouseAction(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;

        if (KeyInputHelper.isPress(event, options.keyShift)) {
            handleShiftPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player, options);
        }
        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player);
        }
    }

    public static void startCraw(Player player) {
        if (player.isSpectator()) return;
        player.addTag("craw");
        player.setSprinting(false);
        PoseHelper.forcePose(player, Pose.SWIMMING);
        TaczCompat.syncCrawling(player, true);
        NetworkHandler.CHANNEL.sendToServer(new CrawlPacket(true));
    }

    public static void startTemporaryCraw(Player player, int ticks) {
        if (!player.entityTags().contains("craw") || player.entityTags().contains("auto_craw")) {
            player.addTag("auto_craw");
            startCraw(player);
            autoCrawReleaseTicks = ticks;
        }
    }

    public static void startLeap(Player player, Vec3 direction) {
        if (!Config.enable("Leap") || player.isSpectator() || player.isInWater()) return;
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        player.setSprinting(true);
        player.setDeltaMovement(player.getDeltaMovement().add(
                horizontal.x * MoveAttributeResolver.getDouble(player, MoveAttribute.LEAP_FORWARD_BOOST),
                MoveAttributeResolver.getDouble(player, MoveAttribute.LEAP_VERTICAL_BOOST),
                horizontal.z * MoveAttributeResolver.getDouble(player, MoveAttribute.LEAP_FORWARD_BOOST)
        ));
        player.addTag("auto_craw");
        autoCrawReleaseTicks = MoveAttributeResolver.getInt(player, MoveAttribute.LEAP_AUTO_CRAW_TICKS);
        startCraw(player);
        lastJumpPressTime = 0;
    }

    public static void cancelCraw(Player player) {
        player.removeTag("craw");
        player.removeTag("auto_craw");
        autoCrawReleaseTicks = 0;
        PoseHelper.clearForcedPose(player);
        TaczCompat.syncCrawling(player, false);
        NetworkHandler.CHANNEL.sendToServer(new CrawlPacket(false));
    }

    public static boolean isAutoCraw(Player player) {
        return player.entityTags().contains("auto_craw");
    }

    public static boolean canLeap(Player player) {
        return Config.enable("Leap")
                && !player.entityTags().contains("craw")
                && player.isSprinting()
                && System.currentTimeMillis() - lastJumpPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.LEAP_JUMP_TIMER)
                && player.getDeltaMovement().y > 0
                && !player.onGround()
                && !player.isInWater();
    }

    public static boolean canCrawSlide(Player player) {
        return Config.enable("CrawSlide")
                && player.getPose() == Pose.SWIMMING
                && player.onGround()
                && SlideEvent.cooldown <= 0
                && !player.entityTags().contains("slide");
    }

    private static void handleShiftPress(Player player) {
        long currentTime = System.currentTimeMillis();
        if (player.entityTags().contains("craw")) {
            cancelCraw(player);
        } else if (Config.enable("Craw") && currentTime - lastShiftPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.CRAW_DOUBLE_PRESS_DELAY) && player.onGround()) {
            startCraw(player);
        } else if (canLeap(player)) {
            Vec3 lookDirection = player.getLookAngle();
            player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            lookDirection.x * MoveAttributeResolver.getDouble(player, MoveAttribute.CRAW_LEAP_FORWARD_BOOST),
                            MoveAttributeResolver.getDouble(player, MoveAttribute.CRAW_LEAP_VERTICAL_BOOST),
                            lookDirection.z * MoveAttributeResolver.getDouble(player, MoveAttribute.CRAW_LEAP_FORWARD_BOOST)
                    )
            );
            startCraw(player);
            lastJumpPressTime *= 10;
        }
        lastShiftPressTime = currentTime;
    }

    private static void handleJumpPress(Player player, Options options) {
        if (player.onGround()) {
            lastJumpPressTime = System.currentTimeMillis();
        }
        if (Config.enable("JumpCancelCraw") && player.entityTags().contains("craw")) {
            cancelCraw(player);
            options.keyJump.setDown(false);
        }
    }

    private static void handleSprintPress(Player player) {
        if (canCrawSlide(player)) {
            SlideEvent.startSlide(player);
        }
    }
}
