package com.jht.chimera.service.consoleconfig;

import com.jht.datamatrixlib.DataMatrix;

class BooleanConfigSetting extends ConfigSetting {
    private final String TAG = "BooleanConfigSetting";
    BooleanConfigSetting(Object dataMatrixKey) {
        super(dataMatrixKey);
    }

    /**
     * This will set the datamatrix key only if the provided text is a different boolean value
     * @param text the boolean value
     */
    void update(String text) {
        //if text is true and current value is false, set to true
        if (text.equalsIgnoreCase("true") && !DataMatrix.getBoolean(key())) {
            DataMatrix.putBoolean(key(), true);
            // if text is false and current value is true, set to false
        } else if (text.equalsIgnoreCase("false") && DataMatrix.getBoolean(key())) {
            DataMatrix.putBoolean(key(), false);
        }
    }

    @Override
    boolean getBool() {
        return DataMatrix.getBoolean(key());
    }

    @Override
    String getString() {
        return null; // don't use
    }

    @Override
    int getInt() {
        return 0; // don't use
    }

    @Override
    String getType() {
        return "boolean";
    }
}
