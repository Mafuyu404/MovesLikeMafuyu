package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.network.payload.KnockPayload;
import com.mafuyu404.moveslikemafuyu.network.payload.TagPayload;
import com.mafuyu404.moveslikemafuyu.util.KeyInputHelper;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class SlideEvent {
    private static double timer;
    private static int air_timer;
    public static int cooldown;
    public static int dap_times;
    public static double dap_motion = 1;
    private static boolean canDap = false;
    private static boolean dap_refreshed = false;
    private static long lastKnockTime = 0;
    private static long lastShiftPressTime;
    private static CameraType storedCameraType;
    @SubscribeEvent
    public static void slideAction(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;

        if (cooldown > 0 && cooldown <= MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_COOLDOWN)) {
            cooldown--;
            double speed = player.getDeltaMovement().length();
            if (!player.isSprinting()) cooldown--;
            if (player.getSpeed() > 0 && player.getSpeed() < 0.15) cooldown--;
            if (speed < 0.05) cooldown--;
            return;
        }

        if (canSlide(player)) KeyPrompts.show(options.keyShift.getKey().toString(), "smartkeyprompts.moveslikemafuyu.slide");
        if (canRefreshDap(player)) KeyPrompts.show(options.keyJump.getKey().toString(), "smartkeyprompts.moveslikemafuyu.dap");

        if (player.getTags().contains("slide")) {
            if (storedCameraType != null) options.setCameraType(storedCameraType);
            if (player.getDeltaMovement().length() < 0.1) {
                cancel(player);
                return;
            }
            options.keyShift.setDown(true);
            Vec3 motion = player.getDeltaMovement();
            Vec3 lookDirection = player.getLookAngle();
            if (dap_times == MoveAttributeResolver.getInt(player, MoveAttribute.DAP_TIMES) && player.isInWater() && !canDap) {
                dap_times--;
                player.setDeltaMovement(
                    motion.add(0, MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_INITIAL_DAP_VERTICAL_BOOST), 0)
                );
            }
            else if (player.isInWater() && canDap && Config.enable("Dap")) {
//                System.out.print("canDap = false;\n");
                canDap = false;
                dap_times--;
                player.setDeltaMovement(
                    motion.add(0, MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_DAP_VERTICAL_BOOST) * dap_motion, 0)
                );
                dap_motion *= MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_DAP_MOTION_DECAY);
            }
            if (!player.onGround() && !player.isInWater()) {
                if (dap_times > 0 && dap_times != MoveAttributeResolver.getInt(player, MoveAttribute.DAP_TIMES) && !canDap) {
                    canDap = true;
                    dap_refreshed = false;
                    player.setDeltaMovement(
                        motion.add(
                                lookDirection.x * MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_AIR_FORWARD_BOOST),
                                0,
                                lookDirection.z * MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_AIR_FORWARD_BOOST)
                        )
                    );
                }
                // 仅增加下坠
                player.setDeltaMovement(
                    player.getDeltaMovement().add(0, MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_AIR_FALL_ACCELERATION), 0)
                );
                air_timer--;
                if (Config.enable("SlideRepeat")) timer = MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_DURATION);
            } else {
                // 在地上滑行
                air_timer = MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_AIR_DURATION); // 落地重置滞空时间
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
        if (player == null || player.isSpectator()) return;
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyShift)) {
            handleShiftPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyDown)) {
            handleDownPress(player);
        }
    }
    @SubscribeEvent
    public static void onMouseAction(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyShift)) {
            handleShiftPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyDown)) {
            handleDownPress(player);
        }
    }
    @SubscribeEvent
    public static void onCollision(PlayerTickEvent.Pre event) {
        if (!Config.enable("SlideKnock")) return;
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (!player.getTags().contains("slide")) return;
        if (System.currentTimeMillis() - lastKnockTime < MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_KNOCK_DELAY)) return;
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
        PacketDistributor.sendToServer(new KnockPayload(entityId));
        lastKnockTime = System.currentTimeMillis();
    }
    public static void startSlide(Player player) {
        startSlide(player, player.getLookAngle());
    }

    public static void startSlide(Player player, Vec3 direction) {
        Options options = Minecraft.getInstance().options;
        if (!Config.enable("Slide") || cooldown > 0) return;
        timer = MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_DURATION);
        air_timer = MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_AIR_DURATION);
        dap_times = MoveAttributeResolver.getInt(player, MoveAttribute.DAP_TIMES);
        canDap = false;
        dap_motion = 1;
        storedCameraType = options.getCameraType();
        PacketDistributor.sendToServer(new TagPayload("slide", true));
        player.setSprinting(true);
        player.addTag("slide");
        Vec3 lookDirection = horizontalDirection(direction);
        player.startFallFlying();
        player.setDeltaMovement(
            player.getDeltaMovement().add(
                    lookDirection.x * MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_START_BOOST),
                    0,
                    lookDirection.z * MoveAttributeResolver.getDouble(player, MoveAttribute.SLIDE_START_BOOST)
            )
        );
        options.keyShift.setDown(true);
        player.playSound(
                SoundEvents.GENERIC_SMALL_FALL,
                0.5f,  // 音量
                0.8f   // 音调
        );
    }
    private static Vec3 horizontalDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            Player player = Minecraft.getInstance().player;
            horizontal = player == null ? new Vec3(0, 0, 1) : new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        }
        return horizontal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : horizontal.normalize();
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
        cooldown = MoveAttributeResolver.getInt(player, MoveAttribute.SLIDE_COOLDOWN);
    }
    @SubscribeEvent
    public static void avoidDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().getTags().contains("slide") && event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
            event.setCanceled(true);
        }
    }

    public static boolean canSlide(Player player) {
        return player.isSprinting() && player.onGround() && !player.isInWater() && !player.isFallFlying() && player.isLocalPlayer() && !Minecraft.getInstance().options.keyJump.isDown() && Config.enable("Slide");
    }
    public static boolean canRefreshDap(Player player) {
        return player.getTags().contains("slide") && Config.enable("Dap") && canDap && !dap_refreshed;
    }
    private static void handleJumpPress(Player player) {
        if (player.getTags().contains("slide")) {
            if (canRefreshDap(player)) {
                dap_refreshed = true;
                dap_times++;
            }
            else cancel(player);
        }
    }

    private static void handleShiftPress(Player player) {
        long currentTime = System.currentTimeMillis();
        boolean doubleTapSlide = !Config.enable("SlideDoubleTapTrigger")
                || currentTime - lastShiftPressTime < MoveAttributeResolver.getInt(player, MoveAttribute.CRAW_DOUBLE_PRESS_DELAY);
        if (canSlide(player) && doubleTapSlide) {
            if (!player.getTags().contains("craw")) startSlide(player);
        }
        lastShiftPressTime = currentTime;
    }

    private static void handleDownPress(Player player) {
        if (player.getTags().contains("slide")) {
            cancel(player);
        }
    }
}
