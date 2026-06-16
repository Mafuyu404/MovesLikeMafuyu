package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.event.RollEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class RollLivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void rollPlayerModel(T entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (entity != player || !RollEvent.isRolling(player)) return;
        double centerY = player.getDimensions(Pose.CROUCHING).height * 0.5D;
        poseStack.translate(0.0D, centerY, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-RollEvent.getRollDegrees(partialTicks)));
        poseStack.translate(0.0D, -centerY, 0.0D);
    }
}
