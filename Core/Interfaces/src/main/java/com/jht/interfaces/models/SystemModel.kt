package com.jht.interfaces.models

import com.jht.data.ISoftwareUpdateStatus
import com.jht.data.TvTunerInformation
import com.jht.data.UserFieldText
import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam
import com.jht.enums.UiEvent
import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.BoolVariableListener
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatVariableListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.IntVariableListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.StringValueListener

/** Integer value for no error code */
const val ERROR_CODE_NONE: Int = 0
//

/**
 * Model for accessing various system information
 */
abstract class SystemModel : BaseModel {
    /////////////Language and Date Time//////////////////////////////
    /** Provides a list of the languages available on the system */
    abstract val languageList: List<String>

    /** Provides the index of the currently selected system language relative to the language list */
    abstract val currentLanguageIndex: IntValueListener

    /** Provides a means to change the currently active language on the system */
    abstract fun setLanguage(languageIndex: Int,updateDefault: Boolean = false)

    /** Provides a means to reset the language on the system to its default value */
    abstract fun resetLanguage()

    /////////////UI Logos and Backgrounds////////////////////////////

    /** Provides the current brand logo being used by the system as a URI string */
    abstract val logoUri: StringValueListener

    /** Provides a list of custom background assets being used by the system as a List of URI strings */
    abstract val customBackgroundUriList: DataValueListener<List<String>>

    /** Provides a list of custom background videos being used by the system as a List of URI strings */
    abstract val customVideoUriList: DataValueListener<List<String>>

    /** Provides the time that the custom backgrounds should cycle */
    abstract val customBackgroundTimeSeconds: Int

    /** Provides the resource ID for the connect to smart watch image. Returns 0 if no resource ID has been defined for machine*/
    abstract val connectWatchResourceId: Int

    /////////////////////////////////////////////////////////////////

    /** Provides pressed state of physical GO key (TENTATIVE - GO button handling may need more discussion) */
    abstract val keyGoPressed: BoolValueListener

    /** Event emitted when splash screen performs reset */
    abstract val onSplashReset: EventListener

    /** Event emitted when console performs factory reset */
    abstract val onFactoryReset: EventListener

    /** Event emitted if the frame has changed */
    abstract val onFrameChange: EventListener // TODO - should we just make the frame type a listener? do we need a generic frame config class for this?

    /////////////Product Dashboard Information///////////////////////

    /** Provides the current/most recent error code thrown by the machine as an int, a value of [ERROR_CODE_NONE] indicates no active error code */
    abstract val errorCode: IntValueListener // TODO - is this the best spot for error codes?

    /** Indicates if machine has been setup */
    abstract val isMachineSetup: BoolValueListener

    /** Provides the serial number of the frame of the machine as a string */
    abstract val serialNumberFrame: String

    /** Provides the serial number of the console of the machine as a string */
    abstract val serialNumberConsole: String

    /** Provides the serial number of the bridge (mastboard) as a string */
    abstract val serialNumberBridge: String

    /** Unique identifier for this machine */
    abstract val machineUUID: DataValueListener<String>

    /** Provides the club id of the console as an int */
    abstract val facilityID: DataValueListener<Int>

    /** Provides the total accumulated distance of the lifetime of the treadmill in km as a float */
    abstract val accumulatedDistanceKm: Float

    /** Provides the total accumulated floors of the lifetime of the climbmill as a float */
    abstract val accumulatedFloors: Float

    /** Provides the total accumulated run time of the lifetime of the treadmill in seconds as an int */
    abstract val accumulatedTime: Int

    /** Provides the current chimera software version as a string */
    abstract val softwareVersion: String

    /** Provides the current lcb version as a string */
    abstract val lcbVersion: String

    /** Provides the current lcb type as an int */
    abstract val lcbType: Int

    /** Provides the current wireless ip address as a string */
    abstract val wirelessIpAddress: String

    /** Provides the current wireless mac address as a string */
    abstract val wirelessMacAddress: String

    /** Provides the current wireless SSID as a string */
    abstract val wirelessSSID: String

    /** Provides the current ethernet ip address as a string */
    abstract val ethernetIpAddress: String

    /** Provides the current ethernet mac address as a string */
    abstract val ethernetMacAddress: String

    /** Provides the current dapi environment as a string */
    abstract val dapiEnvironment: DataValueListener<String> // TODO - keep as string or get the enum in here? need to be listener?

    /** Indicates if console has ethernet as an option */
    abstract val hasEthernet: Boolean

    /** Indicates if console believes it has internet access available */
    abstract val isInternetAvailable: DataValueListener<Boolean>

    /** Provides means to save logs */
    abstract fun saveLogs()

    /** Listener to tell when logs have been saved */
    abstract val saveLogsCompleteEvent: EventListener

    /** Provides means to reboot the console */
    abstract fun rebootSystem(reason: String = "")

    /** Provides means to enter ERP */
    abstract fun enterERP()

    /** Indicates if system is out-of-order */
    abstract val outOfOrder: BoolVariableListener

    /** Simulated RPM for DEMO mode */
    abstract val demoRpm: IntVariableListener

    /** Simulated Heart Rate for DEMO mode */
    abstract val demoHeartRate: IntVariableListener

    /** Provides a means to check and set that it is safe to update */
//    abstract var isSafeToUpdateBool: Boolean

    /** Processes the various key codes entered in the dashboard */
    abstract fun processKeypadCode(code: Int): Boolean
    /** if Passcode required to access console settings, this is input requirements, return null otherwise */
    abstract val settingsPasscodeField: UserFieldText?
    /** Checks the settings passcode, return true if passes the check */
    abstract fun settingsPasscodeCheck(code: String): Boolean

    /////////////////////////////////////////////////////////////////

    /** Provides a means to get the amount of time until the screen times out in seconds */
    abstract var screenTimeoutSeconds: Int

    /** Provides a means to get the amount of time until the welcome screen times out in seconds */
    abstract var welcomeScreenTimeoutSeconds: Int

    /** Provides URL for post-workout survey or empty string if not enabled */
    abstract val postWorkoutSurveyUrl: String

    /** Base URL of the ML post workout survey, empty when not configured or not on a host allowed JUMP tokens */
    abstract val mlPostWorkoutSurveyUrl: String

    /** Locale of the currently selected system language, e.g. `en_US` or `zh_TW` */
    abstract val currentLocale: String

    /** TEMPORARY - Sends Onyx/P1 LCD/LED events */
    @Deprecated("Short term only", ReplaceWith("TBD system rework"))
    open fun sendUiEvent(uiEvent: UiEvent) {
    }

    /**
     * Lets the console's physical Home key go to the splash screen while no workout is running
     *
     * The key handler clears this after one press and at every workout stop, so call it on each resume.
     */
    abstract fun enablePhysicalHomeKey()

    /** Records a Home button press for console analytics */
    abstract fun reportHomeButtonPress()

    /** Get/set audio mute */
    abstract val audioMute: BoolVariableListener

    /** Get/set audio volume as a range of [0, 1], system might clear mute on volume set */
    abstract val audioVolume: FloatVariableListener

    /** Set a callback for the status of software updates. Callback should fire upon being added with the current state. */
    abstract fun setSoftwareUpdateCallback(callback: ISoftwareUpdateStatus)

    /** Provides TV tuner information (If console has no tuner, [TvTunerInformation.hasTuner] will be false) */
    abstract val tvTunerInformation: ParcelableValueListener<TvTunerInformation>

    /** True when the LG 750E STB WebSocket connection is open and the tuner is ready to use */
    abstract val tvTunerConnected: BoolValueListener

    /** Provides if the welcome screen is the home screen (for idle timeout behavior) */
    abstract val isWelcomeTheHomeScreen: Boolean

    /** Gets min/max info for the specified parameter */
    abstract fun getMinMaxInfoForParameter(param: MinMaxParam): MinMaxInfo

    /** Show a debug menu using the UI service. Only run on debug builds. */
    abstract fun showDebugMenu()

    abstract val appStoreFilterName : String
}
