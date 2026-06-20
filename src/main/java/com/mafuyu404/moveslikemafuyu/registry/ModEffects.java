package com.mafuyu404.moveslikemafuyu.registry;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.effect.AutoDodgeEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MovesLikeMafuyu.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> AUTO_DODGE = EFFECTS.register("auto_dodge", AutoDodgeEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
