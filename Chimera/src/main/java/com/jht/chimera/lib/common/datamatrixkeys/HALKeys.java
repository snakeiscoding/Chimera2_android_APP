package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;
import com.jht.datamatrixlib.PersistentDataDefault;
import com.jht.gattserver2.WatchState;

import java.util.LinkedList;
import java.util.List;

public enum HALKeys {
    DATA_CONSOLE_SERVICES_INIT_BOOL(new DataDefault<>(false)),
    DATA_HAL_SERVICE_READY_BOOL(new DataDefault<>(false)),

    ACTION_SETUP_FRAME,
    ACTION_CONSOLE_REBOOT_STRING, // commands a reboot, can pass reason string

    DATA_UNREGISTER_NOTIFICATION_INT(new DataDefault<>(0)),

    DATA_LCB_TYPE_INT(new DataDefault<>(0)),
    DATA_FRAME_CONSOLE_COMBO_INVALID_BOOL(new DataDefault<>(false)),
    ACTION_SET_SPEED_KPH_FLOAT,
    DATA_TARGET_SPEED_KPH_FLOAT(new DataDefault<>(0f)),
    DATA_ACTUAL_SPEED_KPH_FLOAT(new DataDefault<>(0f)),
    ACTION_SET_SPEED_RPM_INT,
    ACTION_TM_REVERSE_BELT_BOOL,
    DATA_TM_IS_REVERSE_BELT_BOOL(new DataDefault<>(false)),
    ACTION_STOP_INCLINE,
    ACTION_SET_INCLINE_FLOAT,

    // Handles sending and receiving the 0x7e LCB command
    ACTION_SEND_TREADMILL_OPERATE_PARCELABLE,
    ACTION_RECEIVED_TREADMILL_OPERATE_PARCELABLE,

    // Handles receiving the 0x7f LCB cadence command
    EVENT_RECEIVED_TREADMILL_CADENCE_PARCELABLE,

    // Friction test commands
    ACTION_START_FRICTION_TEST_DATA_COLLECTION,
    ACTION_STOP_FRICTION_TEST_DATA_COLLECTION,
    ACTION_SET_FRICTION_TEST_TARGET_SPEED_KPH_FLOAT,
    ACTION_SET_FRICTION_TEST_TARGET_INCLINE_FLOAT,


    DATA_TARGET_INCLINE_FLOAT(new DataDefault<>(0f)),
    DATA_ACTUAL_INCLINE_FLOAT(new DataDefault<>(0f)),
    DATA_INCLINE_MOVING_BOOL(new DataDefault<>(false)),
    DATA_MAIN_MOTOR_STATUS_BOOL(new DataDefault<>(false)),
    DATA_RPM_ENCODER_STATUS_BOOL(new DataDefault<>(false)),
    ACTION_SET_RESISTANCE_INT,
    DATA_SHOW_LCB_ANALYTICS_BOOL(new DataDefault<>(false)),
    ACTION_AUTO_OVER_CURRENT_STATUS_BOOL,
    DATA_DC_BUS_CURRENT_OC_INT(new DataDefault<>(0)),
    DATA_DC_BUS_TRIGGER_LIMIT_UPPER_INT(new DataDefault<>(0)),
    DATA_DC_BUS_TRIGGER_LIMIT_LOWER_INT(new DataDefault<>(0)),
    DATA_TM_IS_SAFE_TO_START_BITMASK_INT(new DataDefault<>(0)),

    /**
     * This contains the last LCB error code that happened on the system. 0 if no error code.
     */
    DATA_LCB_ERROR_CODE_INT(new DataDefault<>(0)),

    ACTION_SIMULATE_LCB_ERROR_CODE_INT, // use to simulate LCB error code from adb shell

    /**
     * Listeners receive notification of which error codes need to be reported to Asset Management.
     */
    ACTION_REPORT_ERROR_CODE_INT,

    /**
     * This is the event that is published when any error code happens on the system; not just LCB
     * error codes.
     */
    ACTION_ERROR_CODE_INT,
    DATA_OC_CONTROL_TYPE_INT(new DataDefault<>(0)),
    // Sets the resistance of the frame to target the sent value of the watts
    ACTION_SET_TARGET_METS_FLOAT,
    DATA_RESISTANCE_INT(new DataDefault<>(0)),
    DATA_RPM_INT(new DataDefault<>(0)),
    DATA_SPM_INT(new DataDefault<>(0)),
    DATA_WATTS_FLOAT(new DataDefault<>(0f)),
    DATA_CALORIES_PER_SEC_FLOAT(new DataDefault<>(0f)),

    /**
     * Added EVolt calories per sec float for observing it from computeTreadmill value.
     * //TODO: Where is/should this be used?
     */
    DATA_EVOLT_CALORIES_PER_SEC_FLOAT(new DataDefault<>(0f)),
    DATA_METS_FLOAT(new DataDefault<>(0f)),
    DATA_HEART_RATE_INT(new DataDefault<>(0)),
    DATA_HEART_RATE_SOURCE_INT(new DataDefault<>(0)),
    DATA_DEMO_HEART_RATE_INT(new DataDefault<>(0)),
    ACTION_UPDATE_BLE_HEART_RATE_INT,
    ACTION_UPDATE_GYM_KIT_HEART_RATE_INT,
    ACTION_UPDATE_SAMSUNG_HEART_RATE_INT,
    DATA_DEMO_RPM_INT(new DataDefault<>(30)),
    DATA_SAFETY_KEY_STATUS_INT(new DataDefault<>(0)),
    DATA_PERSON_IN_PLACE_BOOL(new DataDefault<>(false)),
    DATA_IS_SAFE_TO_START_WORKOUT_BOOL(new DataDefault<>(true)),
    DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL(new DataDefault<>(true)),
    DATA_CLIMB_START_CONTROL_STATUS_BOOL(new DataDefault<>(false)),
    EVENT_GET_CLIMB_FRAME_CAL_SCALING,
    DATA_CLIMBMILL_FRAME_SCALE_INT(new DataDefault<>(1000)),

    // Athena Specific calibration data/actions
    DATA_SPINDOWN_TEST_STATUS_INT(new DataDefault<>(0)),
    ACTION_RAPTOR_PARA_BACKUP,
    ACTION_STOP_SPINDOWN_TEST,
    ACTION_START_SPINDOWN_TEST,
    ACTION_RAPTOR_PARA_RESTORE,
    ACTION_CLEAR_TORQUE,
    ACTION_GET_INFO_SPIN_DOWN_BOOL,
    DATA_INFO_SPIN_DOWN_RPM_INT(new DataDefault<>(0)),
    DATA_INFO_SPIN_DOWN_TORQUE_SCALAR_FLOAT(new DataDefault<>(0f)),
    DATA_INFO_SPIN_DOWN_CORRECTION_FACTOR_FLOAT(new DataDefault<>(0f)),
    DATA_INFO_SPIN_DOWN_INERTIA_FLOAT(new DataDefault<>(0f)),

    // Provide a generic means to request a raw ADC value (actual implementation is handler specific)
    ACTION_REQUEST_RAW_ADC_INT, // value passed can be used to pick from multiple sources of ADC, if there are more than one
    ACTION_RECEIVE_RAW_ADC_INT, // value passed is the raw ADC reading

    DATA_VERSION_CHIMERA_STRING(new DataDefault<>("N/A")),
    DATA_VERSION_OS_STRING(new DataDefault<>("N/A")),
    DATA_VERSION_LCB_STRING(new DataDefault<>("")),
    DATA_VERSION_IO_STRING(new DataDefault<>("N/A")),
    DATA_VERSION_NFC_STRING(new DataDefault<>("N/A")),
    DATA_RFID_MODULE_NAME_STRING(new PersistentDataDefault<>("")),
    DATA_VERSION_SALUTRON_STRING(new DataDefault<>("N/A")),
    DATA_VERSION_AUDIO_STRING(new DataDefault<>("N/A")),

    ACTION_IO_PLAY_BEEP,
    ACTION_ENTER_ERP,
    EVENT_LCB_POWERED_OFF,
    ACTION_IO_ENTER_ERP,
    ACTION_IO_LEAVE_ERP,
    ACTION_IO_UPDATE_PATH_STRING,
    ACTION_IO_SEND_IR_COMMAND_BYTES,
    DATA_IO_SET_IR_FREQUENCY_KHZ_INT(new DataDefault<>(0)),
    ACTION_IO_POWER_MONITOR,
    EVENT_IO_IR_RESULT_BOOL,
    DATA_IO_SET_FAN_SPEED_INT(new DataDefault<>(0)),
    DATA_IO_FAN_RPM_INT(new DataDefault<>(0)),
    DATA_IO_MOCK_CPU_TEMP_CELCIUS_INT(new DataDefault<>(0)),
    ACTION_IO_POWER_ON_DEVICE_ENUM_INT,
    DATA_IO_POWER_ON_DEVICE_ENUM_INT(new DataDefault<>(0)),
    ACTION_SET_TV_IO_POWER_STATE_BOOL,
    DATA_TV_IO_POWER_STATE_BOOL(new DataDefault<>(false)),
    DATA_IO_IS_SAFETY_PULLED_BOOL(new DataDefault<>(false)),
    DATA_IO_EXTERNAL_POWER_BOOL(new PersistentDataDefault<>(false)),
    DATA_IO_DISABLE_WATCHDOG_REFRESH_BOOL(new DataDefault<>(false)), // key will be updated via adb command line
    DATA_CONSOLE_VOLTAGE_INT(new DataDefault<>(0)),
    DATA_EXTEND_VOLTAGE_INT(new DataDefault<>(0)),
    DATA_USB_VOLTAGE_INT(new DataDefault<>(0)),
    DATA_CONSOLE_CURRENT_INT(new DataDefault<>(0)),
    DATA_EXTEND_CURRENT_INT(new DataDefault<>(0)),
    DATA_EXTEND_ERROR_BOOL(new DataDefault<>(false)),
    ACTION_START_RFID_MANAGER_BOOL(new DataDefault<>(false)),

    DATA_RFID_MANAGER_SERIAL_PORT_STRING(new DataDefault<>(null)),
    DATA_BT_SINK_PORT_STRING(new DataDefault<>(null)),

    ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT,
    DATA_IOS_AUDIO_SAMPLE_RATE_INT(new DataDefault<>(0)),
    DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT(new DataDefault<>(0)),
    DATA_SYSTEM_VOLUME_INT(new DataDefault<>(0)),
    DATA_SYSTEM_VOLUME_LIMIT_RATIO_FLOAT(new DataDefault<>(0f)),
    DATA_SYSTEM_VOLUME_MUTE_BOOL(new DataDefault<>(false)),
    DATA_IS_HEADPHONE_CONNECTED_BOOL(new DataDefault<>(false)),


    ACTION_START_MACHINE,
    ACTION_STOP_MACHINE,
    ACTION_CALIBRATE_BOOL,
    ACTION_SHOW_CZ_IR_FREQ_BOOL,
    DATA_CALIBRATION_STATUS_INT(new DataDefault<>(0)),
    EVENT_GET_MAGNENT_ANGLE,
    EVENT_AUTO_TUNE_MAGNENT_ANGLE,
    EVENT_GET_MOTOR_TYPE,
    DATA_GET_MOTOR_TYPE_INT(new DataDefault<>(0)),
    DATA_GET_MOTOR_VOLTAGE_INT(new DataDefault<>(0)),

    EVENT_KEYPRESS_BITMAP_INT,
    EVENT_SCREEN_TOUCH,
    DATA_HOMEKEY_ENABLE_BOOL(new DataDefault<>(false)),

    EVENT_RFID_TAG_DETECTED_PARCELABLE,
    EVENT_APPLE_WATCH_DETECTED_PARCELABLE,
    EVENT_SAMSUNG_WATCH_DETECTED_PARCELABLE,
    EVENT_PF_BARCODE_DETECTED_PARCELABLE,
    EVENT_WALLET_PASS_DETECTED_PARCELABLE,
    DATA_SIMULATED_ESTOP_BOOL(new DataDefault<>(false)),
    DATA_IS_BT_SINK_CONNECTED_BOOL(new DataDefault<>(false)),
    DATA_IS_USING_BLE_BROWSING_BOOL(new DataDefault<>(false)),

    DATA_CAN_HEART_RATE_INT(new DataDefault<>(0)),
    DATA_GYM_KIT_FIRMWARE_VERSION_STRING(new DataDefault<>("N/A")),
    DATA_GYM_KIT_CALORIES_ACTIVE_INT(new DataDefault<>(0)),
    DATA_GYM_KIT_CALORIES_TOTAL_INT(new DataDefault<>(0)),
    DATA_GYM_KIT_AVE_HEART_RATE_INT(new DataDefault<>(0)),
    DATA_GYM_KIT_STATUS_INT(new DataDefault<>(WatchState.Disconnected.ordinal())),

    DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT(new DataDefault<>(WatchState.Disconnected.ordinal())),
    ACTION_DISCONNECT_SMART_WATCHES,
    ACTION_BLOCK_SMART_WATCHES_BOOL,
    ACTION_RFID_OPERATING_MODE_INT, // see RfidOperatingMode ordinals
    DATA_RFID_FINDER_FIRST_TRY_COMPLETE_BOOL(new DataDefault<>(false)),

    ACTION_ADD_BT_HEADPHONE_STRING,
    ACTION_REMOVE_BT_HEADPHONE_STRING,
    ACTION_CONNECT_BLE_PARCELABLE,
    ACTION_DISCONNECT_BLE_PARCELABLE,
    EVENT_BLE_CONNECTED_PARCELABLE,
    EVENT_BLE_CONNECTION_FAILED_PARCELABLE,
    DATA_MY_ZONE_CONNECTED_BOOL(new DataDefault<>(false)),

    DATA_FOLDER_PATH_COUNTER_INT(new DataDefault<>(0)),
    EVENT_GET_FOLDER_ITEMS,
    ACTION_CHANGE_PATH_COMMAND_PARCELABLE,
    ACTION_SET_CURRENT_PLAYER_APP_SETTING_VALUE_PARCELABLE,
    ACTION_PLAY_ITEM_COMMAND_STRING,
    ACTION_SEND_BT_SINK_COMMAND_INT,
    ACTION_SEND_BT_DEVICE_COMMAND_PARCELABLE,
    ACTION_START_BT_SINK_SCAN_INT,
    DATA_BT_SINK_CONNECTED_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING(new DataDefault<>("")),
    DATA_BT_SINK_CONNECTED_DEVICE_BYTES(new DataDefault<>(new byte[0])),
    EVENT_BT_SINK_DEVICE_FOUND_PARCELABLE,
    EVENT_MACHINE_SETUP_COMPLETE,
    DATA_BLE_HEART_RATE_ADDRESS_STRING(new DataDefault<>("")),

    ACTION_START_SALUTRON_SCAN,
    DATA_SALUTRON_CONNECTED_DEVICE_BYTES(new DataDefault<>(new byte[0])),
    DATA_SALUTRON_CONNECTED_BOOL(new DataDefault<>(false)),
    EVENT_SALUTRON_DEVICE_CONNECTION_FAIL,
    EVENT_SALUTRON_DEVICE_FOUND_PARCELABLE,
    EVENT_SALUTRON_DEVICE_CONNECT_PARCELABLE,
    EVENT_SALUTRON_DEVICE_DISCONNECT_PARCELABLE,

    // Used to send/receive a Bundle with "device" string and "payload" byte array via IO mcu comms passthrough
    ACTION_SEND_DATA_TO_IO_PASSTHROUGH_PARCELABLE,
    EVENT_RECEIVE_DATA_FROM_IO_PASSTHROUGH_PARCELABLE,

    ACTION_SPLASH_RESET,
    DATA_IS_USB_CONNECTED_BOOL(new DataDefault<>(false)),

    ACTION_SHUTDOWN_SYSTEM,
    EVENT_TV_FIRMWARE_UPDATING,
    EVENT_TV_OPERATION_START,
    EVENT_TV_OPERATION_DONE,

    DATA_MAX_WORKOUT_SPEED_KPH_FLOAT(new DataDefault<>(-1f)), // The max speed for the kph setting, in kph.  Used for over current protection.
    DATA_MAX_WORKOUT_SPEED_MPH_FLOAT(new DataDefault<>(-1f)), // The max speed for the mph setting, in mph.  Used for over current protection.
    DATA_PERFORMANCE_PLUS_TM_PFC_MODE_BOOL(new DataDefault<>(false)),
    DATA_FLOAT_STAT_PANEL_INFO_STRING(new DataDefault<>("")),
    ACTION_UPDATE_LCB_SERIAL_NUMBERS,
    DATA_SCAN_RFID_TAGS_BOOL(new DataDefault<>(false)),
    DATA_SCAN_WATCHES_BOOL(new DataDefault<>(true)),

    DATA_REQUIRES_SAFETY_RESET_BOOL(new DataDefault<>(false)), // Not Currently Used
    ACTION_PWM_CAL_TABLE,
    DATA_PWM_CAL_POINTS_STRING(new DataDefault<>("")),

    DATA_ATHENA_MONITOR_SENSOR_DATA_BOOL(new DataDefault<>(false)),
    DATA_ATHENA_SENSOR_DATA_STRING(new DataDefault<>("---")),

    EVENT_HAL_HEARTBEAT_CHECK,
    EVENT_HAL_HEARTBEAT_RESPONSE,

    DATA_TM548_CONTROL_STATE_INT,
    EVENT_TM548_MES_ECB_RUN_RELEASE,
    EVENT_TM548_MES_ECB_RUN_BRAKE,

    DATA_FLEET_CLAIM_CERT_STRING(new DataDefault<>("")),
    DATA_FLEET_CLAIM_KEY_STRING(new DataDefault<>("")),
    DATA_FLEET_PUBLIC_KEY_BASE64_STRING(new DataDefault<>("")),
    DATA_FLEET_SIGNATURE_BASE64_STRING(new DataDefault<>("")),
    ACTION_FLEET_PUSH_APPLE_MSG_BASE64_STRING,
    ACTION_FLEET_PUSH_GOOGLE_MSG_BASE64_STRING,

    DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL(new DataDefault<>(false)),
    DATA_USE_MYZONE_SIMULATOR_BOOL(new DataDefault<>(false)),
    DATA_MYZONE_CALORIES_INT(new DataDefault<>(0)),
    DATA_MYZONE_MEPS_INT(new DataDefault<>(0)),
    DATA_MYZONE_EFFORT_INT(new DataDefault<>(0)),
    DATA_MYZONE_TIME_IN_ZONES_PARCELABLE(new ParcelableDataDefault()),
    DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL(new DataDefault<>(false)),
    DATA_MYZONE_CALCULATED_AVG_MAX_HR_INT (new DataDefault<>(-1));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (HALKeys s : HALKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    HALKeys(){
        this.DataDefault = null;
    }

    HALKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public static final int STATUS_GYM_KIT_CONNECTED = WatchState.Connected.ordinal();
    public static final int STATUS_GYM_KIT_DISCONNECTED = WatchState.Disconnected.ordinal();
    public static final int STATUS_GYM_KIT_OUT_OF_RANGE = WatchState.TemporarilyDisconnected.ordinal();
}
