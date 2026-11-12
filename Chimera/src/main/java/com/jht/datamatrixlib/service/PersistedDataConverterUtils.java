package com.jht.datamatrixlib.service;

import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistedDataConverterUtils {
    private final String TAG = "PersistedDataConvert.";

    public static final String SHARED_PREF_FILE_NAME = "SystemSettings";
    public static final String VERSION_KEY = "PERSISTED_DATA_VERSION";
    /* Version to write into persisted data file to track what changes may have already been applied.
        1.0.0.1     Updated for 1.9.4.2 hotfix CGC-21383
        1.0.0.2     CGC-25801: touch should use basic theme again
     */
    public static final String CURRENT_VERSION = "1.0.0.2";

    private static PersistedDataConverterUtils instance;
    private final Map<String, PersistedKeyData> persistedSettingKeyThesaurus;

    private PersistedDataConverterUtils() {
        //private class to prevent instantiation

        persistedSettingKeyThesaurus = new HashMap<>();
        populateObsoleteKeyNames();
    }

    public static PersistedDataConverterUtils getInstance() {
        if (instance == null) {
            instance = new PersistedDataConverterUtils();
        }

        return instance;
    }

    public Map<String, PersistedKeyData> getPersistedSettingKeyThesaurus() {
        return persistedSettingKeyThesaurus;
    }

    private void populateObsoleteKeyNames() {
        addGeneralSettingKeys();
        addWorkoutSettingKeys();
        addUpdateSettingKeys();
        addAssetManagementSettingKeys();
        addWeatherSettingKeys();
        addSetupDataDefaultKeys();
        addApplicationDataDefaultKeys();
        addCalibrationDataDefaultKeys();
        addHardwareDataDefaultKeys();
        addDebugDataDefaultKeys();
        addTVDataDefaultKeys();
        addExperimentalDataDefaultKeys();
        addProjectOneDataDefaultKeys();
    }

    private void addGeneralSettingKeys() {
        for (SettingKeys.General currentGeneralSetting : SettingKeys.General.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentGeneralSetting) {
                case DATA_SCREEN_TIMEOUT_SEC_INT:
                    obsoleteKeyNames.add("SCREEN_TIMEOUT_SEC_INT");
                    break;
                case DATA_IANA_TIME_ZONE_STRING:
                    obsoleteKeyNames.add("IANA_TIME_ZONE_STRING");
                    break;
                case DATA_TIME_ZONE_INDEX_INT:
                    obsoleteKeyNames.add("TIME_ZONE_INDEX");
                    break;
                case DATA_DATE_FORMAT_ENUM_INT:
                    obsoleteKeyNames.add("DATE_FORMAT_ENUM");
                    break;
                case DATA_CLOCK_FORMAT_24_HOURS_BOOL:
                    obsoleteKeyNames.add("CLOCK_FORMAT_24_HOURS_BOOL");
                    break;
                case DATA_CONSOLE_LANGUAGE_ENUM_INT:
                    obsoleteKeyNames.add("CONSOLE_LANGUAGE_ENUM");
                    break;
                case DATA_REBOOT_INACTIVE_TIME_HOUR_INT:
                    obsoleteKeyNames.add("REBOOT_INACTIVE_TIME_HOUR_INT");
                    break;
                case DATA_DAPI_ENVIRONMENT_ENUM_INT:
                    obsoleteKeyNames.add("DAPI_ENVIRONMENT_ENUM");
                    break;
                case DATA_ACCUMULATED_WORKOUTS_INT:
                    obsoleteKeyNames.add("ACCUMULATED_WORKOUTS_INT");
                    break;
                case DATA_ACCUMULATED_TIME_SECONDS_INT:
                    obsoleteKeyNames.add("ACCUMULATED_TIME_SECONDS_INT");
                    break;
                case DATA_ACCUMULATED_DISTANCE_KM_FLOAT:
                    obsoleteKeyNames.add("ACCUMULATED_DISTANCE_KM_FLOAT");
                    break;
                case DATA_ACCUMULATED_FLOORS_FLOAT:
                    obsoleteKeyNames.add("ACCUMULATED_FLOORS_FLOAT");
                    break;
                case DATA_FRAME_TYPE_STRING:
                    obsoleteKeyNames.add("FRAME_TYPE_STRING");
                    break;
                case DATA_LCB_TYPE_INT:
                    obsoleteKeyNames.add("LCB_TYPE_INT");
                    break;
                case DATA_SHOW_OUT_OF_ORDER_BOOL:
                    obsoleteKeyNames.add("SHOW_OUT_OF_ORDER_BOOL");
                    break;
                case DATA_AUTO_DATE_TIME_BOOL:
                    obsoleteKeyNames.add("AUTO_DATE_TIME_BOOL");
                    break;
                case DATA_SELECTED_LANGUAGE_ENUM_INT:
                    obsoleteKeyNames.add("SELECTED_LANGUAGE_ENUM");
                    break;
                case DATA_IS_GUEST_USER_BOOL:
                    obsoleteKeyNames.add("IS_GUEST_USER_BOOL");
                    break;
                case DATA_IS_PTP_USER_BOOL:
                    obsoleteKeyNames.add("IS_PTP_USER_BOOL");
                    break;
                case DATA_DYNAMIC_THEME_URI_STRING:
                    // CGC-23331 renamed this key
                    obsoleteKeyNames.add("DATA_THEME_URI_STRING");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.General value, " + currentGeneralSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentGeneralSetting.toString(),
                        new PersistedKeyData(currentGeneralSetting.DataDefault, obsoleteKeyNames));

            }
        }
    }

    private void addWorkoutSettingKeys() {
        for (SettingKeys.Workout currentWorkoutSetting : SettingKeys.Workout.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentWorkoutSetting) {
                case DATA_KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT:
                    obsoleteKeyNames.add("KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT");
                    break;
                case DATA_MAX_WORKOUT_TIME_MIN_INT:
                    obsoleteKeyNames.add("MAX_WORKOUT_TIME_MIN_INT");
                    break;
                case DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT:
                    obsoleteKeyNames.add("MAX_WORKOUT_DISTANCE_KM_FLOAT");
                    break;
                case DATA_MAX_WORKOUT_CALORIES_INT:
                    obsoleteKeyNames.add("MAX_WORKOUT_CALORIES_INT");
                    break;
                case DATA_MAX_WORKOUT_FLOORS_INT:
                    obsoleteKeyNames.add("MAX_WORKOUT_FLOORS_INT");
                    break;
                case DATA_MAX_CLIMBMILL_LEVEL_INT:
                    obsoleteKeyNames.add("MAX_CLIMBMILL_LEVEL_INT");
                    break;
                case DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT:
                    obsoleteKeyNames.add("MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT");
                    break;
                case DATA_PAUSE_TIME_MIN_INT:
                    obsoleteKeyNames.add("PAUSE_TIME_MIN_INT");
                    break;
                case DATA_START_SPEED_KPH_FLOAT:
                    obsoleteKeyNames.add("START_SPEED_KPH_FLOAT");
                    break;
                case DATA_TREAD_SENSE_ENABLED_BOOL:
                    obsoleteKeyNames.add("TREAD_SENSE_ENABLED_BOOL");
                    break;
                case DATA_TREAD_SENSE_TIMEOUT_SEC_INT:
                    obsoleteKeyNames.add("TREAD_SENSE_TIMEOUT_SEC");
                    break;
                case DATA_TREAD_SENSE_COUNTDOWN_SEC_INT:
                    obsoleteKeyNames.add("TREAD_SENSE_COUNTDOWN_SEC");
                    break;
                case DATA_ELEVATION_ABOVE_5000_FT_BOOL:
                    obsoleteKeyNames.add("ELEVATION_ABOVE_5000_FT_BOOL");
                    break;
                case DATA_CM_ESTOP_ENABLE_BOOL:
                    obsoleteKeyNames.add("CM_ESTOP_ENABLE_BOOL");
                    break;
                case DATA_CM_DETECT_PERSON_BOOL:
                    obsoleteKeyNames.add("CM_DETECT_PERSON_BOOL");
                    break;
                case DATA_COOLDOWN_TIME_INT:
                    obsoleteKeyNames.add("COOLDOWN_TIME_INT");
                    break;
                case DATA_MAX_COOLDOWN_TIME_INT:
                    obsoleteKeyNames.add("MAX_COOLDOWN_TIME_INT");
                    break;
                case DATA_MIGROS_ENABLED_BOOL:
                    obsoleteKeyNames.add("MIGROS_ENABLED_BOOL");
                    break;
                case DATA_MAX_SPEED_KPH_FLOAT:
                    obsoleteKeyNames.add("MAX_SPEED_KPH_FLOAT");
                    break;
                case DATA_MAX_SPEED_MPH_FLOAT:
                    obsoleteKeyNames.add("MAX_SPEED_MPH_FLOAT");
                    break;
                case DATA_MAX_INCLINE_FLOAT:
                    obsoleteKeyNames.add("MAX_INCLINE_FLOAT");
                    break;
                case DATA_SHOW_MARATHON_MODE_BOOL:
                    obsoleteKeyNames.add("SHOW_MARATHON_MODE_BOOL");
                    break;
                case DATA_IFIT_ENABLED_DEFAULT_ON_BOOL:
                    obsoleteKeyNames.add("IFIT_ENABLED_DEFAULT_ON_BOOL");
                    break;
                case DATA_IFIT_TREADMILL_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_TREADMILL_ENABLED_BOOL");
                    break;
                case DATA_IFIT_ELLIPTICAL_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_ELLIPTICAL_ENABLED_BOOL");
                    break;
                case DATA_IFIT_ASCENT_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_ASCENT_ENABLED_BOOL");
                    break;
                case DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL");
                    break;
                case DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_RECUMBENT_BIKE_ENABLED_BOOL");
                    break;
                case DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_HYBRID_BIKE_ENABLED_BOOL");
                    break;
                case DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_UPRIGHT_BIKE_ENABLED_BOOL");
                    break;
                case DATA_IFIT_STEPPER_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_STEPPER_ENABLED_BOOL");
                    break;
                case DATA_IFIT_CLIMBMILL_ENABLED_BOOL:
                    obsoleteKeyNames.add("IFIT_CLIMBMILL_ENABLED_BOOL");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Workout value, " + currentWorkoutSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentWorkoutSetting.toString(),
                        new PersistedKeyData(currentWorkoutSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addUpdateSettingKeys() {
        for (SettingKeys.Update currentUpdateSetting : SettingKeys.Update.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentUpdateSetting) {
                case DATA_AUTO_UPDATE_HOUR_INT:
                    obsoleteKeyNames.add("AUTO_UPDATE_HOUR_INT");
                    break;
                case DATA_AUTO_UPDATE_BOOL:
                    obsoleteKeyNames.add("AUTO_UPDATE_BOOL");
                    break;
                case DATA_AUTO_UPDATE_DLG_SHOWN_BOOL:
                    obsoleteKeyNames.add("AUTO_UPDATE_DLG_SHOWN_BOOL");
                    break;
                case DATA_LAST_CHECK_TIME_LONG:
                    obsoleteKeyNames.add("LAST_CHECK_TIME_LONG");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Update value, " + currentUpdateSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentUpdateSetting.toString(),
                        new PersistedKeyData(currentUpdateSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addAssetManagementSettingKeys() {
        for (SettingKeys.AssetManagement currentAssetManagementSetting : SettingKeys.AssetManagement.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch (currentAssetManagementSetting) {
                case DATA_USE_ASSET_MANAGEMENT_BOOL:
                    obsoleteKeyNames.add("USE_ASSET_MANAGEMENT_BOOL");
                    break;
                case DATA_USE_WORKOUT_TRACKING_BOOL:
                    obsoleteKeyNames.add("USE_WORKOUT_TRACKING_BOOL");
                    break;
                case DATA_CUSTOM_SKIN_VERSION_STRING:
                    obsoleteKeyNames.add("CUSTOM_SKIN_VERSION_STRING");
                    break;
                case DATA_CLUB_ID_INT:
                    obsoleteKeyNames.add("CLUB_ID_INT");
                    break;
                case DATA_ENABLE_PHYSICAL_GO_BTN_BOOL:
                    obsoleteKeyNames.add("ENABLE_PHYSICAL_GO_BTN_BOOL");
                    break;
                case DATA_ENABLE_XID_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_XID_LOGIN_BOOL");
                    break;
                case DATA_ENABLE_BARCODE_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_BARCODE_LOGIN_BOOL");
                    break;
                case DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_PHONE_NUMBER_LOGIN_BOOL");
                    break;
                case DATA_ENABLE_EMAIL_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_EMAIL_LOGIN_BOOL");
                    break;
                case DATA_ENABLE_FITLINXX_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_FITLINXX_LOGIN_BOOL");
                    break;
                case DATA_ENABLE_CUSTOM_LOGIN_BOOL:
                    obsoleteKeyNames.add("ENABLE_CUSTOM_LOGIN_BOOL");
                    break;
                case DATA_CUSTOM_LOGIN_NAME_STRING:
                    obsoleteKeyNames.add("CUSTOM_LOGIN_NAME_STRING");
                    break;
                case DATA_CUSTOM_LOGIN_LABEL_STRING:
                    obsoleteKeyNames.add("CUSTOM_LOGIN_LABEL_STRING");
                    break;
                case DATA_CUSTOM_LOGIN_INPUT_TYPE_INT:
                    obsoleteKeyNames.add("CUSTOM_LOGIN_INPUT_TYPE_INT");
                    break;
                case DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING:
                    obsoleteKeyNames.add("CUSTOM_LOGIN_PASSWORD_LABEL_STRING");
                    break;
                case DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT:
                    obsoleteKeyNames.add("CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT");
                    break;
                case DATA_USE_CUSTOM_LOGO_BOOL:
                    obsoleteKeyNames.add("USE_CUSTOM_LOGO_BOOL");
                    break;
                case DATA_USE_CUSTOM_BACKGROUND_BOOL:
                    obsoleteKeyNames.add("USE_CUSTOM_BACKGROUND_BOOL");
                    break;
                case DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL:
                    obsoleteKeyNames.add("USE_CUSTOM_BACKGROUND_COLOR_BOOL");
                    break;
                case DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL:
                    obsoleteKeyNames.add("USE_CUSTOM_WELCOME_SCREEN_BOOL");
                    break;
                case DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT:
                    obsoleteKeyNames.add("WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT");
                    break;
                case DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT:
                    obsoleteKeyNames.add("WELCOME_SCREEN_ICON_POSITION_ENUM");
                    break;
                case DATA_ALLOW_GUEST_BOOL:
                    obsoleteKeyNames.add("ALLOW_GUEST_BOOLEAN");
                    break;
                case DATA_SHOW_WELCOME_SCREEN_BOOL:
                    obsoleteKeyNames.add("SHOW_WELCOME_SCREEN_BOOLEAN");
                    break;
                case DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT:
                    obsoleteKeyNames.add("WELCOME_SCREEN_SLIDESHOW_SEC_INT");
                    break;
                case DATA_ENABLE_CLUB_COM_BOOL:
                    obsoleteKeyNames.add("ENABLE_CLUB_COM_BOOL");
                    break;
                case DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT:
                    obsoleteKeyNames.add("WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM");
                    break;
                case DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT:
                    obsoleteKeyNames.add("WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT");
                    break;
                case DATA_CONSOLE_SERIAL_STRING:
                    obsoleteKeyNames.add("CONSOLE_SERIAL_STRING");
                    break;
                case DATA_FRAME_SERIAL_STRING:
                    obsoleteKeyNames.add("FRAME_SERIAL_STRING");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.AssetManagement value, " + currentAssetManagementSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentAssetManagementSetting.toString(),
                        new PersistedKeyData(currentAssetManagementSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addWeatherSettingKeys() {
        for (SettingKeys.Weather currentWeatherSetting : SettingKeys.Weather.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentWeatherSetting) {
                case DATA_WEATHER_LONGITUDE_FLOAT:
                    obsoleteKeyNames.add("WEATHER_LONGITUDE_FLOAT");
                    break;
                case DATA_WEATHER_LATITUDE_FLOAT:
                    obsoleteKeyNames.add("WEATHER_LATITUDE_FLOAT");
                    break;
                case DATA_WEATHER_CITY_STRING:
                    obsoleteKeyNames.add("WEATHER_CITY_STRING");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Weather value, " + currentWeatherSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentWeatherSetting.toString(),
                        new PersistedKeyData(currentWeatherSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addSetupDataDefaultKeys() {
        for (SettingKeys.SetupDefault currentSetupDataDefault : SettingKeys.SetupDefault.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch (currentSetupDataDefault) {
                case DATA_DEFAULT_LEVEL_INT:
                    obsoleteKeyNames.add("DEFAULT_LEVEL_INT");
                    break;
                case DATA_DEFAULT_AGE_INT:
                    obsoleteKeyNames.add("DEFAULT_AGE_INT");
                    break;
                case DATA_DEFAULT_WEIGHT_KG_FLOAT:
                    obsoleteKeyNames.add("DEFAULT_WEIGHT_KG_FLOAT");
                    break;
                case DATA_DEFAULT_HEIGHT_CM_FLOAT:
                    obsoleteKeyNames.add("DEFAULT_HEIGHT_CM_FLOAT");
                    break;
//                case DATA_DEFAULT_GENDER_IS_MALE_BOOL:
//                    obsoleteKeyNames.add("DEFAULT_GENDER_IS_MALE_BOOL");
//                    break;
                case DATA_DEFAULT_WORKOUT_TIME_MIN_INT:
                    obsoleteKeyNames.add("DEFAULT_WORKOUT_TIME_MIN_INT");
                    break;
                case DATA_DEFAULT_WORKOUT_CALORIES_INT:
                    obsoleteKeyNames.add("DEFAULT_WORKOUT_CALORIES_INT");
                    break;
                case DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT:
                    obsoleteKeyNames.add("DEFAULT_WORKOUT_DISTANCE_KM_FLOAT");
                    break;
                case DATA_DEFAULT_WORKOUT_FLOORS_INT:
                    obsoleteKeyNames.add("DEFAULT_WORKOUT_FLOORS_INT");
                    break;
                case DATA_DEFAULT_WEBPAGE_STRING:
                    obsoleteKeyNames.add("DEFAULT_WEBPAGE_STRING");
                    break;
                case DATA_DEFAULT_USE_BAIDU_BOOL:
                    obsoleteKeyNames.add("DEFAULT_USE_BAIDU_BOOL");
                    break;
                case DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL:
                    obsoleteKeyNames.add("DEFAULT_POST_WORKOUT_SURVEY_BOOL");
                    break;
                case DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING:
                    obsoleteKeyNames.add("DEFAULT_POST_WORKOUT_SURVEY_URL_STRING");
                    break;
                case DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING:
                    obsoleteKeyNames.add("DEFAULT_MID_WORKOUT_SURVEY_URL_STRING");
                    break;
                case DATA_DEFAULT_UNIT_IS_METRIC_BOOL:
                    obsoleteKeyNames.add("DEFAULT_UNIT_IS_METRIC_BOOL");
                    break;
                default:
                    Log.e(TAG, "The SettingKeys.SetupDefault value, " + currentSetupDataDefault + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentSetupDataDefault.toString(),
                        new PersistedKeyData(currentSetupDataDefault.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addApplicationDataDefaultKeys() {
        for (SettingKeys.Application currentApplicationSetting : SettingKeys.Application.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentApplicationSetting) {
                case DATA_APP_ORDER_STRING:
                    obsoleteKeyNames.add("APP_ORDER_STRING");
                    break;
                case DATA_APP_INACTIVE_LIST_STRING:
                    obsoleteKeyNames.add("APP_INACTIVE_LIST_STRING");
                    break;
                case DATA_WEATHER_APP_ENUM_INT:
                    obsoleteKeyNames.add("WEATHER_APP_ENUM");
                    break;
                case DATA_USER_PROFILE_SELECTED_SLOT_INT:
                    // Fall through - this key is correctly named and does not require conversion
                    break;
                case DATA_VERSION_RAPTOR_PARAMETER_STRING:
                    obsoleteKeyNames.add("DATA_VERSION_RAPTOR_PARAMETER");
                    break;
                case DATA_HIDE_WEB_AND_SURVEY_APPS_BOOL:
                    obsoleteKeyNames.add("HIDE_WEB_AND_SURVEY_APPS");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Application value, " + currentApplicationSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentApplicationSetting.toString(),
                        new PersistedKeyData(currentApplicationSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addCalibrationDataDefaultKeys() {
        for (SettingKeys.Calibration currentCalibrationSetting : SettingKeys.Calibration.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentCalibrationSetting) {
                case DATA_TREADMILL_INLINE_ADC_MAX_INT:
                    obsoleteKeyNames.add("TREADMILL_INLINE_ADC_MAX_INT");
                    break;
                case DATA_TREADMILL_INLINE_ADC_MIN_INT:
                    obsoleteKeyNames.add("TREADMILL_INLINE_ADC_MIN_INT");
                    break;
                case DATA_CLIMBMILL_USE_LCB_WATTS_BOOL:
                    obsoleteKeyNames.add("CLIMBMILL_USE_LCB_WATTS_BOOL");
                    break;
                case DATA_CLIMBMILL_CZ_FREQUENCY_INT:
                    obsoleteKeyNames.add("CLIMBMILL_CZ_FREQUENCY_INT");
                    break;
                case DATA_CLIMBMILL_IR_FREQUENCY_INT:
                    obsoleteKeyNames.add("CLIMBMILL_IR_FREQUENCY_INT");
                    break;
                case DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT:
                    obsoleteKeyNames.add("CLIMBMILL_CAL_WEIGHT_KG_FLOAT");
                    break;
                case DATA_CLIMBMILL_CAL_SPM_INT:
                    obsoleteKeyNames.add("CLIMBMILL_CAL_SPM_INT");
                    break;
                case DATA_CLIMBMILL_CAL_TIME_SEC_INT:
                    obsoleteKeyNames.add("CLIMBMILL_CAL_TIME_SEC_INT");
                    break;
                case DATA_CLIMBMILL_CAL_FACTOR_INT:
                    obsoleteKeyNames.add("CLIMBMILL_CAL_FACTOR_INT");
                    break;
                case DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT:
                    obsoleteKeyNames.add("TREADMILL_INCLINE_ADC_OFFSET_MIN_INT");
                    break;
                case DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT:
                    obsoleteKeyNames.add("TREADMILL_INCLINE_ADC_OFFSET_MAX_INT");
                    break;
                case DATA_SETTING_RAPTOR_PARA_DATA_STRING:
                    obsoleteKeyNames.add("SETTING_RAPTOR_PARA_DATA_STRING");
                    break;
                case DATA_TREADMILL_MAGNET_ANGLE_INT:
                    obsoleteKeyNames.add("TREADMILL_MAGNET_ANGLE_INT");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Calibration value, " + currentCalibrationSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentCalibrationSetting.toString(),
                        new PersistedKeyData(currentCalibrationSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addHardwareDataDefaultKeys() {
        for (SettingKeys.Hardware currentHardwareSetting : SettingKeys.Hardware.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentHardwareSetting) {
                case DATA_ENERGY_SAVER_MODE_ENUM_INT:
                    obsoleteKeyNames.add("ENERGY_SAVER_MODE_ENUM");
                    break;
                case DATA_ENERGY_SAVER_TIMEOUT_MIN_INT:
                    obsoleteKeyNames.add("ENERGY_SAVER_TIMEOUT_MIN_INT");
                    break;
                case DATA_ENERGY_SAVER_TIME_MIN_INT:
                    obsoleteKeyNames.add("ENERGY_SAVER_TIME_MIN_INT");
                    break;
                case DATA_KEY_BEEP_BOOL:
                    obsoleteKeyNames.add("KEY_BEEP_BOOL");
                    break;
                case DATA_COUNTDOWN_BEEP_BOOL:
                    obsoleteKeyNames.add("COUNTDOWN_BEEP_BOOL");
                    break;
                case DATA_MASTER_VOLUME_V2_INT:
                    obsoleteKeyNames.add("MASTER_VOLUME_INT_V2");
                    break;
                case DATA_IGNORE_INCLINE_ERRORS_BOOL:
                    obsoleteKeyNames.add("IGNORE_INCLINE_ERRORS_BOOL");
                    break;
                case DATA_IGNORE_B_LEVEL_ERRORS_BOOL:
                    obsoleteKeyNames.add("IGNORE_B_LEVEL_ERRORS_BOOL");
                    break;
                case DATA_TREADMILL_MOTOR_TYPE_INT:
                    obsoleteKeyNames.add("TREADMILL_MOTOR_TYPE_INT");
                    break;
                case DATA_BACKLIGHT_PROGRESS_INT:
                    obsoleteKeyNames.add("BACKLIGHT_PROGRESS_INT");
                    break;
                case DATA_PROMOTIONAL_VIDEO_TIMEOUT_MIN_INT:
                    obsoleteKeyNames.add("PROMOTIONAL_VIDEO_TIMEOUT_MIN_INT");
                    break;
                case DATA_ANT_PLUS_DEVICE_SEND_DATA_BOOL:
                    obsoleteKeyNames.add("ANT_PLUS_DEVICE_SEND_DATA_BOOL");
                    break;
                case DATA_FTMS_SHOULD_ADVERTISE_BOOL:
                    obsoleteKeyNames.add("FTMS_SHOULD_ADVERTISE");
                    break;
                case DATA_FTMS_ADVERTISING_ENABLED_BOOL:
                    obsoleteKeyNames.add("FTMS_ADVERTISING_ENABLED");
                    break;
                case DATA_PERFORMANCE_PLUS_TM_PFC_MODE_BOOL:
                    obsoleteKeyNames.add("PERFORMANCE_PLUS_TM_PFC_MODE_BOOL");
                    break;
                case DATA_FAN_MODE_INT:
                    obsoleteKeyNames.add("FAN_MODE_INT");
                    break;
                case DATA_GYMKIT_ENABLE_BOOL:
                    obsoleteKeyNames.add("GYMKIT_ENABLE_BOOL");
                    break;
                case DATA_MFI_ENABLE_BOOL:
                    obsoleteKeyNames.add("MFI_ENABLE_BOOL");
                    break;
                case DATA_IS_MACHINE_SETUP_BOOL:
                    obsoleteKeyNames.add("IS_MACHINE_SETUP_BOOL");
                    break;
                case DATA_HEADPHONE_A_COUNT_INT:
                    obsoleteKeyNames.add("HEADPHONE_A_COUNT_INT");
                    break;
                case DATA_HEADPHONE_B_COUNT_INT:
                    obsoleteKeyNames.add("HEADPHONE_B_COUNT_INT");
                    break;
                case DATA_USB_ATTACH_COUNT_INT:
                    obsoleteKeyNames.add("USB_ATTACH_COUNT_INT");
                    break;
                case DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL:
                    obsoleteKeyNames.add("IGNORE_ALL_LEVEL_ERRORS_BOOL");
                    break;
                case DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT:
                    obsoleteKeyNames.add("ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT");
                    break;
                case DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT:
                    obsoleteKeyNames.add("ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT");
                    break;
                case DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL:
                    obsoleteKeyNames.add("ENABLE_PROMOTIONAL_VIDEO_BOOL");
                    break;
                case DATA_ENABLE_OVER_CURRENT_PROTECTION_BOOL:
                    obsoleteKeyNames.add("ENABLE_OVER_CURRENT_PROTECTION_BOOL");
                    break;
                case DATA_ANT_PLUS_DEVICE_ID_INT:
                    obsoleteKeyNames.add("ANT_PLUS_DEVICE_ID_INT");
                    break;
                case DATA_ANT_PLUS_PAGE_INT:
                    obsoleteKeyNames.add("ANT_PLUS_PAGE_INT");
                    break;
                case DATA_SALUTRON_BLE_NAME_STRING:
                    obsoleteKeyNames.add("SALUTRON_BLE_NAME_STRING");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Hardware value, " + currentHardwareSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentHardwareSetting.toString(),
                        new PersistedKeyData(currentHardwareSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addDebugDataDefaultKeys() {
        for (SettingKeys.Debug currentDebugSetting : SettingKeys.Debug.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentDebugSetting) {
                case DATA_IS_DEMO_MODE_BOOL:
                    obsoleteKeyNames.add("IS_DEMO_MODE_BOOL");
                    break;
                case DATA_WATCHDOG_REBOOT_COUNTER_INT:
                    obsoleteKeyNames.add("WATCHDOG_REBOOT_COUNTER_INT");
                    break;
                case DATA_ENABLE_PERSISTENT_LOG_BOOL:
                    obsoleteKeyNames.add("ENABLE_PERSISTENT_LOG_BOOL");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Debug value, " + currentDebugSetting + ", is invalid");
            }
            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentDebugSetting.toString(),
                        new PersistedKeyData(currentDebugSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addTVDataDefaultKeys() {
        for (SettingKeys.TV currentTVSetting : SettingKeys.TV.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentTVSetting) {
                case DATA_TV_SOURCE_STRING:
                    obsoleteKeyNames.add("TV_SOURCE");
                    break;
                case DATA_CAB_TV_SOURCE_STRING:
                    obsoleteKeyNames.add("CAB_TV_SOURCE");
                    break;
                case DATA_TV_REMOTE_TV_BOOL:
                    obsoleteKeyNames.add("TV_REMOTE_TV_BOOL");
                    break;
                case DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_REMOTE_TV_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_REMOTE_TV_VOLUME_INT:
                    obsoleteKeyNames.add("TV_REMOTE_TV_VOLUME_INT");
                    break;
                case DATA_TV_SWIFT_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_SWIFT_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_SWIFT_US7_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_VIVID_DEFAULT_CHANNEL_STRING:
                    obsoleteKeyNames.add("TV_VIVID_DEFAULT_CHANNEL");
                    break;
                case DATA_TV_MST_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_MST_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_IPTV_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_IPTV_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING:
                    obsoleteKeyNames.add("TV_SET_TOP_BOX_DEFAULT_CHANNEL");
                    break;
                case DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT:
                    obsoleteKeyNames.add("TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_CAB_DEFAULT_CHANNEL_STRING:
                    obsoleteKeyNames.add("TV_CAB_DEFAULT_CHANNEL_INT");
                    break;
                case DATA_TV_SET_TOP_BOX_SELECTION_STRING:
                    obsoleteKeyNames.add("TV_SET_TOP_BOX_SELECTION_STRING");
                    break;
                case DATA_TV_IPTV_USE_SAP_BOOL:
                    obsoleteKeyNames.add("TV_IPTV_USE_SAP");
                    break;
                case DATA_TV_SKNET_TARGET_ADDRESS_STRING:
                    obsoleteKeyNames.add("TV_SKNET_TARGET_ADDRESS");
                    break;
                case DATA_TV_SKNET_TARGET_PORT_STRING:
                    obsoleteKeyNames.add("TV_SKNET_TARGET_PORT");
                    break;
                case DATA_CAB_CLOSED_CAPTIONING_BOOL:
                    obsoleteKeyNames.add("CAB_CLOSED_CAPTIONING");
                    break;
                case DATA_ENSEO_LEGACY_MODE_INITIALIZED_BOOL:
                    obsoleteKeyNames.add("ENSEO_LEGACY_MODE_INITIALIZED");
                    break;
                case DATA_SHOW_STB_ADVANCED_SETTINGS_BOOL:
                    obsoleteKeyNames.add("SHOW_STB_ADVANCED_SETTINGS");
                    break;
                default:
                    Log.e(TAG, "The SettingKeys.TV value, " + currentTVSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentTVSetting.toString(),
                        new PersistedKeyData(currentTVSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addExperimentalDataDefaultKeys() {
        for (SettingKeys.Experimental currentExperimentalSetting : SettingKeys.Experimental.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch(currentExperimentalSetting) {
                case DATA_BANDWIDTH_TEST_URL_STRING:
                    obsoleteKeyNames.add("BANDWIDTH_TEST_URL_STRING");
                    break;
                case DATA_EGYM_ENABLED_BOOL:
                    obsoleteKeyNames.add("EGYM_ENABLED_BOOL");
                    break;
                case DATA_EGYM_TESTING_WEBSITE_2_STRING:
                    obsoleteKeyNames.add("EGYM_TESTING_WEBSITE_2");
                    break;
                case DATA_EGYM_DEV_WEBSITE_2_STRING:
                    obsoleteKeyNames.add("EGYM_DEV_WEBSITE_2");
                    break;
                case DATA_EGYM_PROD_WEBSITE_2_STRING:
                    obsoleteKeyNames.add("EGYM_PROD_WEBSITE_2");
                    break;
                case DATA_EGYM_ENVIRONMENT_2_STRING:
                    obsoleteKeyNames.add("EGYM_ENVIRONMENT_STRING_2");
                    break;
                case DATA_EGYM_REFRESH_HOUR_INT:
                    obsoleteKeyNames.add("EGYM_REFRESH_HOUR_INT");
                    break;
                case DATA_IPTV_PLAYER_TYPE_INT:
                    obsoleteKeyNames.add("IPTV_PLAYER_TYPE_INT");
                    break;
                case DATA_WIRED_HEADPHONE_HDMI_DELAY_INT:
                    obsoleteKeyNames.add("WIRED_HEADPHONE_HDMI_DELAY");
                    break;
                case DATA_WIRELESS_HEADPHONE_HDMI_DELAY_INT:
                    obsoleteKeyNames.add("WIRELESS_HEADPHONE_HDMI_DELAY");
                    break;
                case DATA_ENABLE_ANALYTICS_BOOL:
                    obsoleteKeyNames.add("ENABLE_ANALYTICS_BOOL");
                    break;
                case DATA_LOG_MCU_PACKETS_BOOL:
                    obsoleteKeyNames.add("LOG_MCU_PACKETS_BOOL");
                    break;
                case DATA_LOG_LCB_PACKETS_BOOL:
                    obsoleteKeyNames.add("LOG_LCB_PACKETS_BOOL");
                    break;
                case DATA_LOG_DAPI_PACKETS_BOOL:
                    obsoleteKeyNames.add("LOG_DAPI_PACKETS_BOOL");
                    break;
                case DATA_LOG_URLS_BOOL:
                    obsoleteKeyNames.add("LOG_URLS_BOOL");
                    break;
                case DATA_LOG_GYMKIT_BOOL:
                    obsoleteKeyNames.add("LOG_GYMKIT_BOOL");
                    break;
                case DATA_LOG_RFID_BOOL:
                    obsoleteKeyNames.add("LOG_RFID_BOOL");
                    break;
                case DATA_LOG_CSAFE_BOOL:
                    obsoleteKeyNames.add("LOG_CSAFE_BOOL");
                    break;
                case DATA_LOG_PTP_BOOL:
                    obsoleteKeyNames.add("LOG_PTP_BOOL");
                    break;
                case DATA_CRASH_MONITORING_BOOL:
                    obsoleteKeyNames.add("CRASH_MONITORING");
                    break;
                case DATA_POWER_LIMITING_BOOL:
                    obsoleteKeyNames.add("POWER_LIMITING");
                    break;
                case DATA_HDMI_ERROR_THRESHOLD_V2_INT:
                    obsoleteKeyNames.add("HDMI_ERROR_THRESHOLD_V2");
                    break;
                case DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT:
                    obsoleteKeyNames.add("IPTV_DEINTERLACE_FRAME_SKIP");
                    break;
                case DATA_ADB_VIA_NETWORK_BOOL:
                    obsoleteKeyNames.add("ADB_VIA_NETWORK");
                    break;
                case DATA_FORCE_HIDE_IFIT_APP_BOOL:
                    obsoleteKeyNames.add("FORCE_HIDE_IFIT_APP_BOOL");
                    break;
                case DATA_RECENT_WORKOUT_APP_LIMIT_2_INT:
                    obsoleteKeyNames.add("RECENT_WORKOUT_APP_LIMIT_2");
                    break;
                case DATA_SHOW_STB_COMMAND_LIST_BOOL:
                    obsoleteKeyNames.add("SHOW_STB_COMMAND_LIST_BOOL");
                    break;
                case DATA_DELAY_LOAD_VA_BOOL:
                    obsoleteKeyNames.add("DELAY_LOAD_VA");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.Experimental value, " + currentExperimentalSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentExperimentalSetting.toString(),
                        new PersistedKeyData(currentExperimentalSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    private void addProjectOneDataDefaultKeys() {
        for (SettingKeys.ProjectOne currentProjectOneSetting: SettingKeys.ProjectOne.values()) {
            List<String> obsoleteKeyNames = new ArrayList<>();
            boolean addDataToThesaurus = true;
            switch (currentProjectOneSetting) {
                case DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL:
                    obsoleteKeyNames.add("DATA_HAPTIC_MOTOR_ON_SYSTEM_BOOL");
                    break;
                default:
                    addDataToThesaurus = false;
                    Log.e(TAG, "The SettingKeys.ProjectOne value, " + currentProjectOneSetting + ", is invalid");
            }

            if (addDataToThesaurus) {
                persistedSettingKeyThesaurus.put(currentProjectOneSetting.toString(),
                        new PersistedKeyData(currentProjectOneSetting.DataDefault, obsoleteKeyNames));
            }
        }
    }

    public static class PersistedKeyData {
        private final DataDefault DataDefault;
        private final List<String> obsoleteKeyNames;

        public PersistedKeyData(DataDefault<?> DataDefault, List<String> obsoleteKeyNames) {
            this.DataDefault = DataDefault;
            this.obsoleteKeyNames = obsoleteKeyNames;
        }

        public DataDefault<?> getDataDefault() {
            return this.DataDefault;
        }

        public List<String> getObsoleteKeyNames() {
            return this.obsoleteKeyNames;
        }
    }
}
