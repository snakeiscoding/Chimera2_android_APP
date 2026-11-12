package com.jht.chimera.service.consoleconfig

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.jht.androidcommonalgorithms.network.AddressHelper
import com.jht.androidcommonalgorithms.util.FileUtils
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.SystemPropertyUtil
import com.jht.chimera.lib.common.VersionHelper
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.dapi.DapiUtil
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.chimera.lib.webapp.WebAppHelper
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.tiers.CacheControl
import com.jht.datamatrixlib.DataMatrix
import com.jht.oauthlib.auth.BksCredentialProvider
import com.jht.services.InlineConsoleService
import com.jht.services.ConsoleServiceState
import com.jht.services.ConsoleServiceStateCallback
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.jetbrains.annotations.NonNls
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.Scanner

private const val TAG: @NonNls String = "ConsoleConfigService"

@SuppressLint("SdCardPath")
private val configFile = File("/sdcard/Download/consoleConfig.xml") // NON-NLS

@SuppressLint("SdCardPath")
private val themeFile = File("/sdcard/Download/chimera_theme.zip") // NON-NLS


//Helper constants to create json objects for the post message KEY-VALUE pair
private const val KEY: @NonNls String = "key"
private const val VALUE: @NonNls String = "value"

//JSON array category names for the console config upload
private const val CONFIGURATION: @NonNls String = "configuration"
private const val SOFTWARE_VERSIONS: @NonNls String = "software_versions"

/** [ConsoleConfigService] handles remote console settings
 * [url] parameter is for test fixture overrides
 * */
class ConsoleConfigService(private val url: String? = null, context: Context, onStateChange: ConsoleServiceStateCallback): InlineConsoleService(context, onStateChange) {
    private val mDataMatrix = DataMatrix()
    private var cache: CacheControl = CacheControl()
    private val settingsMap = getSettingsMap()
    private val coreSecret: String by lazy {
        BksCredentialProvider(context).getString(BksCredentialProvider.ALIAS_CORE_CONSOLE_CONFIG_PASSWORD)
    }
    private val credentialProvider = BksCredentialProvider(context.applicationContext)

    private var cachedTunerMacAddress = ""
    private val facilityServices = FacilityConfigHelper(context) // at some point facility services might replace the old config service
    private fun checkFacilityServices() {
        facilityServices.checkFacilityConfigEndpoints(
            iDigitalProductResponse = { secaEnabled -> DataMatrix.putBoolean(DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL, secaEnabled) }
        )
    }

    //Console Config Upload Variables
    private var originalSettings = JSONObject()
    private var uploadedOnBoot = false

    companion object {
        const val ID = TAG // UUID or unique friendly name or just use the log tag?
    }
    override val id: String = ID // do we need instance to be able to return its ID?

    /**
     * Used for testing the class.  During testing we need to link to a local web server and
     * set the context because this will not be running as a service.
     *
     * @param url       The url to the test web server.
     * @param context   Context for loading assets.
     */
    @VisibleForTesting
    constructor(url: String? = null, context: Context) : this(url, context, {_,_->})

    override fun start() {
        setServiceState(ConsoleServiceState.STARTED)
        monitorForEventsThatTriggerConsoleConfig()
        setServiceState(ConsoleServiceState.READY)
    }

    override fun stop() {
        mDataMatrix.finish()
        facilityServices.close()
        setServiceState(ConsoleServiceState.STOPPED)
    }

    override fun onWebReady(ready: Boolean) {
        if (ready) {
            updateConsoleTheme()
            //Only need to send once on boot
            if (!uploadedOnBoot) {
                uploadConsoleConfig()
            }
        }
    }

    override fun onWebPushData(data: Bundle) {
        /* not fully tested yet
        when (data.getString("type")) {
            "fs_welcome_screen_video" -> {
                checkFacilityServices() // should we just process the payload or do the full pull?
            }
        }*/
    }

    private var clubId = 0
    private fun monitorForEventsThatTriggerConsoleConfig() {
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value: Int? ->
            if (value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                updateConsoleTheme()
            }
        }, false)

        clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
        mDataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, { s: Int? ->
                if (clubId != s) {
                    clubId = s!!
                    val executeAsRoot = ExecuteAsRoot()
                    executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/theme/*")
                    executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/settings/theme")
                    executeAsRoot.addCommand("sync")
                    executeAsRoot.execute()
                }
                updateConsoleTheme()
            },
            false
        )
        mDataMatrix.createStringAdapter(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, { tunerMacAddress: String ->
            if (cachedTunerMacAddress == tunerMacAddress) return@createStringAdapter
            cachedTunerMacAddress = tunerMacAddress
            uploadConsoleConfig()
        }, false)
    }

    private fun updateConsoleTheme() {
        if (ProductConfig.isRetail()) return  // Disabled for retail until retail implements using the settings
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)
            && DataMatrix.getBoolean(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL)
            && AddressHelper.isNetworkConnected(context)) {
            Thread { this.loadConsoleConfig() }.also { it.name = "loadConsoleConfig" }.start()
            checkFacilityServices()
        }
    }

    @VisibleForTesting
    fun loadConsoleConfig() {
        val url = if (this.url == null) WebAppHelper.getURL("consoleconfiguration.jfit.co") else WebAppHelper.addURLParameters(this.url) // NON-NLS
        if (url == null) {
            return
        }

        try {
            val connection = url.openConnection() as HttpURLConnection
            val saltToken = credentialProvider.getString("HOTP_SALT_TOKEN")
            WebAppHelper.addStandardAuthorization(connection, "00000000000000", saltToken)
            cache.addHeaders(connection)
            if (responseCodeValid(connection.getResponseCode()) && contentTypeValid(connection.contentType)) {
                cache.updateFields(connection)
                val md5 = connection.getHeaderField("md5")
                if (FileUtils.copyFile(connection.getInputStream(), configFile) && configSignatureValid(md5)) {
                    ConsoleEvent.logEvent("ConsoleConfig downloaded")
                    parseConfig(FileInputStream(configFile))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking console config ", e)
        }
    }

    private fun configSignatureValid(md5: String?): Boolean {
        return md5 == null || md5.isEmpty() || md5 == FileUtils.computeMd5(configFile)
    }

    private fun responseCodeValid(responseCode: Int): Boolean {
        return responseCode == HttpURLConnection.HTTP_OK
    }

    private fun contentTypeValid(contentType: String): Boolean {
        return contentType.startsWith("text/xml") || contentType.startsWith("application/xml")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseConfig(stream: InputStream?) {
        var setting: ConfigSetting? = null
        val parser = getXMLParser(stream)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name.compareTo("Skin") == 0) {
                    parseConsoleSkin(parser)
                } else {
                    setting = settingsMap.get(parser.name.lowercase())
                }
            } else if (eventType == XmlPullParser.TEXT && setting != null) {
                setting.update(parser.text)
                setting = null
            }
            eventType = parser.next()
        }
    }

    @Throws(XmlPullParserException::class)
    private fun getXMLParser(stream: InputStream?): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")
        return parser
    }

    /**
     * Console Configuration - anything added here will automatically be added to the JSON
     * that is sent in a POST request below
     */
    private fun getSettingsMap(): HashMap<String?, ConfigSetting?> {
        val settings = HashMap<String?, ConfigSetting?>()
        if (ProductConfig.isRetail()) {
            // settings.put("enableRetailSurvey".toLowerCase(Locale.ENGLISH), new BooleanConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL)); // NON-NLS
            // settings.put("retailSurveyURL".toLowerCase(Locale.ENGLISH), new StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING)); // NON-NLS
            settings.put("treoURL".lowercase(), StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_TREO_URL_STRING)) // NON-NLS
            settings.put("workoutSnapshotSetting".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_WORKOUT_SNAPSHOT_SETTING_INT)) // NON-NLS
        } else {
            settings.put("welcomeImageRefreshRate".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT)) // NON-NLS
            settings.put("enableGymkit".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL)) // NON-NLS
            settings.put("enableSamsungWatch".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)) // NON-NLS
            settings.put("enableMFi".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL)) // NON-NLS
            settings.put("enableDapiFallback".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_ENABLE_DAPI_FALLBACK_BOOL)) // NON-NLS

            settings.put("gymkitLevel".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_MARKETING_GYMKIT_LEVEL_INT)) // NON-NLS
            settings.put("enableWelcomeAds".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_MARKETING_WELCOME_SCREEN_ADS_BOOL)) // NON-NLS
            settings.put("enableFeatureAds".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_MARKETING_FEATURE_CAROUSEL_ADS_BOOL)) // NON-NLS

            // at some point we may need to switch to defined values instead of ordinals, but we could do that internally (at this time we have not been asked to expose theme uri yet)
            settings.put("uiSystem".lowercase(), IntegerConfigSetting(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT)) // NON-NLS

            settings.put("enableIFITOnyx".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ONYX_ENABLED_BOOL)) // NON-NLS
            if (ProductConfig.ifitByDefault()) {
                // If not LCG, has iFIT enabled by default. (See CGC-15581).
                settings.put("enableIFITAthena".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL)) // NON-NLS
                settings.put("enableIFITTreadmill".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITElliptical".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITAscent".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ASCENT_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITLowerBodyAscent".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITHybridBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITUprightBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITRecumbentBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITStepper".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_BOOL)) // NON-NLS
                settings.put("enableIFITClimbmill".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_BOOL)) // NON-NLS
            } else {
                // If LCG, requires iFIT be enabled via CORE. (See CGC-15581).
                settings.put("lcgEnableIFITTreadmill".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITElliptical".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITAscent".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_ASCENT_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITLowerBodyAscent".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITHybridBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITUprightBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITRecumbentBike".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITStepper".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_TB_BOOL)) // NON-NLS
                settings.put("lcgEnableIFITClimbMill".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_TB_BOOL)) // NON-NLS
            }

            settings.put("persistentLogs".lowercase(), PersistentLogSetting(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL)) // NON-NLS
            settings.put("enableYMCA360".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL)) // NON-NLS
            settings.put("enableHulu".lowercase(), BooleanConfigSetting(SettingKeys.Application.DATA_HULU_PLUS_ENABLED_BOOL)) // NON-NLS
            settings.put("enableMileHighRunClub".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL)) // NON-NLS
            settings.put("enableSurveyMonkey".lowercase(), BooleanConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL)) // NON-NLS
            settings.put("postWorkoutSurveyURL".lowercase(), StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING)) // NON-NLS
            settings.put("mlAfterExerciseSurvey".lowercase(), StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING)) // NON-NLS
            settings.put("midWorkoutSurveyURL".lowercase(), StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING)) // NON-NLS
            settings.put("enableEGYM".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL)) // NON-NLS
            settings.put("setEnvironmentEGYM".lowercase(), StringConfigSetting(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING)) // NON-NLS
            settings.put("enableLogsMCU".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL)) // NON-NLS
            settings.put("enableLogsCSAFE".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL)) // NON-NLS
            settings.put("enableLogsURL".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) // NON-NLS
            settings.put("enableLogsLCB".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL)) // NON-NLS
            settings.put("enableLogsGymkit".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL)) // NON-NLS
            settings.put("enableLogsPTP".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_PTP_BOOL)) // NON-NLS
            settings.put("enableLogsRFID".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_LOG_RFID_BOOL)) // NON-NLS
            settings.put("enableDapiAnalytics".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_ENABLE_DAPI_ANALYTICS_BOOL)) // NON-NLS
            settings.put("workoutSnapshotSetting".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_WORKOUT_SNAPSHOT_SETTING_INT)) // NON-NLS  This has been added to the Console Config Documentation
            settings.put("enableCrashReporting".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_CRASH_MONITORING_BOOL)) // NON-NLS
            settings.put("disableCrashEndpoints".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_ENDPOINT_BOOL)) // NON-NLS
            settings.put("crashIncidentEndpointOverride".lowercase(), StringConfigSetting(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_STRING)) // NON-NLS
            settings.put("crashLogEndpointOverride".lowercase(), StringConfigSetting(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_STRING)) // NON-NLS

            // CGC-13856 adds the following:
            settings.put("enableCustomLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL)) // NON-NLS
            settings.put("enableXIDLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL)) // NON-NLS
            settings.put("enableXIDRegister".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL)) // NON-NLS
            settings.put("enableRFIDLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_RFID_LOGIN_BOOL)) // NON-NLS
            settings.put("enableShowWelcomeScreen".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)) // NON-NLS
            settings.put("enableAssetManagement".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL)) // NON-NLS
            settings.put("enableCustomBackground".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL)) // NON-NLS
            settings.put("enableCustomBackgroundColor".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL)) // NON-NLS
            settings.put("enableCustomLogo".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL)) // NON-NLS
            settings.put("enableCustomWelcomeScreen".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL)) // NON-NLS
            settings.put("enableWorkoutTracking".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL)) // NON-NLS
            settings.put("welcomeScreenIconPosition".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT)) // NON-NLS
            settings.put("enableAllowGuest".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) // NON-NLS
            settings.put("enablePhysicalGo".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) // NON-NLS
            settings.put("enableDemoMode".lowercase(), BooleanConfigSetting(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) // NON-NLS
            settings.put("enableTreadSense".lowercase(), BooleanConfigSetting(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL)) // NON-NLS
            settings.put("enableShowOutOfOrder".lowercase(), BooleanConfigSetting(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL)) // NON-NLS
            settings.put("enableAutoDateTime".lowercase(), BooleanConfigSetting(SettingKeys.General.DATA_AUTO_DATE_TIME_BOOL)) // NON-NLS
            settings.put("enableAutoUpdate".lowercase(), BooleanConfigSetting(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL)) // NON-NLS
            settings.put("enableBarcodeLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)) // NON-NLS
            settings.put("enableEmailLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL)) // NON-NLS
            settings.put("enableFitlinxxLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL)) // NON-NLS
            settings.put("enableWalletLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL)) // NON-NLS
            settings.put("enablePhoneNumberLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL)) // NON-NLS
            settings.put("enableForceDowngradeUpdates".lowercase(), BooleanConfigSetting(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL)) // NON-NLS
            settings.put("enableCustomRFIDOnly".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL)) // NON-NLS
            settings.put("enableDefaultBaidu".lowercase(), BooleanConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_USE_BAIDU_BOOL)) // NON-NLS
            settings.put("enableIgnoreAllErrors".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL)) // NON-NLS
            settings.put("enableIgnoreBLevelErrors".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_IGNORE_B_LEVEL_ERRORS_BOOL)) // NON-NLS
            settings.put("enableIgnoreInclineErrors".lowercase(), BooleanConfigSetting(SettingKeys.Hardware.DATA_IGNORE_INCLINE_ERRORS_BOOL)) // NON-NLS

            // Added for CGC-17491
            settings.put("enableConcierge".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL))

            // Added for AppStore CGC-21709
            settings.put("enableAppStore".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL))

            // Added for CGC-15895
            settings.put("enableFeaturedWorkoutsNonAthena".lowercase(), StringConfigSetting(SettingKeys.Workout.DATA_FEATURED_WORKOUT_NON_ATHENA_STRING)) // NON-NLS
            settings.put("enableFeaturedWorkoutsAthena".lowercase(), StringConfigSetting(SettingKeys.Workout.DATA_FEATURED_WORKOUT_ATHENA_STRING)) // NON-NLS
            settings.put("defaultWebpageURL".lowercase(), StringConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING)) // NON-NLS
            settings.put("customLoginName".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_NAME_STRING)) // NON-NLS
            settings.put("customLoginNameLabel".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_LABEL_STRING)) // NON-NLS
            settings.put("customLoginInputType".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_INPUT_TYPE_INT)) // NON-NLS
            settings.put("customLoginPasswordLabel".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING)) // NON-NLS
            settings.put("customLoginPasswordInputType".lowercase(), IntegerConfigSetting(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT)) // NON-NLS

            // Added for CGC-21408
            settings.put("primaryLoginMethod".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_PRIMARY_LOGIN_METHOD_STRING)) // NON-NLS

            // Added for CGC-15954
            settings.put("colorLEDDefault".lowercase(), ColorHexConfigSetting(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT))
            settings.put("colorLEDRunning".lowercase(), ColorHexConfigSetting(SettingKeys.Experimental.DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT))
            settings.put("colorLEDPaused".lowercase(), ColorHexConfigSetting(SettingKeys.Experimental.DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT))
            settings.put("colorLEDSummary".lowercase(), ColorHexConfigSetting(SettingKeys.Experimental.DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT))
            //settings.put("colorLEDConcierge".toLowerCase(Locale.ENGLISH), new ColorHexConfigSetting(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT)); // set from concierge API
            settings.put("brightnessLED".lowercase(), IntegerConfigSetting(SettingKeys.ProjectOne.DATA_LED_BRIGHTNESS_INT))
            settings.put("enableLEDColorOverride".lowercase(), BooleanConfigSetting(SettingKeys.ProjectOne.DATA_OVERRIDE_LED_COLOR_BOOL))

            // Added for CGC-21158
            settings.put("timeZone".lowercase(), IntegerConfigSetting(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT))
            settings.put("timeFormatIs24".lowercase(), BooleanConfigSetting(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL))
            settings.put("dateFormat".lowercase(), IntegerConfigSetting(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT))
            settings.put("unitFormatIsMetric".lowercase(), BooleanConfigSetting(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL))

            // Added for CGC-24205
            settings.put("enableQrLogin".lowercase(), BooleanConfigSetting(SettingKeys.AssetManagement.DATA_ENABLE_QR_LOGIN_BOOL)) // NON-NLS

            // Added for CGC-24281: redirect app scope for the OAuth device-authorization (QR login) flow
            settings.put("jumpDeviceRedirectApp".lowercase(), StringConfigSetting(SettingKeys.AssetManagement.DATA_JUMP_DEVICE_REDIRECT_APP_STRING)) // NON-NLS
        }

        return settings
    }

    @Throws(IOException::class)
    private fun parseConsoleSkin(parser: XmlPullParser) {
        val currentSkinVersion = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_SKIN_VERSION_STRING)
        val parserSkinVersion = parser.getAttributeValue("", "Version")
        if (!parserSkinVersion.equals(currentSkinVersion, ignoreCase = true)) {
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_SKIN_VERSION_STRING, parserSkinVersion)
            downloadConsoleSkin(parser.getAttributeValue("", "url"))
            extractConsoleSkin()
            DataMatrix.sendEvent(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY)
        }
    }

    @Throws(IOException::class)
    private fun downloadConsoleSkin(skinURL: String?) {
        val skinUri = skinURL?.toUri()
        FileUtils.getStreamFromUri(context, skinUri).use { inputStream ->
            if (themeFile.exists()) {
                themeFile.delete()
            }
            FileUtils.copyFile(inputStream, themeFile)
            ExecuteAsRoot.runSyncCommand()
        }
    }

    private fun extractConsoleSkin() {
        if (themeFile.exists()) {
            try {
                moveSkinToPackage(unzipSkin())
            } catch (e: ZipException) {
                Log.e(TAG, "Exception extracting console skin zip package, clearing theme assets ", e)
                //If an empty zip file is received from console config (which throws a zip exception),
                // we should remove the current existing theme assets
                clearThemePackage()
            }
            ExecuteAsRoot.fixFilesInPackage("com.jht.chimera")
        }
    }

    private fun clearThemePackage() {
        val executeAsRoot = ExecuteAsRoot()
        executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/theme/*")
        executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/settings/theme")
        executeAsRoot.addCommand("sync")
        executeAsRoot.execute()
    }

    private fun moveSkinToPackage(destination: File) {
        val executeAsRoot = ExecuteAsRoot()
        executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/theme/*")
        executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/settings/theme")
        executeAsRoot.addCommand("cp -rf " + destination.absolutePath + "/* /data/data/com.jht.chimera/files/settings/theme")
        executeAsRoot.addCommand("sync")
        executeAsRoot.execute()
    }

    @SuppressLint("SdCardPath")
    @Throws(ZipException::class)
    private fun unzipSkin(): File {
        val zipFile = ZipFile(themeFile) //NON-NLS
        val destination = File("/sdcard/Download/chimera_theme")
        FileUtils.delete(destination)
        destination.mkdirs()
        zipFile.extractAll(destination.absolutePath)
        return destination
    }

    /** Send Console Management via POST
    * uploads the finished JSON with all settings keys to core - triggered by hitting the home
    * button on engineering (with changes), and on boot
    */
    fun uploadConsoleConfig() {
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)){  // don't send if the machine has not been setup yet
            Thread {
                val newSettings = createConsoleConfigJSON()
                if (checkIfSettingsChanged(newSettings) || !uploadedOnBoot) {
                    Log.d(TAG, "sending console config via POST")
                    try {
                        sendConsoleConfigPOST(
                            parseTotpToken(totpTokenPOST).trim { it <= ' ' },
                            newSettings.toString()
                        )
                        uploadedOnBoot = true
                    } catch (e: Exception) {
                        Log.e(TAG, "sending console config ERROR: ", e)
                    }
                }
                originalSettings = newSettings
            }.also { it.name = "uploadConsoleConfig" }.start()
        }
    }

    private val totpTokenPOST: String
        /**
         * sends a POST Request with the secret access code to get a response with a totp token
         * needed to send the console config to core
         * @return response containing a totp token
         */
        get() {
            val configUrlPOST =
                DapiUtil.getDapiConfigUrlPOST(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)) + "machine-auth"
            //Log.d(TAG, "url for getting totp token via POST: " + configUrlPOST);
            val url: URL?
            var conn: HttpURLConnection? = null
            var httpResponseMessage = ""
            val postParams = JSONObject()
            try {
                postParams.put("cmapi_secret", coreSecret)
                url = URL(configUrlPOST)
                conn = url.openConnection() as HttpURLConnection?
                conn!!.setRequestMethod("POST")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setReadTimeout(10000) //maybe don't need?
                conn.setConnectTimeout(15000) //maybe don't need?
                conn.setDoInput(true)
                conn.setDoOutput(true)

                // Send the data
                val bufferedOutputStream = BufferedOutputStream(conn.getOutputStream())
                bufferedOutputStream.write(postParams.toString().toByteArray(StandardCharsets.UTF_8))
                bufferedOutputStream.flush()
                bufferedOutputStream.close()

                val responseCode = conn.getResponseCode()
                when (responseCode) {
                    HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> httpResponseMessage = getResponseData(conn)
                    else -> Log.e(TAG, "getTotpTokenPOST response code: $responseCode")
                }
            } catch (e: IOException) {
                Log.e(TAG, "getTotpTokenPOST ERROR: ", e)
            } catch (e: JSONException) {
                Log.e(TAG, "getTotpTokenPOST ERROR: ", e)
            } finally {
                conn?.disconnect()
            }
            //Log.d(TAG, "httpResponseMessage: " + httpResponseMessage);
            return httpResponseMessage
        }

    /**
     * helper method to collect the POST response in a digestible format
     * @param httpURLConnection current connection being used
     * @return response from POST request
     */
    private fun getResponseData(httpURLConnection: HttpURLConnection): String {
        val resultStringBuilder = StringBuilder()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))
            var currentLine: String?
            while ((bufferedReader.readLine().also { currentLine = it }) != null) {
                resultStringBuilder.append(currentLine).append("\n")
            }
            bufferedReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
            resultStringBuilder.append("")
        }
        return resultStringBuilder.toString()
    }

    /**
     * parse the response from the POST request to get the totp token to use in the new POST to send
     * the console config to core
     * @param message original POST request response parsed through the helper method
     * @return totp token
     */
    private fun parseTotpToken(message: String): String {
        val splitToken = message.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        var totpToken = splitToken[1]
        totpToken = totpToken.trim { it <= ' ' }
        totpToken = totpToken.replace("\"".toRegex(), "")
        totpToken = totpToken.replace("\\}".toRegex(), "")
        return totpToken
    }

    /**
     * sends the console configuration in json format to core
     * @param token totp token needed to authenticate with core
     */
    private fun sendConsoleConfigPOST(token: String?, settingsString: String) {
        val configUrlPOST =
            DapiUtil.getDapiConfigUrlPOST(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)) + "machine-config"
        //Log.d(TAG, "url for sending config via POST: " + configUrlPOST);
        val url: URL?
        var conn: HttpURLConnection? = null
        try {
            url = URL(configUrlPOST)
            conn = url.openConnection() as HttpURLConnection?
            conn!!.setRequestMethod("POST")
            conn.setRequestProperty("Authorization", token)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setReadTimeout(10000) //maybe don't need?
            conn.setConnectTimeout(15000) //maybe don't need?
            conn.setDoInput(true)
            conn.setDoOutput(true)
            // Send the data
            val bufferedOutputStream = BufferedOutputStream(conn.getOutputStream())
            bufferedOutputStream.write(settingsString.toByteArray(StandardCharsets.UTF_8))
            bufferedOutputStream.flush()
            bufferedOutputStream.close()
            val responseCode = conn.getResponseCode()
            when (responseCode) {
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {}
                else -> Log.e(TAG, "sendConsoleConfigPOST response code: $responseCode")
            }
        } catch (e: IOException) {
            Log.e(TAG, "sendConsoleConfigPOST ERROR: ", e)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * rewritten console config service to match the documentation given by alpha team:
     * {
     * "machine_id": "12341234-1234-1234-1234-123412341234",
     * "club_id": 1963,
     * "frame_serial_number": "fsn123123",
     * "console_serial_number": "csn123123",
     * "software_configuration_class": "chimera",
     * "configuration": [
     * { "key": "DATA_KEY", "value": "..." }
     * ],
     * "software_versions": [
     * { "software_key": "NAME", "version": "1.0.0" }
     * ]
     * }
     * @return json structured like above
     */
    private fun createConsoleConfigJSON(): JSONObject {
        val consoleConfigJSON = JSONObject()
        var softwareConfigClass = ProductConfig.platform.toString().lowercase()
        if (ProductConfig.isProjectOne()) {
            softwareConfigClass = ProductConfig.product.toString().lowercase()
        }
        try {
            consoleConfigJSON.put("machine_id", DapiUtil.getMachineId())
            consoleConfigJSON.put("club_id", clubId)
            consoleConfigJSON.put("frame_serial_number", DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING))
            consoleConfigJSON.put("console_serial_number", DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING))
            consoleConfigJSON.put("software_configuration_class", softwareConfigClass)

            //populate configuration settings array
            val settingsArray = JSONArray()
            // get ALL keys from keys page default list, this json object's gonna be huuuuuuuuuuuuge
            for (setting in SettingKeys.sDataDefaultList) {
                when (setting.dataType()) {
                    "integer" -> {
                        //NON-NLS
                        val value = DataMatrix.getInt(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value.toString()))
                    }
                    "string" -> {
                        //NON-NLS
                        val value = DataMatrix.getString(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value))
                    }
                    "boolean" -> {
                        //NON-NLS
                        val value = DataMatrix.getBoolean(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value.toString()))
                    }
                    "double" -> {
                        //NON-NLS
                        val value = DataMatrix.getDouble(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value.toString()))
                    }
                    "long" -> {
                        //NON-NLS
                        val value = DataMatrix.getLong(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value.toString()))
                    }
                    "float" -> {
                        //NON-NLS
                        val value = DataMatrix.getFloat(setting.name)
                        settingsArray.put(createKeyValue(setting.name, value.toString()))
                    }
                    else -> {}
                }
            }

            // Add console network information
            settingsArray.put(createKeyValue("CONSOLE_ETHERNET_MAC_ADDRESS", AddressHelper.getMACAddressStr("eth0", true)))
            settingsArray.put(createKeyValue("CONSOLE_WIFI_MAC_ADDRESS", AddressHelper.getMACAddressStr("wlan0", true)))
            settingsArray.put(createKeyValue("TUNER_MAC_ADDRESS", cachedTunerMacAddress))
            settingsArray.put(createKeyValue("TUNER_AP_MAC_ADDRESS", SystemPropertyUtil.get("persist.jht.tuner.ap_mac")))

            // add date time key-value pair
            settingsArray.put(createKeyValue("CONSOLE_CURRENT_DATE_TIME_UTC", ZonedDateTime.now().toString()))

            consoleConfigJSON.put(CONFIGURATION, settingsArray)

            val versionsArray = JSONArray()
            // get chimera version
            val softwareVersion = JSONObject()
            softwareVersion.put("software_key", ProductConfig.platform.toString())
            softwareVersion.put("version", BuildConfig.SoftwareVersion)
            versionsArray.put(softwareVersion)
            // get firmware versions
            val firmwareVersionsString = VersionHelper.getFirmwareVersions("", "\n")
            val scanner = Scanner(firmwareVersionsString)
            while (scanner.hasNextLine()) {
                val temp = scanner.nextLine().split(":".toRegex()).dropLastWhile { it.isEmpty() }
                val firmwareVersion = JSONObject()
                firmwareVersion.put("software_key", temp[0].trim { it <= ' ' })
                firmwareVersion.put("version", temp[1].trim { it <= ' ' })
                versionsArray.put(firmwareVersion)
            }
            // get apk versions
            val pm = context.packageManager
            val packages = AppManager.getInstance().getApkPackages()
            for (p in packages) {
                try {
                    val info = pm.getPackageInfo(p, 0)
                    val apkVersion = JSONObject()
                    info.applicationInfo?.let {
                        apkVersion.put("software_key", pm.getApplicationLabel(it))
                    }
                    apkVersion.put("version", info.versionName)
                    versionsArray.put(apkVersion)
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "package not found: $p")
                }
            }
            // put versions in main configuration JSON
            consoleConfigJSON.put(SOFTWARE_VERSIONS, versionsArray)
        } catch (e: JSONException) {
            Log.e(TAG, "createConsoleConfigJSON error", e)
        }

        // too big to see all of it on one line, use the print statements within the switch cases to see all the key/value pairs
        //Log.d(TAG, "Final console config JSON: " + consoleConfigJSON);
        return consoleConfigJSON
    }

    /**
     * Creates JSON object for key value pair of settings
     */
    private fun createKeyValue(name: String?, value: String?): JSONObject {
        val `object` = JSONObject()
        try {
            `object`.put(KEY, name)
            `object`.put(VALUE, value)
        } catch (_: Exception) {
        }
        return `object`
    }

    /**
     * Comparison function to see if any settings have changed.
     * We want to ignore a few keys that frequently change like accumulated values or timestamps
     * to reduce frequency of sending.
     */
    private fun checkIfSettingsChanged(newSettings: JSONObject) : Boolean{
        //Check if any settings have changed
        if(originalSettings.has(CONFIGURATION)) {
            val config = originalSettings.getJSONArray(CONFIGURATION)
            val newConfig = newSettings.getJSONArray(CONFIGURATION)
            for (i in 0 until config.length()) {
                val newSetting = newConfig.get(i).toString()
                if (newSetting != config.get(i).toString()) {
                    //We want to ignore accumulated totals and the current time change since that is
                    //frequently changing
                    if (!newSetting.contains("DATA_ACCUMULATED") &&
                        !newSetting.contains("CURRENT_DATE")
                    ) {
                        return true
                    }
                }
            }
        }
        //Check if any software versions changed
        if(originalSettings.has(SOFTWARE_VERSIONS)) {
            val config = originalSettings.getJSONArray(SOFTWARE_VERSIONS)
            val newConfig = newSettings.getJSONArray(SOFTWARE_VERSIONS)
            for (i in 0 until config.length()) {
                if (newConfig.get(i).toString() != config.get(i).toString()) {
                    return true
                }
            }
        }
        return false
    }
}
