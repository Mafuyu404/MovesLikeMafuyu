package com.mafuyu404.moveslikemafuyu.network;

import com.mafuyu404.moveslikemafuyu.client.ClientMoveAttributesHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MoveAttributesMessage {
    private final CompoundTag tag;

    public MoveAttributesMessage(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(MoveAttributesMessage msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.tag);
    }

    public static MoveAttributesMessage decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new MoveAttributesMessage(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(MoveAttributesMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMoveAttributesHandler.handle(msg.tag))
        );
        ctx.get().setPacketHandled(true);
    }
}
