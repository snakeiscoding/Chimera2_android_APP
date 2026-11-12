package com.jht.progression.ui.setup.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.EventIndex
import com.jht.progression.R
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmOptionDialog
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

@Composable
fun RacePreviewScreen(
    viewModel: PreviewViewModel,
    onBackPressed: () -> Unit,
    onCompleteProgram: () -> Unit,
    launchWorkout: (WorkoutInfo, EventIndex) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    RacePreviewScreenContent(
        previewState = uiState.previewState,
        onCompleteProgram = onCompleteProgram,
        onStartRace = { viewModel.startWorkout(launchWorkout) },
        onSkipToRace = { viewModel.skipToWorkout() },
        onBack = onBackPressed
    )
}

@Composable
private fun RacePreviewScreenContent(
    previewState: PreviewState,
    onCompleteProgram: () -> Unit,
    onStartRace: () -> Unit,
    onSkipToRace: () -> Unit,
    onBack: () -> Unit
) {
    val topBarColor = getPreviewTopBarColor(previewState)
    val background = getPreviewBackgroundId(previewState)
    var skipDialogOpen by remember { mutableStateOf(false) }

    MmSetupFrame(
        topBarColor = topBarColor,
        backgroundId = background,
        onBackPressed = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Race Preview Screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(id = R.string.five_k_challenge_uc),
                color = Color.White,
                fontSize = 35.sp.scaleSize(),
                style = oxaniumStyle,
                modifier = Modifier.testTag("race_5k_challenge_title")
            )

            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp.scaleSize())
                    .testTag("race_flag_image"),
                painter = painterResource(id = R.drawable.ic_race_flag),
                contentScale = ContentScale.Fit,
                contentDescription = ""
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .testTag("race_description_text"),
                text = stringResource(id = R.string.five_k_race_description_pg),
                color = Color.White,
                fontSize = 20.sp.scaleSize(),
                textAlign = TextAlign.Center,
                style = oxaniumStyle,
                lineHeight = 31.sp.scaleSize()
            )

            Row {
                /*
                 * When this is a future workout - skip -> brings up dialog
                 * When this is an active workout skip -> goes to final screen begin -> begins race
                 * When this is completed -> race times in the future? No buttons
                 */
                when (previewState) {
                    PreviewState.STATE_COMPLETED -> {
                        // No buttons
                    }
                    PreviewState.STATE_ACTIVE -> {
                        MmThemedButton(
                            modifier = Modifier
                                .padding(end = 40.dp.scaleSize())
                                .testTag("skip_button"),
                            onClick = onCompleteProgram,
                            text = stringResource(id = R.string.skip_uc)
                        )

                        MmThemedButton(
                            onClick = onStartRace,
                            color = MmColors.futuristicBlue,
                            text = stringResource(id = R.string.begin_uc),
                            modifier = Modifier.testTag("begin_button")
                        )
                    }
                    PreviewState.STATE_FUTURE -> {
                        MmThemedButton(
                            modifier = Modifier.testTag("skip_button"),
                            onClick = { skipDialogOpen = true },
                            text = stringResource(id = R.string.skip_uc)
                        )
                    }
                }
            }
        }
    }

    MmOptionDialog(
        open = skipDialogOpen,
        titleText = stringResource(id = R.string.you_are_not_here_yet_tc),
        bodyText = stringResource(id = R.string.you_are_not_here_yet_message_pg),
        onDismiss = {
            skipDialogOpen = false
        },
        onYesClicked = {
            skipDialogOpen = false
            onSkipToRace()
        }
    )
}

@Composable
@AllDevicePreviews
private fun Preview() {
    RacePreviewScreenContent(
        previewState = PreviewState.STATE_ACTIVE,
        onCompleteProgram = {},
        onStartRace = {},
        onSkipToRace = {},
        onBack = {}
    )
}