package com.mafuyu404.moveslikemafuyu.network.payload;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MoveAttributesPayload(CompoundTag tag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoveAttributesPayload> TYPE =
            new CustomPacketPayload.Type<>(MovesLikeMafuyu.id("move_attributes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveAttributesPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            MoveAttributesPayload::tag,
            MoveAttributesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
