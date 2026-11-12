package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.DropDownMenu
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.viewmodels.TvSourceSelectionViewModel
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


@Composable
fun TvSourceUI(
    viewModel: TvSourceSelectionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    isCalibrateNext: Boolean = true,
    onSkipTvSetupClicked: () -> Unit = {},
    onNextClicked: (showIptv: Boolean) -> Unit = {},
    isEdit: Boolean = false,
    onExitEdit: () -> Unit = {}
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.value
    TvSourceView(
        modifier = modifier,
        isCalibrateNext = isCalibrateNext,
        onSkipTvSetupClicked = onSkipTvSetupClicked,
        onNextClicked = { onNextClicked(uiState.value.isIptvEnabled) },
        tvSourceList = state.tvSources,
        connectionTypes = state.connectionTypes,
        onSaveSettings = { tvSourceIndex, connectionTypeIndex -> viewModel.onSaveSettings(tvSourceIndex,connectionTypeIndex) },
        isEdit = isEdit,
        onExitEdit = onExitEdit,
    )
}
@Composable
fun TvSourceView(
    modifier: Modifier = Modifier,
    isCalibrateNext: Boolean = true,
    onSkipTvSetupClicked: () -> Unit = {},
    isEdit: Boolean = false,
    onExitEdit: () -> Unit = {},
    onNextClicked: () -> Unit = {},
    tvSourceList: List<String>,
    connectionTypes: List<String>,
    onSaveSettings: (tvSourceIndex: Int, connectionTypeIndex:Int) -> Unit = { _,_ -> },


    ) {
    var selectedTvSource by remember { mutableIntStateOf(0) }

    var expandTvSource by remember { mutableStateOf(false) }

    var expandConnectionType by remember { mutableStateOf(false) }

    var selectedConnectionType by remember { mutableIntStateOf(0) }


    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.tv_setup_tc),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("FTSU:TvSource:Title:Text")
                    .fillMaxWidth()
                    .padding(top = 35.dp),
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.tv_setup_connect_your_service_pg),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .testTag("FTSU:TvSource:Body:Text")
                    .fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(.3f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var textFieldSize by remember { mutableStateOf(Size.Zero) }

            DropDownMenu(
                modifier = Modifier.fillMaxWidth(.35f).onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                },
                menu = tvSourceList,
                title = stringResource(R.string.source_selection_tc),
                onExpandedMenuItemSelected = { sourceIndex ->
                    selectedTvSource = sourceIndex
                    expandTvSource = false
                },
                onExpandClicked = { expandTvSource = it },
                expandMenu = expandTvSource,
                selectedItemIndex = selectedTvSource,
                testTag = "FTSU:TvSource:SourceSelection",
                menuWidth = textFieldSize
            )

            DropDownMenu(
                modifier = Modifier.fillMaxWidth(.35f).onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                },
                menu = connectionTypes,
                title = stringResource(R.string.connection_type_tc),
                onExpandedMenuItemSelected = { sourceIndex ->
                    selectedConnectionType = sourceIndex
                    expandConnectionType = false
                },
                onExpandClicked = { expandConnectionType = it },
                expandMenu = expandConnectionType,
                selectedItemIndex = selectedConnectionType,
                testTag = "FTSU:ConnectionType:SourceSelection",
                menuWidth = textFieldSize
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .padding(horizontal = 64.dp.computeSize(), vertical = 24.dp.computeSize())
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FTSUButton(
                label = stringResource(R.string.how_to_setup_tc),
                buttonType = FTSUButtonType.Tertiary,
                imageStart = {
                    Icon(imageVector = Icons.Filled.Info, tint = Color.White.copy(.9f), contentDescription = "Information icon")
                },
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .testTag("FTSU:TvSource:SetupInfo:Button")
            )

            Row(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                FTSUButton(
                    label = stringResource(if(isCalibrateNext) R.string.machine_calibration_tc else R.string.summary_tc),
                    buttonType = FTSUButtonType.Secondary,
                    modifier = Modifier
                        .testTag("FTSU:TvSource:Skip:Button")
                        .weight(1f),
                    onClick =
                        if (isEdit) {
                            onExitEdit
                        }
                        else {
                            onSkipTvSetupClicked
                        }

                )

                FTSUButton(
                    label = stringResource(R.string.next_tc),
                    buttonType = FTSUButtonType.Primary,
                    imageEnd = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            tint = Color.Black, contentDescription = "Next icon",
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    modifier = Modifier
                        .testTag("FTSU:TvSource:Next:Button")
                        .defaultMinSize(200.dp, 60.dp)
                        .weight(1f),
                    onClick = {
                        onSaveSettings(selectedTvSource,selectedConnectionType)
                        onNextClicked( )
                    }
                )

            }
        }
    }
}

@Composable
@AllDevicePreviews
private fun TVSourceViewPreview() {
    val tvSources = listOf("US9", "US10", "The ULTIMATE TV TUNER")
    val connectionType = listOf("COAX", "ETHERNET", "TYPE-C")
    TvSourceView(
        modifier = Modifier.fillMaxSize(),
        tvSourceList = tvSources,
        connectionTypes = connectionType
    )
}