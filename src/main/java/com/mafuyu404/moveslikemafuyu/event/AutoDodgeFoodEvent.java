package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class AutoDodgeFoodEvent {
    private static final int AUTO_DODGE_DURATION = 8 * 60 * 20;

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;
        if (!event.getItem().is(Items.HEART_OF_THE_SEA)) return;
        player.addEffect(new MobEffectInstance(ModEffects.AUTO_DODGE, AUTO_DODGE_DURATION, 0, false, true, true));
    }
}
