package com.mafuyu404.moveslikemafuyu.capability;

import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicReference;

public class MoveAttributeResolver {
    public static int getInt(Player player, MoveAttribute attribute) {
        AtomicReference<Integer> value = new AtomicReference<>((int) Math.round(attribute.defaultValue()));
        if (player != null) {
            player.getCapability(ModCapabilities.PLAYER_MOVE_ATTRIBUTES)
                    .ifPresent(attributes -> value.set(attributes.getInt(attribute)));
        }
        return value.get();
    }

    public static double getDouble(Player player, MoveAttribute attribute) {
        AtomicReference<Double> value = new AtomicReference<>(attribute.defaultValue());
        if (player != null) {
            player.getCapability(ModCapabilities.PLAYER_MOVE_ATTRIBUTES)
                    .ifPresent(attributes -> value.set(attributes.getDouble(attribute)));
        }
        return value.get();
    }
}
