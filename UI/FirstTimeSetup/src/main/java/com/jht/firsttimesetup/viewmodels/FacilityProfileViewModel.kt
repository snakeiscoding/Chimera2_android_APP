package com.jht.firsttimesetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.jht.firsttimesetup.CommonFunctions
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapFacilityToProfile
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.ui.FacilityProfile
import com.jht.interfaces.models.SystemModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Viewmodel for facility profile
 */
@HiltViewModel
class FacilityProfileViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    private val systemModel: SystemModel
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(FacilityProfileUiModel())

    //general settings are used to capture general settings from model and then converted to FacilityProfile
    private var generalSettings = GeneralSettingsUiState()
    private var facility: Facility? = null

    val uiState = _uiState.asStateFlow()

    init {
        updateUI()
        updateSSIDName()
    }

    private fun updateUI() {
        generalSettings = GeneralSettingsUiState(
            timeZones = ftsuModel.getTimeZoneList(),
            selectedTimeZoneIndex = ftsuModel.getCurrentTimeZoneIndex(),
            dateFormats = ftsuModel.getDateFormats(),
            selectedDateFormatIndex = ftsuModel.getCurrentDateFormatIndex(),
            isUnitFormatMetric = ftsuModel.getIsMetricFormat(),
            unitFormats = ftsuModel.getUnitFormatTypes(),
            timeFormats = ftsuModel.getSupportedTimeFormats(),
            is24Hour = ftsuModel.getIs24HourTimeFormatType(),
            showLedColorSelection = ftsuModel.isOnyx()
        )
    }

    /**
     * Sets club id locally for console
     */
    fun setClubId(clubId: Int) {
        ftsuModel.setClubId(clubId)
    }

    /**
     * Imports TV channel list from CORE for the specified club ID.
     * This bypasses the DataMatrix event mechanism to avoid race conditions.
     */
    fun importChannelsFromCore(clubId: Int) {
        viewModelScope.launch {
            ftsuModel.importChannelsFromCore(clubId).collect { /* fire and forget */ }
        }
    }

    /**
     * Sets local console settings based on facility settings
     */
    fun setLocalSettingsToImportedSettings() {
        val facility = facility ?: return
        val timeZoneIndex = facility.timeZoneIndex
        ftsuModel.getIanaTimeZoneList().getOrNull(timeZoneIndex)?.let { ftsuModel.setTimeZone(it, timeZoneIndex) }
        val systemSettings = facility.systemSettings
        systemSettings?.let {
            ftsuModel.setTimeFormatType(it.timeFormatIs24)
            ftsuModel.setIsMetricFormat(it.unitFormatIsMetric)
            val ledColor = it.colorLedDefault.toIntOrNull()
            ledColor?.let { color -> ftsuModel.setLedColor(color) }
            ftsuModel.setDateFormat(systemSettings.dateIndex)
        }
    }

    /**
     * Updates wireless network name and ethernet ipaddress
     */
    private fun updateSSIDName() {
        val profile = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            profile = profile.copy(
                wirelessConnectionName = systemModel.wirelessSSID,
                wiredConnectionName = systemModel.ethernetIpAddress
            )
        )

    }

    /**
     * Sets facility profile details including channel preview data
     */
    fun setFacility(facility: Facility) {
        val localTimeZones = ftsuModel.getTimeZoneList()
        val localDateFormats = ftsuModel.getDateFormats()
        this.facility = facility

        // Facility-level data: shown whether or not the optional systemSettings came with it.
        val channelPreview = facility.channelPreview
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(
                showLedColor = generalSettings.showLedColorSelection,
                timeZone = localTimeZones.getOrNull(facility.timeZoneIndex) ?: "",
                digitalProducts = facility.digitalProducts,
                numberOfChannels = channelPreview.channels.size.toString(),
                defaultChannel = channelPreview.defaultChannel?.name ?: "--",
            )
        )
        facility.systemSettings?.let { profile ->
            val currentProfile = _uiState.value.profile
            val mappedProfile = mapFacilityToProfile(
                profile,
                generalSettings,
                dates = localDateFormats
            )
            _uiState.value = _uiState.value.copy(
                profile = currentProfile.copy(
                    dateFormat = mappedProfile.dateFormat,
                    unitFormat = mappedProfile.unitFormat,
                    ledColor = mappedProfile.ledColor,
                    ledHexString = mappedProfile.ledHexString,
                    timeFormat = mappedProfile.timeFormat,
                )
            )
        }
    }

    override fun stop() {
        systemModel.close()
    }

    fun getTimeZone(): String {
        val index = CommonFunctions.getDefalutTimeZone()
        return ftsuModel.getTimeZoneList()[index]
    }

    fun showCustomUiExperienceDialog() {
        _uiState.value = _uiState.value.copy(
            showUiExperienceSelectionDialog = true
        )
    }

    fun hideCustomUiExperienceDialog() {
        _uiState.value = _uiState.value.copy(
            showUiExperienceSelectionDialog = false
        )
    }

    fun setUiExperience(uiExperience: String) {
        ftsuModel.setUiExperience(uiExperience)
    }

    fun getCurrentUiExperience(): String {
        return ftsuModel.getUiExperience()
    }

    fun getUiExperiences(): Map<String, String> {
        return ftsuModel.getUiExperiences()
    }
}

/**
 * Model class for UI related to facility profile
 */
data class FacilityProfileUiModel(
    val profile: FacilityProfile = FacilityProfile(),
    val hasCustomUiExperience: Boolean = profile.digitalProducts.contains(
        DigitalProduct("Custom UI Experience", true) //todo update name to match official digital product name
    ),
    var showUiExperienceSelectionDialog: Boolean = false,
)