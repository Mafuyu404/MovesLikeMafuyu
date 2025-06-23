package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Timer;
import java.util.TimerTask;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class SwimEvent {
    private static int COOLDOWN;
    private static int AIR_COST;
    private static int cooldown = COOLDOWN;
    @SubscribeEvent
    public static void swim(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
        }
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
        if (Config.enable("SwimmingBoost") && event.getKey() == options.keySprint.getKey().getValue()) {
            if (cooldown <= 0 && player.isSwimming() && event.getAction() == InputConstants.PRESS) {
                cooldown = COOLDOWN;
                Vec3 lookDirection = player.getLookAngle();
                double boost = 0.4;
                player.setDeltaMovement(
                    player.getDeltaMovement().add(lookDirection.x * boost, lookDirection.y * boost, lookDirection.z * boost)
                );
                player.setAirSupply(player.getAirSupply() - AIR_COST);
                // 播放水声
                player.playSound(
                        SoundEvents.AMBIENT_UNDERWATER_ENTER,
                        0.9f,  // 音量
                        0.8f   // 音调
                );
            }
        }
        if (event.getKey() == options.keyJump.getKey().getValue()) {
            if (!player.isUnderWater() && player.isInWater() && player.isSwimming()) {
//                options.keyJump.setDown(false);
                if (SlideEvent.cooldown > 0 || !Config.enable("SwimmingPush") || event.getAction() != InputConstants.PRESS || player.getTags().contains("slide")) return;
                player.setSwimming(false);
                if (!player.getTags().contains("slide")) {
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            SlideEvent.startSlide(player);
                        }
                    },110);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onConfigLoad(PlayerEvent.PlayerLoggedInEvent event) {
        COOLDOWN = Config.ConfigCache.getInt("SwimmingBoostCooldown");
        AIR_COST = Config.ConfigCache.getInt("SwimmingBoostAirCost");
    }
}
