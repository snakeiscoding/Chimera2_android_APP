package com.jht.chimera.service.consoleconfig;

import com.jht.datamatrixlib.DataMatrix;

class StringConfigSetting extends ConfigSetting {
    private final String TAG = "StringConfigSetting";
    StringConfigSetting(Object dataMatrixKey) {
        super(dataMatrixKey);
    }

    void update(String text) {
        //only update if the value changed
        if(!DataMatrix.getString(key()).equals(text)){
            DataMatrix.putString(key(), text);
        }
    }

    @Override
    boolean getBool() {
        return false; // don't use
    }

    @Override
    String getString() {
        return DataMatrix.getString(key());
    }

    @Override
    int getInt() {
        return 0; // don't use
    }

    @Override
    String getType() {
        return "string";
    }
}
