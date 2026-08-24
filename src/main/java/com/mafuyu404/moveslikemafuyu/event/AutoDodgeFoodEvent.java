package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.MaidCompat;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class AutoDodgeFoodEvent {
    private static final int AUTO_DODGE_DURATION = 8 * 60 * 20;
    private static final double MAID_BUFF_RADIUS = 32.0;

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;
        if (!event.getItem().is(Items.HEART_OF_THE_SEA)) return;
        player.addEffect(new MobEffectInstance(ModEffects.AUTO_DODGE.get(), AUTO_DODGE_DURATION, 0, false, true, true));
        // 附近的小女仆也获得自动闪避 buff（可选环境依赖，装了才生效）。
        applyToNearbyMaids(player, AUTO_DODGE_DURATION);
    }

    private static void applyToNearbyMaids(Player player, int duration) {
        if (!MaidCompat.isLoaded()) return;
        List<LivingEntity> maids = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MAID_BUFF_RADIUS), MaidCompat::isMaid);
        for (LivingEntity maid : maids) {
            maid.addEffect(new MobEffectInstance(ModEffects.AUTO_DODGE.get(), duration, 0, false, true, true));
        }
    }
}
