package com.jht.chimera.service.consoleconfig;


import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Date;
import java.util.Locale;


class PersistentLogSetting extends BooleanConfigSetting {

    PersistentLogSetting(Object dataMatrixKey) {
        super(dataMatrixKey);
    }

    void update(String text) {
        if(text.toLowerCase(Locale.ENGLISH).equals("true")) {
            if(!DataMatrix.getBoolean(key())) {
                DataMatrix.putLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG, new Date().getTime());
            }
        }
        super.update(text);
    }

}