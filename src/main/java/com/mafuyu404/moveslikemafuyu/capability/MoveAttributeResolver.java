package com.mafuyu404.moveslikemafuyu.capability;

import com.mafuyu404.moveslikemafuyu.attachment.ModAttachments;
import net.minecraft.world.entity.player.Player;

public class MoveAttributeResolver {
    public static int getInt(Player player, MoveAttribute attribute) {
        return player == null ? (int) Math.round(attribute.defaultValue()) : player.getData(ModAttachments.PLAYER_MOVE_ATTRIBUTES).getInt(attribute);
    }

    public static double getDouble(Player player, MoveAttribute attribute) {
        return player == null ? attribute.defaultValue() : player.getData(ModAttachments.PLAYER_MOVE_ATTRIBUTES).getDouble(attribute);
    }
}
