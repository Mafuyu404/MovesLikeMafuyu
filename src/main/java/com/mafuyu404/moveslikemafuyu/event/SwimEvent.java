package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.KeyPrompts;
import com.mafuyu404.moveslikemafuyu.network.payload.SwimmingBoostPayload;
import com.mafuyu404.moveslikemafuyu.util.KeyInputHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class SwimEvent {
    private static int cooldown;
    private static int swimmingPushDelay;

    @SubscribeEvent
    public static void swim(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= MoveAttributeResolver.getInt(player, MoveAttribute.SWIMMING_BOOST_COOLDOWN)) {
            cooldown--;
        }
        if (swimmingPushDelay > 0 && --swimmingPushDelay == 0) {
            SlideEvent.startSlide(player);
        }

        if (canSwimmingBoost(player)) KeyPrompts.show(options.keySprint.getKey().toString(), "smartkeyprompts.moveslikemafuyu.swimming_boost");
        if (canSwimmingPush(player)) KeyPrompts.show(options.keyJump.getKey().toString(), "smartkeyprompts.moveslikemafuyu.swimming_push");

        boolean canShallowSwim = Config.enable("ShallowSwimming") && player.isInWater() && !player.isUnderWater();
        if (canShallowSwim && !options.keySprint.isDown() && player.getDeltaMovement().length() < 0.1) {
            player.setSwimming(false);
            player.setSprinting(false);
        }
        if (canShallowSwim && (options.keySprint.isDown() || player.isSprinting())) {
            player.setSprinting(true);
            player.setSwimming(true);
        }
        if (Config.enable("Freestyle") && !player.isUnderWater() && player.isSwimming() && !player.getTags().contains("slide")) {
            options.keyJump.setDown(false);
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
        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
        }
    }

    @SubscribeEvent
    public static void onMouseAction(InputEvent.MouseButton.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;
        if (KeyInputHelper.isPress(event, options.keySprint)) {
            handleSprintPress(player);
        }
        if (KeyInputHelper.isPress(event, options.keyJump)) {
            handleJumpPress(player);
        }
    }

    public static boolean canSwimmingBoost(Player player) {
        return Config.enable("SwimmingBoost") && cooldown <= 0 && player.isSwimming();
    }

    public static boolean canSwimmingPush(Player player) {
        return Config.enable("SwimmingPush") && !player.isUnderWater() && player.isInWater() && player.isSwimming() && SlideEvent.cooldown <= 0 && !player.getTags().contains("slide");
    }

    private static void handleSprintPress(Player player) {
        if (canSwimmingBoost(player)) {
            cooldown = MoveAttributeResolver.getInt(player, MoveAttribute.SWIMMING_BOOST_COOLDOWN);
            Vec3 lookDirection = player.getLookAngle();
            player.setDeltaMovement(
                    player.getDeltaMovement().add(
                            lookDirection.x * MoveAttributeResolver.getDouble(player, MoveAttribute.SWIMMING_BOOST_STRENGTH),
                            lookDirection.y * MoveAttributeResolver.getDouble(player, MoveAttribute.SWIMMING_BOOST_STRENGTH),
                            lookDirection.z * MoveAttributeResolver.getDouble(player, MoveAttribute.SWIMMING_BOOST_STRENGTH)
                    )
            );
            player.setAirSupply(player.getAirSupply() - MoveAttributeResolver.getInt(player, MoveAttribute.SWIMMING_BOOST_AIR_COST));
            PacketDistributor.sendToServer(new SwimmingBoostPayload());
            player.playSound(
                    SoundEvents.AMBIENT_UNDERWATER_ENTER,
                    0.9f,
                    0.8f
            );
        }
    }

    private static void handleJumpPress(Player player) {
        if (!player.isUnderWater() && player.isInWater() && player.isSwimming() && canSwimmingPush(player)) {
            swimmingPushDelay = 3;
        }
    }

}
