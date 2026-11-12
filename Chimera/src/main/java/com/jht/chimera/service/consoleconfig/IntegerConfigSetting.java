package com.jht.chimera.service.consoleconfig;

import com.jht.datamatrixlib.DataMatrix;

class IntegerConfigSetting extends ConfigSetting {
    private final String TAG = "IntegerConfigSetting";
    IntegerConfigSetting(Object dataMatrixKey) {
        super( dataMatrixKey);
    }

    void update(String text) {
        try {
            int value = Integer.parseInt(text);
            //only update if the value changed
            DataMatrix.updateInt(key(), value);
        }
        catch (NumberFormatException ignored) {}
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
