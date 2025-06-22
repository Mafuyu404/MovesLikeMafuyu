package com.zhenshiz.moveslikemafuyu.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.zhenshiz.moveslikemafuyu.Config;
import com.zhenshiz.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Timer;
import java.util.TimerTask;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class SwimEvent {
    public static int COOLDOWN;
    public static int AIR_COST;
    private static int cooldown = COOLDOWN;

    @SubscribeEvent
    public static void swim(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
        }
        if (Config.ENABLE_SHALLOW_SWIMMING.get() && player.isInWater() && options.keySprint.isDown()) {
            player.setSprinting(true);
            player.setSwimming(true);
        }
        if (Config.ENABLE_FREESTYLE.get() && !player.isUnderWater() && player.isSwimming()) {
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
        if (Config.ENABLE_SWIMMING_BOOST.get() && event.getKey() == options.keySprint.getKey().getValue()) {
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
                if (SlideEvent.cooldown > 0 || !Config.ENABLE_SWIMMING_PUSH.get() || event.getAction() != InputConstants.PRESS || player.getTags().contains("slide"))
                    return;
                player.setSwimming(false);
                if (!player.getTags().contains("slide")) {
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            SlideEvent.startSlide(player);
                        }
                    }, 110);
                }
            }
        }
    }
}
