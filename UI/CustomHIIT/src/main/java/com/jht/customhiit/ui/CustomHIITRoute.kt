package com.jht.customhiit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jht.customhiit.CustomHIITViewModel

@Composable
fun CustomHIITRoute(
    customHIITViewModel: CustomHIITViewModel,
) {

    val intervals = remember {
        customHIITViewModel.intervals
    }
    val intervalData = remember {
        customHIITViewModel.intervalData
    }
    val selectedInterval = remember {
        customHIITViewModel.selectedInterval
    }
    val timeElapsed = remember {
        customHIITViewModel.timeElapsed
    }
    val speedUnit = remember {
        customHIITViewModel.speedUnit
    }
    val isMetric = remember {
        customHIITViewModel.isMetric
    }
    val indicatorParameters = remember {
        customHIITViewModel.indicatorParameters
    }

    CustomHIITScreen(
        intervals = intervals.value,
        intervalData = intervalData.value,
        selectedInterval = selectedInterval.value,
        timeElapsed = timeElapsed.value,
        speedUnit = speedUnit.value,
        isMetric = isMetric.value,
        indicatorParameters = indicatorParameters.value,
    ) { interval ->
        customHIITViewModel.updateInterval(interval)
    }

}