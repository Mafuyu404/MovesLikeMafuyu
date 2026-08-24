package com.mafuyu404.moveslikemafuyu.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.control.MaidMoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MaidMoveControl.class)
public class MoveControlMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void qqq(CallbackInfo ci) {
        ci.cancel();
    }
}