package com.mafuyu404.moveslikemafuyu.client;

import com.mafuyu404.moveslikemafuyu.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class ClientMoveAttributesHandler {
    public static void handle(CompoundTag tag) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCapability(ModCapabilities.PLAYER_MOVE_ATTRIBUTES)
                .ifPresent(attributes -> attributes.deserializeNBT(tag));
    }
}
