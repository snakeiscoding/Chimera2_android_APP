package com.jht.updated

import com.android.build.api.variant.Variant
import com.jht.updated.bundler.JHTFlavor

sealed class BaseConfig(allConfigs: MutableSet<BaseConfig>, val name: String, var comment: String? = null) {
    /*
     * When a config value is initialized, ensure it is added to the set of all configs. This throws
     * an error if the config cannot be added because it already exists.
     */
    init {
        if (!allConfigs.add(this)) {
            error("Encountered duplicate config: $name")
        }
    }

    /**
     * The name for a config value must be unique. Therefore, when checking equality only the names
     * need comparing.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as BaseConfig

        return name == other.name
    }

    /**
     * Because the name is the only indicator of unique-ness, it is the only thing necessary to
     * include in the hashCode determination.
     */
    override fun hashCode(): Int {
        return name.hashCode()
    }
}

/**
 * Class containing all defined build configs. A class is required to ensure a unique instance is
 * created and maintained for each build flavor.
 */
class DefaultConfigs {
    /**
     * The set of all build configuration fields. This is populated during the initialization of
     * this class using the default value provided during instantiation.
     */
    private val allConfigs = mutableSetOf<BaseConfig>()

    /** Class to use when creating a nullable String build config field. */
    inner class StringConfig(name: String, var value: String?, comment: String? = null): BaseConfig(allConfigs, name, comment)

    /** Class to use when creating a nullable Boolean build config field. */
    inner class BooleanConfig(name: String, var value: Boolean?, comment: String? = null): BaseConfig(allConfigs, name, comment)

    /** Class to use when creating a non-null Boolean build config field. */
    inner class NonNullBooleanConfig(name: String, var value: Boolean, comment: String? = null): BaseConfig(allConfigs, name, comment)

    /** Class to use then creating a nullable Int build config field. */
    inner class IntConfig(name: String, var value: Int?, comment: String? = null): BaseConfig(allConfigs, name, comment)

    val appTheme = StringConfig("APP_THEME", "ChimeraTheme")
    val autoUpdateBool = BooleanConfig("DATA_AUTO_UPDATE_BOOL", true)
    val bleManufacturerId = IntConfig("BLE_MANUFACTURER_ID", 0x07C4)
    val brandId = IntConfig("BRAND_ID", 0)
    val checkTMOverspeed = BooleanConfig("CHECK_TM_OVERSPEED", false)
    val clubIdInt = IntConfig("DATA_CLUB_ID_INT", 60)
    val dapiMachineIDReady = BooleanConfig("DAPIMachineIdReady", true)
    val defaultWebPageString = StringConfig("DATA_DEFAULT_WEBPAGE_STRING", "http://www.matrixfitness.com")
    val enableXidLoginBool = BooleanConfig("DATA_ENABLE_XID_LOGIN_BOOL", true)
    val energySaverModeEnum = IntConfig("DATA_ENERGY_SAVER_MODE_ENUM_INT", 0)
    val exposeBumpSensorThresholdChanges = BooleanConfig("EXPOSE_BUMP_SENSOR_THRESHOLD_CHANGES", false, "Expose the bump sensor trigger threshold change settings in Engineering Debug iff this is true.")
    val ftmsAdvertisingEnabledBool = BooleanConfig("DATA_FTMS_ADVERTISING_ENABLED_BOOL", true)
    val ftmsDataAlwaysStream = BooleanConfig("FTMS_DATA_ALWAYS_STREAM", false)
    val ftmsEnableControlPoint = BooleanConfig("FTMS_ENABLE_CONTROL_POINT", false)
    val gymkitEnabledDefault = BooleanConfig("DATA_GYMKIT_ENABLED_BOOL", false)
    val hasBridgeSN = NonNullBooleanConfig("HAS_BRIDGE_SN", false, "Determines whether the frame needs a Bridge Serial Number displaying. See CGC-15320 for more information.")
    val hasBR2Tuner = BooleanConfig("HAS_BR2_TUNER", true)
    val hasBTSink = BooleanConfig("HAS_BT_SINK", true)
    // fixme: The default here should likely be true
    val hasEthernet = BooleanConfig("HAS_ETHERNET", false)
    val hasGymkit = BooleanConfig("HAS_GYMKIT", false)
    val hasIFit = BooleanConfig("HAS_IFIT", false)
    val hasTV = BooleanConfig("HAS_TV", true)
    val hdmiInterface = StringConfig("HDMI_INTERFACE", null)
    val headphonesScanner = BooleanConfig("HEADPHONES_SCANNER", true)
    val heartRateScanner = BooleanConfig("HEART_RATE_SCANNER", true)
    val masterVolumeIntV2 = IntConfig("DATA_MASTER_VOLUME_V2_INT", 10)
    val maxCooldownTimeInt = IntConfig("DATA_MAX_COOLDOWN_TIME_INT", 5)
    val maxVolume = IntConfig("MAX_VOLUME", 30)
    val maxWorkoutTimeMinInt = IntConfig("DATA_MAX_WORKOUT_TIME_MIN_INT", 120)
    val mediaPlayerScanner = BooleanConfig("MEDIA_PLAYER_SCANNER", true)
    val newOCProtection = BooleanConfig("NEW_OC_PROTECTION", true)
    val nightlyBuildURL = StringConfig("NIGHTLY_BUILD_URL", null, "Loads the URL, when not null, so that nightly updates can be downloaded via the Check for Updates button. See CGC-15340 for more information.")
    val powerLimitingEnabled = BooleanConfig("DATA_POWER_LIMITING_BOOL", false)
    val recentWorkoutAppLimit = IntConfig("DATA_RECENT_WORKOUT_APP_LIMIT_2_INT", 3)

    val samsungWatchCompatible = BooleanConfig("SAMSUNG_WATCH_COMPATIBLE", false)
    val showClimbmillIRCZSafteyScreen = BooleanConfig("SHOW_CLIMBMILL_IR_CZ_SAFETY_SCREEN", true)
    val supportsSaveConsoleSerial = BooleanConfig("SUPPORTS_SAVE_CONSOLE_SERIAL", true)
    val supportsSSO = BooleanConfig("SUPPORTS_SSO", false)
    val treadSenseEnabledBool = BooleanConfig("DATA_TREAD_SENSE_ENABLED_BOOL", true)
    val useCustomBackgroundBool = BooleanConfig("DATA_USE_CUSTOM_BACKGROUND_BOOL", true)
    val useCustomBackgroundColorBool = BooleanConfig("DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL", true)
    val useCustomLogoBool = BooleanConfig("DATA_USE_CUSTOM_LOGO_BOOL", true)
    val useCustomWelcomeScreenBool = BooleanConfig("DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL", true)
    val clearIqiyiCachePeriodically = BooleanConfig("CLEAR_IQIYI_CACHE_PERIODICALLY", true)

    /** These are the target market values, they are annoying to deal with.*/
    var targetMarket: Market = Market.Commercial
        set (value) {
            // Set this value
            field = value

            // Update the related values
            marketCommerial.value = value == Market.Commercial
            marketLightCommercial.value = value == Market.LightCommercial
            marketRetail.value = value == Market.Retail
        }
    private val marketCommerial = BooleanConfig("MARKET_COMMERCIAL", targetMarket == Market.Commercial)
    private val marketLightCommercial = BooleanConfig("MARKET_LIGHT_COMMERCIAL", targetMarket == Market.LightCommercial)
    private val marketRetail = BooleanConfig("MARKET_RETAIL", targetMarket == Market.Retail)

    //----------------------------------------------------------------------------------------------
    // Platform configurations must be applied first, then ui customizations, then product
    // configurations. You cannot apply the product configuration, then do ui or platform. Likewise,
    // you cannot apply ui customizations and then apply platform configurations.
    //
    // This section adds logic to prevent incorrect usage.
    //----------------------------------------------------------------------------------------------
    private var platformAllowed = true
    private var uiAllowed = true
    private var productAllowed = true

    private fun applyPlatformCustomizations(customizations: () -> Unit) {
        if (!platformAllowed) error("Cannot apply additional platform customizations.")
        platformAllowed = false
        customizations()
    }

    /** Apply the UI customizations and restrict any future customizations. */
    private fun applyUICustomizations(customizations: () -> Unit) {
        if (!uiAllowed) error("Cannot apply UI customizations.")
        uiAllowed = false
        platformAllowed = false
        customizations()
    }

    /** Apply the product customizations and restrict future customizations. */
    private fun applyProductCustomizations(customizations: () -> Unit) {
        if (!productAllowed) error("Cannot apply product customizations.")

        // Disallow additional product customization attempts.
        productAllowed = false

        // Disallow additional ui customization attempts.
        uiAllowed = false

        // Disallow additional platform customization attempts.
        platformAllowed = false
        customizations()
    }

    /** Apply the customizations for the chimera platform. */
    private fun customizeForPlatformChimera() {
        applyPlatformCustomizations {
            println("Customizing for chimera platform...")
            brandId.value = 0
            gymkitEnabledDefault.value = true
            hasEthernet.value = true
            hasGymkit.value = true
            hasIFit.value = true
            hdmiInterface.value = "CAMERA"
            mediaPlayerScanner.value = false
            newOCProtection.value = false
            nightlyBuildURL.value = "https://doore.aws-jhtna.net/chimera/manifest/0.0.0.0/update_ota.xml"
            samsungWatchCompatible.value = true
            supportsSSO.value = true
            targetMarket = Market.Commercial
        }
    }

    /**
     * Apply the customizations for the lcgui platform.
     *
     * FIXME: LCGUI is Chimera running on Parker. Therefore, LGUI should be the product and Parker
     *  should be the platform.
     */
    private fun customizeForPlatformLCGUI() {
        applyPlatformCustomizations {
            println("Applying customizations for LCGUI platform...")
            brandId.value = 0
            ftmsDataAlwaysStream.value = false
            ftmsEnableControlPoint.value = false
            gymkitEnabledDefault.value = false
            hasBTSink.value = true
            hasEthernet.value = true
            hasIFit.value = false
            hdmiInterface.value = "RK3399"
            maxVolume.value = 30
            mediaPlayerScanner.value = false
            newOCProtection.value = false
            supportsSSO.value = true
            targetMarket = Market.Commercial
            nightlyBuildURL.value = "https://doore.aws-jhtna.net/lowcostgui/manifest/0.0.0.0/update_ota.xml"
        }
    }

    /** Apply the customizations for the Parker platform which includes LCG, Vision, Parker 1, Parker 3. */
    private fun customizeForPlatformParker() {
        applyPlatformCustomizations {
            println("Customizing for Parker platform...")
            clubIdInt.value = 0
            ftmsDataAlwaysStream.value = false
            ftmsEnableControlPoint.value = false
            gymkitEnabledDefault.value = false
            hasBTSink.value = false
            hasEthernet.value = false
            hdmiInterface.value = "RK3399"
            masterVolumeIntV2.value = 5
            maxVolume.value = 15
            powerLimitingEnabled.value = true
            recentWorkoutAppLimit.value = 0
            supportsSSO.value = true
            useCustomBackgroundBool.value = false
            useCustomBackgroundColorBool.value = false
            useCustomLogoBool.value = false
            useCustomWelcomeScreenBool.value = false
            targetMarket = Market.Commercial
            nightlyBuildURL.value = "https://doore.aws-jhtna.net/parker/manifest/0.0.0.0/update_ota.xml"
        }
    }



    /** Apply the customizations for the matrix user interface. */
    private fun customizeForUiMatrix() {
        applyUICustomizations {
            println("Customizing for the matrix ui...")
            appTheme.value = "ChimeraTheme"
        }
    }

    /** Apply the customizations for the vision 600e product. */
    private fun customizeForProductVision600e() {
        applyProductCustomizations {
            println("Customizing for Vision600e product...")
            autoUpdateBool.value = false
            brandId.value = 1
            clubIdInt.value = 0
            defaultWebPageString.value = "http://world.visionfitness.com"
            enableXidLoginBool.value = false
            ftmsAdvertisingEnabledBool.value = false
            targetMarket = Market.LightCommercial
        }
    }

    /** Apply the customizations for the t600xe product. */
    private fun customizeForProductT600xe() {
        applyProductCustomizations {
            println("Customizing for T600xe product...")
            autoUpdateBool.value = false
            brandId.value = 0
            enableXidLoginBool.value = false
            ftmsAdvertisingEnabledBool.value = false
            hasTV.value = true
            targetMarket = Market.LightCommercial
        }
    }

    /** Apply the customizations for the project one product. */
    private fun customizeForProductProjectOne() {
        applyProductCustomizations {
            println("Customizing for Project One product...")
            brandId.value = 0
            dapiMachineIDReady.value = true
            exposeBumpSensorThresholdChanges.value = true
            ftmsDataAlwaysStream.value = false
            ftmsEnableControlPoint.value = false
            gymkitEnabledDefault.value = true
            hasBTSink.value = true
            hasBridgeSN.value = true
            hasEthernet.value = true
            hasGymkit.value = true
            hasIFit.value = true
            maxVolume.value = 30
            newOCProtection.value = false
            nightlyBuildURL.value = "https://doore.aws-jhtna.net/projectone/manifest/0.0.0.0/update_ota.xml"
            samsungWatchCompatible.value = true
            supportsSSO.value = true
            targetMarket = Market.Commercial
        }
    }

    /** Apply the customizations for the parker product. */
    private fun customizeForProductParkerMain() {
        applyProductCustomizations {
            println("Customizing for parker_main product...")
            brandId.value = 5
            checkTMOverspeed.value = true
            defaultWebPageString.value = "http://www.google.com"
            energySaverModeEnum.value = 1
            ftmsDataAlwaysStream.value = true
            ftmsEnableControlPoint.value = true
            hasBR2Tuner.value = false
            hasIFit.value = true
            hasTV.value = false
            maxCooldownTimeInt.value = 20
            maxWorkoutTimeMinInt.value = 480
            showClimbmillIRCZSafteyScreen.value = true
            supportsSaveConsoleSerial.value = false
            treadSenseEnabledBool.value = false
            nightlyBuildURL.value = "https://doore.aws-jhtna.net/parker/manifest/0.0.0.0/update_ota.xml"
            targetMarket = Market.Retail
        }
    }

    /** Add all build config fields to the */
    fun applyConfigs(variant: Variant, flavor: JHTFlavor, softwareVersion: String) {
        println("Applying variant specific configs...")
        variant.putStringBuildConfig("SoftwareVersion", softwareVersion)
        variant.putStringBuildConfig("UPDATE_EQUIPMENT", flavor.equipment)

        // WARNING: When applying the changes, the order these are applied is:
        //      Platform -> UI -> Product
        // Which coincidentally is the order they appear in the build files
        println("Apply build config customizations for ${flavor.flavorCapitalized}...")
        when (flavor) {
            JHTFlavor.ChimeraMainMatrixChimera -> {
                customizeForPlatformChimera()
                customizeForUiMatrix()
            }
            JHTFlavor.ParkerMainMatrixParker -> {
                customizeForPlatformParker()
                customizeForUiMatrix()
                customizeForProductParkerMain()
            }
            JHTFlavor.Vision600eMatrixParker -> {
                customizeForPlatformParker()
                customizeForUiMatrix()
                customizeForProductVision600e()
            }
            JHTFlavor.T600xeMatrixParker -> {
                customizeForPlatformParker()
                customizeForUiMatrix()
                customizeForProductT600xe()
            }
            JHTFlavor.ChimeraMainMatrixLcgui -> {
                customizeForPlatformLCGUI()
                customizeForUiMatrix()
            }
            JHTFlavor.ProjectoneMatrixChimera -> {
                customizeForPlatformChimera()
                customizeForUiMatrix()
                customizeForProductProjectOne()
            }
        }

        println("Applying all build configs....")
        allConfigs.forEach {
            when (it) {
                is BooleanConfig -> variant.putBooleanBuildConfig(it.name, it.value, it.comment)
                is IntConfig -> variant.putIntBuildConfig(it.name, it.value, it.comment)
                is NonNullBooleanConfig -> variant.putNonNullBooleanBuildConfig(it.name, it.value, it.comment)
                is StringConfig -> variant.putStringBuildConfig(it.name, it.value, it.comment)
            }
        }
    }
}