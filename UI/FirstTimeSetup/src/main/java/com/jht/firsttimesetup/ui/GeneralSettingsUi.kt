package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.CommonFunctions.TWENTY_FOUR_HOUR_INDEX
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.DropDownMenu
import com.jht.firsttimesetup.ui.commoncomponents.ErrorView
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.viewmodels.GeneralSettingsUiState
import com.jht.firsttimesetup.viewmodels.GeneralSettingsViewModel
import com.jht.firsttimesetup.viewmodels.TVSetupState
import com.jht.firsttimesetup.viewmodels.TvSetupViewModel
import com.jht.firsttimesetup.viewmodels.TvSourceSelectionViewModel
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


internal enum class ExpandedMenu {
    TIME_ZONE, TIME_FORMAT, DATE_FORMAT, UNIT_FORMAT, NONE
}


/**
 * Container for all general settings
 * @param [showLedColorOptions] -> Flag that determines whether to show customize LED color card
 * @param [onTvSetupClicked] -> Navigation event to show TV setup
 * @param [currentSubsegment] -> Subsegment to be displayed
 * @param [isEdit] -> Determines if the view is in edit mode or traditional flow layout
 */
@Composable
internal fun SettingsView(
    modifier: Modifier = Modifier,
    showLedColorOptions: Boolean = false,
    onTvSetupClicked: () -> Unit = {},
    onSubSubsegmentClicked: (parentSegment: Segment) -> Unit,
    currentSubsegment: Segment?,
    generalSettingsViewModel: GeneralSettingsViewModel = hiltViewModel(),
    onNextClicked: () -> Unit = {},
    isEdit: Boolean = false,
    onNavigateBack: () -> Unit = {},
    isCalibrateNext: Boolean = true,
    isEngineeringMode: Boolean,
    isTvSetupNext: Boolean = true,
) {

    LaunchedEffect(isEdit) {
        generalSettingsViewModel.setIsEdit(isEdit)
    }

    Box(modifier.fillMaxSize()) {
        GeneralSettingsView(
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
            isEdit = isEdit,
            onNavigateBack = onNavigateBack,
            isTVSettingsNext = isTvSetupNext,
            isCalibrateNext = isCalibrateNext,
            onSaveClicked = { generalSettingsViewModel.onSaveClicked() },
        )
    }

}

/**
 * Container view that toggles between the entire tv setup views
 * @param [vm] viewmodel that keeps track of navigation
 */
@Composable
fun TvSetupView(
    tvSourceViewModel: TvSourceSelectionViewModel = hiltViewModel(),
    vm: TvSetupViewModel = hiltViewModel(),
    onNextClicked: () -> Unit = {},
    isCalibrateNext: Boolean,
    onReturnToSummary: () -> Unit = {},
    isEdit: Boolean = false,
    onExitEdit: () -> Unit = {},
    isEngineeringMode: Boolean,
) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val state = uiState.value.state

    val tvState = tvSourceViewModel.uiState.collectAsStateWithLifecycle()

    var showIptvSettings by remember { mutableStateOf(false) }

    if (showIptvSettings) {
        Dialog(
            onDismissRequest = { showIptvSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            IptvSettingsView(
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .fillMaxHeight(.85f),
                onExitClicked = { showIptvSettings = false },
                onSaveClicked = {
                    //todo
                },
                onRebootClicked = {
                    //todo
                })
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (state) {
            TVSetupState.TV_SOURCE -> {
                TvSourceUI(
                    modifier = Modifier.fillMaxSize(),
                    isCalibrateNext = isCalibrateNext,
                    onExitEdit = onExitEdit,
                    onSkipTvSetupClicked = onNextClicked,
                    isEdit = isEdit,
                    onNextClicked = { showIptv -> vm.setState(TVSetupState.IMPORT_CHANNELS) },
                    viewModel = tvSourceViewModel
                )
            }

            TVSetupState.IMPORT_CHANNELS -> {
                TvChannelImportView(
                    isCalibrateNext = isCalibrateNext,
                    onSetupChannelClick = { vm.setState(TVSetupState.CUSTOMIZE_CHANNELS) },
                    onSkipTvSetupClicked = onNextClicked,
                    onImportCoreClick = { vm.onImportChannelsClicked() },
                    downloadState = vm.downloadState,
                    onImportUsbClick = { vm.onImportFromUsb() },
                    importState = vm.importState,
                    isEngineeringMode = isEngineeringMode,
                    isEdit = isEdit,
                    onExitEdit = onExitEdit,
                )
            }

            TVSetupState.CUSTOMIZE_CHANNELS -> {
                CustomizeChannelView(
                    onNextClick = onNextClicked,
                    onIptvClicked = {
                        showIptvSettings = true
                    },
                    onReturnToSummary = onReturnToSummary,
                    isEdit = isEdit,
                    isEngineeringMode = isEngineeringMode,
                    showIptvSettingsButton = tvState.value.isIptvEnabled,
                    onHideIpTvSettingsDialog = {},
                    showIpTvSettingsDialog = showIptvSettings
                )
            }

            TVSetupState.ERROR -> {
                ErrorView(
                    modifier = Modifier.fillMaxSize(),
                    errorMessage = stringResource(R.string.tv_setup_error_tc),
                    actionButton = {
                        FTSUButton(
                            Modifier.widthIn(200.dp),
                            buttonType = FTSUButtonType.Primary,
                            onClick = { /** todo **/ },
                            label = stringResource(R.string.try_again_tc)
                        )
                    })

                FTSUButton(
                    buttonType = FTSUButtonType.Secondary,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    label = stringResource(R.string.try_again_tc)
                )
            }
        }

    }

}


/**
 * General settings view
 * @param [showLedColorOptions] -> Flag that shows LED color selection UI
 * @param [onTvSetupClicked] -> Navigation event to show tv setup
 * @param [onPreselectedColorClicked] -> called when user selects a preselected Led color and passes the selected color's index
 */
@Composable
internal fun GeneralSettingsView(
    modifier: Modifier = Modifier,
    onNextClicked: () -> Unit = {},
    onPreselectedColorClicked: ((colorIndex: Int) -> Unit)? = null,
    state: StateFlow<GeneralSettingsUiState>,
    onSetLedColor: (color: Int) -> Unit = {},
    onSetTimeZone: (timeZone: String, index: Int) -> Unit = { timeZone, index -> },
    onSetTimeFormat: (is24Hour: Boolean) -> Unit = {},
    setDateFormat: (index: Int) -> Unit = {},
    setUnitFormat: (isMetric: Boolean) -> Unit = {},
    isEdit: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    isTVSettingsNext: Boolean = true,
    isCalibrateNext: Boolean = true,
) {

    val uiState = state.collectAsStateWithLifecycle()

    var selectedColorIndex by remember { mutableIntStateOf(0) }

    var expandedType by remember { mutableStateOf(ExpandedMenu.NONE) }

    val timeZoneIndex = uiState.value.selectedTimeZoneIndex

    val timeFormatIndex = if (uiState.value.is24Hour) {
        1
    } else {
        0
    }

    val dateFormatIndex = uiState.value.selectedDateFormatIndex

    val unitFormatIndex = if (uiState.value.isUnitFormatMetric) {
        0
    } else {
        1
    }

    val showLedColorOptions = uiState.value.showLedColorSelection

    val timeZones = uiState.value.timeZones

    val timeFormats = uiState.value.timeFormats

    val dateFormat = uiState.value.dateFormats

    val unitFormat = uiState.value.unitFormats

    //Sets the default color to the first index
    LaunchedEffect(Unit) {
        val color = 0xFF3651e8.toInt() //first color in preselected array
        onSetLedColor(color)
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.general_settings_tc),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier
                .testTag("FTSU:GeneralSettings:Title:Text")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            //adding extra padding if we aren't showing the pick color card
            Spacer(Modifier.weight(if (showLedColorOptions) .25f else 1f))

            if (showLedColorOptions) {
                CustomLedColorCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(.4f)
                        .fillMaxHeight(.6f),
                    onPreselectedColorClicked = { index, color ->
                        selectedColorIndex = index
                        onSetLedColor(color)
                    },
                    selectedColorIndex = selectedColorIndex,
                    onColorSelected = { color -> onSetLedColor(color) })

                Spacer(Modifier.width(32.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                modifier = Modifier
                    .weight(if (showLedColorOptions) 1f else 2f)
                    .fillMaxWidth(.4f)
                    .height(IntrinsicSize.Max)
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp.computeSize()),
                    modifier = Modifier
                        .padding(
                            start = 16.dp.computeSize(),
                            end = 16.dp.computeSize(),
                            top = 8.dp.computeSize(),
                            bottom = 8.dp.computeSize()
                        )
                        .verticalScroll(
                            enabled = true, state = rememberScrollState()
                        )
                ) {

                    var textFieldSize by remember { mutableStateOf(Size.Zero) }

                    DropDownMenu(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                textFieldSize = coordinates.size.toSize()
                            },
                        title = stringResource(R.string.time_zone_tc),
                        menu = timeZones,
                        expandMenu = expandedType == ExpandedMenu.TIME_ZONE,
                        selectedItemIndex = timeZoneIndex,
                        onExpandedMenuItemSelected = { index ->
                            expandedType = ExpandedMenu.NONE
                            onSetTimeZone(timeZones[index], index)
                        },
                        onExpandClicked = { expand ->
                            expandedType = if (expand) ExpandedMenu.TIME_ZONE else ExpandedMenu.NONE
                        },
                        testTag = "FTSU:GeneralSettings:TimeZone",
                        menuWidth = textFieldSize
                    )

                    DropDownMenu(
                        title = stringResource(R.string.time_format_tc),
                        menu = timeFormats,
                        expandMenu = expandedType == ExpandedMenu.TIME_FORMAT,
                        selectedItemIndex = timeFormatIndex,
                        onExpandedMenuItemSelected = { index ->
                            onSetTimeFormat(index == TWENTY_FOUR_HOUR_INDEX)
                            expandedType = ExpandedMenu.NONE

                        },
                        onExpandClicked = { expand ->
                            expandedType =
                                if (expand) ExpandedMenu.TIME_FORMAT else ExpandedMenu.NONE
                        },
                        testTag = "FTSU:GeneralSettings:TimeFormat",
                        menuWidth = textFieldSize
                    )

                    DropDownMenu(
                        title = stringResource(R.string.dateformat_tc),
                        menu = dateFormat,
                        expandMenu = expandedType == ExpandedMenu.DATE_FORMAT,
                        selectedItemIndex = dateFormatIndex,
                        onExpandedMenuItemSelected = { index ->
                            setDateFormat(index)
                            expandedType = ExpandedMenu.NONE
                        },
                        onExpandClicked = { expand ->
                            expandedType =
                                if (expand) ExpandedMenu.DATE_FORMAT else ExpandedMenu.NONE
                        },
                        testTag = "FTSU:GeneralSettings:DateFormat",
                        menuWidth = textFieldSize
                    )

                    DropDownMenu(
                        title = stringResource(R.string.unit_format_tc),
                        menu = unitFormat,
                        expandMenu = expandedType == ExpandedMenu.UNIT_FORMAT,
                        selectedItemIndex = unitFormatIndex,
                        onExpandedMenuItemSelected = { index ->
                            setUnitFormat(index == 0)
                            expandedType = ExpandedMenu.NONE
                        },
                        onExpandClicked = { expand ->
                            expandedType =
                                if (expand) ExpandedMenu.UNIT_FORMAT else ExpandedMenu.NONE
                        },
                        testTag = "FTSU:GeneralSettings:UnitFormat",
                        menuWidth = textFieldSize

                    )
                }
            }

            //adding extra padding if we aren't showing the pick color card
            Spacer(Modifier.weight(if (showLedColorOptions) .25f else 1f))

        }

        if (isEdit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FTSUButton(
                    label = stringResource(R.string.cancel_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = { onNavigateBack() },
                    modifier = Modifier
                        .widthIn(200.dp.computeSize())
                        .testTag("FTSU:GeneralSettings:Cancel:Button")
                        .padding(end = 24.dp),
                )
                Spacer(Modifier.size(48.dp))

                FTSUButton(
                    label = stringResource(R.string.save_tc),
                    buttonType = FTSUButtonType.Primary,
                    onClick = {
                        onSaveClicked()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .widthIn(200.dp.computeSize())
                        .testTag("FTSU:GeneralSettings:Save:Button")
                        .padding(end = 24.dp),
                )
            }

        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp.computeSize(), vertical = 24.dp.computeSize()),
                horizontalArrangement = Arrangement.End
            ) {
                FTSUButton(
                    label = stringResource(
                        if (isTVSettingsNext) R.string.tv_setup_tc
                        else if(isCalibrateNext) R.string.machine_calibration_tc
                        else R.string.summary_tc
                    ),
                    buttonType = FTSUButtonType.Primary,
                    onClick = onNextClicked,
                    modifier = Modifier
                        .testTag("FTSU:GeneralSettings:TvSetup:Button")
                        .widthIn(200.dp),
                    imageEnd = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            "", tint = Color.Black, modifier = Modifier.size(32.dp.computeSize())
                        )
                    }
                )
            }
        }
    }


}

@AllDevicePreviews
@Composable
private fun GeneralSettingsViewPreview() {
    val state = GeneralSettingsUiState(
        showLedColorSelection = false
    )
    val flow = MutableStateFlow(state)
    GeneralSettingsView(
        state = flow,
        isTVSettingsNext = false
    )
}

@AllDevicePreviews
@Composable
private fun GeneralSettingsEditViewPreview() {
    val state = GeneralSettingsUiState(
        showLedColorSelection = false
    )
    val flow = MutableStateFlow(state)
    GeneralSettingsView(
        state = flow, isEdit = true
    )
}

@AllDevicePreviews
@Composable
private fun GeneralSettingsViewOnyxPreview() {
    val state = GeneralSettingsUiState(
        showLedColorSelection = true
    )
    val flow = MutableStateFlow(state)
    GeneralSettingsView(
        state = flow
    )
}

@AllDevicePreviews
@Composable
private fun GeneralSettingsEditViewOnyxPreview() {
    val state = GeneralSettingsUiState(
        showLedColorSelection = true
    )
    val flow = MutableStateFlow(state)
    GeneralSettingsView(
        state = flow, isEdit = true
    )
}