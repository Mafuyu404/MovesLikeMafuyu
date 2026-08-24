package com.mafuyu404.moveslikemafuyu.util;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 直接读取配置文件的小工具。
 * <p>
 * 按键注册发生在客户端启动阶段，此时 Server 类型的 Forge 配置可能尚未加载，
 * 读取 Config.ENABLE_SHORTCUT 只会得到默认值，无法反映玩家实际编辑过的内容，
 * 因此这里绕过 Forge 配置系统，直接解析配置文件。
 */
public final class ConfigFileHelper {
    private ConfigFileHelper() {
    }

    /**
     * 从 config/{modid}-server.toml 中读取指定布尔键的值。
     *
     * @param key          配置键名（不含等号与缩进）
     * @param defaultValue 文件缺失或解析失败时返回的默认值
     */
    public static boolean readBoolean(String key, boolean defaultValue) {
        Path path = FMLPaths.CONFIGDIR.get().resolve(MovesLikeMafuyu.MODID + "-server.toml");
        try {
            if (Files.isRegularFile(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    int eq = trimmed.indexOf('=');
                    if (eq > 0 && key.equals(trimmed.substring(0, eq).trim())) {
                        String value = trimmed.substring(eq + 1).trim();
                        if (value.startsWith("true")) return true;
                        if (value.startsWith("false")) return false;
                    }
                }
            }
        } catch (Exception ignored) {
            // 读取失败时退回默认值，不影响游戏启动。
        }
        return defaultValue;
    }

    public static boolean isShortcutEnabled() {
        return readBoolean("enableShortcut", true);
    }
}
