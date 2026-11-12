package com.jht.workoutsummary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun WorkoutSummaryRoute(
    workoutSummaryViewModel: WorkoutSummaryViewModel,
) {

    val theme = remember { workoutSummaryViewModel.theme }
    val quickStats = remember { workoutSummaryViewModel.quickStatList }
    val fullStats = remember { workoutSummaryViewModel.fullStatTable }
    val workoutSplits = remember { workoutSummaryViewModel.workoutSplits }
    val fitnessTestResult = remember { workoutSummaryViewModel.fitnessTestResult }
    val myZoneResults = remember { workoutSummaryViewModel.myZoneResults }

    WorkoutSummaryScreen(
        quickStats = quickStats.value,
        fullStats = fullStats.value,
        workoutSplits = workoutSplits.value,
        fitnessTestResult = fitnessTestResult.value,
        myZoneResults = myZoneResults.value,
        theme = theme.value,
        surveyUrl = workoutSummaryViewModel.surveyUrl,
        onHome = workoutSummaryViewModel::onHome,
    )
}
