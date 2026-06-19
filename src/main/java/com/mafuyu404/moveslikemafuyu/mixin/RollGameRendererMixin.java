package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.event.RollEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class RollGameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void rollCamera(DeltaTracker deltaTracker, CallbackInfo ci, float partialTick, boolean renderBlockOutline, Camera camera, Entity entity, float cameraPartialTick, double fov, Matrix4f projectionMatrix, PoseStack poseStack) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || mainCamera.isDetached() || !Config.enable("RollCamera") || !RollEvent.isRolling(player)) return;
        Vec3 axis = RollEvent.getRollAxis();
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(RollEvent.getRollDegrees(partialTick)), (float) axis.x, (float) axis.y, (float) axis.z));
    }
}
