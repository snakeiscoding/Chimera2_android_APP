package com.jht.sprint8.sprint8classic.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.enums.FrameType
import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam
import com.jht.sprint8.R
import com.jht.sprint8.Sprint8ViewModel
import com.jht.sprint8.model.CustomStatInfo
import com.jht.sprint8.sprint8classic.components.CustomBarGraphComponent
import com.jht.sprint8.sprint8classic.components.CustomGraphLegend
import com.jht.sprint8.sprint8classic.components.CustomWorkoutTextField
import com.jht.sprint8.sprint8classic.components.SaveButtonComp
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_SAVE_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_CONTENT_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_SCORE_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter.formatDecimal
import com.jht.widgets.numpad.NumPadDisplay
import com.jht.widgets.PillBox
import com.jht.widgets.PillBoxStyle
import com.jht.widgets.scaleSize
import kotlinx.coroutines.flow.drop
import java.math.RoundingMode
import kotlin.math.roundToInt

data class CustomIntervalData(
    val primaryValue: MutableState<Float>,
    val secondaryValue: MutableState<Float>? = null
)

/**
 * Composable screen for configuring and previewing a custom Sprint8 workout.
 *
 * This screen allows users to view and adjust custom workout statistics, and
 * start the workout with the provided configuration data.
 *
 * @param frameType Defines the visual frame or border style applied to the screen.
 * @param customStats [CustomStatInfo] containing the statistics and parameters
 * for the custom workout.
 * @param onCustomWorkoutStart Callback invoked when the custom workout is started,
 * providing lists of speed/resistance data and incline data. Defaults to a no-op lambda.
 *
 **/

@Composable
fun CustomWorkoutScreen(
    frameType: FrameType,
    customStats: CustomStatInfo,
    onCustomWorkoutStart: (speedOrResistanceData: List<Float>, inclineData: List<Float>) -> Unit = { _, _ -> },
    sprint8ViewModel : Sprint8ViewModel = hiltViewModel()
)
{
    val uiState by sprint8ViewModel.customWorkoutScreenUIState.collectAsStateWithLifecycle()
    val workoutData= uiState.customIntervalDataList
    var selectedSprint= uiState.selectedSprint
    val hideSaveWorkoutUi = uiState.hideSaveWorkoutUi //CGC-22110 - Custom save functionality removed for now.

    SubsequentIntervalUpdates(workoutData)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        println("Tapped outside")
                    }
                )
            }
            .background(Color.Black)
            .padding(
                start = 5.dp.computeSize(1920f),
                top = 20.dp.computeSize(1920f)
            )
    ) {
        if (!hideSaveWorkoutUi) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(366.dp.computeSize(1920f))
                    .background(
                        color = SPRINT8_DISABLED_BTN_CONTENT_COLOR,
                        RoundedCornerShape(32.dp.computeSize(1920f))
                    )
                    .padding(horizontal = 20.dp.computeSize(1920f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 20.dp.computeSize(1920f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.your_workouts_tc),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 22.sp.computeSize(1920f),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp.computeSize(1920f))
                                .border(
                                    border = BorderStroke(
                                        2.dp.computeSize(1920f),
                                        SUMMARY_GRAPH_BAR_COLOR_1
                                    ),
                                    shape = RoundedCornerShape(
                                        18.dp.computeSize(1920f)
                                    )
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plus_white_v2),
                                contentDescription = null,
                                tint = SUMMARY_GRAPH_BAR_COLOR_1,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp.computeSize(1920f))
                            )
                        }
                    }
                    CustomWorkoutListItem(
                        borderColor = SUMMARY_GRAPH_BAR_COLOR_1,
                        textColor = SUMMARY_GRAPH_BAR_COLOR_1
                    )
                }
                Text(
                    stringResource(R.string.log_in_to_save_custom_pg),
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp.computeSize(1920f),
                    color = SUMMARY_SCORE_COLOR,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp.computeSize(1920f), end = 20.dp.computeSize(1920f), bottom = 20.dp.computeSize(1920f)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp.computeSize(1920f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!hideSaveWorkoutUi) {
                    CustomWorkoutTextField(
                        modifier = Modifier
                            .wrapContentWidth(),
                    )

                    SaveButtonComp()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = SPRINT8_DISABLED_BTN_CONTENT_COLOR,
                        RoundedCornerShape(32.dp.computeSize(1920f))
                    )
                    .padding(20.dp.scaleSize())
            ) {
                // custom workout bar graph
                CustomBarGraphComponent(
                    workoutData = workoutData,
                    selectedSprint = selectedSprint,
                    onChangeIndex = { sprint8ViewModel.updateSelectedSprint(it) },
                    onAddInterval = { sprint8ViewModel.addInterval() },
                    maxBarValue = customStats.primaryStat.second.max,
                    minBarValue = customStats.primaryStat.second.min,
                    formatValues = { formatValue(it, frameType) },
                    frameType = frameType,
                    modifier = Modifier
                        .padding(bottom = 20.dp.computeSize(1920f))
                        .fillMaxWidth()
                        .height(280.dp.computeSize(1920f))
                )

                CustomGraphLegend(
                    workoutData = workoutData,
                    statInfo = customStats,
                )
            }

            Spacer(Modifier.height(20.dp.computeSize(1920f)))

            // shows pill boxes
            CustomPillBoxGridComp(
                modifier = Modifier.weight(1f),
                selectedSprint = selectedSprint,
                pillboxStates = getPillboxStates(selectedSprint, workoutData),
                workoutData = workoutData,
                statInfo = customStats,
                onCustomWorkoutStart = onCustomWorkoutStart,
                onDeleteSegment = {
                    sprint8ViewModel.updateSelectedSprint(0)
                    val totalSprints = (workoutData.size - 1) / 2
                    if (totalSprints > 1) {
                        sprint8ViewModel.deleteInterval(selectedSprint)
                    }
                }
            )
        }
    }
}

private fun formatValue(value: Float, frameType: FrameType): String {
    return when (frameType) {
        FrameType.Treadmill -> {
            formatDecimal("0.0", value, RoundingMode.HALF_UP)
        }
        else -> {
            value.roundToInt().toString()
        }
    }
}



@Composable
private fun SubsequentIntervalUpdates(workoutData: List<CustomIntervalData>) {
    workoutData.forEachIndexed { index, stateData ->
        LaunchedEffect(stateData.primaryValue.value) {
            snapshotFlow { stateData.primaryValue.value }
                .drop(1) // ignore the initial composition
                .collect {
                    updateSubsequentPrimaryValues(index, workoutData)
                }
        }

        if (stateData.secondaryValue != null) {
            LaunchedEffect(stateData.secondaryValue.value) {
                snapshotFlow { stateData.secondaryValue.value }
                    .drop(1)
                    .collect {
                        updateSubsequentSecondaryValues(index, workoutData)
                    }
            }
        }
    }
}

private fun updateSubsequentPrimaryValues(
    changedIndex: Int,
    workoutData: List<CustomIntervalData>
) {
    // do nothing if it's the warmup
    if (changedIndex == 0) return
    val value = workoutData[changedIndex].primaryValue
    val cooldownIndex = workoutData.lastIndex

    var i = changedIndex + 2
    while (i < workoutData.size) {
        if (i != cooldownIndex) workoutData[i].primaryValue.value = value.value
        i += 2
    }
}

private fun updateSubsequentSecondaryValues(
    changedIndex: Int,
    workoutData: List<CustomIntervalData>
) {
    // do nothing if it's the warmup
    if (changedIndex == 0) return
    val value = workoutData[changedIndex].secondaryValue
    val cooldownIndex = workoutData.lastIndex

    var i = changedIndex + 2
    while (i < workoutData.size) {
        if (value != null && i != cooldownIndex) workoutData[i].secondaryValue?.value = value.value
        i += 2
    }
}

data class PillboxStates(
    val mainPrimaryState: MutableState<Float>? = null,
    val mainSecondaryState: MutableState<Float>? = null,
    val recoveryPrimaryState: MutableState<Float>? = null,
    val recoverySecondaryState: MutableState<Float>? = null,
)

@Composable
private fun getPillboxStates(
    selectedSprint: Int,
    workoutData: List<CustomIntervalData>
): PillboxStates {
    // for 0 just get warmup state
    // otherwise get both sprint and recovery
    if (selectedSprint <= 0) {
        return PillboxStates(
            workoutData[0].primaryValue,
            workoutData[0].secondaryValue
        )
    } else {
        val firstIndex = selectedSprint * 2 - 1 // should be 1 when sprint 1
        val secondIndex = firstIndex + 1

        return PillboxStates(
            workoutData[firstIndex].primaryValue,
            workoutData[firstIndex].secondaryValue,
            workoutData[secondIndex].primaryValue,
            workoutData[secondIndex].secondaryValue,
        )
    }
}

@Composable
fun CustomWorkoutListItem(borderColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = CUSTOM_WORKOUT_LIST_ITEM_COLOR,
                shape = RoundedCornerShape(18.dp.computeSize(1920f))
            )
            .border(
                width = 1.dp.computeSize(1920f), color = borderColor,
                shape = RoundedCornerShape(18.dp.computeSize(1920f))
            )
    ) {
        Text(
            text = stringResource(R.string.workout_1_tc),
            fontSize = 22.sp.computeSize(1920f),
            fontFamily = FontFamily.SansSerif,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                vertical = 29.dp.computeSize(1920f),
                horizontal = 36.dp.computeSize(1920f)
            )
        )
    }
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun CustomPillBoxGridComp(
    statInfo: CustomStatInfo,
    selectedSprint: Int,
    pillboxStates: PillboxStates,
    workoutData: List<CustomIntervalData>,
    onCustomWorkoutStart: (speedOrResistanceData: List<Float>, inclineData: List<Float>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    onDeleteSegment: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = SPRINT8_DISABLED_BTN_CONTENT_COLOR,
                RoundedCornerShape(32.dp.computeSize(1920f))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp.computeSize(1920f)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                items(2) { index ->
                    val pillboxState = if (index == 0) pillboxStates.mainPrimaryState else pillboxStates.mainSecondaryState
                    val statPair = if (index == 0) statInfo.primaryStat else statInfo.secondaryStat

                    if (pillboxState != null && statPair != null) {
                        val title = getSprintPillboxTitle(statPair,selectedSprint)
                        val stat = statPair.second
                        PillBox(
                            modifier = modifier
                                .height(98.dp.computeSize(1920f))
                                .width(330.dp.computeSize(1920f)),
                            pillboxValue = pillboxState,
                            titleLabel = title,
                            unitLabel = stat.unitString,
                            stepValue = stat.stepSize,
                            decimalPlaces = stat.precision,
                            displayType = NumPadDisplay.Number,
                            min = stat.min,
                            max = stat.max,
                            forceOnlyStepValues = true,
                            usePercentageBar = true,
                            testTag = "sprint 8 pill box",
                            style = PillBoxStyle(
                                borderStroke = BorderStroke(
                                    1.dp.computeSize(1920f),
                                    color = Color.White
                                ),
                                dividerStroke = BorderStroke(
                                    1.dp.computeSize(1920f),
                                    color = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp.computeSize(1920f)),
                                labelTextStyle = TextStyle.Default.copy(color = Color.White),
                                valueBackground = Color.Black,
                                valueTextStyle = TextStyle.Default.copy(color = Color.White),
                                valueIconId = com.jht.widgets.R.drawable.ic_keypad_black,
                                valueBar = Color(0xFFffa600),
                                buttonBackground = Color.Black,
                                buttonForeground = Color(0xFFffa600)
                            )
                        )
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        vertical = 24.dp.computeSize(1920f),
                        horizontal = 24.dp.computeSize(1920f)
                    ),
                thickness = 1.dp.computeSize(1920f),
                color = CUSTOM_WORKOUT_LIST_BORDER_COLOR
            )
            LazyColumn(verticalArrangement = Arrangement.SpaceBetween) {
                items(2) { index ->
                    val pillboxState = if (index == 0) pillboxStates.recoveryPrimaryState else pillboxStates.recoverySecondaryState
                    val statPair = if (index == 0) statInfo.primaryStat else statInfo.secondaryStat

                    if (pillboxState != null && statPair != null) {
                        val lastSprint = ((workoutData.size - 1) / 2)
                        val title = getRecoveryPillboxTitle(statPair,selectedSprint == lastSprint)
                        val stat = statPair.second

                        PillBox(
                            modifier = modifier
                                .height(98.dp.computeSize(1920f))
                                .width(330.dp.computeSize(1920f)),
                            pillboxValue = pillboxState,
                            titleLabel = title,
                            unitLabel = stat.unitString,
                            stepValue = stat.stepSize,
                            decimalPlaces = stat.precision,
                            displayType = NumPadDisplay.Number,
                            min = stat.min,
                            max = stat.max,
                            forceOnlyStepValues = true,
                            usePercentageBar = true,
                            testTag = "sprint 8 pill box",
                            style = PillBoxStyle(
                                borderStroke = BorderStroke(
                                    1.dp.computeSize(1920f),
                                    color = Color.White
                                ),
                                dividerStroke = BorderStroke(
                                    1.dp.computeSize(1920f),
                                    color = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp.computeSize(1920f)),
                                labelTextStyle = TextStyle.Default.copy(color = Color.White),
                                valueBackground = Color.Black,
                                valueTextStyle = TextStyle.Default.copy(color = Color.White),
                                valueIconId = com.jht.widgets.R.drawable.ic_keypad_black,
                                valueBar = Color(0xFF8cf048),
                                buttonBackground = Color.Black,
                                buttonForeground = Color(0xFF8cf048)
                            )
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .padding(
                        top = 32.dp.computeSize(1920f),
                        bottom = 32.dp.computeSize(1920f),
                        end = 16.dp.computeSize(1920f)
                    ),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                if (selectedSprint!=0){
                    Box(
                        modifier = Modifier
                            .size(64.dp.computeSize(1920f))
                            .background(
                                color = CUSTOM_WORKOUT_SAVE_BTN_COLOR,
                                shape = RoundedCornerShape(
                                    18.dp.computeSize(1920f)
                                )
                            )
                            .clickable(onClick = onDeleteSegment)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp.computeSize(1920f))
                        )
                    }
                }
                StartWorkoutButton {
                    val primaryList = mutableListOf<Float>()
                    val secondaryList = mutableListOf<Float>()
                    for(data in workoutData){
                        primaryList.add(data.primaryValue.value)
                        data.secondaryValue?.let { it1 -> secondaryList.add(it1.value) }
                    }
                    onCustomWorkoutStart(primaryList,secondaryList)
                }
            }
        }
    }
}

@Composable
private fun getSprintPillboxTitle(paramPair: Pair<MinMaxParam, MinMaxInfo>, selectedSprint: Int): String {
    val title = when (paramPair.first) {
        MinMaxParam.Speed -> { if (selectedSprint==0)stringResource(R.string.warm_up_speed_tc) else stringResource(R.string.sprint_speed_tc) }
        MinMaxParam.Incline -> {if (selectedSprint==0)stringResource(R.string.warm_up_incline_tc)  else stringResource(R.string.sprint_incline_tc)  }
        MinMaxParam.Resistance -> { if (selectedSprint==0)stringResource(R.string.warm_up_resistance_tc)  else stringResource(R.string.sprint_resistance_tc)  }
        else -> { "invalid param" }
    }
    return if (paramPair.second.unitString.isNotEmpty()) {
        "$title (${paramPair.second.unitString})"
    } else {
        title
    }
}

@Composable
private fun getRecoveryPillboxTitle(
    paramPair: Pair<MinMaxParam, MinMaxInfo>,
    isLastSprint: Boolean
): String {
    val title = when (paramPair.first) {
        MinMaxParam.Speed -> { if (isLastSprint) stringResource(R.string.cooldown_speed_tc) else stringResource(R.string.recovery_speed_tc)}
        MinMaxParam.Incline -> {if (isLastSprint) stringResource(R.string.cooldown_incline_tc) else stringResource(R.string.recovery_incline_tc) }
        MinMaxParam.Resistance -> { if (isLastSprint) stringResource(R.string.cooldown_resistance_tc) else stringResource(R.string.recovery_resistance_tc)}
        else -> { "invalid param" }
    }
    return if (paramPair.second.unitString.isNotEmpty()) {
        "$title (${paramPair.second.unitString})"
    } else {
        title
    }
}

@AllDevicePreviews
@Composable
private fun CustomWorkoutPreview() {
    val customStatInfo = CustomStatInfo(
        primaryStat = Pair(MinMaxParam.Speed, MinMaxInfo()),
        secondaryStat = Pair(MinMaxParam.Incline, MinMaxInfo())
    )

    CustomWorkoutScreen(
        FrameType.Treadmill,
        customStats = customStatInfo,
    )
}