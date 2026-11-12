@file:OptIn(ExperimentalMaterial3Api::class)

package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jht.firsttimesetup.CommonFunctions.showToast
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun TvChannelImportView(
    isCalibrateNext: Boolean = true,
    onImportCoreClick: () -> Unit = {},
    onImportUsbClick: () -> Unit = {},
    onSetupChannelClick: () -> Unit = {},
    onSkipTvSetupClicked: () -> Unit = {},
    isEngineeringMode: Boolean,
    downloadState: Flow<Boolean?> = emptyFlow(),
    importState: Flow<Boolean?> = emptyFlow(),
    isEdit: Boolean = false,
    onExitEdit: () -> Unit = {},
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val successMessage = stringResource(R.string.success_lc)
    val errorMessage = stringResource(R.string.errors_tc)

    LaunchedEffect(downloadState) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            downloadState.collect { isSuccess ->
                if (isSuccess == true) {
                    showToast(context, successMessage)
                } else if (isSuccess == false) {
                    showToast(context, errorMessage)
                }
            }
        }
    }

    LaunchedEffect(importState) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            importState.collect { isSuccess ->
                if (isSuccess == true) {
                    showToast(context, successMessage)
                } else if (isSuccess == false) {
                    showToast(context, errorMessage)
                }
            }
        }
    }

    // The fixed 400dp icon plus fixed gaps no longer fit on 1366x768, which cut off the bottom button,
    // especially inside EditScreen. The content now sits in a weighted, centred column above the
    // button, and the icon is the part that shrinks.
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.tv_setup_tc),
                style = MaterialTheme.typography.headlineLarge,
                color = colorResource(R.color.light_text),
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .testTag("FTSU:TvSetup:PageTitle:Text")
            )

            Text(
                stringResource(R.string.tv_setup_instruction_pg),
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(R.color.light_text),
                modifier = Modifier.testTag("FTSU:TvSetup:PageTitle2:Text")
            )

            Spacer(Modifier.size(32.dp))

            //icon todo CGC-20900 get from creative
            Icon(
                painter = painterResource(R.drawable.tv),
                "temp tv icon",
                // Up to 400dp, shrinking to whatever height is left so the buttons below stay on screen.
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true),
                tint = Color.Unspecified
            )

            Spacer(Modifier.size(64.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Spacer(Modifier.weight(1f))

                FTSUButton(
                    modifier = Modifier
                        .widthIn(min = 200.dp.computeSize())
                        .testTag("FTSU:TvSetup:CORE:Button"),
                    label = stringResource(R.string.import_ch_from_core_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = onImportCoreClick
                )

                FTSUButton(
                    modifier = Modifier
                        .widthIn(min = 200.dp.computeSize())
                        .testTag("FTSU:TvSetup:USB:Button"),
                    label = stringResource(R.string.import_ch_from_usb_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = onImportUsbClick
                )

                FTSUButton(
                    modifier = Modifier
                        .widthIn(min = 200.dp.computeSize())
                        .testTag("FTSU:TvSetup:SetupChannels:Button"),
                    label = stringResource(R.string.setup_channels_tc),
                    buttonType = FTSUButtonType.Primary,
                    onClick = onSetupChannelClick
                )

                Spacer(Modifier.weight(1f))
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(end = 64.dp.computeSize(), bottom = 24.dp.computeSize())
                .fillMaxWidth()
        ) {
            FTSUButton(
                onClick = if(isEdit) onExitEdit else onSkipTvSetupClicked,
                label = stringResource(if (isCalibrateNext) R.string.machine_calibration_tc else R.string.summary_tc),
                buttonType = FTSUButtonType.Secondary,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(200.dp)
                    .testTag("FTSU:TvSetup:SkipForNow:Button")
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewTvImportEngineerModeOnChannels() {
    TvChannelImportView(isEngineeringMode = true)
}

@AllDevicePreviews
@Composable
private fun PreviewTvImportEngineerModeOffChannels(){
    TvChannelImportView(isEngineeringMode = false)
}
