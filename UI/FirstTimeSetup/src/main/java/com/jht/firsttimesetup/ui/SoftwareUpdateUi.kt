package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.CommonFunctions.showToast
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.viewmodels.SoftwareUpdateViewModel
import com.jht.firsttimesetup.viewmodels.UpdateState
import com.jht.ui.AllDevicePreviews


/**
 * @param [navigateToMachineCalibration] -> navigation event to move to next segment
 */
@Composable
fun SoftwareUpdateView(
    vm: SoftwareUpdateViewModel = hiltViewModel(),
    navigateToMachineCalibration: (shouldUpdate: Boolean) -> Unit
) {

    val context = LocalContext.current

    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val state = uiState.value.updateState

    when(state) {
        UpdateState.UPDATE_AVAILABLE -> {
            val version = uiState.value.softwareVersion
            UpdateFound(version, onUpdateClicked =  {
                vm.startBackgroundDownload()
                showToast(context, "Starting your download")
                navigateToMachineCalibration(true)
            },
                onSkipClicked = { navigateToMachineCalibration(false) })
        }
        UpdateState.NO_UPDATE -> {
            navigateToMachineCalibration(false)
        }

        UpdateState.CHECK_UPDATES -> {
            ProgressLoader(
                title = "Checking for updates",
                centerText = stringResource(R.string.check_for_updates_tc),
                onDismissClicked = {
                    navigateToMachineCalibration(false)
                }
            )
        }
    }
}

@Composable
fun SoftwareUpdateUI(
    onSkipClicked: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            stringResource(R.string.software_update_tc),
            style = MaterialTheme.typography.headlineLarge,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 18.dp)
                .testTag("FTSU:Update:Title:Text")
        )

        Icon(
            painter = painterResource(R.drawable.usb),
            "Usb icon",
            tint = colorResource(R.color.primary),
            modifier = Modifier
                .padding(bottom = 32.dp)
                .width(200.dp)
                .aspectRatio(1f)
        )

        Text(
            stringResource(R.string.update_with_usb_pg),
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 16.dp)
                .testTag("FTSU:Update:UpdateTitle:Text")
        )

        Text(
            stringResource(R.string.usb_update_instruction_pg),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colorResource(R.color.light_text),
            modifier = Modifier
                .testTag("FTSU:Update:UpdateBody:Text")
                .padding(bottom = 60.dp)
                .widthIn(max = 900.dp)
        )

        Row(horizontalArrangement = Arrangement.Center) {
            FTSUButton(
                modifier = Modifier
                    .testTag("FTSU:Update:SkipForNow:Button")
                    .requiredWidthIn(min = 200.dp),
                label = stringResource(R.string.skip_for_now_tc),
                buttonType = FTSUButtonType.Secondary,
                onClick = onSkipClicked
            )

            Spacer(Modifier.size(48.dp))

            FTSUButton(
                modifier = Modifier
                    .testTag("FTSU:Update:ScanUsb:Button")
                    .requiredWidthIn(min = 200.dp),
                label = stringResource(R.string.scan_usb_tc),
                buttonType = FTSUButtonType.Primary
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun UpdatePreview() {
    SoftwareUpdateUI()
}

@Composable
fun UpdateFound(
    version: String = "",
    onUpdateClicked: () -> Unit = {},
    onSkipClicked: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            stringResource(R.string.update_found_tc),
            style = MaterialTheme.typography.headlineLarge,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Icon(
            painter = painterResource(R.drawable.download),
            "Download Icon",
            tint = colorResource(R.color.primary),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            stringResource(R.string.software_tc) + " $version " + stringResource(R.string.found_tc),
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            stringResource(R.string.would_you_like_to_update_pg),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 60.dp)
        )

        Row(horizontalArrangement = Arrangement.Center) {
            FTSUButton(
                label = stringResource(R.string.skip_for_now_tc),
                buttonType = FTSUButtonType.Secondary,
                modifier = Modifier
                    .requiredWidthIn(min = 200.dp),
                onClick = onSkipClicked
            )

            Spacer(Modifier.size(48.dp))

            FTSUButton(
                label = stringResource(R.string.update_tc),
                buttonType = FTSUButtonType.Primary,
                modifier = Modifier
                    .requiredWidthIn(min = 200.dp),
                onClick = onUpdateClicked
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun UpdateFoundPreview() {
    UpdateFound()
}

@Composable
fun DownloadingUpdateUI(){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressLoader(
            title = stringResource(R.string.downloading_update_tc),
            centerText = stringResource(R.string.downloading_tc),
            footer = { TimeRemainingMessage() }
        )


    }
}
@Composable
@AllDevicePreviews
private fun DownloadUpdatePreview(){
    DownloadingUpdateUI()
}

@Composable
fun InstallingUpdateUI(){
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressLoader(
            title = stringResource(R.string.installing_update_tc),
            centerText = stringResource(R.string.installing_tc),
            footer = { TimeRemainingMessage() }
        )
    }
}

@Composable
@AllDevicePreviews
private fun InstallingUpdatePreview(){
    InstallingUpdateUI()
}

@Composable
private fun TimeRemainingMessage(){
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.do_not_turn_off_pg),
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(R.color.light_text),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            stringResource(R.string.time_remaining_tc) + ": 10 " + stringResource(R.string.minutes_tc),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.light_text)
        )
    }
}

@Composable
fun UpdateComplete() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.check_circle),
                "Update Complete Icon",
                tint = colorResource(R.color.primary),
                modifier = Modifier
                    .size(264.dp)
                    .padding(bottom = 32.dp)
            )

            Text(
                stringResource(R.string.you_are_up_to_date_pg),
                style = MaterialTheme.typography.headlineSmall,
                color = colorResource(R.color.light_text)
            )
        }

        FTSUButton(
            label = stringResource(R.string.summary_tc),
            buttonType = FTSUButtonType.Primary,
            imageEnd = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "next step arrow",
                    tint = colorResource(R.color.dark_text)
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(64.dp)
        )
    }
}


@AllDevicePreviews
@Composable
private fun UpdateCompletePreview() {
    UpdateComplete()
}