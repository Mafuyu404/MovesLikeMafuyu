package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import com.mafuyu404.moveslikemafuyu.network.ConfigMessage;
import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class ServerEvent {
    private static CompoundTag config = new CompoundTag();
    private static final int ROLL_INVULNERABLE_TIME = RollEvent.DURATION;
    private static final Set<UUID> rollInvulnerablePlayers = new HashSet<>();

    @SubscribeEvent
    public static void onConfigLoad(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.isLocalPlayer()) return;
        if (config.isEmpty()) config = Config.getAllConfig();
        // 将服务端配置同步给客户端
        NetworkHandler.sendToClient((ServerPlayer) event.getEntity(), new ConfigMessage(config));
        player.removeTag("slide");
        player.removeTag("craw");
        player.removeTag("roll");
    }
    @SubscribeEvent
    public static void serverSwim(TickEvent.PlayerTickEvent event) {
        // 服务端同步才能改玩家碰撞箱
        Player player = event.player;
        if (player.isLocalPlayer() || player.isSpectator()) return;

        if (player.getTags().contains("roll")) {
            rollInvulnerablePlayers.add(player.getUUID());
            player.invulnerableTime = Math.max(player.invulnerableTime, ROLL_INVULNERABLE_TIME);
        } else if (rollInvulnerablePlayers.remove(player.getUUID()) && player.invulnerableTime <= ROLL_INVULNERABLE_TIME) {
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
        if (player.invulnerableTime <= ROLL_INVULNERABLE_TIME) player.invulnerableTime = 0;
        player.setForcedPose(null);
        TaczCompat.syncCrawling(player, false);
    }
}
