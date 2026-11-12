package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;
import com.jht.datamatrixlib.StringListDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum MainAppKeys {
    DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST(new StringListDataDefault()),
    // Flag to signal that a change has been made to the workout db and it needs to be copied over
    DATA_IS_SAFE_TO_UPDATE_BOOL(new DataDefault<>(false)),
    DATA_INSTALL_AFTER_WORKOUT_BOOL(new DataDefault<>(false)),

    // Prevents the first time setup from running until the console is rebooted.
    DATA_SYSTEM_BOOTED_BOOL(new DataDefault<>(false)),
    // Flag to signal that DAPI analytics needs to be updated with the machine id
    DATA_SPLASH_ANALYTICS_BOOL(new DataDefault<>(false)),


    /////  Athena  /////
    DATA_TT_RPM_TARGET_INT(new DataDefault<>(60)),
    DATA_TT_TARGET_BPM_INT(new DataDefault<>(0)),
    DATA_TT_BPM_ZONE_HIGH_INT(new DataDefault<>(0)),
    DATA_TT_BPM_ZONE_LOW_INT(new DataDefault<>(0)),
    DATA_TT_BPM_ZONE_SELECTED_INDEX_INT(new DataDefault<>(0)),
    DATA_TT_TARGET_DISTANCE_KM_FLOAT(new DataDefault<>(0f)),
    DATA_TT_TARGET_CALORIES_INT(new DataDefault<>(0)),
    DATA_TT_TARGET_TRAINING_TYPE_INT(new DataDefault<>(0)),
    DATA_TT_TIME_GOAL_TARGET_INT(new DataDefault<>(0)),
    DATA_TT_FTP_ZONE_HIGH_FLOAT(new DataDefault<>(125f)),
    DATA_TT_FTP_ZONE_LOW_FLOAT(new DataDefault<>(100f)),
    DATA_TT_FTP_ZONE_SELECTED_INDEX_INT(new DataDefault<>(3)),
    DATA_TT_FTP_ZONE_FITNESS_SELECTED_INDEX_INT(new DataDefault<>(3)),
    DATA_TT_RPM_ZONE_HIGH_FLOAT(new DataDefault<>(0f)),
    DATA_TT_RPM_ZONE_LOW_FLOAT(new DataDefault<>(0f)),
    DATA_WEB_BROWSER_CURRENT_PAGE_STRING(new DataDefault<>("")),
    DATA_RUNNING_INSTRUMENTATION_TEST_BOOL(new DataDefault<>(false)),
    DATA_RUNNING_INSTRUMENTATION_SPLASH_TEST_BOOL(new DataDefault<>(false)),

    ////  XareFit Integration  ////
    EVENT_XARE_FIT_LOGIN_SUCCESSFUL,
    ACTION_XARE_FIT_HIDE_QR_CODE,

    //// Login Fragment RFID Association Process ////
    DATA_ASSOCIATE_RFID_CHIP_PARCELABLE,

    // Serialized String containing a list of FacilityRequestType
    DATA_CONCIERGE_FACILITY_CONFIG_RESPONSE_SERIALIZED_STRING(new DataDefault<>("")),
    DATA_CONCIERGE_SERVICES_AVAILABLE_BOOL(new DataDefault<>(false)),
    ACTION_CREATE_CONCIERGE_REQUEST_WITH_OPTIONAL_REQUEST_ID_STRING(new DataDefault<>("")),
    ACTION_CANCEL_CONCIERGE_REQUEST,
    DATA_CONCIERGE_EVENT_PARCELABLE(new ParcelableDataDefault());

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (MainAppKeys s : MainAppKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    MainAppKeys(){
        this.DataDefault = null;
    }

    MainAppKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public String getDefaultValue() {
        return DataDefault.valueAsString();
    }

    public static class TT_DEFAULTS
    {
        public static final int TT_RPM_MIN_VALUE = 30;
        public static final int TT_RPM_MAX_VALUE = 150;
        public static final int TT_WATTS_MIN_VALUE = 50;
        public static final int TT_WATTS_DEFAULT_VALUE = 100;
        public static final int TT_WATTS_MAX_VALUE = 500;
        public static final float TT_DEFAULT_DISTANCE_KM = 1.60934f;
        public static final float TT_MIN_DISTANCE_KM = 1.60934f;
        public static final float TT_MAX_DISTANCE_KM = 50f;
        public static final int TT_DEFAULT_DISTANCE_TIME_MINUTES = 30;
        public static final int TT_MIN_TIME_MINUTES = 1;
        public static final int TT_MAX_TIME_MINUTES = 120;
        public static final int TT_CALORIES_MIN_VALUE = 10;
        public static final int TT_CALORIES_MAX_VALUE = 1000;
        public static final int TT_BPM_MIN_VALUE = 80;
        public static final int TT_BPM_DEFAULT_VALUE = 145;
        public static final int TT_BPM_MAX_VALUE = 210;

        // MAX Watts value is settable in settings
    }
}
