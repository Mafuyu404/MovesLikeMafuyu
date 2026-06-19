package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttribute;
import com.mafuyu404.moveslikemafuyu.capability.MoveAttributeResolver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwimmingBoostMessage {
    public SwimmingBoostMessage() {
    }

    public static void encode(SwimmingBoostMessage msg, FriendlyByteBuf buffer) {
    }

    public static SwimmingBoostMessage decode(FriendlyByteBuf buffer) {
        return new SwimmingBoostMessage();
    }

    public static void handle(SwimmingBoostMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !Config.enable("SwimmingBoost") || !player.isSwimming()) return;
            int airCost = MoveAttributeResolver.getInt(player, MoveAttribute.SWIMMING_BOOST_AIR_COST);
            player.setAirSupply(player.getAirSupply() - airCost);
        });
        ctx.get().setPacketHandled(true);
    }
}
