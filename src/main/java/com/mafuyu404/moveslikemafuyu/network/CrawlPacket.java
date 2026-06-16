package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.compat.TaczCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CrawlPacket {
    private final boolean start;

    public CrawlPacket(boolean start) {
        this.start = start;
    }

    public static void encode(CrawlPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.start);
    }

    public static CrawlPacket decode(FriendlyByteBuf buf) {
        return new CrawlPacket(buf.readBoolean());
    }

    public static void handle(CrawlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.start) {
                player.addTag("craw");
                player.setForcedPose(Pose.SWIMMING);
                TaczCompat.syncCrawling(player, true);
            } else {
                player.removeTag("craw");
                player.setForcedPose(null);
                TaczCompat.syncCrawling(player, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
