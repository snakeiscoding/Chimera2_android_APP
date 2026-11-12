package com.jht.progression.ui.setup.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.IntervalType
import com.jht.data.EventIndex
import com.jht.progression.R
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.oxaniumStyle
import com.jht.progression.robotoStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmOptionDialog
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.progression.util.createTintedPainter
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

val gradientZone1 = Brush.verticalGradient(listOf(Color(0xFF7169FF), Color(0xFF272B3D)))
val gradientZone2 = Brush.verticalGradient(listOf(Color(0xFF00DBFF), Color(0xFF05465F)))
val gradientZone3 = Brush.verticalGradient(listOf(Color(0xFF33DB95), Color(0xFF1E5A41)))
val gradientZone4 = Brush.verticalGradient(listOf(Color(0xFFFFCC54), Color(0xFF67552D)))
val gradientZone5 = Brush.verticalGradient(listOf(Color(0xFFF5502B), Color(0xFF38211E)))
const val designWidth = 1920f

@Composable
fun WorkoutPreviewScreen(
    viewModel: PreviewViewModel,
    onBackPressed: () -> Unit,
    launchWorkout: (WorkoutInfo, EventIndex) -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()
    var skipDialogOpen by remember { mutableStateOf(false) }

    PreviewScreenFrame(
        uiState = uiState,
        onBackPressed = onBackPressed,
        onStartWorkout = { viewModel.startWorkout(launchWorkout) },
        onSkipToWorkout = {
            skipDialogOpen = true
        }
    )

    MmOptionDialog(
        open = skipDialogOpen,
        icon = createTintedPainter(R.drawable.ic_lock_mm, MmColors.futuristicBlue),
        titleText = stringResource(id = R.string.mm_start_future_workout_tc),
        bodyText = stringResource(R.string.mm_start_future_workout_pg),
        acceptButtonText = stringResource(R.string.start_uc),
        dismissButtonText = stringResource(R.string.cancel_uc),
        onDismiss = {
            skipDialogOpen = false
        },
        onYesClicked = {
            skipDialogOpen = false
            viewModel.skipToWorkout()
            viewModel.startWorkout(launchWorkout)
        }
    )
}

@Composable
fun PreviewScreenFrame(
    uiState: MmPreviewState,
    onBackPressed: () -> Unit,
    onStartWorkout: () -> Unit,
    onSkipToWorkout: () -> Unit,
) {
    val topBarColor: Color = getPreviewTopBarColor(uiState.previewState)
    val backgroundId: Int = getPreviewBackgroundId(uiState.previewState)

    MmSetupFrame(
        topBarColor = topBarColor,
        backgroundId = backgroundId,
        onBackPressed = onBackPressed
    ) {
        WorkoutPreview(
            uiState = uiState,
            onBackPressed = onBackPressed,
            onStartWorkout = onStartWorkout,
            onSkipToWorkout = onSkipToWorkout
        )
    }
}

@Composable
fun getPreviewTopBarColor(previewState: PreviewState): Color {
    return when (previewState) {
        PreviewState.STATE_ACTIVE -> Color(0xFF010F1B)
        PreviewState.STATE_FUTURE -> Color(0xFF191919)
        PreviewState.STATE_COMPLETED -> Color(0xFF011B0D)
    }
}

@Composable
fun getPreviewBackgroundId(previewState: PreviewState): Int {
    return when (previewState) {
        PreviewState.STATE_ACTIVE -> R.drawable.mm_squares_bg
        PreviewState.STATE_FUTURE -> R.drawable.mm_squares_bg_gray
        PreviewState.STATE_COMPLETED -> R.drawable.mm_squares_bg_green
    }
}

@Composable
fun WorkoutPreview(
    uiState: MmPreviewState,
    onBackPressed: () -> Unit,
    onStartWorkout: () -> Unit,
    onSkipToWorkout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp.scaleSize(designWidth)), contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(id = uiState.workoutPreviewTitle),
                color = Color.White,
                fontSize = 46.sp.scaleSize(designWidth),
                style = oxaniumStyle.copy(
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 54.sp.scaleSize(designWidth)
                ),
                modifier = Modifier
                    .testTag("workout_preview_title")
                    .fillMaxWidth()
                    .padding(bottom = 16.dp.scaleSize(designWidth))
                    .height(54.dp.scaleSize(designWidth))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp.scaleSize(designWidth)), contentAlignment = Alignment.Center
        ) {
            WorkoutInfoBox(
                useMetric = uiState.useMetric,
                totalTime = uiState.totalTime,
                intervals = uiState.intervals
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(242.dp.scaleSize(designWidth))
                .padding(top = 40.dp.scaleSize(designWidth))
            , contentAlignment = Alignment.TopCenter
        ) {
            BottomButton(
                modifier = Modifier
                    .size(290.dp.scaleSize(designWidth), 105.dp.scaleSize(designWidth)),
                previewState = uiState.previewState,
                onBackPressed = onBackPressed,
                onStartWorkout = onStartWorkout,
                onSkipToWorkout = onSkipToWorkout
            )
        }
    }
}

@Composable
private fun BottomButton(
    modifier: Modifier = Modifier,
    previewState: PreviewState,
    onBackPressed: () -> Unit,
    onStartWorkout: () -> Unit,
    onSkipToWorkout: () -> Unit,
) {
    val buttonTextId: Int = when (previewState) {
        PreviewState.STATE_ACTIVE -> R.string.start_uc
        PreviewState.STATE_COMPLETED -> R.string.back_uc
        PreviewState.STATE_FUTURE -> R.string.start_uc
    }

    val buttonColor: Color = when (previewState) {
        PreviewState.STATE_ACTIVE -> MmColors.futuristicBlue
        PreviewState.STATE_COMPLETED -> Color.White
        PreviewState.STATE_FUTURE -> Color.White
    }
    if (previewState != PreviewState.STATE_COMPLETED) {

        MmThemedButton(
            fitBackgroundToBounds = true,
            text = stringResource(buttonTextId),
            onClick = {
                onButtonClickEvent(
                    previewState = previewState,
                    onBackPressed = onBackPressed,
                    onSkipToWorkout = onSkipToWorkout,
                    onStartWorkout = onStartWorkout
                )
            },
            color = buttonColor,
            modifier = modifier.testTag(stringResource(buttonTextId).lowercase() + "_button"),
            textStyle = TextStyle(fontSize = 34.sp.scaleSize(designWidth), fontWeight = FontWeight.Bold),
            leadingIcon = if (previewState == PreviewState.STATE_FUTURE) {
                {
                    Icon(
                        modifier = Modifier
                            .padding(end = 16.dp.scaleSize(designWidth))
                            .size(40.dp.scaleSize(designWidth))
                            .testTag("speed_icon"),
                        painter = painterResource(id = R.drawable.ic_lock_mm),
                        contentDescription = "button_Icon",
                        tint = Color.Unspecified
                    )
                }
            } else null,
            iconSpacing = 8.dp.scaleSize(designWidth)
        )
    }

}

private fun onButtonClickEvent(
    previewState: PreviewState,
    onStartWorkout: () -> Unit,
    onBackPressed: () -> Unit,
    onSkipToWorkout: () -> Unit,
) {
    when (previewState) {
        PreviewState.STATE_ACTIVE -> onStartWorkout()
        PreviewState.STATE_COMPLETED -> onBackPressed()
        PreviewState.STATE_FUTURE -> onSkipToWorkout()
    }
}

@Composable
private fun WorkoutInfoBox(
    useMetric: Boolean,
    totalTime: String,
    intervals: List<PreviewViewModel.IntervalState>,
) {
    Box(
        modifier = Modifier.size(1665.dp.scaleSize(designWidth), 640.dp.scaleSize(designWidth)),
        contentAlignment = Alignment.BottomCenter
    ) {

        Image(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(0x80000000),
                    shape = CutCornerShape(40.dp.scaleSize(designWidth))
                )
                .testTag("workout_preview_info_box"),
            painter = painterResource(id = R.drawable.mm_workout_preview_graph_bg),
            contentDescription = "stat background",
            contentScale = ContentScale.FillBounds,
            alpha = 0.5f
        )

        // This is the placeholder for the graph
        WorkoutPreviewBarGraph(
            useMetric = useMetric,
            totalTime = totalTime,
            intervals = intervals
        )
    }
}

@Composable
private fun WorkoutPreviewBarGraph(
    useMetric: Boolean,
    totalTime: String,
    intervals: List<PreviewViewModel.IntervalState>,
) {
    // Y-axis values
    var yValues = listOf(3, 6, 9, 12, 15)
    if (useMetric) {
        yValues = listOf(5, 10, 15, 20, 25)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(1030.dp.scaleSize())
                .fillMaxHeight(), contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .size(20.dp.scaleSize(), 366.dp.scaleSize())
                    .padding(top = 15.dp.scaleSize())
                    .testTag("bar_graph_y_axis"),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier
                        .padding(bottom = 13.dp.scaleSize(), top = 6.dp.scaleSize())
                        .size(20.dp.scaleSize())
                        .testTag("speed_icon"),
                    painter = painterResource(id = R.drawable.ic_speed_white),
                    contentDescription = "Workout Preview Speed Icon",
                    tint = Color(0xFF999999)
                )

                yValues.reversed().forEach { value ->
                    Text(
                        text = "$value",
                        color = Color(0xFF999999),
                        style = robotoStyle.copy(
                            fontSize = 16.sp.scaleSize(),
                            lineHeight = 24.sp.scaleSize()
                        ),
                    )
                    Spacer(modifier = Modifier.height(33.dp.scaleSize()))
                }

            }

        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(857.dp.scaleSize(), 280.dp.scaleSize())
                    .padding(bottom = 3.dp.scaleSize()),
                contentAlignment = Alignment.Center
            ) {
                BarGraph(intervals, useMetric)
            }
            Row {
                Box(
                    modifier = Modifier.size(850.dp.scaleSize(), 130.dp.scaleSize()),
                    contentAlignment = Alignment.CenterStart
                )
                {
                    BarGraphIndex(useMetric, intervals)
                }
                Text(
                    text = "${stringResource(id = R.string.total_duration_tc)}: $totalTime",
                    color = Color(0xFF999999),
                    style = robotoStyle.copy(
                        fontSize = 16.sp.scaleSize(),
                        lineHeight = 24.sp.scaleSize()
                    ),
                    modifier = Modifier.padding(top = 26.dp.scaleSize())

                )
            }
        }
    }
}

@Composable
private fun BarGraph(
    intervals: List<PreviewViewModel.IntervalState>,
    useMetric: Boolean
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom
    ) {
        val maxYValue = if(useMetric) 25f else 15f
        intervals.forEachIndexed { index, interval ->
            // Calculate the height based on the max Y-axis value
            val speedValue = intervalSpeedValue(interval.speed, useMetric)
            val barHeightRatio = speedValue / maxYValue
            val maxBarHeight = 260.dp.scaleSize()
            val barHeight = maxBarHeight * barHeightRatio

            val barGradientActive: Brush = when (interval.intervalType) {
                IntervalType.ZONE_1 -> {
                    gradientZone1
                }

                IntervalType.ZONE_2 -> {
                    gradientZone2
                }

                IntervalType.ZONE_3 -> {
                    gradientZone3
                }

                IntervalType.ZONE_4 -> {
                    gradientZone4
                }

                IntervalType.ZONE_5 -> {
                    gradientZone5
                }

                else -> {
                    gradientZone1
                }
            }

            val borderColor1 = Color(0x00FFFFFF).copy(0f)
            val borderColor2 = Color(0x4CFFFFFF).copy(0.1f)
            val borderBrush = Brush.verticalGradient(listOf(borderColor1, borderColor2))
            Box(
                modifier = Modifier
                    .height(barHeight)
                    .padding(start = if (index == 0) 1.2.dp.scaleSize() else 2.2.dp.scaleSize())
                    .weight(interval.goalValue, true)
                    .background(
                        brush = barGradientActive,
                        shape = RoundedCornerShape(
                            topStart = 6.dp.scaleSize(),
                            topEnd = 6.dp.scaleSize(),
                            bottomStart = 3.dp.scaleSize(),
                            bottomEnd = 3.dp.scaleSize()
                        )
                    )
                    .border(
                        width = 1.dp.scaleSize(),
                        brush = borderBrush,
                        shape = RoundedCornerShape(
                            topStart = 6.dp.scaleSize(),
                            topEnd = 6.dp.scaleSize(),
                            bottomStart = 3.dp.scaleSize(),
                            bottomEnd = 3.dp.scaleSize()
                        )
                    )
                    .testTag("workout_preview_bars")
            )
        }
    }
}

@Composable
private fun BarGraphIndex(
    useMetric: Boolean,
    intervals: List<PreviewViewModel.IntervalState>,
) {
    // Get unique zones
    val uniqueZones = mutableSetOf<IntervalType>()
    val distinctIntervals = intervals.filter { uniqueZones.add(it.intervalType) }

    // Sort in order by zones name
    val sortedIntervals = distinctIntervals.sortedBy { interval ->
        when (interval.intervalType) {
            IntervalType.ZONE_1 -> 1
            IntervalType.ZONE_2 -> 2
            IntervalType.ZONE_3 -> 3
            IntervalType.ZONE_4 -> 4
            IntervalType.ZONE_5 -> 5
            else -> 6
        }
    }

    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        sortedIntervals.forEach { interval ->
            when (interval.intervalType) {
                IntervalType.ZONE_1 -> SetupBarGraphIndex(
                    stringResource(id = R.string.warmup_cooldown_tc),
                    MmColors.zone1Color,
                    goalValue = interval.goalValue,
                    intervalSpeed = interval.speed,
                    useMetric = useMetric
                )

                IntervalType.ZONE_2 -> SetupBarGraphIndex(
                    stringResource(id = R.string.recovery_tc),
                    MmColors.zone2Color,
                    goalValue = interval.goalValue,
                    intervalSpeed = interval.speed,
                    useMetric = useMetric
                )

                IntervalType.ZONE_3 -> SetupBarGraphIndex(
                    stringResource(id = R.string.moderate_tc),
                    MmColors.zone3Color,
                    goalValue = interval.goalValue,
                    intervalSpeed = interval.speed,
                    useMetric = useMetric
                )

                IntervalType.ZONE_4 -> SetupBarGraphIndex(
                    stringResource(id = R.string.intense_tc),
                    MmColors.zone4Color,
                    goalValue = interval.goalValue,
                    intervalSpeed = interval.speed,
                    useMetric = useMetric
                )

                IntervalType.ZONE_5 -> SetupBarGraphIndex(
                    stringResource(id = R.string.max_effort_tc),
                    MmColors.zone5Color,
                    goalValue = interval.goalValue,
                    intervalSpeed = interval.speed,
                    useMetric = useMetric
                )

                else -> {}
            }
        }
    }
}

private fun intervalSpeedValue(intervalSpeed: String, useMetric: Boolean): Float {
    return if(useMetric){
        intervalSpeed.toFloat().coerceIn(0.8f, 20.0f)
    } else{
        intervalSpeed.toFloat().coerceIn(0.5f, 12.5f)
    }
}

@Composable
private fun SetupBarGraphIndex(
    indexText: String,
    indexColor: Color,
    goalValue: Float,
    intervalSpeed: String,
    useMetric: Boolean,
) {
    val intervalMinutes = if (goalValue != 0f) (goalValue / 1000 / 60).toInt() else 0
    val timeLabel =
        if (intervalMinutes > 1) stringResource(R.string.minutes_lc) else stringResource(R.string.minute_lc)
    val speedUnit =
        if (useMetric) stringResource(R.string.kph_lc) else stringResource(R.string.mph_lc)
    Box(
        modifier = Modifier
            .size(30.dp.scaleSize(designWidth))
            .background(color = indexColor, shape = RoundedCornerShape(4.dp.scaleSize()))
            .testTag("bar_graph_index_box")
    )
    Column(
        modifier = Modifier
            .padding(start = 10.dp.scaleSize(), end = 45.dp.scaleSize())
            .testTag("bar_graph_index_text")
    ) {
        Text(
            text = indexText,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 5.dp.scaleSize()),
            style = robotoStyle.copy(fontSize = 16.sp.scaleSize(), lineHeight = 24.sp.scaleSize())
        )
        Text(
            text = "${intervalSpeedValue(intervalSpeed, useMetric)} $speedUnit",
            color = Color.White,
            modifier = Modifier
                .padding(bottom = 5.dp.scaleSize())
                .alpha(0.8f),
            style = robotoStyle.copy(fontSize = 16.sp.scaleSize(), lineHeight = 24.sp.scaleSize())
        )
        Text(
            text = "$intervalMinutes $timeLabel",
            color = Color.White,
            modifier = Modifier
                .padding(bottom = 5.dp.scaleSize())
                .alpha(0.8f),
            style = robotoStyle.copy(fontSize = 16.sp.scaleSize(), lineHeight = 24.sp.scaleSize())
        )
    }
}

@Composable
@AllDevicePreviews
private fun Preview() {
    val uiState = MmPreviewState(
        useMetric = true,
        previewState = PreviewState.STATE_FUTURE,
        totalTime = "30:00",
        workoutPreviewTitle = R.string.long_distance_workout_tc,
        intervals = listOf(
            PreviewViewModel.IntervalState(
                intervalType = IntervalType.ZONE_1,
                goalValue = 300000f,
                speed = "0.5"
            ),
            PreviewViewModel.IntervalState(
                intervalType = IntervalType.ZONE_2,
                goalValue = 240000f,
                speed = "3.5"
            ),
            PreviewViewModel.IntervalState(
                intervalType = IntervalType.ZONE_3,
                goalValue = 180000f,
                speed = "4.5"
            ),
            PreviewViewModel.IntervalState(
                intervalType = IntervalType.ZONE_4,
                goalValue = 120000f,
                speed = "5.2"
            ),
            PreviewViewModel.IntervalState(
                intervalType = IntervalType.ZONE_5,
                goalValue = 60000f,
                speed = "8.0"
            )
        )
    )

    PreviewScreenFrame(
        uiState = uiState,
        onBackPressed = {},
        onStartWorkout = {},
        onSkipToWorkout = {})
}