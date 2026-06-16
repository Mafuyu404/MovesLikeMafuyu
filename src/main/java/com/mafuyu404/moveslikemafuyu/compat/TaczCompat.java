package com.mafuyu404.moveslikemafuyu.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TaczCompat {
    private static final String TACZ_MODID = "tacz";
    private static final Map<Class<?>, Optional<Method>> CRAWL_METHODS = new ConcurrentHashMap<>();

    private TaczCompat() {
    }

    public static void syncCrawling(Player player, boolean crawling) {
        if (!ModList.get().isLoaded(TACZ_MODID)) return;
        Method method = getCrawlMethod(player);
        if (method == null) return;
        try {
            method.invoke(player, crawling);
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
    }

    private static Method getCrawlMethod(Player player) {
        return CRAWL_METHODS.computeIfAbsent(player.getClass(), TaczCompat::findCrawlMethod).orElse(null);
    }

    private static Optional<Method> findCrawlMethod(Class<?> playerClass) {
        try {
            return Optional.of(playerClass.getMethod("crawl", boolean.class));
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }
}
