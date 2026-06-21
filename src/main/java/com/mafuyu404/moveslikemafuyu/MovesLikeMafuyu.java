package com.mafuyu404.moveslikemafuyu;

import com.mafuyu404.moveslikemafuyu.attachment.ModAttachments;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(MovesLikeMafuyu.MOD_ID)
public class MovesLikeMafuyu {
    public static final String MOD_ID = "moveslikemafuyu";
    public static final String MODID = MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public MovesLikeMafuyu(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        ModAttachments.register(modEventBus);
        ModEffects.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "moveslikemafuyu-common.toml");
        if (dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
