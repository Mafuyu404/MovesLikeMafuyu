package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mafuyu404.moveslikemafuyu.network.SlideMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class SlideEvent {
    private static final int TIMER = 25;
    private static final int AIR_TIMER = 30;
    private static final int COOLDOWN = 60;
    private static final int DAP_TIMES = 2;
    private static int timer = TIMER;
    private static int air_timer = AIR_TIMER;
    public static int cooldown = 0;
    private static int dap_times = DAP_TIMES;
    private static boolean canDap = true;
    @SubscribeEvent
    public static void slideAction(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!player.isLocalPlayer()) return;
        Options options = Minecraft.getInstance().options;

        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
            return;
        }

        if (player.getTags().contains("slide")) {
            if (options.keyDown.isDown()) {
                cancel(player);
                return;
            }
            options.keyShift.setDown(true);
            if (player.isInWater() && dap_times > 0) {
                canDap = true;
                player.setDeltaMovement(
                    player.getDeltaMovement().add(0, 0.07, 0)
                );
            }
            if (!player.onGround() && !player.isInWater()) {
                if (canDap) {
                    canDap = false;
                    dap_times--;
                }
                air_timer--;
                timer = TIMER;

                player.setDeltaMovement(
                    player.getDeltaMovement().add(0, -0.03, 0)
                );
            } else {
                air_timer = AIR_TIMER; // 落地重置滞空时间
                timer--;
                if (player.getDeltaMovement().y > 0) {
                    timer -= 2;
                    Vec3 lookDirection = player.getLookAngle();
                    double boost = 0.03;
                    player.setDeltaMovement(
                        player.getDeltaMovement().add(lookDirection.x * boost, 0, lookDirection.z * boost)
                    );
                }
            }
            if (timer <= 0 || air_timer <= 0) cancel(player);
            return;
        }

        if (player.isSprinting() && player.isShiftKeyDown() && player.onGround() && !player.isInWater() && !player.isFallFlying() && cooldown == 0 && player.isLocalPlayer()) {
            startSlide(player);
        }
    }

    public static void startSlide(Player player) {
        Options options = Minecraft.getInstance().options;
        NetworkHandler.CHANNEL.sendToServer(new SlideMessage(true));
        player.addTag("slide");
        Vec3 lookDirection = player.getLookAngle();
        double boost = 0.4;
        player.startFallFlying();
        player.setDeltaMovement(
            player.getDeltaMovement().add(lookDirection.x * boost, 0, lookDirection.z * boost)
        );
        options.keyShift.setDown(true);
    }

    private static void cancel(Player player) {
        NetworkHandler.CHANNEL.sendToServer(new SlideMessage(false));
        Minecraft.getInstance().options.keyShift.setDown(false);
        player.setShiftKeyDown(false);
        player.stopFallFlying();
        player.setSprinting(true);
        if (player.isInWater()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0, motion.z);
        }
        player.removeTag("slide");
        timer = TIMER;
        air_timer = AIR_TIMER;
        cooldown = COOLDOWN;
        dap_times = DAP_TIMES;
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.isLocalPlayer() && player.getTags().contains("slide")) {
                cancel(player);
            }
        }
    }
}
