package com.mafuyu404.moveslikemafuyu.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

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

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void moveslikemafuyu$appendHeartOfTheSeaTooltip(ItemStack stack, @Nullable Level level,
                                                             List<Component> tooltip, TooltipFlag flag,
                                                             CallbackInfo ci) {
        if ((Object) this != Items.HEART_OF_THE_SEA) {
            return;
        }

        boolean maidLoaded = ModList.get().isLoaded("touhou_little_maid");
        String eatTooltip = maidLoaded
                ? "tooltip.moveslikemafuyu.heart_of_the_sea.eat_with_maid"
                : "tooltip.moveslikemafuyu.heart_of_the_sea.eat";
        tooltip.add(Component.translatable(eatTooltip).withStyle(ChatFormatting.GRAY));
        if (maidLoaded) {
            tooltip.add(Component.translatable("tooltip.moveslikemafuyu.heart_of_the_sea.maid_bauble")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
