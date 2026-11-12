package com.jht.trackapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jht.trackapp.TrackAppViewModel

@Composable
fun TrackAppRoute(
    trackAppViewModel: TrackAppViewModel
) {

    val mode = remember { trackAppViewModel.mode }
    val lapDistance = remember {
        trackAppViewModel.currentLapDistance
    }
    val lapTime = remember {
        trackAppViewModel.lapTime
    }
    val lapProgress = remember {
        trackAppViewModel.lapProgress
    }
    val totalLaps = remember {
        trackAppViewModel.lapList
    }
    val spm = remember {
        trackAppViewModel.spm
    }
    val strideLength = remember {
        trackAppViewModel.strideLength
    }

    TrackAppScreen(
        mode = mode.value,
        currentLapDistance = lapDistance.value,
        lapTime = lapTime.intValue,
        lapProgress = lapProgress.floatValue,
        laps = totalLaps.value,
        spm = spm.value,
        strideLength = strideLength.value,
        showCadenceStats = trackAppViewModel.cadenceEnabled,
    ) {
        trackAppViewModel.updateLapDistance(it)
    }

}