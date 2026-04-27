package com.mafuyu404.moveslikemafuyu.mixin;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class HeartOfTheSeaFoodMixin {
    private static final FoodProperties MOVESLIKEMAFUYU_HEART_OF_THE_SEA_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.1f)
            .alwaysEat()
            .build();

    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    private void moveslikemafuyu$isHeartOfTheSeaEdible(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Items.HEART_OF_THE_SEA) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void moveslikemafuyu$getHeartOfTheSeaFoodProperties(CallbackInfoReturnable<FoodProperties> cir) {
        if ((Object) this == Items.HEART_OF_THE_SEA) {
            cir.setReturnValue(MOVESLIKEMAFUYU_HEART_OF_THE_SEA_FOOD);
        }
    }
}
