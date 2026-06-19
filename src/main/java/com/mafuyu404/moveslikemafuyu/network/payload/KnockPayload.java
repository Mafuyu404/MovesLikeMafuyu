package com.mafuyu404.moveslikemafuyu.network.payload;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record KnockPayload(List<Integer> entityIds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KnockPayload> TYPE =
            new CustomPacketPayload.Type<>(MovesLikeMafuyu.id("knock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnockPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
            KnockPayload::entityIds,
            KnockPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
