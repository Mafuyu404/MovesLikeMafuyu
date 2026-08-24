package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.compat.MaidCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MaidDodgeRendererMixin<T extends LivingEntity> {
    private static final double DUCK_CENTER_HEIGHT = 0.3;

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void renderMaidDodge(T entity, PoseStack poseStack, float ageInTicks,
                                 float rotationYaw, float partialTicks, CallbackInfo callback) {
        if (!MaidCompat.isMaid(entity)
                || entity.getPose() != Pose.SWIMMING
                || entity.isInWater()) return;

        double standingCenter = entity.getDimensions(Pose.STANDING).height * 0.5;
        poseStack.translate(0.0, DUCK_CENTER_HEIGHT, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0, -standingCenter, 0.0);
    }
}
