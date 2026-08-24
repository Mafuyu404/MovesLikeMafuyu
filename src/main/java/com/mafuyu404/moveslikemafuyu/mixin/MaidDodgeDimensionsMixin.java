package com.mafuyu404.moveslikemafuyu.mixin;

import com.mafuyu404.moveslikemafuyu.compat.MaidCompat;
import com.mafuyu404.moveslikemafuyu.event.MaidAutoDodgeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MaidDodgeDimensionsMixin {
    private static final double DUCK_HEIGHT = 0.6;

    @Inject(method = "refreshDimensions", at = @At("TAIL"))
    private void keepMaidDodgeDimensions(CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (!MaidCompat.isMaid(entity)) return;
        boolean serverDodge = entity.getTags().contains(MaidAutoDodgeEvent.DUCK_TAG);
        boolean clientDodge = entity.level().isClientSide()
                && entity.getPose() == Pose.SWIMMING
                && !entity.isInWater();
        if (!serverDodge && !clientDodge) return;

        AABB box = entity.getBoundingBox();
        entity.setBoundingBox(new AABB(
                box.minX, box.minY, box.minZ,
                box.maxX, box.minY + DUCK_HEIGHT, box.maxZ
        ));
    }
}
