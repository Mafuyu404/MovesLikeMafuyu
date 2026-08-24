package com.mafuyu404.moveslikemafuyu.compat;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LittleMaidExtension
public final class MaidBaubleExtension implements ILittleMaid {
    private static final IMaidBauble HEART_OF_THE_SEA_BAUBLE = new HeartOfTheSeaBauble();

    @Override
    public void bindMaidBauble(BaubleManager manager) {
        manager.bind(Items.HEART_OF_THE_SEA, HEART_OF_THE_SEA_BAUBLE);
    }

    private static final class HeartOfTheSeaBauble implements IMaidBauble {
        private static final int EFFECT_DURATION = 40;
        private static final int REFRESH_AT_DURATION = 20;

        @Override
        public void onPutOn(EntityMaid maid, ItemStack baubleItem) {
            maintainAutoDodge(maid);
        }

        @Override
        public void onTick(EntityMaid maid, ItemStack baubleItem) {
            maintainAutoDodge(maid);
        }

        private static void maintainAutoDodge(EntityMaid maid) {
            if (maid.level().isClientSide()) return;

            MobEffectInstance current = maid.getEffect(ModEffects.AUTO_DODGE.get());
            if (current != null) {
                int refreshAt = current.getAmplifier() > 0 ? 1 : REFRESH_AT_DURATION;
                if (current.isInfiniteDuration() || current.getDuration() > refreshAt) return;
            }
            maid.addEffect(new MobEffectInstance(
                    ModEffects.AUTO_DODGE.get(), EFFECT_DURATION, 0, false, false, false
            ));
        }
    }
}
