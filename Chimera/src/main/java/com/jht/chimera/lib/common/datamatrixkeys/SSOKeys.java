package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.StringListDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum SSOKeys {
    DATA_SSO_SERVICE_STATE_INT(new DataDefault<>(0)), // only set within service, can get anywhere
    ACTION_SSO_SAVE_USER_CUSTOM_WORKOUT,
    DATA_SSO_DOWNLOADED_APP_PACKAGE_STRING_LIST(new StringListDataDefault()),
    DATA_USE_SSO_FOR_WEIGHT_BOOL(new DataDefault<>(false)),
    ACTION_SSO_UPDATE_APP_STRING;

    public static final class States {
        public static final int UNKNOWN = 0;
        public static final int IDLE = 1;
        public static final int DOWNLOADING = 2;
        public static final int UPLOADING = 3;
    }

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (SSOKeys s : SSOKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    SSOKeys(){
        this.DataDefault = null;
    }

    SSOKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
