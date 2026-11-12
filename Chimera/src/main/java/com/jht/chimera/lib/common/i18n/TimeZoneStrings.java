package com.jht.chimera.lib.common.i18n;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TimeZoneStrings {

    private String[] displayList;
    private String[] ianaList;

    public TimeZoneStrings(@NonNull Context context) {
        displayList = context.getResources().getStringArray(R.array.timezoneDisplay);
        ianaList = context.getResources().getStringArray(R.array.ianaTimezone);
    }

    public int getCurrentIndex() {
        int index = DataMatrix.getInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT);
        if(index >= 0 && index < displayList.length) {
            return index;
        }
        String currentIANA = getIANAString();
        if(ianaList != null) {
            for(int i = 0; i < ianaList.length; i++) {
                if(ianaList[i].equals(currentIANA)) {
                    return i;
                }
            }
        }
        return -1;
    }


    public String getIANAString() {
        return DataMatrix.getString(SettingKeys.General.DATA_IANA_TIME_ZONE_STRING);
    }

    public String getIANAString(int index) {
        if(index >= 0 && index < ianaList.length) {
            return ianaList[index];
        }
        return "";

    }

}
