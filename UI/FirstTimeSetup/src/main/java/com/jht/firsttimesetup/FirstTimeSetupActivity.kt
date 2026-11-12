package com.jht.firsttimesetup

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.ui.CustomizeChannelView
import com.jht.firsttimesetup.ui.FacilityScreen
import com.jht.firsttimesetup.ui.FirstTimeSetupTopAppBar
import com.jht.firsttimesetup.ui.GeneralSettingsView
import com.jht.firsttimesetup.ui.LanguageSelectionScreen
import com.jht.firsttimesetup.ui.MachineCalibrationUiView
import com.jht.firsttimesetup.ui.NetworkSelectScreen
import com.jht.firsttimesetup.ui.SetupState
import com.jht.firsttimesetup.ui.SummaryEditType
import com.jht.firsttimesetup.ui.SummaryUiView
import com.jht.firsttimesetup.ui.TvChannelImportView
import com.jht.firsttimesetup.ui.TvSourceUI
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.viewmodels.FirstTimeSetupState
import com.jht.firsttimesetup.viewmodels.FirstTimeSetupViewModel
import com.jht.firsttimesetup.viewmodels.GeneralSettingsViewModel
import com.jht.firsttimesetup.viewmodels.SearchFacilityIdViewModel
import com.jht.firsttimesetup.viewmodels.TvSetupViewModel
import com.jht.firsttimesetup.viewmodels.TvSourceSelectionViewModel
import com.jht.theme.DynamicTheme
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class FirstTimeSetupActivity : RestringThemedActivity() {

    private val firstTimeSetupViewModel: FirstTimeSetupViewModel by viewModels()
    private val scanner by lazy {
        BarcodeScannerHelper(
            activity = this,
            consoleSerialHandler =  object : ConsoleSerialHandler {
                override fun updateSerial(serial: String) {
                    firstTimeSetupViewModel.updateConsoleSerial(serial)
                }

                override fun getSerial(): String {
                    return firstTimeSetupViewModel.getConsoleSerialNumber()
                }
            }
        )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        scanner.dispatchKeyEvent(event)
        return super.dispatchKeyEvent(event)
    }

    fun setScannerEnabled(enabled: Boolean) {
        scanner.setEnabled(enabled)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicTheme {
                FirstTimeSetupNavigationView(
                    onScannerEnabledChanged = ::setScannerEnabled
                )
            }

        }
    }
}

/**
 *  Container for top app bar and segments for First Time Setup
 *  @param [tvViewModel] -> navigation and settings viewmodel
 *  @param [onNavigateToFacilityDetail] -> navigation event for facility detail view
 *  @param [onEditNavigationClicked] -> navigation event for summary
 *  This view assumes that the frame is successfully configured
 *  @param [shouldInstallUpdate] -> Flag that determines whether we update the console
 *  after completing FTSU
 */
@Composable
internal fun FtsuSuccessView(
    ftsuViewModel: FirstTimeSetupViewModel,
    tvViewModel: TvSourceSelectionViewModel = hiltViewModel(),
    importChannelsViewModel: TvSetupViewModel = hiltViewModel(),
    generalSettingsViewModel: GeneralSettingsViewModel = hiltViewModel(),
    onNavigateToFacilityDetail: () -> Unit,
    facilitySearchViewModel: SearchFacilityIdViewModel,
    onEditNavigationClicked: (type: SummaryEditType) -> Unit,
    shouldInstallUpdate: Boolean,
    onSkipFtsu: () -> Unit,
    navigateToFacilityProfile: () -> Unit,
    navigateToMachineInfo: () -> Unit,
    onScannerEnabledChanged: (enabled: Boolean) -> Unit,
) {

    val uiState = ftsuViewModel.uiState.collectAsStateWithLifecycle()
    val tvUiState = tvViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        FirstTimeSetupTopAppBar(
            modifier = Modifier.fillMaxHeight(.15f),
            segments = uiState.value.setupList,
            currentSegmentIndex = uiState.value.segmentIndex,
            isEngineeringMode = uiState.value.isEngineeringMode,
            onMESCodeEnter = { ftsuViewModel.onMesCodeEnter(it) },
            onSegmentClicked = ftsuViewModel::onSegmentClicked
        )

        when (uiState.value.currentSegment.type) {
            SetupState.LANGUAGE -> {
                LanguageSelectionScreen(
                    languages = uiState.value.supportedLanguages,
                    onClick = { selectedLanguage -> ftsuViewModel.setSelectedLanguage(selectedLanguage) },
                    onNextClicked = ftsuViewModel::moveToNextSegment,
                    selectedLanguage = uiState.value.selectedLanguage,
                    isEngineeringMode = ftsuViewModel.isEngineeringMode(),
                    onSkipFtsu = onSkipFtsu,
                    onImportFileClicked = { ftsuViewModel.importFacilityProfile() },
                    showDialog = uiState.value.showImportProfileDialog,
                    facility = uiState.value.importProfile,
                    onDismissDialog = { ftsuViewModel.setShouldShowImportDialog(false) },
                    onConfirmFacilityClicked = {
                        ftsuViewModel.setShouldShowImportDialog(false)
                        ftsuViewModel.onConfirmImportProfile()
                        navigateToFacilityProfile()
                    },
                    onEditMachineBackdoor = { navigateToMachineInfo() }
                )
                onScannerEnabledChanged(true) // enable the barcode scanner on language screen
            }

            SetupState.NETWORK -> {
                NetworkSelectScreen(
                    onNextClicked = ftsuViewModel::moveToNextSegment,
                    isEngineeringMode = ftsuViewModel.isEngineeringMode(),
                    onSkipFtsu = onSkipFtsu
                )
                onScannerEnabledChanged(false) // disable the barcode scanner after the first screen
            }

            SetupState.FACILITY_ID -> {
                FacilityScreen(
                    onSkipClicked = ftsuViewModel::moveToNextSegment,
                    onNavigateToDetail = onNavigateToFacilityDetail,
                    vm = facilitySearchViewModel
                )
            }

            SetupState.SETTINGS -> {  }

            SetupState.MACHINE_CALIBRATION -> {
                MachineCalibrationUiView(
                    onNextClicked = ftsuViewModel::moveToNextSegment,
                    isEngineeringMode = ftsuViewModel.isEngineeringMode()
                )
            }

            SetupState.SUMMARY -> {
                val facility = ftsuViewModel.facilityProfile
                SummaryUiView(
                    onEditClicked = { type ->
                        //In order to allow updating we need to stop importing the current profile
                        //if the user wishes to edit some of the imported settings
                        ftsuViewModel.setShouldImportProfileSettings(false)
                        onEditNavigationClicked(type)
                    },
                    facilityProfile = facility,
                    shouldInstallUpdate = shouldInstallUpdate,
                    shouldImportFacilitySettings = uiState.value.shouldImportProfileSettings
                )
            }

            SetupState.TV_SETUP -> {}

            SetupState.GENERAL_SETTINGS -> {
                GeneralSettingsView(
                    onNextClicked = ftsuViewModel::moveToNextSegment,
                    state = generalSettingsViewModel.uiState,
                    onSetLedColor = { color -> generalSettingsViewModel.setLedColor(color) },
                    onSetTimeZone = { timeZone, index ->
                        generalSettingsViewModel.setTimeZone(
                            timeZone, index
                        )
                    },
                    onSetTimeFormat = { is24hour -> generalSettingsViewModel.setTimeFormat(is24hour) },
                    setDateFormat = { index -> generalSettingsViewModel.setDateFormat(index) },
                    setUnitFormat = { isMetric -> generalSettingsViewModel.setUnitFormat(isMetric) },
                    isTVSettingsNext = ftsuViewModel.isTunerSupported(),
                    isCalibrateNext = ftsuViewModel.isFrameCalibratable(),
                )
            }

            SetupState.TV_SETUP_TUNER -> {
                TvSourceUI(
                    modifier = Modifier.fillMaxSize(),
                    isCalibrateNext = ftsuViewModel.isFrameCalibratable(),
                    onSkipTvSetupClicked = ftsuViewModel::moveToNextAuntNode,
                    onNextClicked = { ftsuViewModel.moveToNextSegment() },
                )
            }

            SetupState.TV_SETUP_IMPORT_CHANNELS -> {
                TvChannelImportView(
                    isCalibrateNext = ftsuViewModel.isFrameCalibratable(),
                    onSetupChannelClick = ftsuViewModel::moveToNextSegment,
                    onSkipTvSetupClicked = ftsuViewModel::moveToNextAuntNode,
                    onImportCoreClick = { importChannelsViewModel.onImportChannelsClicked() },
                    downloadState = importChannelsViewModel.downloadState,
                    onImportUsbClick = { importChannelsViewModel.onImportFromUsb() },
                    importState = importChannelsViewModel.importState,
                    isEngineeringMode = uiState.value.isEngineeringMode,
                )
            }

            SetupState.TV_SETUP_EDIT_CHANNELS -> {

                CustomizeChannelView(
                    onNextClick = ftsuViewModel::moveToNextSegment,
                    onIptvClicked = { tvViewModel.setShowIpTvSettings(true) },
                    isEngineeringMode = ftsuViewModel.isEngineeringMode(),
                    showIptvSettingsButton = tvUiState.value.isIptvEnabled,
                    showIpTvSettingsDialog = tvUiState.value.showIpTvSettingsDialog,
                    onHideIpTvSettingsDialog = { tvViewModel.setShowIpTvSettings(false) }
                )
            }
        }
    }
}

object SerialNumberEntryUtils {
    const val SERIAL_MIN_LENGTH = 7
    const val SERIAL_MAX_LENGTH = 32
    fun filterSerialInput(input: String): String =
        input
            .uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(SERIAL_MAX_LENGTH)
}

/**
 *  Container for top app bar and segments for First Time Setup
 *  @param [viewModel] -> navigation and settings viewmodel
 *  @param [onNavigateToFacilityDetail] -> navigation event for facility detail view
 *  @param [facilitySearchViewModel] -> viewModel for searching for facility
 *  @param [onEditNavigationClicked] -> navigation event for detail version of FTSU views
 *  @param [shouldInstallUpdate] -> Flag that determines whether we update the console
 *  after completing FTSU
 */
@Composable
internal fun FirstTimeSetupView(
    viewModel: FirstTimeSetupViewModel,
    onNavigateToFacilityDetail: () -> Unit,
    facilitySearchViewModel: SearchFacilityIdViewModel,
    onEditNavigationClicked: (type: SummaryEditType) -> Unit,
    shouldInstallUpdate: Boolean,
    onSkipFtsu: () -> Unit,
    navigateToFacilityProfile: () -> Unit,
    navigateToMachineInfo: () -> Unit,
    onScannerEnabledChanged: (enabled: Boolean) -> Unit,
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    val showMachineInfo = viewModel.showMachineInfo.collectAsStateWithLifecycle(false)

    LaunchedEffect(showMachineInfo.value ) {
        if(showMachineInfo.value) {
            navigateToMachineInfo()
        }
    }

    when (uiState.value.state) {
        FirstTimeSetupState.SUCCESS -> {
            FtsuSuccessView(
                ftsuViewModel = viewModel,
                onEditNavigationClicked = onEditNavigationClicked,
                onNavigateToFacilityDetail = onNavigateToFacilityDetail,
                facilitySearchViewModel = facilitySearchViewModel,
                shouldInstallUpdate = shouldInstallUpdate,
                onSkipFtsu = onSkipFtsu,
                navigateToFacilityProfile = navigateToFacilityProfile,
                navigateToMachineInfo = navigateToMachineInfo,
                onScannerEnabledChanged = onScannerEnabledChanged,
            )
        }

        FirstTimeSetupState.FAILURE -> {
            navigateToMachineInfo()
        }

        FirstTimeSetupState.LOADING -> {
            EditScreen(
                onMESCodeEnter = { code -> viewModel.onMesCodeEnter(code) }
            ) {
                ProgressLoader()
            }
        }
    }
}
