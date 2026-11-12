package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum LoggerKeys {

    ACTION_CHECK_BELT_LUBE,
    DATA_ERROR_LOG_LIST_PARCELABLE(new ParcelableDataDefault()),
    DATA_USB_STICK_ATTACHED_BOOL(new DataDefault<>(false)),

    ACTION_VIEW_ANALYTICS,
    ACTION_EXPORT_ANALYTICS,
    ACTION_RESET_ANALYTICS,
    ACTION_RESET_PTP_ANALYTICS,
    ACTION_SEND_PERSISTENT_CRASH,
    ACTION_SEND_MANUAL_CRASH,
    ACTION_CLEAR_CRASH_REPORTING_CACHE,
    ACTION_UPDATE_CRASH_REPORTING_CACHE,
    ACTION_DOWNLOAD_CACHED_LOGS,
    ACTION_ADD_SERVICE_PARCELABLE,
    ACTION_DELETE_SERVICE_LOG_PARCELABLE,
    DATA_SERVICE_LOG_LIST_PARCELABLE(new ParcelableDataDefault()),
    ACTION_ENABLE_ADB_WIFI,
    ACTION_SHOW_HAL_DLG,
    ACTION_SHOW_BANDWIDTH_TEST,
    ACTION_SAVE_LOGS,
    /**
     * Sent from LoggerService when logs are fully copied.
     */
    EVENT_LOGS_SAVED,

    ACTION_RECORD_BOOT_TIME,
    DATA_BOOT_TIME_RECORDED_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (LoggerKeys s : LoggerKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    LoggerKeys(){
        this.DataDefault = null;
    }

    LoggerKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
