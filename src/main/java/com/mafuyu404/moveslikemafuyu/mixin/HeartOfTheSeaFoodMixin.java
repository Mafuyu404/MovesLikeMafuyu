package com.mafuyu404.moveslikemafuyu.mixin;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class HeartOfTheSeaFoodMixin {
    private static final FoodProperties MOVESLIKEMAFUYU_HEART_OF_THE_SEA_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .alwaysEdible()
            .build();

    private static final Consumable MOVESLIKEMAFUYU_HEART_OF_THE_SEA_CONSUMABLE = Consumable.builder()
            .animation(ItemUseAnimation.EAT)
            .sound(SoundEvents.GENERIC_EAT)
            .build();

    @Inject(method = "components", at = @At("RETURN"), cancellable = true)
    private void moveslikemafuyu$heartOfTheSeaFoodComponents(CallbackInfoReturnable<DataComponentMap> cir) {
        if ((Object) this != Items.HEART_OF_THE_SEA) {
            return;
        }
        cir.setReturnValue(DataComponentMap.builder()
                .addAll(cir.getReturnValue())
                .set(DataComponents.FOOD, MOVESLIKEMAFUYU_HEART_OF_THE_SEA_FOOD)
                .set(DataComponents.CONSUMABLE, MOVESLIKEMAFUYU_HEART_OF_THE_SEA_CONSUMABLE)
                .build());
    }
}
