package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class KnockMessage {
    private final int size;
    private final ArrayList<Integer> entityId;

    public KnockMessage(ArrayList<Integer> entityId) {
        this.size = entityId.size();
        this.entityId = entityId;
    }

    public static void encode(KnockMessage msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.size);
        msg.entityId.forEach(buffer::writeInt);
    }

    public static KnockMessage decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        ArrayList<Integer> entityId = new ArrayList<>();
        for (int i = 0; i < size; i++) entityId.add(buffer.readInt());
        return new KnockMessage(entityId);
    }

    public static void handle(KnockMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();
            Vec3 playerMotion = player.getLookAngle();
            msg.entityId.forEach(id -> {
                Entity entity = level.getEntity(id);
                if (entity == null) return;
                String[] type = entity.getType().getDescriptionId().split("\\.");
                if (Config.SLIDE_KNOCK_BLACKLIST.get().contains(type[1] + ":" + type[2])) return;
                Vec3 motion = entity.getDeltaMovement();
                double boost = 1;
                entity.setDeltaMovement(motion.add(playerMotion.x * boost, 0.7, playerMotion.z * boost));
                entity.hurtMarked = true;
            });
            player.setDeltaMovement(playerMotion.add(-playerMotion.x, 0, -playerMotion.z));
            player.hurtMarked = true;
        });
        ctx.get().setPacketHandled(true);
    }
}
