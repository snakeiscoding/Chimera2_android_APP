package com.jht.firsttimesetup.viewmodels

import com.jht.firsttimesetup.CommonFunctions
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.interfaces.models.SystemModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for general settings
 */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    private val systemModel: SystemModel

) : ThemedViewModel() {

    //UI and functionality differ based on this flag
    var isEdit = false

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())

    val uiState = _uiState.asStateFlow()

    init {
        // Apply the default only on a console with no time zone yet. Applying it every time this
        // ViewModel is created would overwrite the time zone of an imported facility.
        if (!ftsuModel.isTimeZoneSet()) {
            val defaultIndex = CommonFunctions.getDefalutTimeZone()
            ftsuModel.getIanaTimeZoneList().getOrNull(defaultIndex)?.let { iana ->
                ftsuModel.setTimeZone(iana, defaultIndex)
            }
        }
        _uiState.value = _uiState.value.copy(selectedTimeZoneIndex = ftsuModel.getCurrentTimeZoneIndex())
        //updates formats after a language change
        systemModel.currentLanguageIndex.listenAndTrigger {
           updateUI()
        }
    }

    private fun updateUI() {
        _uiState.value = _uiState.value.copy(
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

    override fun stop() {
        systemModel.close()
    }

    /**
     * Sets whether we are in the edit version of general settings
     * or the segment flow.
     */
    fun setIsEdit(isEdit: Boolean) {
        this.isEdit = isEdit
    }

    fun setLedColor(color: Int) {
        if (!isEdit) {
            ftsuModel.setLedColor(color)
        }
        _uiState.value = _uiState.value.copy(selectedLedColor = color)
        //todo visual display
    }

    /**
     * Sets current time zone base on @param [index]
     * @param timeZone -> string representation of timezone
     * @param index -> index for timezone
     * and updates index for ui to display
     */
    fun setTimeZone(timeZone: String, index: Int) {
        if (!isEdit) {
            ftsuModel.setTimeZone(timeZone, index)
        }
        _uiState.value = _uiState.value.copy(selectedTimeZoneIndex = index)
    }

    /**
     * Sets date based on given @param [index]
     */
    fun setDateFormat(index: Int) {
        if (!isEdit) {
            ftsuModel.setDateFormat(index)
        }
        _uiState.value = _uiState.value.copy(selectedDateFormatIndex = index)
    }

    /**
     * Sets unit format to metric if true otherwise
     * imperial
     * @param [isUnitFormatMetric]
     */
    fun setUnitFormat(isUnitFormatMetric: Boolean){
        if (!isEdit) {
            ftsuModel.setIsMetricFormat(isUnitFormatMetric)
        }
        _uiState.value = _uiState.value.copy(isUnitFormatMetric = isUnitFormatMetric)
    }

    /**
     * Sets time format based on
     * @param [is24Hour]
     */
    fun setTimeFormat(is24Hour: Boolean) {
        if (!isEdit) {
            ftsuModel.setTimeFormatType(is24Hour)
        }
        _uiState.value = _uiState.value.copy(is24Hour = is24Hour)
    }

    fun onSaveClicked() {
        val timeZone = _uiState.value.selectedTimeZoneIndex
        val timeZoneString = _uiState.value.selectedTimeZoneString
        val dateFormat = _uiState.value.selectedDateFormatIndex
        val ledColor = _uiState.value.selectedLedColor
        val is24Hour = _uiState.value.is24Hour
        val isMetric = _uiState.value.isUnitFormatMetric
        ftsuModel.setTimeFormatType(is24Hour)
        ftsuModel.setIsMetricFormat(isMetric)
        ftsuModel.setDateFormat(dateFormat)
        ftsuModel.setLedColor(ledColor)
        ftsuModel.setTimeZone(timeZoneString,timeZone)

    }
}

/**
 * Data class representing the UI state for general settings
 */
data class GeneralSettingsUiState(
    val clubId: Int = -1,
    val timeZones: List<String> = emptyList(),
    val selectedTimeZoneIndex: Int = 0,
    val selectedTimeZoneString: String = "",
    val dateFormats: List<String> = emptyList(),
    val selectedDateFormatIndex: Int = 0,
    val isUnitFormatMetric: Boolean = false,
    val unitFormats: List<String> = emptyList(),
    val timeFormats: List<String> = emptyList(),
    val is24Hour: Boolean = false,
    val showLedColorSelection: Boolean = false,
    val selectedLedColor: Int = 0
)