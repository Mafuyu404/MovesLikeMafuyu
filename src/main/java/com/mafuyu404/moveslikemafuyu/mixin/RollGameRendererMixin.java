package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.event.RollEvent;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class RollGameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private void rollCamera(DeltaTracker deltaTracker, CallbackInfo ci, @Local PoseStack poseStack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || mainCamera.isDetached() || !Config.enable("RollCamera") || !RollEvent.isRolling(player)) return;
        Vec3 axis = RollEvent.getRollAxis();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(RollEvent.getRollDegrees(partialTicks)), (float) axis.x, (float) axis.y, (float) axis.z));
    }
}
