package com.mafuyu404.moveslikemafuyu.client;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.util.ConfigFileHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 滑铲与翻滚的独立快捷键。
 * <p>
 * 默认滑铲为 Shift，翻滚为 Ctrl。是否注册由配置 {@code enableShortcut} 决定，
 * 该配置是 Server 类型，在按键注册时尚未加载，所以直接读取配置文件来判断。
 */
@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {
    private static final String CATEGORY = "key.categories.moveslikemafuyu";

    public static KeyMapping SLIDE_SHORTCUT;
    public static KeyMapping ROLL_SHORTCUT;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (!ConfigFileHelper.isShortcutEnabled()) return;

        SLIDE_SHORTCUT = new KeyMapping(
                "key.moveslikemafuyu.slide",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LSHIFT,
                CATEGORY);
        ROLL_SHORTCUT = new KeyMapping(
                "key.moveslikemafuyu.roll",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LCONTROL,
                CATEGORY);
        event.register(SLIDE_SHORTCUT);
        event.register(ROLL_SHORTCUT);
    }
}
