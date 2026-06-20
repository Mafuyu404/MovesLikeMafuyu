package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.event.SlideEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class SlideCameraBobMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void dampenSlideCameraBob(CameraRenderState cameraRenderState, float partialTick, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (SlideEvent.isSliding(player)) {
            cameraRenderState.entityRenderState.bob = 0.0F;
        }
    }
}
