package com.mafuyu404.moveslikemafuyu;

import com.mafuyu404.moveslikemafuyu.network.NetworkHandler;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MovesLikeMafuyu.MODID)
public class MovesLikeMafuyu {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "moveslikemafuyu";
    public MovesLikeMafuyu() {
        ModEffects.register(FMLJavaModLoadingContext.get().getModEventBus());
        NetworkHandler.register();
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }
}
