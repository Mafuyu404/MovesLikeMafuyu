package com.mafuyu404.moveslikemafuyu.capability;

import net.minecraft.world.entity.player.Player;

public class MoveAttributeResolver {
    public static int getInt(Player player, MoveAttribute attribute) {
        return player == null ? (int) Math.round(attribute.defaultValue()) : ModCapabilities.get(player).getInt(attribute);
    }

    public static double getDouble(Player player, MoveAttribute attribute) {
        return player == null ? attribute.defaultValue() : ModCapabilities.get(player).getDouble(attribute);
    }
}
