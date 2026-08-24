package com.mafuyu404.moveslikemafuyu.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mafuyu404.moveslikemafuyu.event.MaidAutoDodgeEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityMaid.class)
public abstract class MaidDodgeTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void logDodgeStateBeforeMaidTick(CallbackInfo callback) {
        MaidAutoDodgeEvent.logMaidTickStart((LivingEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void keepDodgeDuckPoseAfterMaidTick(CallbackInfo callback) {
        MaidAutoDodgeEvent.keepDuckPoseAfterMaidTick((LivingEntity) (Object) this);
    }
}
