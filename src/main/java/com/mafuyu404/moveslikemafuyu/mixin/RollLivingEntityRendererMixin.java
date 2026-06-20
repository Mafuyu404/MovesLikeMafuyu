package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.event.RollEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class RollLivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void rollPlayerModel(AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || state.id != player.getId() || !RollEvent.isRolling(player)) return;
        double centerY = player.getDimensions(Pose.CROUCHING).height() * 0.5D;
        Vec3 axis = RollEvent.getRollAxis();
        poseStack.translate(0.0D, centerY, 0.0D);
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(-RollEvent.getRollDegrees(state.partialTick)), (float) axis.x, (float) axis.y, (float) axis.z));
        poseStack.translate(0.0D, -centerY, 0.0D);
    }
}
