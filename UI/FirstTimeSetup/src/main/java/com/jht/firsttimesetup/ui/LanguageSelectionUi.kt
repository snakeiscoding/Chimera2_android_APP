package com.jht.firsttimesetup.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.ui.AllDevicePreviews
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize
import com.jht.widgets.ScrollHintOverlay

/**
 * First time setup language selection related UI
 * @param [languages] languages we support users to choose from
 * @param [onClick] handles user clicking specific language
 * @param [onNextClicked] handles user navigating to next segment of FTSU
 * @param [selectedLanguage] index for currently selected language
 * @param [onImportFileClicked] handler for import facility profile from usb
 * @param [onDismissDialog] called when dialog is dismissed to change dialog's state
 * @param [showDialog] flag that determines whether we show the facility success/ error dialog
 * @param [facility] Object we wish to export to current console. If null we show error
 * @param [onConfirmFacilityClicked] -> Sends user to machine calibration after importing profile
 */
@Composable
fun LanguageSelectionScreen(
    modifier: Modifier = Modifier,
    languages: List<String>,
    onClick: (languageIndex: Int) -> Unit = {},
    onNextClicked: () -> Unit = {},
    selectedLanguage: Int = 0,
    isEngineeringMode: Boolean = false,
    onSkipFtsu: () -> Unit = {},
    onImportFileClicked: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    showDialog: Boolean = false,
    facility: Facility? = null,
    onConfirmFacilityClicked: () -> Unit = {},
    onEditMachineBackdoor: () -> Unit = {},
) {
    var clickCount by remember { mutableIntStateOf(0) }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                onDismissDialog
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            )
        ) {
            if (facility != null) { //import facility is available
                ConfirmFacilityScreen(
                    modifier = Modifier.fillMaxWidth(.5f),
                    facilityId = facility.facilityId,
                    facilityName = facility.name,
                    facilityAddress = facility.location,
                    isImport = true,
                    onConfirmClicked = {
                        onConfirmFacilityClicked()
                    },
                    onCancelClicked = {
                        onDismissDialog()
                    }
                )
            } else { //error such as no usb, failed to import, etc
                ImportFacilityErrorView(
                    modifier = Modifier
                        .fillMaxWidth(.5f)
                        .fillMaxHeight(.5f),
                    onDismissDialog = {
                        onDismissDialog()
                    }
                )

            }

        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {

        Text(
            stringResource(R.string.select_language_tc),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.testTag("FTSU:LanguageSelection:Title:Text")
                .clickable {
                    clickCount++
                    if (clickCount >= 7) { // click 7x to show edit machine
                        onEditMachineBackdoor()
                    }
                }
        )

        Spacer(Modifier.height(25.dp.computeSize()))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.68f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(Modifier.fillMaxWidth(.15F))
            Icon(
                painter = painterResource(id = R.drawable.globe_graphic),
                contentDescription = "",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                tint = Color.Unspecified
            )

            LanguageSelectionListView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                languages = languages,
                onClick = onClick,
                selectedLanguage = selectedLanguage
            )

            Spacer(Modifier.fillMaxWidth(.15F))


        }

        Spacer(Modifier.height(IntrinsicSize.Min))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(horizontal = 64.dp.computeSize(), vertical = 24.dp.computeSize())
        ) {
            if (isEngineeringMode) {
                FTSUButton(
                    label = stringResource(R.string.skip_first_time_setup_tc),
                    buttonType = FTSUButtonType.Tertiary,
                    onClick = onSkipFtsu
                )
            } else {
                Spacer(Modifier.fillMaxWidth(.01f))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Max)
            ) {
                FTSUButton(
                    label = stringResource(R.string.import_profile_tc),
                    onClick = onImportFileClicked,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .testTag("FirstTimeSetup:LanguageSelection:ImportProfile:Button"),
                    imageEnd = { Spacer(Modifier.size(32.dp.computeSize())) },
                    buttonType = FTSUButtonType.Secondary
                )

                Spacer(Modifier.width(35.dp.computeSize()))

                FTSUButton(
                    label = stringResource(R.string.network_tc),
                    imageEnd = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            "",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp.computeSize())
                        )
                    },
                    onClick = onNextClicked,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .testTag("FTSU:LanguageSelection:Network:Button")
                )

            }

        }

    }

}

/**
 * Error view for importing facility
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImportFacilityErrorView(
    modifier: Modifier = Modifier,
    onDismissDialog: () -> Unit = {}
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.network_card)),
        modifier = modifier.semantics { testTagsAsResourceId = true }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp.computeSize())
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            Text(
                color = Color.White,
                text = stringResource(R.string.import_facility_profile_tc),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("FirstTimeSetup:ConfirmImportFacility:PageTitle:Text"),
                textAlign = TextAlign.Center
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                Spacer(Modifier.fillMaxWidth(.1f))
                Text(
                    color = Color.White,
                    text = stringResource(R.string.import_facility_error_pg),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth(.9f)
                        .testTag("FirstTimeSetup:ConfirmImportFacility:Body:Text"),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.fillMaxWidth(.1f))
            }

            FTSUButton(
                modifier = Modifier
                    .testTag("FirstTimeSetup:ConfirmImportFacility:Confirm:Button")
                    .padding(horizontal = 16.dp.computeSize()),
                label = stringResource(R.string.ok_uc),
                onClick = onDismissDialog,
                buttonType = FTSUButtonType.Primary
            )

        }
    }
}

@AllDevicePreviews
@Composable
@Preview
fun ImportFacilityErrorPreview() {
    ImportFacilityErrorView(
        modifier = Modifier
            .fillMaxWidth(.5f)
            .fillMaxHeight(.8f)
    )
}

@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun LanguageSelectionScreenPreview2() {
    val languages = listOf(
        "日本語 (Nihongo) (Japanese)",
        "Espanol (Spanish)",
        "Polski (Polish)",
        "English (The King's)",
        "Pig Latin",
        "Klingon",
        "Deutsch (German)",
        "französisch (French)",
        "Italienisch (Italian) "
    )
    LanguageSelectionScreen(
        languages = languages,
        isEngineeringMode = false
    )
}

@Composable
private fun LanguageSelectionListView(
    modifier: Modifier = Modifier,
    languages: List<String>,
    onClick: (languageIndex: Int) -> Unit = {},
    selectedLanguage: Int = 0
) {
    val listState = rememberLazyListState()

    ScrollHintOverlay(state = listState, modifier = modifier) {
        // testTag stays on the LazyColumn so UI tests scrolling it by tag still find the scrollable node
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("FTSU:LanguageSelection:Languages:View"),
            verticalArrangement = Arrangement.spacedBy(2.dp.computeSize())
        ) {
            itemsIndexed(languages) { index, language ->
                LanguageSelectionItem(
                    language = language,
                    isSelected = index == selectedLanguage,
                    onClick = onClick,
                    languageIndex = index,
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                        .testTag(
                        "FTSU:LanguageSelection:LanguageListItem:$index:Button"
                    )

                )
            }
        }
    }

}

@Preview
@Composable
private fun LanguageSelectionListViewPreview() {
    val languages = listOf(
        "日本語 (Nihongo) (Japanese)",
        "Espanol (Spanish)",
        "Polski (Polish)",
        "English (The King's)",
        "Pig Latin",
        "Klingon"
    )
    LanguageSelectionListView(languages = languages)
}

@Composable
private fun LanguageSelectionItem(
    modifier: Modifier = Modifier,
    language: String,
    isSelected: Boolean = false,
    onClick: (language: Int) -> Unit = {},
    languageIndex: Int = 0
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp.computeSize())
            .clickable { onClick(languageIndex) },
        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
        border = if (isSelected) BorderStroke(
            1.dp,
            colorResource(R.color.selected_color)
        ) else BorderStroke(
            0.dp,
            Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .testTag("FTSU:LanguageSelection:LanguageListItemText:$languageIndex:Text")
            )
        }

    }
}

@Preview
@Composable
private fun LanguageSelectedItemPreview() {
    LanguageSelectionItem(language = "日本語 (Nihongo) (Japanese)", isSelected = true)
}

@Preview
@Composable
private fun LanguageNotSelectedItemPreview() {
    LanguageSelectionItem(language = "日本語 (Nihongo) (Japanese)", isSelected = false)
}