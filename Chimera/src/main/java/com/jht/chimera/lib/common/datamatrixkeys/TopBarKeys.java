package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

public enum TopBarKeys {

    ACTION_SHOW_BUSY_INDICATOR,
    ACTION_HIDE_BUSY_INDICATOR,

    DATA_DAPI_CONNECTED_BOOL(new DataDefault<>(false)),
    DATA_IS_UPDATE_DOWNLOADING_BOOL(new DataDefault<>(false)),
    DATA_WIFI_STRENGTH_ENUM_INT(new DataDefault<>(0)),
    DATA_NETWORK_CONNECTION_BOOL(new DataDefault<>(false)),
    DATA_BT_HEADPHONE_CONNECTED_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (TopBarKeys s : TopBarKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    TopBarKeys(){
        this.DataDefault = null;
    }

    TopBarKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public enum InternetStatus {
        NO_CONNECTION,
        STRENGTH_25,
        STRENGTH_50,
        STRENGTH_75,
        STRENGTH_100,
        ETHERNET
    }
}
