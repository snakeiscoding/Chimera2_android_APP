package com.jht.customhiit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.customhiit.common.components.IndicatorComponent
import com.jht.customhiit.common.components.IntervalCard
import com.jht.customhiit.common.models.CustomHIITInterval
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.ui.AllDevicePreviews
import com.jht.ui.DebounceAction
import com.jht.ui.DebounceActionState
import com.jht.ui.computeSize
import com.jht.unit.kphUnit
import com.jht.widgets.ProfileGraphComponent
import java.util.Locale
import kotlin.math.roundToInt

private const val tag= "CustomHIIT"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CustomHIITScreen(
    intervals: List<CustomHIITInterval>,
    intervalData: Map<HIITIntervalEnum, Map<String, MutableState<Float>>>,
    selectedInterval: Int = -1,
    timeElapsed: Int = 0,
    speedUnit: String = "MPH",
    isMetric: Boolean = true,
    indicatorParameters: Map<String, Float> = emptyMap(),
    updateInterval: (HIITIntervalEnum) -> Unit // callback function to update speed in viewmodel
) {

    BoxWithConstraints(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .background(Color(0xCC000000))
            .testTag("${tag}:Screen")
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.15f)
                    .padding(
                        top = screenHeight * 0.022f,
                        start = screenWidth * 0.012f,
                        end = screenWidth * 0.012f
                    )
                    .align(Alignment.TopCenter),
                horizontalArrangement = if (indicatorParameters.size == 1) Arrangement.End else Arrangement.SpaceBetween
            ) {
                val indicatorModifier = Modifier
                    .width(screenWidth * 0.14f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(15.dp.computeSize()))
                    .background(Color(0xFF191919))
                indicatorParameters.keys.sorted().forEach { parameterName ->
                    val name = when (parameterName) {
                        ConsoleWorkoutVariables.START_SPEED -> "Speed"
                        ConsoleWorkoutVariables.START_INCLINE -> "Incline"
                        ConsoleWorkoutVariables.START_RESISTANCE -> "Resistance"
                        else -> throw RuntimeException("Not a valid workout parameter")
                    }
                    val value = indicatorParameters[parameterName] ?: 0f

                    val displayValue = when (parameterName) {
                        ConsoleWorkoutVariables.START_RESISTANCE -> {
                            value.roundToInt().toString()
                        }
                        ConsoleWorkoutVariables.START_SPEED -> {
                            val speed = if (isMetric) value else value.kphUnit.toMph
                            String.format(Locale.ENGLISH, "%.1f", speed)
                        }
                        else -> {
                            String.format(Locale.ENGLISH, "%.1f", value)
                        }
                    }

                    val subUnit = when (parameterName) {
                        ConsoleWorkoutVariables.START_SPEED -> speedUnit
                        ConsoleWorkoutVariables.START_INCLINE -> "%"
                        else -> ""
                    }
                    IndicatorComponent(
                        modifier = indicatorModifier,
                        title = name,
                        value = displayValue,
                        subUnit = subUnit,
                        testTag = "$tag:$name"   // eg: CustomHIIT:Speed
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = screenHeight * 0.31f),
                horizontalArrangement = Arrangement.spacedBy(
                    screenWidth * 0.024f,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val debounceActionState = remember { DebounceActionState(250) }
                intervals.forEach { interval ->
                    val debounceAction = remember { DebounceAction(debounceActionState) { updateInterval(HIITIntervalEnum.entries[interval.ordinal]) } }
                    IntervalCard(
                        modifier = Modifier.size(screenWidth * 0.28f, screenHeight * 0.33f),
                        intervalType = interval,
                        selectedInterval = selectedInterval,
                        timeElapsed = timeElapsed,
                        /**
                         * It reads existing interval data from the view model, based on available parameters, it set those parameter's value and send it to the Interval Card.
                         *  */
                        availableParameters = linkedMapOf<String, Float>().apply {
                            val intervalParameters = intervalData[HIITIntervalEnum.entries[interval.ordinal]]
                            intervalParameters?.let { parameters ->
                                // A simple check to avoid adding non-existing parameter's value
                                if (parameters.containsKey(ConsoleWorkoutVariables.START_SPEED)) {
                                    put(ConsoleWorkoutVariables.START_SPEED, parameters[ConsoleWorkoutVariables.START_SPEED]?.value ?: 0f)
                                }
                                if (parameters.containsKey(ConsoleWorkoutVariables.START_RESISTANCE)) {
                                    put(ConsoleWorkoutVariables.START_RESISTANCE, parameters[ConsoleWorkoutVariables.START_RESISTANCE]?.value ?: 0f)
                                }
                                if (parameters.containsKey(ConsoleWorkoutVariables.START_INCLINE)) {
                                    put(ConsoleWorkoutVariables.START_INCLINE, parameters[ConsoleWorkoutVariables.START_INCLINE]?.value ?: 0f)
                                }
                            }
                        },
                        isMetric = isMetric,
                        testTag = "$tag:${interval.intervalName}"
                    ) {
                        debounceAction.invoke()
                    }
                }
            }

            // Profile Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.22f)
                    .align(Alignment.BottomCenter)
                    .testTag("${tag}:ProfileGraph"),
            ) {
                ProfileGraphComponent(
                    Modifier
                        .fillMaxWidth()
                        .padding(15.dp.computeSize())
                )
            }

        }
    }

}

@AllDevicePreviews
@Composable
private fun CustomHIITScreenPreview() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight

        CustomHIITScreen(
            listOf(
                CustomHIITInterval.EASY_INTERVAL,
                CustomHIITInterval.MEDIUM_INTERVAL,
                CustomHIITInterval.HARD_INTERVAL
            ),
            selectedInterval = 0,
            intervalData = emptyMap()
        ) {

        }
    }
}