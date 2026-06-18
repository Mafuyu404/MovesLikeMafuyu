package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import com.mafuyu404.moveslikemafuyu.network.CrawlPacket;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class CrawEvent {
    private static long lastShiftPressTime;
    private static long lastJumpPressTime;
    private static int autoCrawReleaseTicks;

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;

        if (canLeap(player))
            KeyPrompts.show(options.keyShift.getKey().toString(), "smartkeyprompts.moveslikemafuyu.leap");
        if (canCrawSlide(player))
            KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.slide");

        if (Config.enable("Craw") && player.getTags().contains("craw")
                && !player.isSpectator() && !player.getTags().contains("slide")) {
            options.keyShift.setDown(false);
            player.setForcedPose(Pose.SWIMMING);
        }
        if (player.getTags().contains("auto_craw")) {
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

        if (event.getKey() == options.keyShift.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
            long currentTime = System.currentTimeMillis();
            if (player.getTags().contains("craw")) {
                cancelCraw(player);
            } else if (Config.enable("Craw") && currentTime - lastShiftPressTime < Config.CRAW_DOUBLE_PRESS_DELAY.get() && player.onGround()) {
                startCraw(player);
            } else if (canLeap(player)) {
                Vec3 lookDirection = player.getLookAngle();
                player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            lookDirection.x * Config.CRAW_LEAP_FORWARD_BOOST.get(),
                            Config.CRAW_LEAP_VERTICAL_BOOST.get(),
                            lookDirection.z * Config.CRAW_LEAP_FORWARD_BOOST.get()
                    )
                );
                startCraw(player);
                lastJumpPressTime *= 10;
            }
            lastShiftPressTime = currentTime;
        }

        if (event.getKey() == options.keyJump.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
            if (player.onGround()) {
                lastJumpPressTime = System.currentTimeMillis();
            }
            if (Config.enable("JumpCancelCraw") && player.getTags().contains("craw")) {
                cancelCraw(player);
                options.keyJump.setDown(false);
            }
        }

        if (event.getKey() == options.keySprint.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
            if (canCrawSlide(player)) {
                SlideEvent.startSlide(player);
            }
        }
    }

    public static void startCraw(Player player) {
        if (player.isSpectator()) return;
        player.addTag("craw");
        player.setSprinting(false);
        player.setForcedPose(Pose.SWIMMING);
        TaczCompat.syncCrawling(player, true);
        NetworkHandler.CHANNEL.sendToServer(new CrawlPacket(true));
    }

    public static void startTemporaryCraw(Player player, int ticks) {
        if (!player.getTags().contains("craw") || player.getTags().contains("auto_craw")) {
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
                horizontal.x * Config.LEAP_FORWARD_BOOST.get(),
                Config.LEAP_VERTICAL_BOOST.get(),
                horizontal.z * Config.LEAP_FORWARD_BOOST.get()
        ));
        player.addTag("auto_craw");
        autoCrawReleaseTicks = Config.LEAP_AUTO_CRAW_TICKS.get();
        startCraw(player);
        lastJumpPressTime = 0;
    }

    public static void cancelCraw(Player player) {
        player.removeTag("craw");
        player.removeTag("auto_craw");
        autoCrawReleaseTicks = 0;
        player.setForcedPose(null);
        TaczCompat.syncCrawling(player, false);
        NetworkHandler.CHANNEL.sendToServer(new CrawlPacket(false));
    }

    public static boolean isAutoCraw(Player player) {
        return player.getTags().contains("auto_craw");
    }

    public static boolean canLeap(Player player) {
        return Config.enable("Leap")
                && !player.getTags().contains("craw")
                && player.isSprinting()
                && System.currentTimeMillis() - lastJumpPressTime < Config.LEAP_JUMP_TIMER.get()
                && player.getDeltaMovement().y > 0
                && !player.onGround()
                && !player.isInWater();
    }

    public static boolean canCrawSlide(Player player) {
        return Config.enable("CrawSlide")
                && player.getPose() == Pose.SWIMMING
                && player.onGround()
                && SlideEvent.cooldown <= 0
                && !player.getTags().contains("slide");
    }
}
