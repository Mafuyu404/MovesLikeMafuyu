package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.event.SlideEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TagMessage {
    private final String tag;
    private final boolean state;

    public TagMessage(String tag, boolean state) {
        this.tag = tag;
        this.state = state;
    }

    public static void encode(TagMessage msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.tag);
        buffer.writeBoolean(msg.state);
    }

    public static TagMessage decode(FriendlyByteBuf buffer) {
        return new TagMessage(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(TagMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.state) {
                if (!player.getTags().contains(msg.tag)) {
                    player.addTag(msg.tag);
                }
                if (msg.tag.equals("craw")) {
                    player.setForcedPose(Pose.SWIMMING);
                }
            }
            else {
                player.removeTag(msg.tag);
                if (msg.tag.equals("craw")) {
                    player.setForcedPose(null);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
