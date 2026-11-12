package com.jht.progression.ui.setup.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmDurationInput
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

@Composable
fun GoalSettingsScreen(
    viewModel: GoalSettingsViewModel,
    onBackPressed: () -> Unit,
    onSetGoalTime: (Float) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    GoalSettingsContent(
        estimatedRaceTime = uiState.estimatedRaceTime,
        goalTime = uiState.goalTime,
        isValidGoalTime = uiState.isValidGoalTime,
        maxRaceTimeReduction = uiState.maxRaceTimeReduction,
        onBackPressed = onBackPressed,
        onGoalTimeChanged = { viewModel.onGoalTimeChanged(it) },
        onSetGoalTimeClicked = { onSetGoalTime(viewModel.computeGoalBaseline(it)) }
    )
}


@Composable
fun GoalSettingsContent(
    estimatedRaceTime: String,
    goalTime: String,
    isValidGoalTime: Boolean,
    maxRaceTimeReduction: Int,
    onBackPressed: () -> Unit,
    onGoalTimeChanged: (String) -> Unit,
    onSetGoalTimeClicked: (String) -> Unit,
) {

    MmSetupFrame(onBackPressed = onBackPressed) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Goal Settings Screen"),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp.scaleSize()))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 346.dp.scaleSize())
                    .testTag("goal_settings_title"),
                text = stringResource(id = R.string.mm_set_goal_5k_time_tc),
                color = Color.White,
                fontSize = 36.sp.scaleSize(),
                textAlign = TextAlign.Center,
                style = oxaniumStyle
            )
            Spacer(modifier = Modifier.height(15.dp.scaleSize()))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 295.dp.scaleSize())
                    .testTag("goal_settings_description"),
                text = stringResource(id = R.string.mm_5k_goal_settings_description_pg),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = oxaniumStyle.copy(fontWeight = FontWeight.Medium),
                fontSize = 18.sp.scaleSize(),
                lineHeight = 26.sp.scaleSize()
            )
            Spacer(modifier = Modifier.height(80.dp.scaleSize()))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 260.dp.scaleSize(),
                        end = 183.dp.scaleSize()
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier.testTag("goal_settings_estimated_time_label"),
                        text = stringResource(id = R.string.estimated_five_k_time_tc).uppercase(),
                        color = Color.White,
                        fontSize = 21.sp.scaleSize(),
                        textAlign = TextAlign.Center,
                        style = oxaniumStyle
                    )
                    Spacer(modifier = Modifier.height(10.dp.scaleSize()))
                    Text(
                        modifier = Modifier.testTag("goal_settings_estimated_time_value"),
                        text = estimatedRaceTime,
                        color = MmColors.futuristicBlue,
                        fontSize = 48.sp.scaleSize(),
                        fontWeight = FontWeight.Bold,
                        style = oxaniumStyle.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                MmDurationInput(
                    duration = goalTime,
                    onDurationChange = { onGoalTimeChanged(it) },
                    label = stringResource(id = R.string.goal_time_tc),
                    hint = stringResource(id = R.string.mm_goal_time_must_be_faster_than_estimate_pg, maxRaceTimeReduction),
                    isInvalid = !isValidGoalTime,
                    modifier = Modifier.width(480.dp.scaleSize()),
                )
            }
            Spacer(modifier = Modifier.height(77.dp.scaleSize()))
            MmThemedButton(
                text = stringResource(id = R.string.set_goal_tc),
                color = if (isValidGoalTime) MmColors.futuristicBlue else Color.White.copy(alpha = 0.25f),
            ) { if (isValidGoalTime) onSetGoalTimeClicked(goalTime) }
        }
    }
}

@Composable
@AllDevicePreviews
private fun Preview() {
    GoalSettingsContent(
        estimatedRaceTime = "00:50:00",
        goalTime = "",
        isValidGoalTime = false,
        maxRaceTimeReduction = 4,
        onBackPressed = {},
        onGoalTimeChanged = {},
        onSetGoalTimeClicked = {}
    )
}