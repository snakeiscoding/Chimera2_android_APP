package com.jht.sprint8.sprint8workoutsummary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun Sprint8WorkoutSummaryRoute(
    sprint8WorkoutSummaryViewModel: Sprint8WorkoutSummaryViewModel,
) {
    val fullStats = remember { sprint8WorkoutSummaryViewModel.fullStatTable }
    val sweatScoreList = remember { sprint8WorkoutSummaryViewModel.sprint8WorkoutSplits.value }
    val totalSweatScore = remember { sprint8WorkoutSummaryViewModel.totalSweatScore }
    val peakSweatScore = remember { sprint8WorkoutSummaryViewModel.peakSweatScore }
    val averageSweatScore = remember { sprint8WorkoutSummaryViewModel.averageSweatScore }
    val numberOfSprints = remember { sprint8WorkoutSummaryViewModel.workoutModel.numberOfSprints.value }
    val isAssessmentVisible =  sprint8WorkoutSummaryViewModel.isAssessmentVisible.collectAsStateWithLifecycle()
    val assessmentLevel =  sprint8WorkoutSummaryViewModel.recommendedAssessmentLevel.collectAsStateWithLifecycle()
    val assessmentRecommendedLevel =  sprint8WorkoutSummaryViewModel.categoryRecommended.collectAsStateWithLifecycle()
    val assessmentLevelBgColor =  sprint8WorkoutSummaryViewModel.assessmentLevelBgColor.collectAsStateWithLifecycle()

    Sprint8SummaryScreen(
        fullStats = fullStats.value,
        sweatScoreList = sweatScoreList,
        totalSweatScore = totalSweatScore,
        peakSweatScore = peakSweatScore,
        averageSweatScore = averageSweatScore,
        numberOfSprints = numberOfSprints,
        isAssessmentVisible = isAssessmentVisible.value,
        recommendedAssessmentLevel = assessmentLevel.value.toString(),
        assessmentRecommendedCategory = assessmentRecommendedLevel.value,
        assessmentLevelBgColor = assessmentLevelBgColor.value
    )
}