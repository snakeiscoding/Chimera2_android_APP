package com.jht.chimera.lib.common.keypress;

import org.jetbrains.annotations.NonNls;

public class KeyPress {

    public static final int KEYCODE_MASK = 0xff00;
    public static final int KEYPRESS_STATE_MASK = 0x00ff;

    public enum KeyState {
        KEY_UP,
        KEY_DOWN
    }

    // Regarding 'matrixcomm', see https://jhtnaee.atlassian.net/wiki/spaces/MG/pages/986611931/Cardio+Communication+Specification#CardioCommunicationSpecification-Key
    public enum KeyCode {
        KEY_UNKNOWN("unknown"),
        KEY_GO("start"), // Presumably go = start?
        KEY_STOP("stop"),
        KEY_PAUSE("pause"), // Comm spec doesn't mention 'pause'.  They get it anyway.
        KEY_SPEED_UP("speedup"),
        KEY_SPEED_DOWN("speeddown"),
        KEY_INCLINE_UP("inclineup"),
        KEY_INCLINE_DOWN("inclinedown"),
        KEY_RESISTANCE_UP("resistanceup"),
        KEY_RESISTANCE_DOWN("resistancedown"),
        KEY_COOLDOWN("cooldown"),
        KEY_VOLUME_UP("volumeup"),
        KEY_VOLUME_DOWN("volumedown"),
        KEY_HOME("home");

        public final String matrixcommName;
        KeyCode(@NonNls String matrixcommName) {
            this.matrixcommName = matrixcommName;
        }
    }

    public static final KeyCode[] sKeyCodeList = KeyCode.values();

    public static int createBitmapKey(KeyCode keyCode, KeyState keyState) {
        return (keyCode.ordinal() << 8) + keyState.ordinal();
    }

    public static KeyCode getKeyCode(int bitmapKey) {
        int keyCodeValue = (bitmapKey & KEYCODE_MASK) >> 8;
        if (keyCodeValue >= sKeyCodeList.length) return KeyCode.KEY_UNKNOWN;
        return sKeyCodeList[keyCodeValue];
    }

    public static KeyState getKeyState(int bitmapKey) {
        int keyCodeValue = bitmapKey & KEYPRESS_STATE_MASK;
        if (keyCodeValue == 0) return KeyState.KEY_UP;
        return KeyState.KEY_DOWN;
    }
}
