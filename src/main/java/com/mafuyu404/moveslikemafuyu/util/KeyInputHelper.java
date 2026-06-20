package com.mafuyu404.moveslikemafuyu.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.InputEvent;

public final class KeyInputHelper {
    private KeyInputHelper() {
    }

    public static boolean isPress(InputEvent.Key event, KeyMapping keyMapping) {
        return event.getAction() == InputConstants.PRESS && matches(event, keyMapping);
    }

    public static boolean isPress(InputEvent.MouseButton.Post event, KeyMapping keyMapping) {
        InputConstants.Key key = keyMapping.getKey();
        return event.getAction() == InputConstants.PRESS
                && key.getType() == InputConstants.Type.MOUSE
                && event.getButton() == key.getValue();
    }

    private static boolean matches(InputEvent.Key event, KeyMapping keyMapping) {
        InputConstants.Key key = keyMapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return event.getKey() == key.getValue();
        }
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return event.getScanCode() == key.getValue();
        }
        return false;
    }
}
