package com.jht.sprint8.sprint8classic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.sprint8.R
import com.jht.sprint8.Sprint8ViewModel
import com.jht.sprint8.utils.Constants
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_DEFAULT
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_PRESSED_COLOR
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_SELECTED_END_GRADIENT
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_SELECTED_START_GRADIENT
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT8_TEXT_GRADIENT
import com.jht.sprint8.utils.Sprint8Colors.MANUAL_SPRINT_SELECTED_BORDER
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
fun ManualResistanceWorkoutScreen(
    sprint8ViewModel: Sprint8ViewModel = hiltViewModel(),
    onCustomWorkoutStart: (resistanceData: List<Float>, inclineData: List<Float>) -> Unit = { _, _ -> }
) {
    val uiState by sprint8ViewModel.customWorkoutScreenUIState.collectAsStateWithLifecycle()
    val workoutData = uiState.customIntervalDataList
    ManualResistanceWorkoutContent(
        onCustomWorkoutStart = onCustomWorkoutStart,
        workoutData = workoutData
    )
}

@Composable
private fun ManualResistanceWorkoutContent(
    onCustomWorkoutStart: (resistanceData: List<Float>, inclineData: List<Float>) -> Unit,
    workoutData: List<CustomIntervalData>,
) {
    var selectedSprintCount by remember { mutableIntStateOf(Constants.DEFAULT_SPRINT_COUNT) }
    val selectedBrush = Brush.verticalGradient(
        colors = listOf(
            MANUAL_SPRINT8_SELECTED_START_GRADIENT,
            MANUAL_SPRINT8_SELECTED_END_GRADIENT
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MANUAL_SPRINT8_BACKGROUND_COLOR)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(500.dp.computeSize())
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(R.string.how_many_sprints_pg),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium
            )
            for (sprintIndex in 1..Constants.TOTAL_SPRINTS) {
                val interactionSource = remember(sprintIndex) { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val isSelected = selectedSprintCount == sprintIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp.computeSize())
                        .padding(10.dp.computeSize())
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            when {
                                isPressed -> Modifier.background(MANUAL_SPRINT8_PRESSED_COLOR)
                                isSelected -> Modifier.background(selectedBrush)
                                else -> Modifier.background(MANUAL_SPRINT8_DEFAULT)
                            }
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) MANUAL_SPRINT_SELECTED_BORDER else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .selectable(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            indication = ripple(color = MANUAL_SPRINT8_PRESSED_COLOR),
                            onClick = {
                                selectedSprintCount = sprintIndex
                            }
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (sprintIndex).toString(),
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, MANUAL_SPRINT8_TEXT_GRADIENT)
                            ),
                            fontSize = 28.sp.computeSize(),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp.computeSize()))
            StartWorkoutButton {
                val segments = ((selectedSprintCount * 2) + 1) // count + warmup
                val selectedData = workoutData.take(segments)
                val primaryList = selectedData.map { it.primaryValue.value }
                val secondaryList = selectedData.mapNotNull { it.secondaryValue?.value }
                onCustomWorkoutStart(primaryList, secondaryList)
            }
        }
    }
}

@Composable
@AllDevicePreviews
private fun ManualResistanceWorkoutPreview() {
    ManualResistanceWorkoutContent(
        onCustomWorkoutStart = { _, _ -> },
        workoutData = emptyList()
    )
}