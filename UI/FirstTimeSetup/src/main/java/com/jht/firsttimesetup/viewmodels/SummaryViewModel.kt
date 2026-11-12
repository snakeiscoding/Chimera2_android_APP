package com.jht.firsttimesetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapGeneralSettingsToFacilityProfile
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.ui.ClassMapper
import com.jht.firsttimesetup.ui.Summary
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Viewmodel for summary ui
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    private val systemModel: SystemModel,
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val uiBaseModel: UIBaseModel
) : ThemedViewModel() {


    /**
     * Object holds all general settings
     * It is fetched from ftsu model and converted to SummaryUi
     * object based on needed properties
     */
    private var generalSettings = GeneralSettingsUiState()
    private val _uiState = MutableStateFlow(SummaryUi())

    internal val uiState = _uiState.asStateFlow()

    /**
     * Using channels for any one time updates to export state
     */
    private val _exportState = Channel<ExportProfileState>(Channel.BUFFERED)

    val exportState: Flow<ExportProfileState> = _exportState.receiveAsFlow()

    override fun stop() {
        systemModel.close()
    }

    var shouldUpdate: Boolean? = null

    init {
        //allows for location once language changes
        systemModel.currentLanguageIndex.listenAndTrigger {
            updateUi()
        }
    }

    /**
     * Sets facility for imported profile
     */
    fun setFacility(facility: Facility?) {
        facility?.let {
            ftsuModel.getIanaTimeZoneList().getOrNull(it.timeZoneIndex)?.let { iana ->
                ftsuModel.setTimeZone(iana, it.timeZoneIndex)
            }
        }
        facility?.systemSettings?.let { settings ->
            ftsuModel.setTimeFormatType(settings.timeFormatIs24)
            ftsuModel.setIsMetricFormat(settings.unitFormatIsMetric)
            ftsuModel.setDateFormat(settings.dateIndex)
            ftsuModel.setLedColor(settings.colorLedDefault.toIntOrNull() ?: -1)
        }
        updateUi()
    }

    fun onExportProfile() {
        viewModelScope.launch(ioDispatcher) {
            _exportState.send(ExportProfileState.EXPORTING)
            val profile = mapGeneralSettingsToFacilityProfile(generalSettings)
            val isExportSuccessful = ftsuModel.exportProfile(profile)
            if (isExportSuccessful) {
                _exportState.send(ExportProfileState.EXPORT_SUCCESS)
            } else {
                _exportState.send(ExportProfileState.EXPORT_FAILED)
            }
        }
    }

    /**
     * Updates general settings and network state
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun updateUi() {
        generalSettings = generalSettings.copy(
            clubId = systemModel.facilityID.value,
            timeZones = ftsuModel.getTimeZoneList(),
            selectedTimeZoneIndex = ftsuModel.getCurrentTimeZoneIndex(),
            dateFormats = ftsuModel.getDateFormats(),
            selectedDateFormatIndex = ftsuModel.getCurrentDateFormatIndex(),
            isUnitFormatMetric = ftsuModel.getIsMetricFormat(),
            unitFormats = ftsuModel.getUnitFormatTypes(),
            timeFormats = ftsuModel.getSupportedTimeFormats(),
            is24Hour = ftsuModel.getIs24HourTimeFormatType(),
            showLedColorSelection = ftsuModel.isOnyx(),
            selectedTimeZoneString = ftsuModel.getCurrentTimezone(),
            selectedLedColor = ftsuModel.getLedColor()
        )

        val color = ftsuModel.getLedColor()
        var hexString = ""
        try{
            hexString = String.format("#%06X", (0xFFFFFF and color))
        } catch (e: Exception) {}

        val summary = ClassMapper.mapGeneralSettingsToSummary(generalSettings).copy(
            networkNameWired = systemModel.ethernetIpAddress,
            networkNameWireless = systemModel.wirelessSSID,
            bridgeSerialNumber = if (ftsuModel.isOnyx()) systemModel.serialNumberBridge else null, // only Onyx has bridge number
            consoleSerialNumber = systemModel.serialNumberConsole,
            frameSerialNumber = systemModel.serialNumberFrame,
            frame = uiBaseModel.frameType.name,
            ledColorHex = hexString,
            consoleType = ftsuModel.getConsoleMarketingName(),
            model = ftsuModel.getFrameMarketingName(systemModel.serialNumberFrame),
            isSerialNumberTupleValid = ftsuModel.isSerialNumberTupleValid(systemModel.serialNumberConsole, systemModel.serialNumberFrame),
            numberOfChannels = ftsuModel.getTvChannelList(false).size.toString(),
            defaultChannel = ftsuModel.getDefaultChannel()
        )

        _uiState.value = _uiState.value.copy(summary = summary)
    }

    /**
     * Finishes first time setup
     * by installing update if available
     */
    fun onFinishClicked() {
        if (shouldUpdate == true) {
            ftsuModel.installUpdate()
        } else {
            ftsuModel.exitFTSU()
        }
    }

    /**
     * Resets export state to default
     */
    fun onDismissExportDialog() {
        viewModelScope.launch(ioDispatcher) {
            _exportState.send(ExportProfileState.DEFAULT)
        }
    }
}

/**
 * Ui class for info displayed in Summary Ui
 */
internal data class SummaryUi(
    val summary: Summary = Summary(),
    val wirelessNetworkName: String = "",
    val wiredIpAddress: String = "",
    val ledColor: Int = -0,

)

/**
 * enum class representing possibilities for export
 * facility profile state
 */
enum class ExportProfileState {
    DEFAULT,
    EXPORTING,
    EXPORT_FAILED,
    EXPORT_SUCCESS
}