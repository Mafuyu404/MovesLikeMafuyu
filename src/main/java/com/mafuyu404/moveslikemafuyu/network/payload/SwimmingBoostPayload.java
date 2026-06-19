package com.mafuyu404.moveslikemafuyu.network.payload;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SwimmingBoostPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SwimmingBoostPayload> TYPE =
            new CustomPacketPayload.Type<>(MovesLikeMafuyu.id("swimming_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwimmingBoostPayload> CODEC =
            StreamCodec.unit(new SwimmingBoostPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
