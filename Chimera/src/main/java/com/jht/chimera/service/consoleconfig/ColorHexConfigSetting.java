package com.jht.chimera.service.consoleconfig;

import android.graphics.Color;

import com.jht.datamatrixlib.DataMatrix;

class ColorHexConfigSetting extends ConfigSetting {
    private final String TAG = "ColorHexConfigSetting";
    ColorHexConfigSetting(Object dataMatrixKey) {
        super(dataMatrixKey);
    }

    void update(String text) {
        try {
            text = text.replace("0x", "#");
            int value = Color.parseColor(text);
            //only update if the value changed
            DataMatrix.updateInt(key(), value);
        }
        catch (Exception ignored) {}
    }

    @Override
    boolean getBool() {
        return false; // don't use
    }

    @Override
    String getString() {
        return null; // don't use
    }

    @Override
    int getInt() {
        return DataMatrix.getInt(key());
    }

    @Override
    String getType() {
        return "integer";
    }
}
