package com.mafuyu404.moveslikemafuyu.compat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

/**
 * Touhou Little Maid 的可选兼容层。
 * <p>
 * 通过 ModList 判断是否加载，并用 {@link Class#forName(String)} 懒加载女仆实体类，
 * 避免对女仆模组产生硬依赖。仅通过反射判断实体类型，不直接引用女仆的类。
 */
public final class MaidCompat {
    private static final String MAID_MODID = "touhou_little_maid";
    private static final String MAID_CLASS = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";

    private static Optional<Class<?>> maidType = Optional.empty();
    private static boolean checked;

    private MaidCompat() {
    }

    public static boolean isLoaded() {
        return maidClass().isPresent();
    }

    public static boolean isMaid(Entity entity) {
        return maidClass().map(type -> type.isInstance(entity)).orElse(false);
    }

    private static Optional<Class<?>> maidClass() {
        if (checked) return maidType;
        checked = true;
        if (ModList.get().isLoaded(MAID_MODID)) {
            try {
                maidType = Optional.of(Class.forName(MAID_CLASS));
            } catch (ClassNotFoundException | LinkageError ignored) {
                maidType = Optional.empty();
            }
        }
        return maidType;
    }
}
