package com.jht.sprint8.sprint8classic.components


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.FrameType
import com.jht.sprint8.R
import com.jht.sprint8.sprint8classic.ui.CustomIntervalData
import com.jht.sprint8.utils.Constants.COOLDOWN_TIME
import com.jht.sprint8.utils.Constants.RECOVERY_TIME
import com.jht.sprint8.utils.Constants.SECONDS_30
import com.jht.sprint8.utils.Constants.WARMUP_TIME
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_CLOCK_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_10
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_BAR_COLOR_2
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

private val sprintColorBrush = Brush.verticalGradient(
    listOf(
        SPRINT_BAR_COLOR_1,
        SPRINT_BAR_COLOR_2
    )
)

private val cooldownColorBrush = Brush.verticalGradient(
    listOf(
        COOLDOWN_CLOCK_ICON_COLOR,
        COOLDOWN_BAR_COLOR_2
    )
)

private val recoveryColorBrush = Brush.verticalGradient(
    listOf(
        RECOVERY_ICON_BORDER_COLOR,
        SPRINT8_BG_10
    )
)

private val warmupColorBrush = Brush.verticalGradient(
    listOf(
        WARMUP_BAR_COLOR_1,
        WARMUP_BAR_COLOR_2
    )
)

private val selectedBrush = Brush.verticalGradient(
    listOf(
        Color(0x00ffffff),
        Color(0x26ffffff),
    )
)

private const val MIN_BAR_HEIGHT_FRACTION = 0.16f
private const val MAX_BAR_HEIGHT_FRACTION = 0.83f

/**
 * Displays a customizable bar graph for visualizing interval-based workout data.
 *
 * This composable renders a bar chart representing a list of workout intervals,
 * allows selection of a specific sprint/interval, and provides callbacks for
 * changing the selected index or adding new intervals. The graph supports
 * configurable value ranges, formatting, and frame styling.
 *
 * @param workoutData List of [CustomIntervalData] representing the values to be
 * displayed in the bar graph.
 * @param selectedSprint Index of the currently selected sprint/interval.
 * @param onChangeIndex Callback invoked when a different bar/interval is selected.
 * The selected index is provided as a parameter.
 * @param onAddInterval Callback invoked when the user requests to add a new interval.
 * @param frameType Defines the visual frame or border style applied to the graph.
 * @param modifier Modifier applied to the root bar graph composable for layout
 * and styling.
 * @param maxBarValue Maximum value used to scale the height of the bars.
 * @param minBarValue Minimum value used to scale the height of the bars.
 * @param formatValues Lambda used to format bar values for display (e.g., labels
 * or tooltips).
 */
@Composable
internal fun CustomBarGraphComponent(
    workoutData: List<CustomIntervalData>,
    selectedSprint: Int,
    onChangeIndex: (Int) -> Unit,
    onAddInterval: () -> Unit,
    frameType: FrameType,
    modifier: Modifier = Modifier,
    maxBarValue: Float = 15f,
    minBarValue: Float = 0f,
    formatValues: (Float) -> String = { it.toString() },
) {
    // Zeplin design shows horizontal guidelines at fixed step values (3, 6, 9, ...)
    // Here, I'm computing a rounded-up "visual" max so both bars and guidelines use the same scale.
    val guidelineStep = 3f
    val visualMaxBarValue = remember(maxBarValue, guidelineStep) {
        if (maxBarValue <= 0f) {
            guidelineStep
        } else {
            val steps = kotlin.math.ceil(maxBarValue / guidelineStep)
            (steps * guidelineStep)
        }
    }

    CustomBarChart(
        workoutData = workoutData,
        selectedSprint = selectedSprint,
        maxBarValue = visualMaxBarValue,
        minBarValue = minBarValue,
        onChangeIndex = onChangeIndex,
        onAddInterval = onAddInterval,
        formatValues = formatValues,
        frameType = frameType,
        modifier = modifier
    )
}

// Places all the custom segments on a chart
@Composable
private fun CustomBarChart(
    workoutData: List<CustomIntervalData>,
    selectedSprint: Int,
    maxBarValue: Float,
    minBarValue: Float,
    onChangeIndex: (Int) -> Unit,
    onAddInterval: () -> Unit,
    frameType: FrameType,
    formatValues: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                WarmupBars(
                    value = workoutData[0].primaryValue.value,
                    maxBarValue = maxBarValue,
                    minBarValue = minBarValue,
                    formatValues = formatValues,
                    barIcon = R.drawable.ic_speedometer,
                    isSelected = selectedSprint == 0,
                    modifier = Modifier.weight(0.5f),
                    onClickSegment = onChangeIndex,
                )

                val segmentValues = getSegmentValues(workoutData)

                // Do a sanity check on selected index when removing intervals
                LaunchedEffect(segmentValues) {
                    if (selectedSprint > segmentValues.size) {
                        onChangeIndex(segmentValues.size - 1)
                    }
                }

                for (i in 0..<8) {
                    Spacer(modifier = Modifier.width(20.dp.computeSizeHd()))

                    val segmentValue = segmentValues.getOrNull(i)
                    SegmentBars(
                        segmentValues = segmentValue,
                        maxBarValue = maxBarValue,
                        minBarValue = minBarValue,
                        formatValues = formatValues,
                        barIcon = R.drawable.ic_speedometer,
                        isCooldownSegment = i == segmentValues.size - 1,
                        segmentNumber = i + 1,
                        isSelected = selectedSprint == i + 1,
                        modifier = Modifier.weight(1f),
                        onClickSegment = onChangeIndex,
                        numSegments = segmentValues.size,
                        onAddSegment = onAddInterval
                    )
                }
            }
        }

        // Time row below bars
        Row {
            WarmupTimes(
                modifier = Modifier.weight(0.5f)
            )

            val segmentValues = getSegmentValues(workoutData)
            for (i in 0..<8) {
                Spacer(modifier = Modifier.width(20.dp.computeSizeHd()))

                val isCooldown = i == segmentValues.size - 1
                SegmentTimes(
                    isFilled = segmentValues.getOrNull(i) != null,
                    isCooldownSegment = isCooldown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun getSegmentValues(workoutData: List<CustomIntervalData>): List<SegmentValues> {
    val numSegments = (workoutData.size - 1) / 2
    val segmentValues = mutableListOf<SegmentValues>()
    for (i in 0..<numSegments) {
        val sprintIndex = i * 2 + 1
        val recoveryIndex = sprintIndex + 1
        segmentValues.add(
            SegmentValues(
                workoutData[sprintIndex].primaryValue.value,
                workoutData[recoveryIndex].primaryValue.value
            )
        )
    }

    return segmentValues
}

// The warmup segment in sprint8
@Composable
private fun WarmupBars(
    value: Float,
    maxBarValue: Float,
    minBarValue: Float,
    formatValues: (Float) -> String,
    barIcon: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClickSegment: (selectedSegment: Int) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickableNoEffect(onClick = { onClickSegment(0) })
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // sprint bar
            CustomBar(
                barValue = value,
                maxBarValue = maxBarValue,
                minBarValue = minBarValue,
                formatValues = formatValues,
                barIcon = barIcon,
                color = warmupColorBrush,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isSelected) 1f else 0.6f),
            )
        }

        if (isSelected) {
            // Selected overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectedBrush)
            )
        }
    }
}

/**
 * Warmup times row only
 */
@Composable
private fun WarmupTimes(
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text = WARMUP_TIME,
            fontSize = 14.sp.computeSize(1920f),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
    }
}

data class SegmentValues(
    val sprintValue: Float,
    val recoveryValue: Float,
)

@Composable
private fun SegmentBars(
    segmentValues: SegmentValues?,
    maxBarValue: Float,
    minBarValue: Float,
    formatValues: (Float) -> String,
    barIcon: Int,
    isCooldownSegment: Boolean,
    segmentNumber: Int,
    numSegments: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClickSegment: (selectedSegment: Int) -> Unit,
    onAddSegment: () -> Unit,
) {
    val isFilledSegment = segmentValues != null
    if (isFilledSegment) {
        val recoveryColor = if (isCooldownSegment) cooldownColorBrush else recoveryColorBrush
        Box(
            modifier = modifier
                .fillMaxHeight()
                .clickableNoEffect(onClick = { onClickSegment(segmentNumber) })
        ) {
            // segment number
            Text(
                text = segmentNumber.toString(),
                fontSize = 20.sp.computeSize(1920f),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp.computeSize(1920f)),
            )

            if (segmentValues != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // sprint bar
                    CustomBar(
                        barValue = segmentValues.sprintValue,
                        maxBarValue = maxBarValue,
                        minBarValue = minBarValue,
                        formatValues = formatValues,
                        barIcon = barIcon,
                        color = sprintColorBrush,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (isSelected) 1f else 0.6f),
                    )

                    Spacer(modifier = Modifier.width(6.dp.computeSize(1920f)))

                    // recovery bar
                    CustomBar(
                        barValue = segmentValues.recoveryValue,
                        maxBarValue = maxBarValue,
                        minBarValue = minBarValue,
                        formatValues = formatValues,
                        barIcon = barIcon,
                        color = recoveryColor,
                        modifier = Modifier
                            .weight(2f)
                            .alpha(if (isSelected) 1f else 0.6f),
                    )
                }

                if (isSelected) {
                    // Selected overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(selectedBrush)
                    )
                }
            }
        }
    } else {
        // Empty segment
        EmptySegment(
            segmentNum = segmentNumber,
            numSegments = numSegments,
            modifier = modifier,
            onAddSegment = onAddSegment,
        )
    }
}

@Composable
private fun SegmentTimes(
    isFilled: Boolean,
    isCooldownSegment: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isFilled) {
        // Keep spacing consistent for empty segments
        Spacer(modifier = modifier)
        return
    }

    Row(modifier = modifier) {
        // sprint time
        Text(
            text = SECONDS_30,
            fontSize = 14.sp.computeSize(1920f),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp.computeSize(1920f)))

        // recovery time
        Text(
            text = if (isCooldownSegment) {
                COOLDOWN_TIME
            } else {
                RECOVERY_TIME
            },
            fontSize = 14.sp.computeSize(1920f),
            color = Color.White,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun EmptySegment(
    segmentNum: Int,
    numSegments: Int,
    modifier: Modifier = Modifier,
    onAddSegment: () -> Unit,
) {
    val isFirstEmptySegment = segmentNum == numSegments + 1
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickableNoEffect(onClick = { if (isFirstEmptySegment) onAddSegment() }),
        contentAlignment = Alignment.Center) {

        if (isFirstEmptySegment) {
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
    }
}

// A single bar in the custom workout chart
@Composable
private fun CustomBar(
    barValue: Float,
    maxBarValue: Float,
    minBarValue: Float,
    formatValues: (Float) -> String,
    barIcon: Int,
    color: Brush,
    modifier: Modifier = Modifier,
    minBarHeight: Float = MIN_BAR_HEIGHT_FRACTION,
    maxBarHeight: Float = MAX_BAR_HEIGHT_FRACTION
) {
    val targetBarHeight = remember(barValue, maxBarValue) {
        val barPct = ((barValue - minBarValue) / (maxBarValue - minBarValue)).coerceIn(0f, 1f)
        val actualPct = minBarHeight + (maxBarHeight - minBarHeight) * barPct
        actualPct
    }
    val animatedBarHeight = animateFloatAsState(targetBarHeight)
    val formattedValue = remember(barValue) { formatValues(barValue) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        // draw background bar
        Box(
            modifier = Modifier
                .fillMaxHeight(animatedBarHeight.value)
                .fillMaxWidth()
                .background(
                    color,
                    shape = RoundedCornerShape(
                        topStart = 8.dp.computeSize(1920f),
                        topEnd = 8.dp.computeSize(1920f)
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(barIcon),
                contentDescription = null,
                modifier = Modifier
                    .testTag("SpeedometerImage:Image"),
            )
            Text(
                text = formattedValue,
                fontSize = 14.sp.computeSize(1920f),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Normal,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .padding(bottom = 3.dp.computeSize(1920f))
                    .wrapContentSize()
                    .testTag("Value:Text"),

                )
        }
    }
}

@Composable
private fun BarGuidelines(
    maxBarValue: Float,
    minBarValue: Float,
    step: Float = 3f,
) {
    if (maxBarValue <= minBarValue || step <= 0f) return

    val density = LocalDensity.current

    val guidelineValues = remember(maxBarValue, step) {
        val maxStep = kotlin.math.ceil(maxBarValue / step).toInt()
        (1..maxStep).map { it * step }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasHeight = size.height
        val canvasWidth = size.width

        val labelPaddingPx = with(density) { 4.dp.toPx() }
        val textSizePx = with(density) { 10.sp.toPx() }
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = textSizePx
            color = android.graphics.Color.WHITE
        }

        guidelineValues.forEach { value ->
            val barPct = ((value - minBarValue) / (maxBarValue - minBarValue))
                .coerceIn(0f, 1f)
            val actualPct = MIN_BAR_HEIGHT_FRACTION +
                    (MAX_BAR_HEIGHT_FRACTION - MIN_BAR_HEIGHT_FRACTION) * barPct
            val y = canvasHeight * (1f - actualPct)

            val label = value.toInt().toString()
            drawContext.canvas.nativeCanvas.drawText(
                label,
                canvasWidth - labelPaddingPx,
                y - labelPaddingPx,
                paint
            )
        }
    }
}

@Preview(widthDp = 406, heightDp = 295)
@Composable
private fun GraphPreview() {
    val frameType = FrameType.Treadmill
    val testData = getDefaultWorkout()
    CustomBarChart(
        workoutData = testData,
        selectedSprint = 2,
        onChangeIndex = { },
        onAddInterval = {},
        frameType = frameType,
        formatValues = { it.toString() },
        maxBarValue = 15f,
        minBarValue = 0f,
    )
}

@Preview(widthDp = 406, heightDp = 295)
@Composable
private fun GraphPreviewShort() {
    val frameType = FrameType.Treadmill
    val testData = getDefaultShortWorkout()
    CustomBarChart(
        workoutData = testData,
        selectedSprint = 2,
        onChangeIndex = { },
        onAddInterval = {},
        frameType = frameType,
        formatValues = { it.toString() },
        maxBarValue = 15f,
        minBarValue = 0f,
    )
}

private fun getDefaultWorkout(): List<CustomIntervalData> {
    return mutableListOf(
        CustomIntervalData(mutableStateOf(1f)), // warmup
        CustomIntervalData(mutableStateOf(5f)), // sprint1
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint2
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint3
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint4
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint5
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint6
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint7
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint8
        CustomIntervalData(mutableStateOf(1.5f)), // cooldown
    )
}


private fun getDefaultShortWorkout(): List<CustomIntervalData> {
    return mutableListOf(
        CustomIntervalData(mutableStateOf(1f)), // warmup
        CustomIntervalData(mutableStateOf(5f)), // sprint1
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint2
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint3
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint4
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint5
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint6
        CustomIntervalData(mutableStateOf(1.5f)), // cooldown
    )
}