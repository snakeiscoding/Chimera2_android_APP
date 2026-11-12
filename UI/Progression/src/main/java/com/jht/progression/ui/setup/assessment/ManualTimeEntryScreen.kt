package com.jht.progression.ui.setup.assessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.data.secondsToHrMinSecString
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmDurationInput
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize

@Composable
fun ManualTimeEntryScreen(
    viewModel: ManualTimeEntryViewModel,
    onPaceSelected: (Float) -> Unit,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    ManualTimeEntryContent(
        onBackPressed = onBackPressed,
        onPaceSelected = { onPaceSelected(viewModel.computeBaseline(uiState.raceTime)) },
        inputTime = uiState.raceTime,
        isValidTime = uiState.isValidRaceTime,
        minRaceTimeInSeconds = uiState.minRaceTimeInSeconds,
        maxRaceTimeInSeconds = uiState.maxRaceTimeInSeconds,
        inputTimeChanged = { viewModel.onRaceTimeChanged(it) }
    )
}

@Composable
private fun ManualTimeEntryContent(
    onBackPressed: () -> Unit,
    onPaceSelected: () -> Unit,
    inputTime: String,
    isValidTime: Boolean,
    minRaceTimeInSeconds: Int,
    maxRaceTimeInSeconds: Int,
    inputTimeChanged: (String) -> Unit
) {
    MmSetupFrame(onBackPressed = onBackPressed) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .testTag("Manual Time Entry Screen")
        ) {
            TitleText(modifier = Modifier.weight(0.4f))
            InputTextField(
                inputTime = inputTime,
                isValid5kTime = isValidTime,
                minRaceTimeInSeconds = minRaceTimeInSeconds,
                maxRaceTimeInSeconds = maxRaceTimeInSeconds,
                inputTimeChanged = inputTimeChanged,
                onPaceSelected = onPaceSelected,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

@Composable
private fun TitleText(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AutoSizeText(
            modifier = Modifier
                .size(670.dp.scaleSize(), 40.dp)
                .testTag("input_5k_time_title"),
            text = stringResource(R.string.input_5k_time_title_tc),
            color = White,
            maxLines = 1,
            style = oxaniumStyle.copy(
                fontSize = 36.sp.scaleSize(),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp.scaleSize()
            )
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp.scaleSize())
                .size(900.dp.scaleSize(), 50.dp.scaleSize())
                .alpha(0.7f)
                .testTag("input_5k_time_subtitle"),
            text = stringResource(R.string.input_5k_time_subtitle_pg),
            color = White,
            style = oxaniumStyle.copy(
                fontSize = 16.sp.scaleSize(),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp.scaleSize()
            )
        )
    }
}

@Composable
private fun InputTextField(
    inputTime: String,
    isValid5kTime: Boolean,
    minRaceTimeInSeconds: Int,
    maxRaceTimeInSeconds: Int,
    inputTimeChanged: (String) -> Unit,
    onPaceSelected: () -> Unit,
    modifier: Modifier
) {
    val minRaceTime = secondsToHrMinSecString(minRaceTimeInSeconds)
    val maxRaceTime = secondsToHrMinSecString(maxRaceTimeInSeconds)

    val hint = when {
        !isValid5kTime && inputTime.isNotEmpty() -> stringResource(
            id = R.string.mm_invalid_assessment_time_pg,
            maxRaceTime,
            minRaceTime
        )

        else -> stringResource(id = R.string.hhmmss_time_format_uc)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MmDurationInput(
            duration = inputTime,
            onDurationChange = { inputTimeChanged(it) },
            label = stringResource(id = R.string.input_5k_time_label_uc),
            hint = hint,
            isInvalid = !isValid5kTime,
            modifier = Modifier
                .width(500.dp.scaleSize())
                .testTag("input_time_field")
        )

        Spacer(modifier = Modifier.height(40.dp.scaleSize()))
        MmThemedButton(
            text = stringResource(R.string.start_program_uc),
            onClick = {
                if (isValid5kTime) onPaceSelected()
            },
            color = if (isValid5kTime) MmColors.futuristicBlue else White.copy(alpha = 0.25f),
            modifier = Modifier
                .testTag("start_program_button")
                .size(390.dp.scaleSize(), 120.dp.scaleSize())
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewManualTimeEntry() {
    ManualTimeEntryContent(
        onPaceSelected = {},
        onBackPressed = {},
        isValidTime = true,
        inputTime = "99:59",
        minRaceTimeInSeconds = 894,
        maxRaceTimeInSeconds = 22370,
        inputTimeChanged = {})
}