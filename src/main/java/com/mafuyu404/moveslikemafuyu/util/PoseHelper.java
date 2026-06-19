package com.mafuyu404.moveslikemafuyu.util;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class PoseHelper {
    public static void forcePose(Player player, Pose pose) {
        player.setForcedPose(pose);
        player.setPose(pose);
        player.refreshDimensions();
    }

    public static void clearForcedPose(Player player) {
        player.setForcedPose(null);
        Pose pose = canUsePose(player, Pose.STANDING) ? Pose.STANDING : Pose.SWIMMING;
        player.setPose(pose);
        player.refreshDimensions();
    }

    private static boolean canUsePose(Player player, Pose pose) {
        EntityDimensions dimensions = player.getDimensions(pose);
        AABB box = dimensions.makeBoundingBox(player.position()).deflate(1.0E-7D);
        return player.level().noCollision(player, box);
    }
}
