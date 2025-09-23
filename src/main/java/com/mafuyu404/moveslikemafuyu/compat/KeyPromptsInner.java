package com.mafuyu404.moveslikemafuyu.compat;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.smartkeyprompts.SmartKeyPrompts;

public class KeyPromptsInner {
    public static void show(String key, String desc) {
        SmartKeyPrompts.custom(MovesLikeMafuyu.MODID, key, desc);
    }
}
