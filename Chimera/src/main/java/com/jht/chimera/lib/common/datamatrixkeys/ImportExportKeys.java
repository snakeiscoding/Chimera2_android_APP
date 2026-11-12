package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.StringListDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum ImportExportKeys {
    ACTION_BROADCAST_SETTINGS,
    EVENT_REFRESH_UI,
    DATA_CONSOLE_IDENTIFICATION_AND_STATUS_STRING_LIST(new StringListDataDefault()),
    DATA_EXPORT_SETTINGS_STRING(new DataDefault<>("")),
    ACTION_EXPORT_SETTINGS_USB,
    EVENT_FINISHED_EXPORT_USB,
    ACTION_IMPORT_SETTINGS_USB,
    ACTION_IMPORT_TV_CHANNELS_INTERNET,
    ACTION_EXPORT_TV_CHANNELS_INTERNET,
    EVENT_FINISHED_TV_CHANNEL_TRANSFER_PARCELABLE,
    EVENT_FINISHED_IMPORT_USB,
    DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL(new DataDefault<>(false)),
    DATA_CURRENT_DATE_FORMAT_INT(new DataDefault<>(0));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (ImportExportKeys s : ImportExportKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    ImportExportKeys(){
        this.DataDefault = null;
    }

    ImportExportKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

}