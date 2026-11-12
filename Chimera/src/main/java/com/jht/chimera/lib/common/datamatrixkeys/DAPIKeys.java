package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.mastboard.data.LEDStyle;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.PersistentDataDefault;
import com.jhtp.dapiservice.um.GenderIndentity;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.List;

/**
 * Collection of keys used with the DAPI Service and User Information
 */
public enum DAPIKeys {
    /**
     * Logged in User settings
     */
    DATA_USER_FIRST_NAME_STRING(new DataDefault<>("")),
    DATA_USER_GENDER_ENUM_INT(new DataDefault<>(GenderIndentity.Male.Value)),
    DATA_USER_WEIGHT_KG_FLOAT(new DataDefault<>(68f)),
    DATA_USER_HEIGHT_METERS_FLOAT(new DataDefault<>(1.7f)), // at the moment only samsung watch provides this
    DATA_USER_AGE_INT(new DataDefault<>(30)),  // 30 is correct.  There is a dictionary that muddies this, but 30 is what is actually used.
    DATA_USER_DAPI_UUID_STRING(new DataDefault<>(DapiUser.GUEST_USER_UUID)),
    DATA_USER_DAPI_USER_XID_STRING(new DataDefault<>("")),
    DATA_USER_DAPI_USER_TOKEN_SESSION_STRING(new DataDefault<>(DapiUser.GUEST_USER_TOKEN)),
    DATA_USER_CREATE_WALLET_PASS_STRING(new DataDefault<>("")),

    DATA_JUMP_SESSION_PARCELABLE(), // JumpSession

    /**
     * Additional User Settings
     */
    //Extended Info
    DATA_EXTENDED_INFO_RECEIVED_BOOL(new DataDefault<>(false)),
    DATA_USER_CUSTOM_WORKOUT_JSON_STRING(new DataDefault<>("")),
    DATA_USER_EXTENDED_DATA_JSON_STRING(new DataDefault<>("")),
    DATA_PROGRESSION_PROGRAM_STRING(new DataDefault<>("")),
    // Bumped on every user-identity switch; lets a progression save sent before the switch be detected as stale and dropped.
    DATA_USER_SESSION_GENERATION_INT(new DataDefault<>(0)),

    //User settings (DAPI command 39)
    DATA_USER_BLE_HR_MAC_ID_STRING,
    DATA_USER_BLE_AUDIO_MAC_ID_STRING,
    DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING,

    //Hand Grips and LEDs.
    DATA_USER_LED_STYLE_INT(new DataDefault<>(LEDStyle.DEFAULT.value)),
    DATA_USER_HAPTICS_ON_BOOL(new DataDefault<>(true)),

    //Fitness Test Data.
    DATA_USER_FTP_INT(new DataDefault<>(100)),
    DATA_USER_MAXHR_INT(new DataDefault<>(-1)),  //-1 as this will only be set by incoming user data, otherwise, an age formula is used for MaxHR

    //Settings update keys
    ACTION_UPDATE_USER_FTP_INT,
    ACTION_UPDATE_USER_MAXHR_INT,

    //SSO Update keys
    ACTION_UPDATE_USER_WEIGHT_KG_FLOAT,
    ACTION_UPDATE_EXTENDED_USER_INFO_JSON_STRING,
    ACTION_UPDATE_CUSTOM_WORKOUT_JSON_STRING,
    ACTION_UPDATE_PROGRESSION_DATA_PARCELABLE, // carries ProgressionUpdate (json + generation stamp)

    /**
     * Integration User Settings
     */
    //Custom settings for EVOLT
    DATA_USER_EVOLT_SMM_FLOAT(new DataDefault<>(0f)),

    //Alternate RFID tag for US Government Integration
    DATA_NON_DAPI_USER_CHIP_UUID_STRING(new DataDefault<>("")),

    //SECA data
    //TODO: Refactor keys classes to house feature flags
    DATA_HAS_SECA_FEATURE_BOOL(new PersistentDataDefault<>(false)),
    DATA_SECA_USER_INFO_PARCELABLE(),

    DATA_MATRIX_METHOD_PROGRAM_ID_STRING(new DataDefault<>("")),

    /**
     * DAPI Service Keys
     */
    //DAPI Status
    DATA_CONNECTED_TO_DAPI_BOOL(new DataDefault<>(false)),
    DATA_DAPI_CONNECTION_LOST_BOOL(new DataDefault<>(false)), // This should get set to true when we lose connection.
    DATA_GETTING_USER_CUSTOM_WORKOUT_BOOL(new DataDefault<>(false)),
    DATA_GETTING_USER_TIERS_BOOL(new DataDefault<>(false)),
    DATA_GETTING_USER_INFO_BOOL(new DataDefault<>(false)),

    //DAPI Commands and Response data
    ACTION_VALIDATE_SETTINGS_STRING,
    EVENT_SETTINGS_VALID,
    EVENT_SETTINGS_INVALID,
    DATA_DAPI_MACHINE_UUID_STRING(new DataDefault<>("")),

    //Dapi Analytics keys
    ACTION_DAPI_ANALYTICS_STRING,
    EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING,
    EVENT_DAPI_ANALYTICS_APP_INT,
    EVENT_DAPI_WORKOUT_COMPLETE_ANALYTICS_PARCELABLE, // Passes information about the completed workout to analytics

    // Setup events.
    DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL(new DataDefault<>(false)),
    DATA_LOCK_CARDIO_INFO_BOOL(new DataDefault<>(false)),
    DATA_CARDIO_INFO_DAPI_RESULT_INT(new DataDefault<>(-1)), // Last cardioInfo DapiResult.Value, -1 = no reply yet

    //Test Keys
    ACTION_SET_DAPI_TEST_URL_STRING,
    ACTION_DO_FIRST_TIME_CONNECTED_TASKS_BOOL,
    DATA_DAPI_SERVICE_READY_BOOL(new DataDefault<>(false)), // Used for instrumentation tests.

    /**
     * Other backend keys
     */
    //Stop Auto BT Pairing for user device on login
    ACTION_STOP_BACKGROUND_BLUETOOTH_EVENT, // Used to stop scanning to automatically connect to bluetooth devices

    //Software Update
    DATA_AUTO_UPDATE_INTENT_SET_BOOL(new DataDefault<>(false)),
    DATA_IS_AUTO_UPDATE_READY_BOOL(new DataDefault<>(false)),

    //Sends console config to core
    ACTION_UPLOAD_CONSOLE_CONFIG,

    ; // END ENUM LIST

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (DAPIKeys s : DAPIKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    DAPIKeys(){
        this.DataDefault = null;
    }

    DAPIKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public static class Constants {
        public static final int DAPI_DEPLOYMENT_PROD = 3;
        public static final int DAPI_DEPLOYMENT_STAGING = 2;
        public static final int DAPI_DEPLOYMENT_QA = 1;
        public static final int DAPI_DEPLOYMENT_DEV = 0;
        public static final int DAPI_GUEST_INDEX = 0;

        /**
         * String constants for evolt user data
         */
        @NonNls public static final String TYPE_EVOL_SKELETAL_MUSCLE_MASS = "skeletal_muscle_mass_result";

        // String constants for user data
        @NonNls public static final String TYPE_USER_SETTING = "phoenix_user_settings";
        @NonNls public static final String TYPE_USER_CONNECTED_DEVICE_SETTING = "BluetoothMacAddress";  // Includes MAC addresses

        // String constants for reading JSON for getting user settings
        @NonNls public static final String SETTING_TYPE = "setting_type";
        @NonNls public static final String MAX_HR_USER_KEY = "max_hr";
        @NonNls public static final String FTP_USER_KEY = "ftp";

        @NonNls public static final String USER_BLE_HR_MAC_ADDRESS = "user_ble_hr_mac_address";
        @NonNls public static final String USER_BLE_AUDIO_MAC_ADDRESS = "user_ble_audio_mac_address";
        @NonNls public static final String USER_ANT_HR_DEVICE_ID = "user_ant_hr_device_id";
        @NonNls public static final String USER_HAPTICS_ON = "user_haptics_on";
        @NonNls public static final String USER_LCD_LAYOUT_STYLE = "user_lcd_layout_style";
        @NonNls public static final String USER_LED_STYLE = "user_led_style";
    }
}
