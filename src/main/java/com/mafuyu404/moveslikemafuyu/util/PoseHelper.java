package com.mafuyu404.moveslikemafuyu.util;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class PoseHelper {
    /**
     * 玩家专用的强制姿态：Forge 给 Player 提供了 {@code setForcedPose}，
     * 强制姿态不会被实体自己的逻辑每 tick 覆盖。
     */
    public static void forcePose(Player player, Pose pose) {
        player.setForcedPose(pose);
        player.setPose(pose);
        player.refreshDimensions();
    }

    /**
     * 通用姿态强制（无 forced pose）：LivingEntity 没有 {@code setForcedPose}，
     * 只能靠每 tick 重新 {@code setPose} 来尽量保持姿态。
     */
    public static void forcePose(LivingEntity entity, Pose pose) {
        entity.setPose(pose);
        entity.refreshDimensions();
    }

    public static void clearForcedPose(Player player) {
        player.setForcedPose(null);
        applySafeStandingPose(player);
    }

    public static void clearForcedPose(LivingEntity entity) {
        applySafeStandingPose(entity);
    }

    private static void applySafeStandingPose(LivingEntity entity) {
        Pose pose = canUsePose(entity, Pose.STANDING) ? Pose.STANDING : Pose.SWIMMING;
        entity.setPose(pose);
        entity.refreshDimensions();
    }

    private static boolean canUsePose(LivingEntity entity, Pose pose) {
        EntityDimensions dimensions = entity.getDimensions(pose);
        AABB box = dimensions.makeBoundingBox(entity.position()).deflate(1.0E-7D);
        return entity.level().noCollision(entity, box);
    }
}
