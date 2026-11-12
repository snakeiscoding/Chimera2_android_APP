package com.jht.chimera.lib.common.enums;

public class JHTTypes {

    public enum REPEAT_MODE {
        NONE, ONE, ALL
    }

    public enum MediaDeviceType {
        iOS, MTP, UMS, BT_SINK, UNKNOWN
    }

    public enum MessageIds {
        // NOTE: web message IDs can be 1+ so keep internal IDs negative
        Keyboard_Disabled(-1),
        RPM_Too_High(-2),
        No_HR_Detected(-3),
        IMPORTING_SETTINGS(-4),
        IMPORT_SETTINGS_SUCCESS(-5),
        IMPORT_SETTINGS_FAIL(-6),
        BLUETOOTH_AUTOPAIR_NOTIFICATION(-7),
        ERGOLINE_NOTIFICATION(-8),
        SPEED_RANGE_ERROR_STOP_WORKOUT(-9),
        SPEED_RANGE_ERROR_REDUCE_INCLINE(-10),
        OVER_SPEED_030C(-11),
        ; // END LIST
        private final int _id;
        MessageIds(int id) { _id = id; }
        public int getId() {
            return _id;
        }
    }

    public enum MeasurementType{
        MEAS_DISTANCE,
        MEAS_SPEED,
        MEAS_RESISTANCE,
        MEAS_HEART_RATE,
        MEAS_WATTS,
        MEAS_CALORIES,
        MEAS_ACTIVE_CALORIES,
        MEAS_FLOORS,
        MEAS_INCLINE,
        MEAS_PACE,
        MEAS_RPM,
        MEAS_SPM;
        int getValue() {
            return ordinal();
        }
    }

    public enum StatType{
        STAT_TYPE_TIME,
        STAT_TYPE_EFFORT,
        STAT_TYPE_SPEED,
        STAT_TYPE_INVALID;
        public int getValue() {
            return ordinal();
        }
    }
}
