package com.mafuyu404.moveslikemafuyu;

import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import com.mafuyu404.moveslikemafuyu.capability.ModCapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MovesLikeMafuyu.MODID)
public class MovesLikeMafuyu {

    public static final String MODID = "moveslikemafuyu";
    public MovesLikeMafuyu(IEventBus modEventBus, ModContainer modContainer) {
        ModEffects.register(modEventBus);
        ModCapabilities.register(modEventBus);
        NetworkHandler.register();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                Config.SPEC
        );
    }
}
