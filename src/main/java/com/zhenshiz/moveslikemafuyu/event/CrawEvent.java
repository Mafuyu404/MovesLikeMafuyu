package com.zhenshiz.moveslikemafuyu.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.zhenshiz.moveslikemafuyu.Config;
import com.zhenshiz.moveslikemafuyu.MovesLikeMafuyu;
import com.zhenshiz.moveslikemafuyu.payload.c2s.TagPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class CrawEvent {
    private static final int DOUBLE_PRESS_DELAY = 250; // 毫秒
    private static final int JUMP_TIMER = 500;
    private static long lastShiftPressTime;
    private static long lastJumpPressTime;

    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;

        if (Config.ENABLE_CRAW.get() && player.getTags().contains("craw") && !player.isSpectator() && !player.getTags().contains("slide")) {
            options.keyShift.setDown(false);
            player.setForcedPose(Pose.SWIMMING);
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
                // 爬行状态下按潜行键就是退出爬行
                cancelCraw(player);
            } else if (Config.ENABLE_CRAW.get() && currentTime - lastShiftPressTime < DOUBLE_PRESS_DELAY && player.onGround()) {
                // 不在爬行状态且双击潜行键那就进入爬行状态
                startCraw(player);
            } else if (Config.ENABLE_LEAP.get() && player.isSprinting() && currentTime - lastJumpPressTime < JUMP_TIMER && player.getDeltaMovement().y > 0 && !player.onGround() && !player.isInWater()) {
                // 满足条件就触发飞扑
                Vec3 lookDirection = player.getLookAngle();
                double boost = 0.25;
                player.setDeltaMovement(
                        player.getDeltaMovement().add(lookDirection.x * boost, 0.15, lookDirection.z * boost)
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
            if (Config.ENABLE_JUMP_CANCEL_CRAW.get() && player.getTags().contains("craw")) {
                cancelCraw(player);
                options.keyJump.setDown(false);
            }
        }
        if (event.getKey() == options.keySprint.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
            if (Config.ENABLE_CRAW_SLIDE.get() && player.getPose() == Pose.SWIMMING && player.onGround()) {
                if (SlideEvent.cooldown <= 0 && !player.getTags().contains("slide")) SlideEvent.startSlide(player);
            }
        }
    }

    public static void startCraw(Player player) {
        if (player.isSpectator()) return;
        PacketDistributor.sendToServer(new TagPayload("craw", true));
        player.addTag("craw");
        player.setSprinting(false);
    }

    public static void cancelCraw(Player player) {
        PacketDistributor.sendToServer(new TagPayload("craw", false));
        player.removeTag("craw");
        player.setForcedPose(null);
    }

}
