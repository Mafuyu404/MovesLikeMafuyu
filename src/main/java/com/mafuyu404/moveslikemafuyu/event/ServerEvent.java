package com.mafuyu404.moveslikemafuyu.event;

import net.neoforged.fml.common.EventBusSubscriber;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import com.mafuyu404.moveslikemafuyu.util.PoseHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class ServerEvent {
    private static final Set<UUID> rollInvulnerablePlayers = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.isLocalPlayer()) return;
        // 清理可能残留在实体上的动作状态
        player.removeTag("slide");
        player.removeTag("craw");
        player.removeTag("roll");
    }
    @SubscribeEvent
    public static void serverSwim(PlayerTickEvent.Post event) {
        // 服务端同步才能改玩家碰撞箱
        Player player = event.getEntity();
        if (player.isLocalPlayer() || player.isSpectator()) return;

        if (player.entityTags().contains("roll") && Config.enable("RollInvulnerability")) {
            rollInvulnerablePlayers.add(player.getUUID());
            player.invulnerableTime = Math.max(player.invulnerableTime, MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DURATION));
        } else if (rollInvulnerablePlayers.remove(player.getUUID()) && player.invulnerableTime <= MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DURATION)) {
            player.invulnerableTime = 0;
        }

        if (player.entityTags().contains("craw")) {
            PoseHelper.forcePose(player, Pose.SWIMMING);
            TaczCompat.syncCrawling(player, true);
            return;
        }
        if (Config.enable("ShallowSwimming") && player.isInWater() && player.isSprinting()) {
            PoseHelper.forcePose(player, Pose.SWIMMING);
            return;
        }
        if (player.getForcedPose() == Pose.SWIMMING) {
            PoseHelper.clearForcedPose(player);
            TaczCompat.syncCrawling(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.isLocalPlayer()) return;
        player.removeTag("craw");
        player.removeTag("roll");
        rollInvulnerablePlayers.remove(player.getUUID());
        if (player.invulnerableTime <= MoveAttributeResolver.getInt(player, MoveAttribute.ROLL_DURATION)) player.invulnerableTime = 0;
        PoseHelper.clearForcedPose(player);
        TaczCompat.syncCrawling(player, false);
    }
}
