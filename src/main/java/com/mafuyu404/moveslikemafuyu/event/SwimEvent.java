package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
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

import java.util.Timer;
import java.util.TimerTask;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class SwimEvent {
    private static int cooldown;
    @SubscribeEvent
    public static void swim(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= Config.SWIMMING_BOOST_COOLDOWN.get()) {
            cooldown--;
        }

        if (canSwimmingBoost(player)) KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.swimming_boost");
        if (canSwimmingPush(player)) KeyPrompts.show(options.keyJump.getKey().toString(), "smartkeyprompts.moveslikemafuyu.swimming_push");

        if (player.getDeltaMovement().length() < 0.1) {
            player.setSwimming(false);
            player.setSprinting(false);
        }
        if (Config.enable("ShallowSwimming") && player.isInWater() && options.keySprint.isDown()) {
            player.setSprinting(true);
            player.setSwimming(true);
        }
        if (Config.enable("Freestyle") && !player.isUnderWater() && player.isSwimming()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0, motion.z);
        }
    }
    @SubscribeEvent
    public static void onAction(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;
        if (event.getAction() == InputConstants.PRESS && event.getKey() == options.keySprint.getKey().getValue()) {
            if (canSwimmingBoost(player)) {
                cooldown = Config.SWIMMING_BOOST_COOLDOWN.get();
                Vec3 lookDirection = player.getLookAngle();
                player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            lookDirection.x * Config.SWIMMING_BOOST_STRENGTH.get(),
                            lookDirection.y * Config.SWIMMING_BOOST_STRENGTH.get(),
                            lookDirection.z * Config.SWIMMING_BOOST_STRENGTH.get()
                    )
                );
                player.setAirSupply(player.getAirSupply() - Config.SWIMMING_BOOST_AIR_COST.get());
                // 播放水声
                player.playSound(
                        SoundEvents.AMBIENT_UNDERWATER_ENTER,
                        0.9f,  // 音量
                        0.8f   // 音调
                );
            }
        }
        if (event.getKey() == options.keyJump.getKey().getValue()) {
            if (!player.isUnderWater() && player.isInWater() && player.isSwimming() && event.getAction() == InputConstants.PRESS) {
                if (canSwimmingPush(player)) {
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            SlideEvent.startSlide(player);
                        }
                    },110);
                }
//                options.keyJump.setDown(false);
//                if (SlideEvent.cooldown > 0 || !Config.enable("SwimmingPush") || event.getAction() != InputConstants.PRESS || player.getTags().contains("slide")) return;
//                player.setSwimming(false);
//                if (!player.getTags().contains("slide")) {
//                    new Timer().schedule(new TimerTask() {
//                        public void run() {
//                            SlideEvent.startSlide(player);
//                        }
//                    },110);
//                }
            }
        }
    }
    public static boolean canSwimmingBoost(Player player) {
        return Config.enable("SwimmingBoost") && cooldown <= 0 && player.isSwimming();
    }
    public static boolean canSwimmingPush(Player player) {
        return Config.enable("SwimmingPush") && !player.isUnderWater() && player.isInWater() && player.isSwimming() && SlideEvent.cooldown <= 0 && !player.getTags().contains("slide");
    }
}
