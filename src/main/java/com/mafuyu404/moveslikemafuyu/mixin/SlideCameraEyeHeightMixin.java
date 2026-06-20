package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.event.SlideEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class SlideCameraEyeHeightMixin {
    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void getSlideCameraEyeHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player && player.isLocalPlayer() && SlideEvent.isSliding(player)) {
            float eyeHeight = SlideEvent.getSlideCameraEyeHeight(player);
            if (!Float.isNaN(eyeHeight)) {
                cir.setReturnValue(eyeHeight);
            }
        }
    }
}
