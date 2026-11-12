package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

public enum AppStoreKeys {
    EVENT_NEW_APP_LIST,
    EVENT_APP_LIST_CHANGED;

    public static final List<DataDefault<?>> defaultValuesList = new LinkedList<>();

    static {
        for (AppStoreKeys s : AppStoreKeys.values()) {
            if (s.dataDefault != null) {
                defaultValuesList.add(s.dataDefault);
            }
        }
    }

    public final DataDefault<?> dataDefault;

    AppStoreKeys() {
        this.dataDefault = null;
    }

}
