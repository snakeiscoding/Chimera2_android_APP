package com.jht.seca2

import com.jht.engage360.Engage360SystemModel
import com.jht.engage360.data.RecommendedExercises
import com.jht.interfaces.models.FullStatData
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.QuickStatData
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.seca.data.SecaUserData
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class SecaViewModel @Inject constructor(
    private val navigationModel: NavigationModel,
    private val summaryModel: WorkoutSummaryModel,
    private val ptpModel: Engage360SystemModel,
) : ThemedViewModel() {
    /**
     * Navigates the user back to the splash screen
     */
    fun navigateToSplash() {
        navigationModel.launchSplashScreen()
    }

    fun getQuickStats() : List<QuickStatData> {
        return summaryModel.getQuickStats()
    }

    fun getDetailedStats() : List<FullStatData> {
        return summaryModel.getFullStats()
    }

    fun getRecommendedExercisesList() : RecommendedExercises {
        return RecommendedExercises(ptpModel.recommendedExercisesList)
    }

    fun getSecaUserData() : SecaUserData? {
        return ptpModel.secaUserData
    }
}