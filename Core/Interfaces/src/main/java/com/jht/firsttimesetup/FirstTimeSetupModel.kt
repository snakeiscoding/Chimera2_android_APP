package com.jht.firsttimesetup

import com.jht.data.ChannelListPreview
import com.jht.data.OperationState
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provides a model for accessing
 */
abstract class FirstTimeSetupModel : BaseModel {
    val DEFAULT_CLUB_ID = -1

    /**
     * Sets tv source in DataMatrix
     */
    abstract fun setTvSourceType(sourceIndex: Int)

    /**
     * Returns list of supported tv source types
     */
    abstract fun getTvSourceList() : List<String>

    /**
     * Get frame type based on serial number (ie Treadmill, Bike, Climbmill, etc.)
     */
    abstract fun getFrameType(frameSerialNumber: String) : String

    /**
     * Get frame config based on serial number
     */
    abstract fun getFrameConfig(frameSerialNumber: String) : String

    /** Get marketing name for given frame */
    abstract fun getFrameMarketingName(frameSerialNumber: String) : String

    /** Get marketing name for given console */
    abstract fun getConsoleMarketingName(): String

    /**
     * Get a list of supported frames
     */
    abstract fun getSupportedFrames(): List<String>

    /**
     * Get a list of supported frames
     */
    abstract fun getFramePrefixes(frameString: String): List<String>

    /**
     * Returns true if the console and frame serial number tuple is a valid combination
     */
    abstract fun isSerialNumberTupleValid(console: String, frame: String): Boolean

    /**
     * Update console serial number
     */
    abstract fun updateConsoleSerialNumber(serialNumber: String)

    /**
     * Update frame serial number
     */
    abstract fun updateFrameSerialNumber(serialNumber: String)

    /**
     * Update bridge serial number
     */
    abstract fun updateBridgeSerialNumber(serialNumber: String)

    /**
     * Reads and sets up frame config
     * Returns true if frame was set
     */
    abstract fun setFrame(frameSerialNumber: String)

    /** Searches for Facility via its ID  returns a [Pair] where
    the first value is [FacilitySearchResponse] and the second value
    is [String] error to display
     **/
    abstract suspend fun searchForFacility(facilityId: String): Pair<FacilitySearchResponse?, String?>

    /**
     * Exports usb settings via [NetworkSettingsImportExport]
     * returns whether the settings were exported successfully
     */
    abstract fun exportWirelessSettings(): Boolean

    /**
     * Imports network settings via wifi using [NetwokrSettingsImportExport]
     * whether the settings were imported successfully
     */
    abstract fun importWirelessSettings() : Boolean

    /**
     * Sets url for bandwidth test
     */
    abstract fun setSpeedTestUrl(url: String)

    /**
     * Sends event to show webview for bandwidth test
     */
    abstract fun sendBandWidthTestEvent()

    abstract fun launchEngineeringScreen()

    abstract fun exitFtsu()

    /**
     * Sets time zone for console
     * @param [timeZone] should come from -> [TimeZoneStrings.getgetIANAString()]
     * @param [position] position of index of newly selected timezone
     */
    abstract fun setTimeZone(timeZone: String, position: Int)

    /**
     * Retrieves list of time zones from resources
     */
    abstract fun getTimeZoneList() : List<String>

    /**
     * Retrieves the IANA ids backing [getTimeZoneList], index for index.
     * Use these, not the display strings, when calling [setTimeZone].
     */
    abstract fun getIanaTimeZoneList() : List<String>

    /**
     * Returns current time zone
     */
    abstract fun getCurrentTimezone() : String

    /**
     * Retrieves last set index for time zone
     */
    abstract fun getCurrentTimeZoneIndex() : Int

    /**
     * True once a time zone has been chosen on this console (by setup, an imported facility or
     * settings). [getCurrentTimeZoneIndex] returns 0 before that, which is a real entry, so use
     * this to tell "never set" apart from "set to index 0".
     */
    abstract fun isTimeZoneSet() : Boolean

    /**
     * Retrieves list of date formats from resources
     */
    abstract fun getDateFormats() : List<String>

    /**
     * Retrieves last set index for date formats
     */
    abstract fun getCurrentDateFormatIndex() : Int

    /**
     * Sets date format based on @param index
     */
    abstract fun setDateFormat(index: Int)

    /**
     * Returns current UI experience that the console will be displayed in.
     */
    abstract fun getUiExperience(): String

    /**
     * Sets UI experience that the console will be displayed in.
     */
    abstract fun setUiExperience(uiExperience: String)

    /**
     * Sets measurement flag from between metric and imperial
     * true means metric
     */
    abstract fun setIsMetricFormat(isMetric: Boolean)

    /**
     * Returns true if the user's preferred measurement is
     * metric
     */
    abstract fun getIsMetricFormat(): Boolean

    /**
     * Returns a list of supported unit types
     */
    abstract fun getUnitFormatTypes() : List<String>

    /**
     * Returns true is format type is 24 hour
     */
    abstract fun getIs24HourTimeFormatType() : Boolean

    /**
     * Sets format type
     */
    abstract fun setTimeFormatType(is24Hour: Boolean)

    /**
     * Gets time formats
     */
    abstract fun getSupportedTimeFormats() : List<String>

    /**
     * Return true if the console type is Onyx
     */
    abstract fun isOnyx() : Boolean

    /**
     * Sets LED color
     */
    abstract fun setLedColor(color: Int)

    abstract fun getLedColor() : Int

    /**
     * Returns true if we support calibration on frame
     */
    abstract fun canCalibrate(): Boolean

    /**
     * Starts calibration if not already calibrating
     */
    abstract fun startCalibration()

    /**
     * Listener to detect calibration state
     */
    abstract val isCalibrating: DataListener<Int>

    /**
     * Returns true is is treadmill
     */
    abstract fun isTreadmill() : Boolean

    /**
     * Returns flag that states whether users can update via OTA
     */
    abstract fun canUpdateOtaFlow() : StateFlow<Boolean?>

    /**
     * Starts background download if update is available
     */
    abstract fun startBackgroundDownload()

    /**
     * installs update
     */
    abstract fun installUpdate()

    /**
     * Returns new software version if available otherwise null
     */
    abstract fun softwareUpdateVersion() : StateFlow<String?>

    /**
     * Checks if there are any over the air updates available for download
     */
    abstract fun checkForOtaUpdates()

    /**
     * Exits to home screen
     */
    abstract fun exitFTSU()

    /**
     * Called when channel up arrow is clicked
     */
    abstract fun tvChannelUp()

    /**
     * Called when tv channel down is clicked
     */
    abstract fun tvChannelDown()

    /**
     * Called when volume up is clicked
     */
    abstract fun tvVolumeUp()

    /**
     * Called when volume down is clicked
     */
    abstract fun tvVolumeDown()

    /**
     * called when ok button clicked
     */
    abstract fun tvOkClick()

    /**
     * Returns list of available channels
     */
    abstract fun getTvChannelList(includeHiddenChannels: Boolean = true) : List<Channel>

    /**
     * Returns default channel name
     */
    abstract fun getDefaultChannel() : String

    /**
     * Called when menu is clicked
     */
    abstract fun tvMenu()

    /**
     * Called when back button is clicked
     */
    abstract fun tvBackButton()

    /**
     * Called when closed caption button is clicked
     */
    abstract fun tvClosedCaption()

    /**
     * Called when up arrow is clicked
     */
    abstract fun tvUpArrow()

    /**
     * Called when down arrow is clicked
     */
    abstract fun tvDownArrow()

    /**
     * Called when right arrow is clicked
     */
    abstract fun tvRightArrow()

    /**
     * Called when left arrow is clicked
     */
    abstract fun tvLeftArrow()

    /**
     * Called when number zero is clicked
     */
    abstract fun tvZeroClick()

    /**
     * Called when number one is clicked
     */
    abstract fun tvOneClick()

    /**
     * Called when number two is clicked
     */
    abstract fun tvTwoClick()

    /**
     * Called when number three is clicked
     */
    abstract fun tvThreeClick()

    /**
     * Called when number four is clicked
     */
    abstract fun tvFourClick()

    /**
     * Called when number five is clicked
     */
    abstract fun tvFiveClick()

    /**
     * Called when number six is clicked
     */
    abstract fun tvSixClick()

    /**
     * Called when number seven is clicked
     */
    abstract fun tvSevenClick()

    /**
     * Called when number eight is clicked
     */
    abstract fun tvEightClick()

    /**
     * Called when number nine is clicked
     */
    abstract fun tvNineClick()

    /**
     * Calls select function on tuner
     */
    abstract fun tvCheckClick()

    /**
     * Called when [-] on remote is clicked
     */
    abstract fun tvDashCLick()

    /**
     * Sets channel based on given @param [channelIndex]
     */
    abstract fun onChannelClicked(channelUuid: String)

    /**
     * Sets custom name for channel
     */
    abstract fun setChannelCustomName(name: String, channelUuid: String)

    /**
     * Hides channel from channel list
     */

    abstract fun toggleHideChannel(channelUuid: String, hide: Boolean)

    /**
     * Sets channel as the default channel
     */
    abstract fun setDefaultChannel(channelUuid: String)

    /**
     * Called to avoid memory leaks
     */
    abstract fun closeTuner()

    /**
     * Checks if tuner is supported by FTSU
     */
    abstract fun hasSupportedTuner(): Boolean

    /**
     * Sets club id on console
     */
    abstract fun setClubId(id: Int)

    abstract fun getClubId(): Int

    /**
     * Preview the TV channel list on CORE for the current TV source & Club ID.
     * Downloads the channel list from CORE and parses it without applying to the tuner.
     *
     * @param facilityId The facility/club ID to fetch channel list for
     * @return A [Flow] emitting [OperationState] with [ChannelListPreview] on success or [Exception] on failure.
     */
    abstract fun previewChannelsFromCore(
        facilityId: Int? = null
    ): Flow<OperationState<ChannelListPreview>>


    abstract fun importChannelsFromCore(
        facilityId: Int? = null
    ): Flow<OperationState<Unit>>

    /**
     * Imports channels from usb
     *
     */
    abstract fun importChannelsFromUsb(onFinished:() -> Unit, onError: () -> Unit, tunerMode: Int = 0)

    /**
     * Exports facility profile to usb
     * User must search for a facility and import
     * profile prior to exporting
     * returns a whether the export was successful
     * @param [profile] --> settings to be exported
     */
    abstract suspend fun exportProfile(profile: ImportFacilityProfile): Boolean

    abstract suspend fun importProfile() : Pair<Boolean, ImportFacilityProfile?>

    abstract fun getUiExperiences(): Map<String, String>

    abstract fun restartFirstTimeSetup()

}


@Serializable
data class FacilitySearchResponse(
    @SerialName("address")
    val address: Address? = null,
    @SerialName("contact")
    val contact: String? = null, //todo find out what kind of object this is
    @SerialName("country")
    val country: Country? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("id")
    val facilityId: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("organization")
    val organization: Organization? = null,
    @SerialName("organizationId")
    val organizationId: Int? = null,
    @SerialName("state")
    val state: State? = null,
    @SerialName("timezone")
    val timezone: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("digitalProducts")
    val digitalProducts: List<String>? = null,
    @SerialName("systemSettings")
    val systemSettings: SystemSettings? = null
)

@Serializable
data class Address(
    @SerialName("city")
    val city: String? = null,
    @SerialName("countryId")
    val countryId: Int? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("facilityId")
    val facilityId: Int? = null,
    @SerialName("id")
    val id: Int? = null,
    @SerialName("postalCode")
    val postalCode: String? = null,
    @SerialName("stateId")
    val stateId: Int? = null,
    @SerialName("street1")
    val street1: String? = null,
    @SerialName("street2")
    val street2: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)

@Serializable
data class Country(
    @SerialName("updatedAt")
    val id: Int? = null,
    @SerialName("iso")
    val iso: String? = null,
    @SerialName("iso3")
    val iso3: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("printableName")
    val printableName: String? = null
)

@Serializable
data class Organization(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("id")
    val id: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)

@Serializable
data class State(
    @SerialName("countryId")
    val countryId: Int? = null,
    @SerialName("id")
    val id: Int? = null,
    @SerialName("iso")
    val iso: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("printableName")
    val printableName: String? = null
)


@Serializable
data class SystemSettings(
    @SerialName("colorLEDDefault")
    val colorLEDDefault: String? = null,
    @SerialName("dateFormat")
    val dateFormat: Int? = null,
    @SerialName("timeFormatIs24")
    val timeFormatIs24: Boolean? = null,
    @SerialName("unitFormatIsMetric")
    val unitFormatIsMetric: Boolean? = null
)
