package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class ServerEvent {
    private static final Set<UUID> rollInvulnerablePlayers = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.isLocalPlayer()) return;
        // 将服务端配置同步给客户端
        player.removeTag("slide");
        player.removeTag("craw");
        player.removeTag("roll");
    }
    @SubscribeEvent
    public static void serverSwim(TickEvent.PlayerTickEvent event) {
        // 服务端同步才能改玩家碰撞箱
        Player player = event.player;
        if (player.isLocalPlayer() || player.isSpectator()) return;

        if (player.getTags().contains("roll") && Config.enable("RollInvulnerability")) {
            rollInvulnerablePlayers.add(player.getUUID());
            player.invulnerableTime = Math.max(player.invulnerableTime, Config.ROLL_DURATION.get());
        } else if (rollInvulnerablePlayers.remove(player.getUUID()) && player.invulnerableTime <= Config.ROLL_DURATION.get()) {
            player.invulnerableTime = 0;
        }

        if (player.getTags().contains("craw")) {
            player.setForcedPose(Pose.SWIMMING);
            TaczCompat.syncCrawling(player, true);
            return;
        }
        if (Config.enable("ShallowSwimming") && player.isInWater() && player.isSprinting()) {
            player.setForcedPose(Pose.SWIMMING);
            return;
        }
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
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
        if (player.invulnerableTime <= Config.ROLL_DURATION.get()) player.invulnerableTime = 0;
        player.setForcedPose(null);
        TaczCompat.syncCrawling(player, false);
    }
}
