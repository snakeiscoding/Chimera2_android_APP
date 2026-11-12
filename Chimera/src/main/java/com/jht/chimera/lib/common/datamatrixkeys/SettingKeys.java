package com.jht.chimera.lib.common.datamatrixkeys;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.chimera.service.hal.lib.hardware.DisplayAdjustmentKt;
import com.jht.mastboard.data.LEDStyle;
import com.jht.datamatrixlib.DMSettingKeys;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.PersistentDataDefault;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.cab.CabCommand;
import com.jht.serialport.peripheral.tuner.channel.IChannel;
import com.jhtp.dapiservice.um.GenderIndentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;

@SuppressWarnings("HardCodedStringLiteral")
public enum SettingKeys {

    EVENT_RESET_FACTORY_DEFAULTS,
    // Maybe these would make more sense somewhere else?
    DATA_IP_LOCATION_COUNTRY_CODE_STRING,
    DATA_IP_LOCATION_IP_OVERRIDE_STRING;

    public static List<DataDefault<?>> sDataDefaultList = new LinkedList<>();

    // create the list of all default settings that will be used
    static {
        for (General s : General.values()) sDataDefaultList.add(s.DataDefault);
        for (Workout s : Workout.values()) sDataDefaultList.add(s.DataDefault);
        for (Update s : Update.values()) sDataDefaultList.add(s.DataDefault);
        for (AssetManagement s : AssetManagement.values()) sDataDefaultList.add(s.DataDefault);
        for (Weather s : Weather.values()) sDataDefaultList.add(s.DataDefault);
        for (SetupDefault s : SetupDefault.values()) sDataDefaultList.add(s.DataDefault);
        for (Application s : Application.values()) sDataDefaultList.add(s.DataDefault);
        for (Calibration s : Calibration.values()) sDataDefaultList.add(s.DataDefault);
        for (Hardware s : Hardware.values()) sDataDefaultList.add(s.DataDefault);
        for (Debug s : Debug.values()) sDataDefaultList.add(s.DataDefault);
        for (TV s : TV.values()) sDataDefaultList.add(s.DataDefault);
        for (Experimental s : Experimental.values()) sDataDefaultList.add(s.DataDefault);
        for (ProjectOne s : ProjectOne.values()) {
            // P1 enums have action/event keys which don't have defaults so need to only add if not null
            if (s.DataDefault != null) sDataDefaultList.add(s.DataDefault);
        }
    }

    public enum General {
        DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL(new PersistentDataDefault<>(BuildConfig.MARKET_RETAIL)),
        DATA_DIGITAL_SAFETY_PIN_STRING(new PersistentDataDefault<>(BuildConfig.MARKET_RETAIL ? "1234" : "1975")),
        DATA_DYNAMIC_THEME_URI_STRING(new PersistentDataDefault<>(ProductConfig.isProjectOne() ? "preset:dark" : "preset:basic")), // CGC-23331: its all dark, but CGC-25801: now they are separate again
        DATA_SCREEN_TIMEOUT_SEC_INT(new PersistentDataDefault<>(30, "all")),
        DATA_ACCUMULATED_WORKOUTS_INT(new PersistentDataDefault<>(0)),
        DATA_ACCUMULATED_TIME_SECONDS_INT(new PersistentDataDefault<>(0)),
        DATA_ACCUMULATED_DISTANCE_KM_FLOAT(new PersistentDataDefault<>(0f)),
        DATA_ACCUMULATED_FLOORS_FLOAT(new PersistentDataDefault<>(0f)),
        DATA_FRAME_TYPE_STRING(new PersistentDataDefault<>("")),
        DATA_LCB_TYPE_INT(new PersistentDataDefault<>(0)),
        DATA_IANA_TIME_ZONE_STRING(new PersistentDataDefault<>("America/Chicago", "all")),
        DATA_TIME_ZONE_INDEX_INT(new PersistentDataDefault<>(-1, "all")),
        DATA_DATE_FORMAT_ENUM_INT(new PersistentDataDefault<>(DateFormat.MONTH_DAY_YEAR.ordinal(), "all")),
        DATA_CLOCK_FORMAT_24_HOURS_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_SHOW_OUT_OF_ORDER_BOOL(new PersistentDataDefault<>(false)),
        DATA_AUTO_DATE_TIME_BOOL(new PersistentDataDefault<>(true)),
        DATA_CONSOLE_LANGUAGE_ENUM_INT(new PersistentDataDefault<>(0, "all")),
        DATA_SELECTED_LANGUAGE_ENUM_INT(new DataDefault<>(0)),
        DATA_REBOOT_INACTIVE_TIME_HOUR_INT(new PersistentDataDefault<>(6, "all")),
        DATA_DAPI_ENVIRONMENT_ENUM_INT(new PersistentDataDefault<>(JHTCommonTypes.DAPIEnvironment.production.ordinal(), "all")),
        DATA_IS_GUEST_USER_BOOL(new DataDefault<>(false)),
        DATA_IS_PTP_USER_BOOL(new DataDefault<>(false));

        public DataDefault<?> DataDefault;

        General(DataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Workout {
        DATA_KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT(new PersistentDataDefault<>(3.2f, "all")),
        DATA_MAX_WORKOUT_TIME_MIN_INT(new PersistentDataDefault<>(BuildConfig.DATA_MAX_WORKOUT_TIME_MIN_INT, "all")),
        DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT(new PersistentDataDefault<>(50f, "all")),
        DATA_MAX_WORKOUT_CALORIES_INT(new PersistentDataDefault<>(1000, "all")),
        DATA_MAX_WORKOUT_FLOORS_INT(new PersistentDataDefault<>(150, "all")),
        DATA_MAX_CLIMBMILL_LEVEL_INT(new PersistentDataDefault<>(20, "all")),
        DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT(new PersistentDataDefault<>(25, "all")),
        DATA_PAUSE_TIME_MIN_INT(new PersistentDataDefault<>(5, "all")),
        DATA_MAX_SPEED_KPH_FLOAT(new PersistentDataDefault<>(-1f)), // The max speed for the kph setting, in kph
        DATA_MAX_SPEED_MPH_FLOAT(new PersistentDataDefault<>(-1f)), // The max speed for the mph setting, in kph
        DATA_MAX_SPEED_KPH_REVERSE_FLOAT(new PersistentDataDefault<>(-1f)), // The max reverse speed for the mph setting, in kph
        DATA_MAX_SPEED_MPH_REVERSE_FLOAT(new PersistentDataDefault<>(-1f)), // The max reverse speed for the mph setting, in kph
        DATA_START_SPEED_KPH_FLOAT(new PersistentDataDefault<>(-1f, "all")),
        DATA_TREAD_SENSE_ENABLED_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_TREAD_SENSE_ENABLED_BOOL, "all")), // perhaps there should be a frame config prop for support?
        DATA_TREAD_SENSE_TIMEOUT_SEC_INT(new PersistentDataDefault<>(30, "all")),
        DATA_TREAD_SENSE_COUNTDOWN_SEC_INT(new PersistentDataDefault<>(10, "all")),
        DATA_ELEVATION_ABOVE_5000_FT_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_CM_ESTOP_ENABLE_BOOL(new PersistentDataDefault<>(true, "all")),
        //CM_ESTOP_SENSITIVITY_INT(new PersistentDataDefault<>(100)),
        DATA_CM_DETECT_PERSON_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_MAX_INCLINE_FLOAT(new PersistentDataDefault<>(-1f)),
        DATA_COOLDOWN_TIME_INT(new PersistentDataDefault<>(5, "all")),
        DATA_MAX_COOLDOWN_TIME_INT(new PersistentDataDefault<>(BuildConfig.DATA_MAX_COOLDOWN_TIME_INT, "all")),
        DATA_MIGROS_ENABLED_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_SHOW_MARATHON_MODE_BOOL(new PersistentDataDefault<>(false)),

        // TODO: Rename someday to mention Athena
        // This is used for Athena, where we want to default IFit to on
        DATA_IFIT_ENABLED_DEFAULT_ON_BOOL(new PersistentDataDefault<>(true)),

        // CGC-18188 temporarily add onyx specific key to enable ifit, do no worry about modalities (there are plans to rework and simplify these ifit keys)
        DATA_IFIT_ONYX_ENABLED_BOOL(new PersistentDataDefault<>(true)),

        DATA_IFIT_TREADMILL_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_ELLIPTICAL_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_ASCENT_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_STEPPER_ENABLED_BOOL(new PersistentDataDefault<>(false)),
        DATA_IFIT_CLIMBMILL_ENABLED_BOOL(new PersistentDataDefault<>(false)),

        // Low Cost GUI specific DM keys (CGC-15581)
        DATA_IFIT_TREADMILL_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_ELLIPTICAL_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_ASCENT_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_HYBRID_BIKE_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_UPRIGHT_BIKE_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_RECUMBENT_BIKE_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_STEPPER_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),
        DATA_IFIT_CLIMBMILL_ENABLED_TB_BOOL(new PersistentDataDefault<>(ProductConfig.ifitByDefault())),

        //Workout Carousel Defaults to VA (47), Sprint 8 (18), and Rolling Hills (9).
        //Climbmills will programmatically convert Sprint 8 to Landmarks
        DATA_FEATURED_WORKOUT_NON_ATHENA_STRING(new PersistentDataDefault<>("47,18,9", "all")),
        DATA_FEATURED_WORKOUT_ATHENA_STRING(new PersistentDataDefault<>("47,18,9", "all"));

        public PersistentDataDefault<?> DataDefault;

        Workout(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Update {
        DATA_AUTO_UPDATE_HOUR_INT(new PersistentDataDefault<>(1, "all")),
        DATA_AUTO_UPDATE_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_AUTO_UPDATE_BOOL, "all")),
        DATA_AUTO_UPDATE_DLG_SHOWN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_LAST_CHECK_TIME_LONG(new PersistentDataDefault<>(0L));

        public PersistentDataDefault<?> DataDefault;

        Update(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum AssetManagement{
        DATA_USE_ASSET_MANAGEMENT_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_USE_WORKOUT_TRACKING_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_CONSOLE_SERIAL_STRING(new PersistentDataDefault<>("")),
        DATA_FRAME_SERIAL_STRING(new PersistentDataDefault<>("")),
        DATA_BRIDGE_SERIAL_STRING(new PersistentDataDefault<>("")),
        DATA_CUSTOM_SKIN_VERSION_STRING(new PersistentDataDefault<>("", "all")),
        DATA_CLUB_ID_INT(new PersistentDataDefault<>(BuildConfig.DATA_CLUB_ID_INT, "all")),
        DATA_ENABLE_PHYSICAL_GO_BTN_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_ENABLE_XID_LOGIN_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_ENABLE_XID_LOGIN_BOOL, "all")),
        DATA_ENABLE_XID_REGISTRATION_BOOL(new PersistentDataDefault<>(true , "all")),
        DATA_ENABLE_BARCODE_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_EMAIL_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_FITLINXX_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_CUSTOM_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_XAREFIT_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_RFID_LOGIN_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_ENABLE_WALLET_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_QR_LOGIN_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_JUMP_DEVICE_REDIRECT_APP_STRING(new PersistentDataDefault<>("", "all")),
        DATA_XAREFIT_SITE_ID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_XAREFIT_SERVER_URL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_XAREFIT_SERVER_PORT_STRING(new PersistentDataDefault<>("", "all")),
        DATA_PRIMARY_LOGIN_METHOD_STRING(new PersistentDataDefault<>("xid", "all")), // login type as defined by DAPI CBOR spec
        DATA_CUSTOM_LOGIN_NAME_STRING(new PersistentDataDefault<>("Custom", "all")),
        DATA_CUSTOM_LOGIN_LABEL_STRING(new PersistentDataDefault<>("User", "all")),
        DATA_CUSTOM_LOGIN_INPUT_TYPE_INT(new PersistentDataDefault<>(0, "all")),
        DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING(new PersistentDataDefault<>("Password", "all")),
        DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT(new PersistentDataDefault<>(0, "all")),
        DATA_USE_CUSTOM_LOGO_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_USE_CUSTOM_LOGO_BOOL, "all")),
        DATA_USE_CUSTOM_BACKGROUND_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_USE_CUSTOM_BACKGROUND_BOOL, "all")),
        DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL, "all")),
        DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL, "all")),
        DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT(new PersistentDataDefault<>(60, "all")),
        DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT(new PersistentDataDefault<>(0, "all")),
        DATA_ALLOW_GUEST_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_SHOW_WELCOME_SCREEN_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_SHOW_SWITCH_STRIDE_MODE_ONLY_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT(new PersistentDataDefault<>(10, "all")),
        DATA_ENABLE_CLUB_COM_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT(new PersistentDataDefault<>(0, "all")),
        DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT(new PersistentDataDefault<>(50, "all")),
        DATA_CUSTOM_WEB_APP_URL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_CUSTOM_WEB_APP_NAME_STRING(new PersistentDataDefault<>("", "all")),
        DATA_CUSTOM_RFID_ONLY_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_CONCIERGE_BOOL(new PersistentDataDefault<>(false)),
        DATA_ENABLE_APPSTORE_BOOL(new PersistentDataDefault<>(true)),
        DATA_MARKETING_GYMKIT_LEVEL_INT(new PersistentDataDefault<>(0, "all")),
        DATA_MARKETING_WELCOME_SCREEN_ADS_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_MARKETING_FEATURE_CAROUSEL_ADS_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_WORKOUT_SNAPSHOT_SETTING_INT(new PersistentDataDefault<>(AssetManagementConstants.WORKOUT_SNAPSHOT_SETTING_LOGGED_IN_ONLY, "all"));

        public PersistentDataDefault<?> DataDefault;

        AssetManagement(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }

        static public boolean isAllLoginTypeDisabled() {
            return !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL)
                    && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_QR_LOGIN_BOOL);
        }
    }

    public enum Weather {
        DATA_WEATHER_LONGITUDE_FLOAT(new PersistentDataDefault<>(0f, "all")),
        DATA_WEATHER_LATITUDE_FLOAT(new PersistentDataDefault<>(0f, "all")),
        DATA_WEATHER_CITY_STRING(new PersistentDataDefault<>("", "all"));

        public PersistentDataDefault<?> DataDefault;

        Weather(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum SetupDefault {
        DATA_DEFAULT_LEVEL_INT(new PersistentDataDefault<>(1, "all")),
        DATA_DEFAULT_AGE_INT(new PersistentDataDefault<>(30, "all")),
        DATA_DEFAULT_WEIGHT_KG_FLOAT(new PersistentDataDefault<>(68.0388f, "all")),
        DATA_DEFAULT_HEIGHT_CM_FLOAT(new PersistentDataDefault<>(177.8f, "all")), // 70 inches = 177.8 centimeters.
        DATA_DEFAULT_GENDER_ENUM_INT(new PersistentDataDefault<>(GenderIndentity.Male.Value, "all")),
        DATA_DEFAULT_WORKOUT_TIME_MIN_INT(new PersistentDataDefault<>(30, "all")),
        DATA_DEFAULT_WORKOUT_CALORIES_INT(new PersistentDataDefault<>(300, "all")),
        DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT(new PersistentDataDefault<>(4.82803f, "all")),
        DATA_DEFAULT_WORKOUT_FLOORS_INT(new PersistentDataDefault<>(20, "all")),
        DATA_DEFAULT_WEBPAGE_STRING(new PersistentDataDefault<>(BuildConfig.DATA_DEFAULT_WEBPAGE_STRING, "all")),
        DATA_DEFAULT_TREO_URL_STRING(new PersistentDataDefault<>("https://console.treowellness.com", "all")),
        DATA_DEFAULT_USE_BAIDU_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING(new PersistentDataDefault<>("https://www.surveymonkey.com", "all")),
        DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING(new PersistentDataDefault<>("https://www.surveymonkey.com", "all")),
        // ML survey shown before the summary; empty keeps it off
        DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_DEFAULT_UNIT_IS_METRIC_BOOL(new PersistentDataDefault<>(false, "all"));



        public PersistentDataDefault<?> DataDefault;

        SetupDefault(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Application {
        DATA_APP_ORDER_STRING(new PersistentDataDefault<>("", "all")),
        DATA_APP_INACTIVE_LIST_STRING(new PersistentDataDefault<>("", "all")),
        DATA_USER_PROFILE_SELECTED_SLOT_INT(new PersistentDataDefault<>(DAPIKeys.Constants.DAPI_GUEST_INDEX)),
        DATA_VERSION_RAPTOR_PARAMETER_STRING(new PersistentDataDefault<>("")),
        DATA_WEATHER_APP_ENUM_INT(new PersistentDataDefault<>(0, "all")),

        // Used to hide the web and survey monkey apps when starting the console. These should have been hidden by default. (Except that retail defaults showing browser)
        DATA_HIDE_WEB_AND_SURVEY_APPS_BOOL(new PersistentDataDefault<>(!BuildConfig.MARKET_RETAIL, "")),
        DATA_HULU_PLUS_ENABLED_BOOL(new PersistentDataDefault<>(true, "")),
        DATA_DISNEY_PLUS_ENABLED_BOOL(new PersistentDataDefault<>(BuildConfig.MARKET_RETAIL, ""));
        public PersistentDataDefault<?> DataDefault;

        Application(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Calibration {
        DATA_TREADMILL_INLINE_ADC_MAX_INT(new PersistentDataDefault<>(-1)),
        DATA_TREADMILL_INLINE_ADC_MIN_INT(new PersistentDataDefault<>(-1)),
        DATA_CLIMBMILL_USE_LCB_WATTS_BOOL(new PersistentDataDefault<>(false)),
        DATA_CLIMBMILL_CZ_FREQUENCY_INT(new PersistentDataDefault<>(0)),
        DATA_CLIMBMILL_IR_FREQUENCY_INT(new PersistentDataDefault<>(0)),
        DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT(new PersistentDataDefault<>(68f)),
        DATA_CLIMBMILL_CAL_SPM_INT(new PersistentDataDefault<>(52)),
        DATA_CLIMBMILL_CAL_TIME_SEC_INT(new PersistentDataDefault<>(50)),
        DATA_CLIMBMILL_CAL_FACTOR_INT(new PersistentDataDefault<>(1000)),
        DATA_CLIMBMILL_CAL_FACTOR_USER_INPUT_INT(new PersistentDataDefault<>(1000)),
        DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT(new PersistentDataDefault<>(-1)),
        DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT(new PersistentDataDefault<>(-1)),
        //DATA_SETTING_RAPTOR_PARA_DATA_STRING(new PersistentDataDefault<>("5555550241a6d1e0425084fec890b6eb3f8395734398ed8ac8158faa43ec2aa0c93f46b44f5f814b000000004211a48ac66eb9d94b35b661d18b05d041230e56000000415eb098513f2833bd000000000000000c"));
        DATA_SETTING_RAPTOR_PARA_DATA_STRING(new PersistentDataDefault<>("")),
        DATA_TREADMILL_MAGNET_ANGLE_INT(new PersistentDataDefault<>(-1));

        public PersistentDataDefault<?> DataDefault;

        Calibration(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Hardware {
        DATA_ENERGY_SAVER_TYPE_ENUM_INT(new PersistentDataDefault<>(ERPType.FullErp.ordinal(), "all")),
        DATA_ENERGY_SAVER_MODE_ENUM_INT(new PersistentDataDefault<>(BuildConfig.DATA_ENERGY_SAVER_MODE_ENUM_INT, "all")),
        DATA_ENERGY_SAVER_TIMEOUT_MIN_INT(new PersistentDataDefault<>(30, "all")),
        DATA_ENERGY_SAVER_TIME_MIN_INT(new PersistentDataDefault<>(60, "all")),
        DATA_SHOW_MACHINE_SETUP_BOOL(new PersistentDataDefault<>(false)), // or perhaps we should make DATA_IS_MACHINE_SETUP_BOOL an enum/integer of states?
        DATA_IS_MACHINE_SETUP_BOOL(new PersistentDataDefault<>(false)),
        DATA_KEY_BEEP_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_COUNTDOWN_BEEP_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_MASTER_VOLUME_V2_INT(new PersistentDataDefault<>(BuildConfig.DATA_MASTER_VOLUME_V2_INT, "all")),
        DATA_HEADPHONE_A_COUNT_INT(new PersistentDataDefault<>(0)),
        DATA_HEADPHONE_B_COUNT_INT(new PersistentDataDefault<>(0)),
        DATA_USB_ATTACH_COUNT_INT(new PersistentDataDefault<>(0)),
        DATA_IGNORE_INCLINE_ERRORS_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_IGNORE_B_LEVEL_ERRORS_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL(new DataDefault<>(false)),
        DATA_TREADMILL_MOTOR_TYPE_INT(new PersistentDataDefault<>(0, "all")),
        DATA_BACKLIGHT_PROGRESS_INT(new PersistentDataDefault<>(100, "all")),
        DATA_DISPLAY_COLOR_TEMPERATURE_INT(new PersistentDataDefault<>(DisplayAdjustmentKt.COLOR_TEMPERATURE_DEFAULT, "all")),
        DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT(new PersistentDataDefault<>(0f)),
        DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT(new PersistentDataDefault<>(0)),
        DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL(new PersistentDataDefault<>(false)),
        DATA_PROMOTIONAL_VIDEO_TIMEOUT_MIN_INT(new PersistentDataDefault<>(5, "all")),
        DATA_ENABLE_OVER_CURRENT_PROTECTION_BOOL(new PersistentDataDefault<>(false)),
        DATA_ANT_PLUS_DEVICE_ID_INT(new PersistentDataDefault<>(0)),
        DATA_ANT_PLUS_DEVICE_SEND_DATA_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ANT_PLUS_PAGE_INT(new PersistentDataDefault<>(0)),
        DATA_SALUTRON_BLE_NAME_STRING(new PersistentDataDefault<>("")),
        DATA_FTMS_SHOULD_ADVERTISE_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_FTMS_ADVERTISING_ENABLED_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_FTMS_ADVERTISING_ENABLED_BOOL, "all")),
        DATA_PERFORMANCE_PLUS_TM_PFC_MODE_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_FAN_MODE_INT(new PersistentDataDefault<>(0, "all")),
        DATA_GYMKIT_ENABLE_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_GYMKIT_ENABLED_BOOL, "all")),
        DATA_SAMSUNG_WATCH_ENABLE_BOOL(new PersistentDataDefault<>(BuildConfig.SAMSUNG_WATCH_COMPATIBLE, "all")),
        DATA_MFI_ENABLE_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_AUDIO_DISABLED_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_UPDATE_LCDS_TOUCH_FIRMWARE_STATUS_INT(new DataDefault<>(0));

        public DataDefault<?> DataDefault;

        Hardware(DataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum Debug {
        DATA_IS_DEMO_MODE_BOOL(new PersistentDataDefault<>(false)),
        DATA_WATCHDOG_REBOOT_COUNTER_INT(new PersistentDataDefault<>(0)),
        DATA_ENABLE_PERSISTENT_LOG_BOOL(new PersistentDataDefault<>(false)),
        DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG(new PersistentDataDefault<>(0L));

        public PersistentDataDefault<?> DataDefault;

        Debug(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum TV {
        DATA_TV_SOURCE_STRING(new PersistentDataDefault<>(ProductConfig.getDefaultTuner().toString(), "all")),
        DATA_CAB_TV_SOURCE_STRING(new PersistentDataDefault<>(TVSource.CAB.toString(), "all")),
        DATA_TV_REMOTE_TV_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(1, "all")),
        DATA_TV_REMOTE_TV_VOLUME_INT(new PersistentDataDefault<>(100, "all")),
        DATA_TV_SWIFT_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_SWIFT_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_KM5_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_KM5_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_US4_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_US4_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_US8_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_US8_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_KM6_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_KM6_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_US9_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_US9_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_KM7_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_KM7_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_BR3_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_BR3_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_JP1_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_JP1_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_VIVID_DEFAULT_CHANNEL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_MST_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")), // TODO: CGC-22432 Remove once UUID migration is complete
        DATA_TV_MST_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_IPTV_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")),
        DATA_TV_IPTV_DEFAULT_CHANNEL_UUID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT(new PersistentDataDefault<>(0, "all")),
        DATA_TV_CAB_DEFAULT_CHANNEL_STRING(new PersistentDataDefault<>("", "all")),
        DATA_TV_SET_TOP_BOX_SELECTION_STRING(new PersistentDataDefault<>("", "all")),
        DATA_ENSEO_LEGACY_MODE_INITIALIZED_BOOL(new PersistentDataDefault<>(false)),
        DATA_TV_IPTV_USE_SAP_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_TV_SKNET_TARGET_ADDRESS_STRING(new PersistentDataDefault<>("192.168.70.27", "all")),
        DATA_TV_SKNET_TARGET_PORT_STRING(new PersistentDataDefault<>("7165", "all")),
        DATA_CAB_CLOSED_CAPTIONING_BOOL(new PersistentDataDefault<>(true, "all")),
        DATA_CAB_ENTER_COMMAND_STRING(new PersistentDataDefault<>(CabCommand.EnterCommand.EnterTV.INSTANCE.getDisplayName())),
        DATA_SHOW_STB_ADVANCED_SETTINGS_BOOL(new PersistentDataDefault<>(false)),
        DATA_HDMI_OUTPUT_RESOLUTION_STRING(new PersistentDataDefault<>(ProductConfig.getHDMIResolutionConfig().getDefaultResolution().name(), "all"));

        public DataDefault<?> DataDefault;

        TV(DataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum UiSystem {
        Legacy,
        Chameleon,
        Vortex,
        ; // END LIST
        public static boolean isLegacy(int ordinal) { return ordinal == Legacy.ordinal(); }
        public static UiSystem getDefault() {
            if (BuildConfig.MARKET_RETAIL) return Legacy;
            return Vortex; // CGC-23331: it is all vortex now
        }
    }

    public enum Experimental {
        DATA_ENABLE_DAPI_FALLBACK_BOOL(new PersistentDataDefault<>(ProductConfig.isRetail())), // until full transition is ready
        DATA_UI_SYSTEM_ORDINAL_INT(new PersistentDataDefault<>(UiSystem.getDefault().ordinal())), // TODO - TEST ONLY - REMOVE LATER
        DATA_ENABLE_AUTO_HIDE_PANELS_BOOL(new PersistentDataDefault<>(false)), // TODO - TEST ONLY - REMOVE LATER
        DATA_ENABLE_STB_STREAM_APPS_BOOL(new PersistentDataDefault<>(ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Vortex))), // TODO - TEST ONLY - REMOVE LATER
        DATA_BANDWIDTH_TEST_URL_STRING(new PersistentDataDefault<>("https://fast.com", "all")),
        DATA_EGYM_ENABLED_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_EGYM_TESTING_WEBSITE_2_STRING(new PersistentDataDefault<>("https://gui.partner.ed.ts.egym.coffee/work/#/", "all")),
        DATA_EGYM_DEV_WEBSITE_2_STRING(new PersistentDataDefault<>("https://preview.gui.partner.api.egym.com/work/#/", "all")),
        DATA_EGYM_PROD_WEBSITE_2_STRING(new PersistentDataDefault<>("https://smartcardio.gui.egym.com/work/#/", "all")),
        DATA_EGYM_ENVIRONMENT_2_STRING(new PersistentDataDefault<>("Production", "all")),
        DATA_EGYM_REFRESH_HOUR_INT(new PersistentDataDefault<>(1, "all")),
        DATA_ENABLE_ANALYTICS_BOOL(new PersistentDataDefault<>(false)),
        DATA_FORCE_DOWNGRADE_UPDATES_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_MCU_PACKETS_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_LCB_PACKETS_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_DAPI_PACKETS_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_URLS_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_GYMKIT_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_RFID_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_CSAFE_BOOL(new PersistentDataDefault<>(false)),
        DATA_LOG_PTP_BOOL(new PersistentDataDefault<>(false)),
        DATA_POWER_LIMITING_BOOL(new PersistentDataDefault<>(BuildConfig.DATA_POWER_LIMITING_BOOL)),
        DATA_HDMI_ERROR_THRESHOLD_V2_INT(new PersistentDataDefault<>(2)),
        DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT(new PersistentDataDefault<>(1)),
        DATA_ADB_VIA_NETWORK_BOOL(new PersistentDataDefault<>(false)),
        DATA_FORCE_HIDE_IFIT_APP_BOOL(new PersistentDataDefault<>(false)),
        DATA_IPTV_PLAYER_TYPE_INT(new PersistentDataDefault<>(0, "all")),
        DATA_RECENT_WORKOUT_APP_LIMIT_2_INT(new PersistentDataDefault<>(BuildConfig.DATA_RECENT_WORKOUT_APP_LIMIT_2_INT)),
        DATA_SHOW_STB_COMMAND_LIST_BOOL(new PersistentDataDefault<>(false)),
        DATA_DELAY_LOAD_VA_BOOL(new PersistentDataDefault<>(false)),
        DATA_WIRED_HEADPHONE_HDMI_DELAY_INT(new PersistentDataDefault<>(50000, "all")),
        DATA_WIRELESS_HEADPHONE_HDMI_DELAY_INT(new PersistentDataDefault<>(50000, "all")),
        DATA_WIRELESS_HEADPHONE_HDMI_DELAY_CHIMERA2_INT(new PersistentDataDefault<>(500, "all")),
        DATA_HDMI_WIDTH_CHIMERA2_INT(new PersistentDataDefault<>(1280, "all")),
        DATA_HDMI_HEIGHT_CHIMERA2_INT(new PersistentDataDefault<>(720, "all")),
        DATA_ENABLE_YMCA_360_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_BUMP_SENSOR_SETTING_INT(new PersistentDataDefault<>("500", "all")),
        DATA_ENABLE_ISC_AT_BOOT_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_DISTANCE_PRECISION_INT(new PersistentDataDefault<>(2, "all")),
        DATA_DISABLE_WIFI_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_OVERRIDE_MAIN_APP_ID_STRING(new PersistentDataDefault<>("", "all")),
        DATA_CUSTOM_APP_OVERRIDE_PACKAGE_NAME_STRING(new PersistentDataDefault<>("", "all")),
        DATA_IO_EXTERNAL_POWER_BOOL(new PersistentDataDefault<>(true)),
        DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_WORKOUT_DATA_ALTERNATE_ENDPOINT_STRING(new PersistentDataDefault<>("", "all")),
        DATA_CRASH_MONITORING_BOOL(new PersistentDataDefault<>(true)),
        //Disables any Crash Reporting logs or incidents going to their endpoints
        DATA_CRASH_REPORTING_DISABLE_ENDPOINT_BOOL(new PersistentDataDefault<>(false, "all")),
        //Boolean that disables the AWS blocklist for blocked crash keywords in logs
        DATA_CRASH_REPORTING_DISABLE_BLOCKLIST_BOOL(new PersistentDataDefault<>(false, "all")),
        //Boolean that overrides the Log API URL when True
        DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL(new PersistentDataDefault<>(false, "all")),
        //Boolean that overrides the Incident API URL when True
        DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_BOOL(new PersistentDataDefault<>(false, "all")),
        //Enables debugging logs for Crash Reporting
        DATA_CRASH_REPORTING_DEBUG_LOGS_BOOL(new PersistentDataDefault<>(false, "all")),
        //Number of Crashes stored in the cache
        DATA_CRASH_REPORTING_CACHED_CRASHES_INT(new PersistentDataDefault<>(0, "all")),
        //Sets the Overridden Log endpoint URL for the API
        DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_STRING(new PersistentDataDefault<>("", "all")),
        //Sets the Overridden Incident endpoint URL for the API
        DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_STRING(new PersistentDataDefault<>("", "all")),
        //Sets the time for a single crash to be collected in minutes for Crash Reporting
        DATA_CRASH_REPORTING_COLLECTION_TIME_MINUTES_INT(new PersistentDataDefault<>(30, "all")),
        DATA_ENABLE_DAPI_ANALYTICS_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_ENABLE_LIVE_CLIMB_BOOL(new PersistentDataDefault<>(false)),
        DATA_FTMS_ENABLE_CONTROL_POINT_BOOL(new PersistentDataDefault<>(BuildConfig.FTMS_ENABLE_CONTROL_POINT)),
        DATA_AMAZON_FREEVEE_ENABLE_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_LED_DEFAULT_COLOR_HEX_INT(new PersistentDataDefault<>(0xff0000ff, "all")),
        DATA_DAILY_FORCE_REBOOT_ENABLE_BOOL(new PersistentDataDefault<>(false, "all")),
        DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT(new PersistentDataDefault<>(0xff00ff00, "all")),
        DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT(new PersistentDataDefault<>(0xff0000ff, "all")),
        DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT(new PersistentDataDefault<>(0xffff0000, "all")),
        DATA_LED_CONCIERGE_COLOR_HEX_INT(new PersistentDataDefault<>(0xffffff00, "all")),
        /**
         * Set to false by default, only implemented on Project One at this time. Used to enable/disable
         * Nightly Updates.
         */
        DATA_ENABLE_NIGHTLY_UPDATES_BOOL(new PersistentDataDefault<>(false, "all")),
        /** if this is not empty, we know there is a passcode to check against prior to opening MES **/
        DATA_SETTINGS_LOCKOUT_PASSCODE_STRING(new PersistentDataDefault<>("","all")),
        /** if this is not 0, we know that the passcode entry should be in a "locked out" state and will unlock in a specified time after the timestamp **/
        DATA_SETTINGS_LOCKOUT_TIMESTAMP_LONG(new PersistentDataDefault<>(0L,"all"));

        public PersistentDataDefault<?> DataDefault;

        Experimental(PersistentDataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public enum TVSource {
        US4("US4 (ProIPTV)", true, false, false, "US4"), // Enseo HD3300
        US5("US5 (ProCoax)", true, false, true, TunerProperties.TUNER_TYPE.US5.name()), // Swift Info Proidiom
        US6("US6 (std NA)", true, true, true, TunerProperties.TUNER_TYPE.US6.name()), // Swift Info Coax
        US7("US7 (ProIPTV2)", true, false, true, TunerProperties.TUNER_TYPE.US7.name()), // US7 no longer disabled
        US8("US8 (LG6500)", true, false, false, "STB-6500-UA", "STB-6500-UB", "KEY0719002F", "KEY0718002F"),
        US9("US9 (750E-NA)", true, false, false, "STB-750E-UA"), // basically a new LG65 with casting
        KM4("KM4 (std KM)", false, false, true, TunerProperties.TUNER_TYPE.KM4.name()),
        KM5("KM5 (std KM)", true, false, true, TunerProperties.TUNER_TYPE.KM5.name()),
        KM6("KM6 (LG6500)", true, false, false, "STB-6500-ZA"),
        KM7("KM7 (750E-EU)", true, false, false, "STB-750E-LA"),
        BR2("BR2 (std BR)", false, false, true, TunerProperties.TUNER_TYPE.BRAZIL.name()),
        BR3("BR3 (750E-SA)", true, false, false, "STB-750E-SA"),
        JP0("JP0 (std JP)", false, false, true, TunerProperties.TUNER_TYPE.JAPAN.name()),
        JP1("JP1 (750E-JP)", true, false, false, "STB-750E-JA"),
        IPTV("Unencrypted IPTV", false, false, false, "UIPTV"), // system name not official?
        STB("STB", false, false, false, "STB"), // system name not official?
        CAB("CAB / CableSAT", "HDMI", true, true, false, "CAB"); // system name not official?

        private final String displayName;
        private final String displayNameAlt;
        private final Set<String> systemNames;
        private final boolean useNewLib;
        private final boolean cabCompatible;
        private final boolean followsSwiftProtocol;


        TVSource(String displayName, boolean useNewLib, boolean cabCompatible, boolean followsSwiftProtocol, String... systemNames) {
            this(displayName, displayName, useNewLib, cabCompatible, followsSwiftProtocol, systemNames);
        }

        TVSource(String displayName, String displayNameAlt, boolean useNewLib, boolean cabCompatible, boolean followsSwiftProtocol, String... systemNames) {
            this.displayName = displayName;
            this.displayNameAlt = displayNameAlt;
            this.useNewLib = useNewLib;
            this.cabCompatible = cabCompatible;
            this.followsSwiftProtocol = followsSwiftProtocol;
            Set<String> names = new LinkedHashSet<>();
            for (String name : systemNames) { names.add(name.toUpperCase()); }
            this.systemNames = Collections.unmodifiableSet(names);
        }

        public static TVSource fromName(String name) { return fromName(name, TVSource.STB); }
        public static TVSource fromName(String name, TVSource defaultIfInvalid) {
            for (TVSource source : values()) {
                if (source.name().equalsIgnoreCase(name)) return source;
            }
            return defaultIfInvalid;
        }

        public static TVSource getSource(TunerProperties.TUNER_TYPE tunerType) { return getSource(tunerType.name(), TVSource.STB);}
        public static TVSource getSource(TunerProperties.TUNER_TYPE tunerType, TVSource defaultIfInvalid) { return getSource(tunerType.name(), defaultIfInvalid); }
        public static TVSource getSource(String tunerType) { return getSource(tunerType, TVSource.STB); }
        public static TVSource getSource(String tunerType, TVSource defaultIfInvalid) {
            if (tunerType != null) {
                for (TVSource source : values()) {
                    if (source.systemNames.contains(tunerType.toUpperCase())) return source;
                }
            }
            return defaultIfInvalid; // old code defaulted to STB if unknown
        }


        public String getDisplayName() { return displayName; }
        public String displayNameAlt() { return displayNameAlt; }
        public boolean useNewLib() { return useNewLib; }
        public boolean followsSwiftProtocol() { return followsSwiftProtocol; }
        public boolean isLG() { return isLG6500() || isLG750E(); } // we could explore more feature flag style tuner config classes?
        public boolean isLG6500() { return this == US8 || this == KM6; }
        public boolean isLG750E() { return this == US9 || this == KM7 || this == BR3 || this == JP1; }
        public boolean supportsCasting() { return isLG750E(); }

        public boolean isDetected() {
            TVSource detectedSource = getSource(TunerProperties.TUNER_TYPE.values()[DataMatrix.getInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT)]);
            return this.equals(detectedSource);
        }

        public static TVSource[] getList() {
            TVSource detectedSource = getSource(TunerProperties.TUNER_TYPE.values()[DataMatrix.getInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT)]);
            List<TVSource> sources = new ArrayList<>();
            for(TVSource source : ProductConfig.getAvailableTuners()) {
                if(!detectedSource.followsSwiftProtocol || !source.followsSwiftProtocol || source.equals(detectedSource)) {
                    sources.add(source);
                }
            }
            return sources.toArray(new TVSource[0]);
        }

        public static TVSource[] getCABList() {
            List<TVSource> sources = new ArrayList<>();
            for(TVSource source : ProductConfig.getAvailableTuners()) {
                if(source.cabCompatible && source.useNewLib) {
                    sources.add(source);
                }
            }
            return sources.toArray(new TVSource[0]);
        }

        /**
         * Checks whether any TV source in the list of available TV sources supports the Swift protocol.
         *
         * @return true if at least one TV source follows the Swift protocol; false otherwise
         */
        public static boolean swiftTvSourceCapable() {
            return Arrays.stream(getList()).anyMatch(TVSource::followsSwiftProtocol);
        }

        /**
         * Returns the {@link IChimeraTuner} implementation associated with the TvSource
         * <p>
         * If the TvSource does not have a {@link IChimeraTuner} implementation, {@code null} is returned
         *
         * @param context Android context used to construct the {@link IChimeraTuner} instance
         * @return The {@link IChimeraTuner} instance associated with the TvSource, or null if no such implementation exists
         */
        public IChimeraTuner<? extends IChannel> toTuner(@NonNull Context context) {
            return TunerFactory.INSTANCE.getTuner(context, this);
        }
    }

    public enum TVHDMIOutputResolution {
        OUTPUT_RESOLUTION_480p("480p"),
        OUTPUT_RESOLUTION_720p("720p"),
        OUTPUT_RESOLUTION_1080p("1080p"),
        OUTPUT_RESOLUTION_2160p("2160p"),
        OUTPUT_RESOLUTION_UNKNOWN("Unknown");

        public final String displayName;

        TVHDMIOutputResolution(@NonNull String displayName) {
            this.displayName = displayName;
        }

        @NonNull
        public static TVHDMIOutputResolution fromName(@NonNull String name) {
            try { return valueOf(name); }
            catch (IllegalArgumentException e) { return OUTPUT_RESOLUTION_UNKNOWN; }
        }

        private static final Map<TVHDMIOutputResolution, TunerProperties.OUTPUT_RESOLUTION> TUNER_RESOLUTION_MAP = Map.of(
                OUTPUT_RESOLUTION_480p, TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_720p,
                OUTPUT_RESOLUTION_720p, TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_720p,
                OUTPUT_RESOLUTION_1080p, TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_1080p,
                OUTPUT_RESOLUTION_2160p, TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_2160p,
                OUTPUT_RESOLUTION_UNKNOWN, TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_UNKNOWN
        );

        @NonNull
        public TunerProperties.OUTPUT_RESOLUTION mapToTunerOutputResolution() {
            TunerProperties.OUTPUT_RESOLUTION result = TUNER_RESOLUTION_MAP.get(this);
            return result != null ? result : TunerProperties.OUTPUT_RESOLUTION.OUTPUT_RESOLUTION_UNKNOWN;
        }
    }

    public enum ERPType {
        FullErp,
        Screen,
    }

    public enum ERPMode {
        Disabled,
        Inactivity,
        Absolute_Time
    }

    public enum FanMode {
        Default,
        Limited1,
        Limited2,
        Disabled
    }

	public enum WeatherApplication {
        MATRIX_WEB_APP,
        MY_WEATHER_APP,
        MOJI_WEATHER_APP
	}

    public enum IPTVPlayerType {
        VLC("VLC"),
        ANDROID("ANDROID");

        private final String displayName;

        IPTVPlayerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

    }

    public enum LCDFirmwareUpdateStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        FAIL,
    }

    public static class AssetManagementConstants {
        public static final int WELCOME_ICON_POSITION_CENTER = 0;
        public static final int WELCOME_ICON_POSITION_TOP= 1;
        public static final int WELCOME_ICON_POSITION_GONE = 2;

        public static final int SCREEN_BRIGHTNESS_AFTER_TIMEOUT_30_MIN = 0;
        public static final int SCREEN_BRIGHTNESS_AFTER_TIMEOUT_60_MIN= 1;
        public static final int SCREEN_BRIGHTNESS_AFTER_TIMEOUT_90_MIN = 2;
        public static final int SCREEN_BRIGHTNESS_AFTER_TIMEOUT_120_MIN = 3;
        public static final int SCREEN_BRIGHTNESS_AFTER_TIMEOUT_0_MIN = 4;

        public static final int WORKOUT_SNAPSHOT_SETTING_LOGGED_IN_ONLY = 0;
        public static final int WORKOUT_SNAPSHOT_SETTING_ON= 1;
        public static final int WORKOUT_SNAPSHOT_SETTING_OFF = 2;
    }

    public enum ProjectOne {
        DATA_P1_ENABLE_DEBUG_VIEW_BOOL(new DataDefault<>(false)),
        /**
         * FIXME: THIS IS A BAND-AIDE. REMOVE AFTER FIXING FEATURED WORKOUTS.
         * <p></p>
         *
         * Force a redraw of the featured workouts panel because the state changed unexpectedly in
         * the Go Screen.
         */
        EVENT_FEATURED_WORKOUTS_UPDATED,
        ACTION_LED_COLOR_HEX_PARCELABLE, // LEDItem.class
        DATA_OVERRIDE_LED_COLOR_BOOL(new PersistentDataDefault<>(false, "all")),
        ACTION_HAPTIC_MOTOR_EVENT_PARCELABLE, // HapticMotorEvent.class
        DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL(new PersistentDataDefault<>(true)),
        DATA_HAPTIC_MOTOR_EFFECT_ID_INT(new PersistentDataDefault<>(80)),
        DATA_LED_STYLE_SYSTEM_INT(new DataDefault<>(LEDStyle.DEFAULT.value, "all")),
        DATA_LED_BRIGHTNESS_INT(new PersistentDataDefault<>(100, "all")),
        /**
         * The default bump sensor threshold is {@code -1}. This value falls outside of the valid
         * min and max threshold settings and if set indicates that the value has not been set for
         * the console yet. If this value isn't {@code -1} the console will set to this value if
         * the bump sensor is connected.
         */
        DATA_BUMP_SENSOR_THRESHOLD_INT(new PersistentDataDefault<>(-1)),
        EVENT_LAST_CHANNEL_TV,
        EVENT_PREVIOUS_CHANNEL_TV,
        EVENT_NEXT_CHANNEL_TV,
        EVENT_SETUP_LCD_DEBUG_LAYOUT;

        public DataDefault<?> DataDefault;
        ProjectOne(){
            this.DataDefault = null;
        }

        ProjectOne(DataDefault<?> DataDefault) {
            this.DataDefault = DataDefault;
            this.DataDefault.name = name();
        }
    }

    public static void resetDefault() {
        // backup values to restore
        float accumulatedDistanceKm = DataMatrix.getFloat(General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
        float accumulatedFloors = DataMatrix.getFloat(General.DATA_ACCUMULATED_FLOORS_FLOAT);
        int accumulatedTimeSec = DataMatrix.getInt(General.DATA_ACCUMULATED_TIME_SECONDS_INT);
        float accumulatedDistanceKmAtLastLube = DataMatrix.getFloat(Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT);
        int accumulatedTimeSecAtLastLube = DataMatrix.getInt(Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT);
        int treadmillInclineADCMax = DataMatrix.getInt(Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT);
        int treadmillInclineADCMin = DataMatrix.getInt(Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT);

        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false);
        DataMatrix.putString(SettingKeys.General.DATA_FRAME_TYPE_STRING, "");
        DataMatrix.performDataMatrixDiagnostic(DMSettingKeys.DM_RESET_FACTORY_DEFAULT);
        DataMatrix.sendEvent(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS);

        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/*");
        executeAsRoot.execute();

        // restore backup values
        DataMatrix.putFloat(General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, accumulatedDistanceKm);
        DataMatrix.putFloat(General.DATA_ACCUMULATED_FLOORS_FLOAT, accumulatedFloors);
        DataMatrix.putInt(General.DATA_ACCUMULATED_TIME_SECONDS_INT, accumulatedTimeSec);
        DataMatrix.putFloat(Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT, accumulatedDistanceKmAtLastLube);
        DataMatrix.putInt(Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT, accumulatedTimeSecAtLastLube);
        DataMatrix.putInt(Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, treadmillInclineADCMax);
        DataMatrix.putInt(Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, treadmillInclineADCMin);

    }

    public static void resetFactoryDefault() {
        String bridgeSerialNumber;

        if (ProductConfig.isRetail()) {
            // parker factory default doesn't restore anything
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false);
            DataMatrix.putString(SettingKeys.General.DATA_FRAME_TYPE_STRING, "");
            DataMatrix.performDataMatrixDiagnostic(DMSettingKeys.DM_RESET_FACTORY_DEFAULT);
            DataMatrix.sendEvent(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS);

            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/*");
            executeAsRoot.execute();

        } else {

            // backup values to restore
            float accumulatedDistanceKm = DataMatrix.getFloat(General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
            float accumulatedFloors = DataMatrix.getFloat(General.DATA_ACCUMULATED_FLOORS_FLOAT);
            int accumulatedTimeSec = DataMatrix.getInt(General.DATA_ACCUMULATED_TIME_SECONDS_INT);
            int headphoneACount = DataMatrix.getInt(Hardware.DATA_HEADPHONE_A_COUNT_INT);
            int headphoneBCount = DataMatrix.getInt(Hardware.DATA_HEADPHONE_B_COUNT_INT);
            String consoleSerialNumber = DataMatrix.getString(AssetManagement.DATA_CONSOLE_SERIAL_STRING);
            String frameSerialNumber = DataMatrix.getString(AssetManagement.DATA_FRAME_SERIAL_STRING);
            if (BuildConfig.HAS_BRIDGE_SN) {
                bridgeSerialNumber = DataMatrix.getString(AssetManagement.DATA_BRIDGE_SERIAL_STRING);
            }

            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false);
            DataMatrix.putString(SettingKeys.General.DATA_FRAME_TYPE_STRING, "");
            DataMatrix.performDataMatrixDiagnostic(DMSettingKeys.DM_RESET_FACTORY_DEFAULT);
            DataMatrix.sendEvent(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS);

            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/*");
            executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/shared_prefs/SystemSettings.xml");
            executeAsRoot.execute();

            // restore backup values
            DataMatrix.putFloat(General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, accumulatedDistanceKm);
            DataMatrix.putFloat(General.DATA_ACCUMULATED_FLOORS_FLOAT, accumulatedFloors);
            DataMatrix.putInt(General.DATA_ACCUMULATED_TIME_SECONDS_INT, accumulatedTimeSec);
            DataMatrix.putInt(Hardware.DATA_HEADPHONE_A_COUNT_INT, headphoneACount);
            DataMatrix.putInt(Hardware.DATA_HEADPHONE_B_COUNT_INT, headphoneBCount);
            DataMatrix.putString(AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(consoleSerialNumber));
            DataMatrix.putString(AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(frameSerialNumber));
            if (BuildConfig.HAS_BRIDGE_SN) {
                DataMatrix.putString(AssetManagement.DATA_BRIDGE_SERIAL_STRING, bridgeSerialNumber);
            }
        }

    }
}
