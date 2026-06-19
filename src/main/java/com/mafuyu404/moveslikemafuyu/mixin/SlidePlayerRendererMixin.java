package com.mafuyu404.moveslikemafuyu.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public class SlidePlayerRendererMixin {
    @Redirect(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getViewXRot(F)F"
            )
    )
    private float lockSlideFallFlyingPitch(AbstractClientPlayer player, float partialTick) {
        return player.isFallFlying() && player.getTags().contains("slide") ? 0.0F : player.getViewXRot(partialTick);
    }
}
