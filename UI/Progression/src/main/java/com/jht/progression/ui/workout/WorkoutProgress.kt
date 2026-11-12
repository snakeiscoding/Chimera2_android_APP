package com.jht.progression.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.progression.ui.MmColors
import com.jht.ui.pxToDp
import com.jht.widgets.scaleSize

@Composable
fun WorkoutProgress(
    intervalList: MmIntervalList,
    timeElapsedMs: Int,
    currentInterval: Int,
    modifier: Modifier = Modifier,
) {
    val totalTimeMs = remember(intervalList) {
        intervalList.intervalList.sumOf { it.goalValue.toInt() }
    }
    val progress = timeElapsedMs.toFloat() / totalTimeMs.toFloat()

    Column(modifier = modifier) {
        ProgressArrow(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp.scaleSize())
                .testTag("workout_interval_progress_arrow")
        )

        WorkoutZones(
            intervalList = intervalList,
            currentInterval = currentInterval,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp.scaleSize())
                .testTag("workout_interval_progress_bars")
        )
    }
}

@Composable
fun ProgressArrow(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val lineWidth = constraints.maxWidth.toFloat()

        // Clamp progress just in case
        val clampedProgress = progress.coerceIn(0f, 1f)

        // calculate the position of the progress arrow
        val arrowPosition =
            with(LocalDensity.current) { (lineWidth * clampedProgress).toDp() } - 6.dp.scaleSize()

        Box(
            modifier = Modifier
                .offset(x = arrowPosition)
                .size(width = 12.dp.scaleSize(), height = 8.dp.scaleSize())
                .background(Color.White, shape = triangleShape())
        )
    }
}

private fun triangleShape() = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}

@Composable
private fun WorkoutZones(
    intervalList: MmIntervalList,
    currentInterval: Int,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
    ) {
        //create the bar of current workout zones
        intervalList.intervalList.forEachIndexed { index, interval ->

            // Apply 50% opacity to completed intervals
            val alpha = if (index < currentInterval) 0.5f else 1.0f

            Box(
                modifier = Modifier
                    .weight(interval.goalValue)
                    .fillMaxHeight()
                    .padding(horizontal = 1.pxToDp().scaleSize())
                    .alpha(alpha)
                    .background(
                        getIntervalBrush(interval.intervalType),
                        RoundedCornerShape(2.dp.scaleSize())
                    )
            )
        }
    }
}

val gradientZone1 = Brush.verticalGradient(listOf(MmColors.zone1Color, Color(0x4C7169FF)))
val gradientZone2 = Brush.verticalGradient(listOf(MmColors.zone2Color, Color(0x4C00DBFF)))
val gradientZone3 = Brush.verticalGradient(listOf(MmColors.zone3Color, Color(0x4C33DB95)))
val gradientZone4 = Brush.verticalGradient(listOf(MmColors.zone4Color, Color(0x4CF0B323)))
val gradientZone5 = Brush.verticalGradient(listOf(MmColors.zone5Color, Color(0x4CF5502B)))

private fun getIntervalBrush(zone: IntervalType): Brush {
    return when (zone) {
        IntervalType.WARMUP, IntervalType.COOLDOWN, IntervalType.RACE, IntervalType.ASSESSMENT -> gradientZone1
        IntervalType.ZONE_1 -> gradientZone1
        IntervalType.ZONE_2 -> gradientZone2
        IntervalType.ZONE_3 -> gradientZone3
        IntervalType.ZONE_4 -> gradientZone4
        IntervalType.ZONE_5 -> gradientZone5
    }
}

@Preview
@Composable
private fun Preview() {
    WorkoutProgress(createIntervalList(), 150000, 0, Modifier.fillMaxSize())
}

private fun createIntervalList(): MmIntervalList {
    val intervals = arrayListOf(
        ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f),
        ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 200000f),
        ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 300000f),
        ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 400000f),
        ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 200000f),
        ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 500000f),
        ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, 100000f),
        ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 200000f),
        ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f),
    )
    return MmIntervalList(intervals)
}

