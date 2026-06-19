package com.mafuyu404.moveslikemafuyu.compat;

import net.neoforged.fml.ModList;

public class KeyPrompts {
    private static final String MOD_ID = "smartkeyprompts";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean show(String key, String desc) {
        if (INSTALLED) {
            KeyPromptsInner.show(key, desc);
        }
        return false;
    }
}
