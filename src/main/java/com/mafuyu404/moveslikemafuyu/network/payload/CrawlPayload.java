package com.mafuyu404.moveslikemafuyu.network.payload;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CrawlPayload(boolean start) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrawlPayload> TYPE =
            new CustomPacketPayload.Type<>(MovesLikeMafuyu.id("crawl"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrawlPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            CrawlPayload::start,
            CrawlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
