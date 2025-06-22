package com.zhenshiz.moveslikemafuyu.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.zhenshiz.moveslikemafuyu.Config;
import com.zhenshiz.moveslikemafuyu.MovesLikeMafuyu;
import com.zhenshiz.moveslikemafuyu.payload.c2s.KnockPayload;
import com.zhenshiz.moveslikemafuyu.payload.c2s.TagPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class SlideEvent {
    public static int TIMER;
    public static int AIR_TIMER;
    public static int COOLDOWN;
    public static int DAP_TIMES;
    private static double timer = TIMER;
    private static int air_timer = AIR_TIMER;
    public static int cooldown = COOLDOWN;
    public static int dap_times = DAP_TIMES;
    public static double dap_motion = 1;
    private static boolean canDap = false;
    private static boolean dap_refreshed = false;
    private static final long Knock_Delay = 500;
    private static long lastKnockTime = 0;
    private static CameraType storedCameraType;

    @SubscribeEvent
    public static void slideAction(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;

        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
            double speed = player.getDeltaMovement().length();
            if (!player.isSprinting()) cooldown--;
            if (player.getSpeed() > 0 && player.getSpeed() < 0.15) cooldown--;
            if (speed < 0.05) cooldown--;
            return;
        }

        if (player.getTags().contains("slide")) {
            if (storedCameraType != null) options.setCameraType(storedCameraType);
            if (player.getDeltaMovement().length() < 0.1) {
                cancel(player);
                return;
            }
            options.keyShift.setDown(true);
            Vec3 motion = player.getDeltaMovement();
            Vec3 lookDirection = player.getLookAngle();
            if (dap_times == DAP_TIMES && player.isInWater() && !canDap) {
                dap_times--;
                player.setDeltaMovement(
                        motion.add(0, 0.5, 0)
                );
            } else if (player.isInWater() && canDap && Config.ENABLE_DAP.get()) {
                canDap = false;
                dap_times--;
                player.setDeltaMovement(
                        motion.add(0, 0.7 * dap_motion, 0)
                );
                dap_motion *= 0.92;
            }
            if (!player.onGround() && !player.isInWater()) {
                if (dap_times > 0 && dap_times != DAP_TIMES && !canDap) {
                    canDap = true;
                    dap_refreshed = false;
                    double boost = 0.1;
                    player.setDeltaMovement(
                            motion.add(lookDirection.x * boost, 0, lookDirection.z * boost)
                    );
                }
                // 仅增加下坠
                player.setDeltaMovement(
                        player.getDeltaMovement().add(0, -0.025, 0)
                );
                air_timer--;
                if (Config.ENABLE_SLIDE_REPEAT.get()) timer = TIMER;
            } else {
                // 在地上滑行
                air_timer = AIR_TIMER; // 落地重置滞空时间
                if (player.level().getBlockState(player.blockPosition().below()).is(BlockTags.ICE)) timer += 0.5;
                timer--;
                if (player.getDeltaMovement().y > 0) {
                    timer -= 2;
                }
            }
            if (timer <= 0 || air_timer <= 0) {
                cancel(player);
            }
        }
    }

    @SubscribeEvent
    public static void onAction(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator() || event.getAction() != InputConstants.PRESS) return;
        if (event.getKey() == options.keyJump.getKey().getValue()) {
            if (player.getTags().contains("slide")) {
                if (Config.ENABLE_DAP.get() && canDap && !dap_refreshed) {
                    dap_refreshed = true;
                    dap_times++;
                } else cancel(player);
            }
        }
        if (event.getKey() == options.keyShift.getKey().getValue()) {
            if (player.isSprinting() && player.onGround() && !player.isInWater() && !player.isFallFlying() && player.isLocalPlayer() && !options.keyJump.isDown()) {
                if (!player.getTags().contains("craw")) startSlide(player);
            }
        }
        if (event.getKey() == options.keyDown.getKey().getValue()) {
            if (player.getTags().contains("slide")) {
                cancel(player);
            }
        }
    }

    @SubscribeEvent
    public static void onCollision(PlayerTickEvent.Pre event) {
        if (!Config.ENABLE_SLIDE_KNOCK.get()) return;
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        if (!player.getTags().contains("slide")) return;
        if (System.currentTimeMillis() - lastKnockTime < Knock_Delay) return;
        List<Entity> AllEntities = player.level().getEntities(player, player.getBoundingBox().inflate(0.1));
        if (AllEntities.isEmpty()) return;
        ArrayList<Entity> entities = new ArrayList<>();
        Vec3 lookDirection = player.getLookAngle();
        AllEntities.forEach(entity -> {
            if (!(entity instanceof LivingEntity)) return;
            boolean xCheck = (entity.position().x - player.position().x) / lookDirection.x > 0;
            if (!xCheck) return;
            boolean zCheck = (entity.position().z - player.position().z) / lookDirection.z > 0;
            if (!zCheck) return;
            entities.add(entity);
        });
        if (entities.isEmpty()) return;
        ArrayList<Integer> entityId = new ArrayList<>();
        entities.forEach(entity -> entityId.add(entity.getId()));
        PacketDistributor.sendToServer(new KnockPayload(entityId.size(), entityId));
        lastKnockTime = System.currentTimeMillis();
    }

    public static void startSlide(Player player) {
        Options options = Minecraft.getInstance().options;
        if (!Config.ENABLE_SLIDE.get() || cooldown > 0) return;
        timer = TIMER;
        air_timer = AIR_TIMER;
        dap_times = DAP_TIMES;
        canDap = false;
        dap_motion = 1;
        storedCameraType = options.getCameraType();
        PacketDistributor.sendToServer(new TagPayload("slide", true));
        player.setSprinting(true);
        player.addTag("slide");
        Vec3 lookDirection = player.getLookAngle();
        double boost = 0.5;
        player.startFallFlying();
        player.setDeltaMovement(
                player.getDeltaMovement().add(lookDirection.x * boost, 0, lookDirection.z * boost)
        );
        options.keyShift.setDown(true);
        player.playSound(
                SoundEvents.GENERIC_SMALL_FALL,
                0.5f,  // 音量
                0.8f   // 音调
        );
    }

    private static void cancel(Player player) {
        PacketDistributor.sendToServer(new TagPayload("slide", false));
        Minecraft.getInstance().options.keyShift.setDown(false);
        player.setShiftKeyDown(false);
        player.stopFallFlying();
        if (!player.getTags().contains("craw")) player.setSprinting(true);
        if (player.isInWater()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0, motion.z);
        }
        player.removeTag("slide");
        cooldown = COOLDOWN;
    }

    @SubscribeEvent
    public static void avoidDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().getTags().contains("slide") && event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
            event.setCanceled(true);
        }
    }
}
