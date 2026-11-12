package com.jht.firsttimesetup.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.ErrorView
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FTSUTextField
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.viewmodels.FacilityIdState
import com.jht.firsttimesetup.viewmodels.SearchFacilityIdViewModel
import com.jht.firsttimesetup.viewmodels.SearchFacilityState
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
fun FacilityScreen(
    onSkipClicked: () -> Unit = {},
    vm: SearchFacilityIdViewModel,
    onNavigateToDetail: () -> Unit,
    isEdit: Boolean = false,
    onCancelClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {}
) {

    val uiState = vm.uiState.collectAsStateWithLifecycle()

    // Initial UI state kept if the user cancels
    val initialUiState = remember(uiState.value) {
        uiState.value.copy()
    }

    LaunchedEffect(Unit) {
        vm.setPreviousUiState(initialUiState)
    }

    when (uiState.value.state) {
        FacilityIdState.SEARCH -> {
            FacilityIDScreen(
                onNextClicked = {
                    onSkipClicked()
                    vm.setFacilityId()
                },
                uiState = uiState.value,
                onSearchFacilityClicked = { vm.onSearchFacilityClicked() },
                onFacilityIdQueryChange = { facilityId -> vm.onQueryChange(facilityId) },
                isEdit = isEdit,
                onCancelClicked = {
                    vm.restorePreviousState()
                    vm.setFacilityId()
                    onCancelClicked()
                },
                onSaveClicked = onSaveClicked,
                isConnected = uiState.value.isConnected,
            )
        }

        FacilityIdState.LOADING -> {
            ProgressLoader(
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .fillMaxHeight(.8f),
                centerText = stringResource(R.string.loading_tc)
            )
        }

        FacilityIdState.ERROR -> {
            FacilityIDScreen(
                onNextClicked = onSkipClicked,
                uiState = uiState.value,
                onSearchFacilityClicked = { vm.onSearchFacilityClicked() },
                onFacilityIdQueryChange = { facilityId -> vm.onQueryChange(facilityId) },
                isEdit = isEdit,
                onCancelClicked = {
                    vm.restorePreviousState()
                    vm.setFacilityId()
                    onCancelClicked()
                },
                isConnected = uiState.value.isConnected,
                facilityNotFound = true // we will now indicate an error here
            )
        }

        FacilityIdState.CONFIRM -> {
            val facility = uiState.value.facility

            facility?.let {
                Dialog(
                    onDismissRequest = { vm.setState(FacilityIdState.SEARCH) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    ConfirmFacilityScreen(
                        modifier = Modifier.fillMaxWidth(.5f),
                        facilityId = facility.facilityId,
                        facilityName = facility.name,
                        facilityAddress = facility.location,
                        onReEnterClicked = { vm.setState(FacilityIdState.SEARCH) },
                        onConfirmClicked = {
                            onNavigateToDetail()
                            vm.setState(FacilityIdState.SEARCH)
                        },
                        isImport = false,
                        onCancelClicked = {}
                    )
                }

            } ?: run {
                ErrorView(
                    modifier = Modifier.fillMaxSize(),
                    actionButton = {
                        FTSUButton(
                            label = stringResource(R.string.try_again_tc),
                            buttonType = FTSUButtonType.Primary,
                            onClick = { vm.setState(FacilityIdState.SEARCH) }
                        )
                    }
                )
            }
        }

        else -> {
            FacilityIDScreen(
                onNextClicked = onSkipClicked,
                uiState = uiState.value,
                onSearchFacilityClicked = { vm.onSearchFacilityClicked() },
                onFacilityIdQueryChange = { facilityId -> vm.onQueryChange(facilityId) },
                isConnected = uiState.value.isConnected
            )
        }
    }

}

/**
 * First time setup Facility Id search screen
 * @param [onNextClicked] -> handles navigating to next segment in FTSU
 * @param [uiState] -> UI state for facility search
 * @param [onSearchFacilityClicked] -> Call viewmodel to load facility via backend
 * @param [onFacilityIdQueryChange] -> Updates facility id in Viewmodel
 * @param [isEdit] -> Flag to determine if this is edit flow vs segments flow
 * @param [onSaveClicked] used to navigate back in edit flow
 * @param [onCancelClicked] used to navigate back in edit flow and revert to originally inputted club
 * @param [isConnected] used to show different strings based on console's connection to internet
 * @param [facilityNotFound] used to indicate in the search field that facility was not found
 */
@Composable
fun FacilityIDScreen(
    onNextClicked: () -> Unit = {},
    uiState: SearchFacilityState,
    onSearchFacilityClicked: () -> Unit = {},
    onFacilityIdQueryChange: (facilityId: String) -> Unit = {},
    isEdit: Boolean = false,
    onCancelClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    isConnected: Boolean,
    facilityNotFound: Boolean = false
) {

    val localFocusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(TextFieldValue("")) }
    text = text.copy(text = uiState.facilityIdSearchQuery)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        localFocusManager.clearFocus()
                    })
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = stringResource(R.string.club_id_tc),
                style = MaterialTheme.typography.headlineLarge,
                color = colorResource(R.color.light_text),
                modifier = Modifier.testTag("FTSU:FacilityId:Title:Text")
            )

            Spacer(Modifier.height(16.dp.computeSize()))

            Text(
                text = stringResource(if (isConnected) R.string.enter_facility_id_connected_pg else R.string.enter_facility_id_unconnected_pg),
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(R.color.light_text),
                modifier = Modifier.testTag("FTSU:FacilityId:Body:Text")
            )

            Spacer(Modifier.height(32.dp.computeSize()))


            FTSUTextField(
                label = stringResource(R.string.club_id_tc),
                text = text.text,
                modifier = Modifier.fillMaxWidth(.4f),
                onTextChange = { facilityId -> onFacilityIdQueryChange(facilityId) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (isConnected) {
                        ImeAction.Search
                    } else {
                        ImeAction.Done
                    }
                ),
                testTag = "FTSU:FacilityId:Search",
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (isConnected) {
                            onSearchFacilityClicked()
                        } else {
                            /** Do nothing**/
                        }
                    },
                    onDone = {
                        localFocusManager.clearFocus()
                    }
                ),
                textFieldModifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            text = text.copy(
                                selection = TextRange(0, text.text.length)
                            )
                        }
                    },
                bottomMessage = if (facilityNotFound) stringResource(R.string.facility_not_found_error_message_pg) else null,
                bottomMessageColor = Color.Red,
                borderColor = if (facilityNotFound) Color.Red else colorResource(R.color.selected_color),
                placeholderString = stringResource(R.string.enter_facility_id_tc)
            )

            Spacer(Modifier.height(32.dp.computeSize()))


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {

                if (isEdit) {
                    FTSUButton(
                        label = stringResource(R.string.cancel_tc),
                        modifier = Modifier
                            .widthIn(200.dp, 400.dp)
                            .testTag("FTSU:FacilityId:Cancel:Button"),
                        onClick = {
                            localFocusManager.clearFocus()
                            onCancelClicked()
                                  },
                        buttonType = FTSUButtonType.Secondary
                    )
                    if (!uiState.isConnected) {
                        FTSUButton(
                            label = stringResource(R.string.save_tc),
                            modifier = Modifier
                                .widthIn(200.dp, 400.dp)
                                .testTag("FTSU:FacilityId:Save:Button"),
                            onClick = {
                                localFocusManager.clearFocus()
                                onSaveClicked()
                            },
                            buttonType = FTSUButtonType.Primary
                        )
                    }
                }

                if (uiState.isConnected) {
                    FTSUButton(
                        label = stringResource(R.string.search_tc),
                        modifier = Modifier
                            .widthIn(200.dp, 400.dp)
                            .testTag("FTSU:FacilityId:Search:Button"),
                        onClick = { onSearchFacilityClicked() },
                        buttonType = if (uiState.facilityIdSearchQuery.isEmpty()) {
                            FTSUButtonType.Disabled
                        } else {
                            FTSUButtonType.Primary
                        }
                    )
                }
            }

            Spacer(Modifier.fillMaxHeight(.35f))

            if (!isEdit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 64.dp.computeSize(), vertical = 24.dp.computeSize()),
                    horizontalArrangement = Arrangement.End
                ) {
                    FTSUButton(
                        label = stringResource(R.string.settings_tc),
                        buttonType = FTSUButtonType.Secondary,
                        onClick = onNextClicked,
                        modifier = Modifier
                            .testTag("FTSU:FacilityId:Settings:Button")
                            .widthIn(200.dp)
                    )
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun FacilityIDScreenPreview() {
    val uiState = SearchFacilityState()
    FacilityIDScreen(
        uiState = uiState,
        isConnected = true
    )
}

@AllDevicePreviews
@Composable
private fun FacilityIDScreenEditPreview() {
    val uiState = SearchFacilityState()
    FacilityIDScreen(uiState = uiState, isEdit = true, isConnected = true)
}

@AllDevicePreviews
@Composable
private fun FacilityIDScreenFacilityNoFoundPreview() {
    val uiState = SearchFacilityState()
    FacilityIDScreen(uiState = uiState, isEdit = true, isConnected = true, facilityNotFound = true)
}




