package com.jht.interfaces.models

import android.content.Context
import android.net.wifi.WifiManager
import com.jht.androidcommonalgorithms.network.AddressHelper
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.BuildConfig
import com.jht.chimera.R
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.app.PostWorkoutSurveyGate
import com.jht.chimera.lib.common.config.ConsoleType
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys
import com.jht.mastboard.data.UiEventKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.file.CustomAssetFiles
import com.jht.chimera.lib.common.i18n.LanguageProperty
import com.jht.chimera.lib.common.i18n.LocaleUtil
import com.jht.chimera.lib.common.keypress.KeyPress
import com.jht.chimera.lib.common.keypress.KeyPress.KeyCode
import com.jht.chimera.lib.widget.sound.SystemVolume
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary
import com.jht.chimera.ui.machinesettings.pages.SettingsLockout
import com.jht.data.ISoftwareUpdateStatus
import com.jht.data.SoftwareUpdateState
import com.jht.data.TvTunerInformation
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldText
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam
import com.jht.enums.UiEvent
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.BoolVariableListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatVariableListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.IntVariableListener
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixBoolVariableListener
import com.jht.interfaces.MatrixEventListener
import com.jht.interfaces.MatrixIntValueListener
import com.jht.interfaces.MatrixIntVariableListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.StringValueListener
import com.jht.liberror.ErrorCodeManager
import com.jht.mock.interfaces.MockParcelableValue
import com.jhtp.dapiservice.an.types.ButtonPressType

class MatrixSystemModel(
) : SystemModel() {
    private val manager: CallbackManager = CallbackManager()
    private val keyCodeMES = 3001
    private val keyCodeFTS = 3002
    private val keyCodeAndroid = 2637643

    override val demoRpm: IntVariableListener = MatrixIntVariableListener(manager, HALKeys.DATA_DEMO_RPM_INT)
    override val demoHeartRate: IntVariableListener = MatrixIntVariableListener(manager, HALKeys.DATA_DEMO_HEART_RATE_INT)

    /*Language and Date Time*/
    override val languageList: List<String>
        get() = LanguageProperty.getSpinnerList()
    override fun setLanguage(languageIndex :Int, updateDefault: Boolean) {
        LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), LanguageProperty.getLanguagePropertyFromIndex(languageIndex), updateDefault)
    }
    override val currentLanguageIndex: IntValueListener = MatrixIntValueListener(manager, SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT)
    /* if we need to specify "updateDefault" at some point, having explicit set function might make more sense?
    override val currentLanguageIndex: IntVariableListener = object : MatrixIntVariableListener(manager, SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT) {
        override var value: Int
            get() = super.value
            set(languageIndex) {
                LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), LanguageProperty.getLanguagePropertyFromIndex(languageIndex), false)
            }
    }*/
    override fun resetLanguage() { // from HomeScreenIdleTimer.java
        val selectedLanguage = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT))
        val defaultLanguage = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT))
        if (selectedLanguage != defaultLanguage) {
            LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), defaultLanguage, false)
        }
        // Reset units, clock, and date format.
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL))
        DataMatrix.putInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT, DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT))
        DataMatrix.putBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL))
    }

    /*Custom Themes and Assets*/
    override val logoUri: StringValueListener = CustomLogoListener(manager)
    override val customBackgroundUriList: DataValueListener<List<String>> by lazy { CustomBackgroundListener(manager) }
    override val customVideoUriList: DataValueListener<List<String>> by lazy { CustomBackgroundListener(manager, true) }
    override val customBackgroundTimeSeconds: Int get() = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT)

    // TODO - Do we need frame-type image maps for console-types? or console-type image maps for frame-configs?
    override val connectWatchResourceId: Int
        get() = when (FrameConfigManager.getInstance().frameType) {
            FrameConfigEnum.Tcv_Cycle_Fr, FrameConfigEnum.FrameConfigTcvIndoorCycle_Fx -> R.drawable.connect_watch_tcv
            else -> if (ProductConfig.isProjectOne()) {
                when (FrameConfigManager.getInstance().frameConfig.machineType) {
                    JHTCommonTypes.MachineType.Treadmill -> R.drawable.connect_watch_treadmill_p1
                    JHTCommonTypes.MachineType.UprightBike -> R.drawable.connect_watch_upright_bike_p1
                    JHTCommonTypes.MachineType.RecumbentBike -> R.drawable.connect_watch_recumbent_bike_p1
                    JHTCommonTypes.MachineType.Ascent -> R.drawable.connect_watch_ascent_p1
                    JHTCommonTypes.MachineType.Climbmill -> R.drawable.connect_watch_climbmill_p1
                    else -> 0
                }
            } else {
                when ((ProductConfig.getConsoleType())) {
                    ConsoleType.athena, ConsoleType.athena2 -> R.drawable.connect_watch_athena
                    ConsoleType.chimera_16, ConsoleType.chimera2_16 -> R.drawable.connect_watch_touch
                    ConsoleType.chimera_22, ConsoleType.chimera2_22 -> R.drawable.connect_watch_touchxl
                    // non-onyx vortex has unique images, onyx vortex uses same enclosure as previous onyx, vortex 4k is onyx only
                    ConsoleType.vortex_16_1080p -> R.drawable.connect_watch_touchpro16
                    ConsoleType.vortex_23_1080p -> R.drawable.connect_watch_touchpro24
                    ConsoleType.vortex_lite_16_1080p -> R.drawable.connect_watch_touchessential16
                    ConsoleType.vortex_lite_23_1080p -> R.drawable.connect_watch_touchessential24
                    else -> 0
                }
            }
        }

    override fun close() {
        manager.cancelAllListeners()
    }

    override val errorCode: IntValueListener = object : IntValueListener {
        override val value: Int
            get() = getErrorCode()
        override fun listenAndTrigger(onChange: (Int) -> Unit) {
            manager.addIntListener(HALKeys.ACTION_ERROR_CODE_INT, onChange, false)
            onChange(getErrorCode())
        }
        override fun listen(onChange: (Int) -> Unit) {
            manager.addIntListener(HALKeys.ACTION_ERROR_CODE_INT, onChange, false)
        }
        private fun getErrorCode() = if (ErrorCodeManager.hasActiveErrorCode) ErrorCodeManager.activeErrorCode.code else ERROR_CODE_NONE
    }

    override val isMachineSetup by lazy { MatrixBoolValueListener(manager, SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL) }
    override val serialNumberFrame: String
        get() = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING)
    override val serialNumberConsole: String
        get() = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING)
    override val serialNumberBridge: String
        get() = DataMatrix.getString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING)
    override val machineUUID: DataValueListener<String> = MatrixStringValueListener(manager, DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING)
    override val facilityID = MatrixIntValueListener(manager, SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
    override val accumulatedDistanceKm: Float
        get() = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT)
    override val accumulatedFloors: Float
        get() = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT)
    override val accumulatedTime: Int
        get() = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT)
    override val softwareVersion: String
        get() = DataMatrix.getString(HALKeys.DATA_VERSION_CHIMERA_STRING)
    override val lcbVersion: String
        get() = DataMatrix.getString(HALKeys.DATA_VERSION_LCB_STRING)
    override val lcbType: Int
        get() = DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT)
    override val wirelessIpAddress: String
        get() = AddressHelper.getIPAddress("wlan0")
    override val wirelessMacAddress: String
        get() = AddressHelper.getMACAddressStr("wlan0", true)
    override val wirelessSSID: String
        get() = (ContextUtil.getApplicationContext()?.getSystemService(Context.WIFI_SERVICE) as WifiManager).getConnectionInfo().ssid.replace("\"","")
    override val ethernetIpAddress: String
        get() = AddressHelper.getIPAddress("eth0")
    override val ethernetMacAddress: String
        get() = AddressHelper.getMACAddressStr("eth0", true)
    override val dapiEnvironment = object : DataValueListener<String> {
        private fun fromOrdinal(ordinal: Int): JHTCommonTypes.DAPIEnvironment = JHTCommonTypes.DAPIEnvironment.entries.find { it.ordinal == ordinal } ?: JHTCommonTypes.DAPIEnvironment.production
        override val value: String
            get() = fromOrdinal(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)).name
        override fun listen(onChange: (String) -> Unit) {
            manager.addIntListener(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, {
                onChange(fromOrdinal(it).name)
            }, false)
        }
        override fun listenAndTrigger(onChange: (String) -> Unit) {
            manager.addIntListener(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, {
                onChange(fromOrdinal(it).name)
            }, true)
        }
    }
    override val hasEthernet: Boolean
        get() = BuildConfig.HAS_ETHERNET
    override val isInternetAvailable by lazy { MatrixBoolValueListener(manager, TopBarKeys.DATA_NETWORK_CONNECTION_BOOL) }
    override fun saveLogs() {
        DataMatrix.sendEvent(LoggerKeys.ACTION_SAVE_LOGS)
    }
    override val saveLogsCompleteEvent: EventListener =
        MatrixEventListener(manager, LoggerKeys.EVENT_LOGS_SAVED)

    override fun rebootSystem(reason: String) {
        DataMatrix.putString(HALKeys.ACTION_CONSOLE_REBOOT_STRING, reason)
    }
    override fun enterERP() {
        DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP)
    }

    override val onSplashReset: EventListener = MatrixEventListener(manager, HALKeys.ACTION_SPLASH_RESET)
    override val onFactoryReset: EventListener = MatrixEventListener(manager, SettingKeys.EVENT_RESET_FACTORY_DEFAULTS)
    override val onFrameChange: EventListener = MatrixEventListener(manager, SettingKeys.General.DATA_FRAME_TYPE_STRING)

    override val outOfOrder: BoolVariableListener = MatrixBoolVariableListener(manager, SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL)

    override var screenTimeoutSeconds: Int
        get() = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT)
        set(timeout_sec_int) = DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, timeout_sec_int)
    override var welcomeScreenTimeoutSeconds: Int
        get() = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT)
        set(timeout_sec_int) = DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, timeout_sec_int)

    override val keyGoPressed: BoolValueListener = KeyGoListener(manager)

    override val postWorkoutSurveyUrl: String
        get() = if (DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL)) {
            DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING)
        } else {
            ""
        }

    override val mlPostWorkoutSurveyUrl: String
        get() = PostWorkoutSurveyGate.allowedMlSurveyUrl(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING))

    override val currentLocale: String
        get() = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT)).locale

    override fun sendUiEvent(uiEvent: UiEvent) { DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, uiEvent.name) }

    override fun enablePhysicalHomeKey() { DataMatrix.putBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL, true) }

    override fun reportHomeButtonPress() {
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HOME_BTN_PRESS.name)
    }

    override fun processKeypadCode(code: Int): Boolean {
        // check special retail codes before the general checks
        if (ProductConfig.isRetail()) when (code) {
            1001 -> {
                LaunchAppUtil.launchSystemSetup()
                return true
            }
            2001 -> {
                LaunchAppUtil.launchSystemSetup("engineering")
                return true
            }
            keyCodeMES -> {
                LaunchAppUtil.launchSystemSetup("service")
                return true
            }
        }
        when (code) {
            keyCodeMES -> {
                LaunchAppUtil.launchSystemSetup()
                return true
            }
            keyCodeFTS -> {
                LaunchAppUtil.launchMachineSetup()
                return true
            }
            keyCodeAndroid -> {
                LaunchAppUtil.launchAndroidLauncher()
                return true
            }
        }
        return false
    }

    override fun settingsPasscodeCheck(code: String): Boolean {
        return SettingsLockout.getInstance().checkPasscode(code)
    }
    override val settingsPasscodeField: UserFieldText?
        get() = if (DataMatrix.getString(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_PASSCODE_STRING, "").isNotEmpty()) UserFieldText(
            function = UserFieldFunction.Password,
            minLength = SettingsLockout.MIN_PASSCODE_LENGTH,
            maxLength = SettingsLockout.MAX_PASSCODE_LENGTH,
        ) else null

    override var audioMute = MatrixBoolVariableListener(manager, HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL)
    override val audioVolume: FloatVariableListener = object : FloatVariableListener {
        override var value: Float
            get() = SystemVolume.getInstance().normalizedVolume
            set(value) { SystemVolume.getInstance().setNormalizedVolume(value) }
        override fun listen(onChange: (Float) -> Unit) {
            manager.addIntListener(HALKeys.DATA_SYSTEM_VOLUME_INT, {
                onChange(SystemVolume.normalizeVolume(it))
            }, false)
        }
        override fun listenAndTrigger(onChange: (Float) -> Unit) {
            manager.addIntListener(HALKeys.DATA_SYSTEM_VOLUME_INT, {
                onChange(SystemVolume.normalizeVolume(it))
            }, true)
        }
    }

    override fun setSoftwareUpdateCallback(callback: ISoftwareUpdateStatus) {
        // probably only need one per model instance
        SoftwareUpdateStatus(manager, callback)
    }

    override val tvTunerInformation: ParcelableValueListener<TvTunerInformation> = if (BuildConfig.HAS_TV) TvTunerInfoListener(manager) else MockParcelableValue(TvTunerInformation())

    /**
     * TODO: We want to generify this code to support later tuners that might
     * also want to enforce a wait period before launching the tv apps like the 6500 or any newer tuners in the future.
     * Wo could have possibly a datamatrix key like is SMART_TUNER_CONNECTED and a isSmartTuner check to generify it for any tuner
     * that wants to support this feature. refer to the comments made below in this PR:
     * https://bitbucket.org/johnsonhealthtech/chimera2/pull-requests/6702/overview
     */
    override val tvTunerConnected: BoolValueListener by lazy {
        object : BoolValueListener {
            private fun isLg750E() = SettingKeys.TVSource.fromName(
                DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name)
            ).isLG750E()

            override val value: Boolean
                get() = !isLg750E() || DataMatrix.getBoolean(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL)

            override fun listen(onChange: (Boolean) -> Unit) {
                manager.addBoolListener(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL, { connected ->
                    onChange(!isLg750E() || connected)
                }, false)
            }

            override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
                manager.addBoolListener(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL, { connected ->
                    onChange(!isLg750E() || connected)
                }, true)
            }
        }
    }

    override val isWelcomeTheHomeScreen: Boolean
        get() = !DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL) &&
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)

    override fun getMinMaxInfoForParameter(param: MinMaxParam): MinMaxInfo {
        val dict = MinMaxDictionary.getMinMaxDictionary(param)
        return MinMaxInfo(
            dict.min,
            dict.max,
            dict.defaultValue,
            dict.stepSize,
            dict.precision,
            dict.captionText,
            dict.unitString,
            dict.measurement
        )
    }

    override fun showDebugMenu() {
        if (BuildConfig.DEBUG) {
            DataMatrix.sendEvent(UiServiceKeys.ACTION_SHOW_DEBUG_PANEL_EVENT)
        }
    }

    override val appStoreFilterName: String = ProductConfig.getConsoleType().appStoreFilterName
}

private class TvTunerInfoListener(private val manager: CallbackManager) : ParcelableValueListener<TvTunerInformation> {
    private var ip = ""
    private var mac = ""
    override val value: TvTunerInformation
        get() = TvTunerInformation(true, ip ,mac)
    override fun listenAndTrigger(onChange: (TvTunerInformation) -> Unit) {
        listen(onChange)
        DataMatrix.sendEvent(TVKeys.ACTION_GET_TUNER_NETWORK_DETAILS) // this is more of a RPC, so we need to kick it
    }
    override fun listen(onChange: (TvTunerInformation) -> Unit) {
        manager.addStringListener(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING, {
            ip = it
            onChange(value)
        }, false)
        manager.addStringListener(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, {
            mac = it
            onChange(value)
        }, false)
    }
}

private class SoftwareUpdateStatus(manager: CallbackManager, private val callback: ISoftwareUpdateStatus) {
    private var isDownloading = false
    private var isReady = false
    init {
        manager.addBoolListener(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, {
            isDownloading = it
            updateStatus()
        }, true)
        manager.addBoolListener(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, {
            isReady = it
            updateStatus()
        }, true)
    }
    private fun updateStatus() {
        if (isReady) {
            callback(SoftwareUpdateState.Ready, null)
        } else if (isDownloading) {
            callback(SoftwareUpdateState.Download, null)
        } else {
            callback(SoftwareUpdateState.Idle, null)
        }
    }
}

/**
 * Helper class to wrap the logic of returning a custom logo from
 * /data/jht/settings/skin/theme/logo.
 *
 * @return  a String Value Listener describing the Uri String of the custom logo
 */
private class CustomLogoListener(private val manager: CallbackManager) : StringValueListener {
    val key = SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL
    override val value: String
        get() = updateValue(DataMatrix.getBoolean(key))

    override fun listen(onChange: (String) -> Unit) {
        manager.addBoolListener(key, { onChange(updateValue(it)) }, false)
    }

    override fun listenAndTrigger(onChange: (String) -> Unit) {
        manager.addBoolListener(key, { onChange(updateValue(it)) }, true)
    }

    private fun updateValue(useCustomLogo: Boolean) : String {
        if (useCustomLogo) {
            return CustomAssetFiles.getCustomLogo()
        }
        //Return an empty String if custom logos are disabled
        return ""
    }
}

/**
 * Helper class to wrap the logic of returning custom backgrounds from
 * /data/jht/settings/skin/theme/welcome.
 *
 * @return  a Data Value Listener describing the List of Uri Strings of the custom backgrounds
 */
private class CustomBackgroundListener(private val manager: CallbackManager, private val video: Boolean = false) : DataValueListener<List<String>> {
    val key = SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL
    override val value: List<String>
        get() = updateValue(DataMatrix.getBoolean(key))

    override fun listen(onChange: (List<String>) -> Unit) {
        manager.addBoolListener(key, { onChange(updateValue(it)) }, false)
    }

    override fun listenAndTrigger(onChange: (List<String>) -> Unit) {
        manager.addBoolListener(key, { onChange(updateValue(it)) }, true)
    }

    private fun updateValue(useCustomBackground: Boolean) : List<String> {
        return if (video) {
            //Return empty list if custom backgrounds are disabled
            if (useCustomBackground) CustomAssetFiles.getCustomVideos() else listOf()
        } else {
            //Return the default background if custom backgrounds are disabled
            if (useCustomBackground) CustomAssetFiles.getCustomBackgrounds() else CustomAssetFiles.getDefaultBackground()
        }
    }
}

private class KeyGoListener(private val manager: CallbackManager) : BoolValueListener {
    val key = HALKeys.EVENT_KEYPRESS_BITMAP_INT
    override val value: Boolean
        get() = updateValue(DataMatrix.getInt(key))

    override fun listen(onChange: (Boolean) -> Unit) {
        manager.addIntListener(key, { onChange(updateValue(it)) }, false)
    }

    override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
        manager.addIntListener(key, { onChange(updateValue(it)) }, true)
    }

    private fun updateValue(keyMask: Int) : Boolean {
        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) return false
        val keyCode = KeyPress.getKeyCode(keyMask)
        val keyState = KeyPress.getKeyState(keyMask)
        return (keyCode == KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN)
    }
}
