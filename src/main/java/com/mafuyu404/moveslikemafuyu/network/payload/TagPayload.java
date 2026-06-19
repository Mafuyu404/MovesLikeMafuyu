package com.mafuyu404.moveslikemafuyu.network.payload;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TagPayload(String tag, boolean state) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TagPayload> TYPE =
            new CustomPacketPayload.Type<>(MovesLikeMafuyu.id("tag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TagPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TagPayload::tag,
            ByteBufCodecs.BOOL,
            TagPayload::state,
            TagPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
