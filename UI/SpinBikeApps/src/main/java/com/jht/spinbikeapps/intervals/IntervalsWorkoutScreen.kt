package com.jht.spinbikeapps.intervals

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.ui.Device22inPreviews
import com.jht.ui.pxToDp

/**
 * Workout screen for the TCV Intervals workouts: live data on the left, previous intervals
 * table on the right. Values arrive pre-formatted, so the caller decides whether the dial
 * counts down time or distance.
 */
@Composable
fun IntervalsWorkoutScreen(
    /**
     * How much of the current interval is done
     */
    @FloatRange(from = 0.0, to = 1.0)
    completionProgress: Float,

    /**
     * Whether each interval counts down time or distance
     */
    intervalGoal: IntervalGoal,

    /**
     * Value in the middle of the countdown dial, pre-formatted (MM:SS or a distance)
     */
    mainText: String,

    /**
     * Accumulated value at the bottom of the screen, pre-formatted
     */
    subText: String,

    modifier: Modifier = Modifier,

    /**
     * Displays the interval value at the top of the screen
     */
    currentIntervalIndex: Int = 0,

    /**
     * Used to display "Work" or "Rest" above the Timer countdown
     */
    currentPhase: IntervalPhase = IntervalPhase.Other,

    /**
     * Displays previous intervals' data [IntervalData] on the side of the screen
     */
    previousIntervalsData: List<IntervalData>,

    /**
     * Used to display the correct units in the previous intervals table
     */
    isMetric: Boolean = true,

    /** Total distance covered in the workout, pre-formatted (e.g. "2.36") */
    totalDistanceText: String = "0.00",

    /** Total time elapsed in the workout, pre-formatted (e.g. "12:18") */
    totalTimeText: String = "0:00",
) {
    val accentColor = if (currentPhase == IntervalPhase.Rest) TargetTrainingColors.GREEN_1 else TargetTrainingColors.BTN_PRIMARY

    DynamicTheme(darkDynamicTheme) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(color = Color.Black),
            horizontalArrangement = Arrangement.Center
        ) {
            CountdownGraphic(
                intervalNum = currentIntervalIndex,
                phaseType = currentPhase,
                dialLabel = when {
                    currentPhase == IntervalPhase.Rest -> stringResource(R.string.time_remaining_tc)
                    intervalGoal == IntervalGoal.Time -> stringResource(R.string.time_remaining_tc)
                    else -> if (isMetric) {
                        stringResource(R.string.km_remaining_tc)
                    } else {
                        stringResource(R.string.miles_remaining_tc)
                    }
                },
                completionProgress = completionProgress,
                mainText = mainText,
                subText = subText,
                subTextUnit = if (isMetric) {
                    stringResource(R.string.km_lc)
                } else {
                    stringResource(R.string.mi_lc)
                },
                subTextStyle = currentDistanceValueTextStyle,
                accentColor = accentColor,
            )
            VerticalDivider(
                modifier = Modifier
                    .width(1.pxToDp())
                    .fillMaxHeight(),
                color = TargetTrainingColors.GRAY_8
            )
            PreviousIntervalsGraphic(
                intervalsData = previousIntervalsData,
                isMetric = isMetric,
                totalDistanceText = totalDistanceText,
                totalTimeText = totalTimeText,
            )
        }
    }
}

/**
 * Left side of the screen, contains the live workout data
 */
@Composable
private fun CountdownGraphic(
    intervalNum: Int,
    phaseType: IntervalPhase,
    dialLabel: String,
    completionProgress: Float,
    mainText: String,
    subText: String,
    subTextUnit: String,
    subTextStyle: TextStyle,
    accentColor: Color,
) {
    Column(
        modifier = Modifier
            .width(countdownGraphicWidth)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(
                    top = intervalRowPadding,
                    start = intervalRowPadding,
                    end = intervalRowPadding,
                    bottom = 0.dp,
                )
        ) {
            Text(
                text = stringResource(R.string.interval_number_tc, intervalNum),
                style = intervalNumStyle,
                modifier = Modifier.wrapContentWidth(Alignment.Start)
            )
        }
        Spacer(modifier = Modifier.height(8.pxToDp()))
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = intervalRowPadding)
                .align(Alignment.CenterHorizontally)
        ) {
            //TODO update the Progression strings to be usable across all of chimera
            Text(
                text = when (phaseType) {
                    IntervalPhase.Rest -> stringResource(R.string.rest_tc)
                    IntervalPhase.Work -> stringResource(R.string.work_tc)
                    else -> ""
                },
                style = workoutPhaseTextStyle.copy(color = if (phaseType == IntervalPhase.Rest) TargetTrainingColors.GREEN_1 else TargetTrainingColors.TEXT_TITLE_PRIMARY)
            )
        }

        //time dial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = intervalRowPadding),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.padding(top = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IntervalsCountdownTimer(
                    dialLabel = dialLabel,
                    completionProgress = completionProgress,
                    mainText = mainText,
                    accentColor = accentColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(32.pxToDp()))
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(start = intervalRowPadding),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "$subText ",
                style = subTextStyle
            )
            if (subTextUnit.isNotEmpty()) {
                Text(
                    text = subTextUnit,
                    style = subTextStyle.copy(
                        fontSize = 26.sp,
                        color = TargetTrainingColors.TIME_REMAINING_LABEL_TEXT.copy(alpha = .6f),
                    ),
                )
            }
        }
    }
}

/**
 * Right side of the screen, contains all the Previous Intervals data
 */
@Composable
private fun PreviousIntervalsGraphic(
    intervalsData: List<IntervalData>,
    isMetric: Boolean = true,
    totalDistanceText: String = "0.00",
    totalTimeText: String = "0:00",
) {
    Box(
        modifier = Modifier
            .width(intervalsGraphicWidth)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(previousIntervalsBoxPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.previous_intervals_tc),
                    style = previousIntervalsTitleStyle,
                )
            }
            Spacer(modifier = Modifier.height(32.pxToDp()))
            IntervalsDataTable(
                intervalsData = intervalsData,
                totalDistanceText = totalDistanceText,
                totalTimeText = totalTimeText,
                isMetric = isMetric
            )
        }
    }
}

//region Previews
@Device22inPreviews
@Composable
private fun PreviewIntervalsTimeScreenWork() {
    IntervalsWorkoutScreen(
        completionProgress = .92f,
        mainText = "0:30",
        intervalGoal = IntervalGoal.Time,
        subText = "2.02",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Work,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = false,
    )
}

@Device22inPreviews
@Composable
private fun PreviewIntervalsTimeScreenRest() {
    IntervalsWorkoutScreen(
        completionProgress = .12f,
        mainText = "5:30",
        intervalGoal = IntervalGoal.Time,
        subText = "2.02",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Rest,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = false,
    )
}

@Device22inPreviews
@Composable
private fun PreviewIntervalsTimeScreenRestMetric() {
    IntervalsWorkoutScreen(
        completionProgress = .12f,
        mainText = "5:30",
        intervalGoal = IntervalGoal.Time,
        subText = "2.02",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Rest,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = true,
    )
}

@Device22inPreviews
@Composable
private fun PreviewIntervalsDistanceScreenWork() {
    IntervalsWorkoutScreen(
        completionProgress = .16f,
        mainText = "4.21",
        intervalGoal = IntervalGoal.Distance,
        subText = "0:30",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Work,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = false,
    )
}

@Device22inPreviews
@Composable
private fun PreviewIntervalsDistanceScreenRest() {
    IntervalsWorkoutScreen(
        completionProgress = .79f,
        mainText = "1.05",
        intervalGoal = IntervalGoal.Distance,
        subText = "1:30",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Rest,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = false,
    )
}

@Device22inPreviews
@Composable
private fun PreviewIntervalsDistanceScreenRestMetric() {
    IntervalsWorkoutScreen(
        completionProgress = .91f,
        mainText = "0.45",
        intervalGoal = IntervalGoal.Distance,
        subText = "1:30",
        currentIntervalIndex = 4,
        currentPhase = IntervalPhase.Rest,
        previousIntervalsData = previewMultipleIntervals,
        isMetric = true,
    )
}

/** Shared fixture for the interval screen previews */
private val previewMultipleIntervals = listOf(
    IntervalData(
        intervalTime = "1:00",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Rest,
    ),
    IntervalData(
        intervalTime = "0:30",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Work,
    ),
    IntervalData(
        intervalTime = "1:00",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Rest,
    ),
    IntervalData(
        intervalTime = "0:30",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Work,
    ),
    IntervalData(
        intervalTime = "1:00",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Rest,
    ),
    IntervalData(
        intervalTime = "0:30",
        intervalDistance = "1.00",
        intervalType = IntervalPhase.Work,
    )
)
//endregion