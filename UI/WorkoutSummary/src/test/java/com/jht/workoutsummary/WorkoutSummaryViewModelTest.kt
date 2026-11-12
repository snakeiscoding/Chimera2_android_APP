package com.jht.workoutsummary

import androidx.lifecycle.SavedStateHandle
import com.jht.mock.interfaces.models.MockIdleTimerModel
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.mock.interfaces.models.MockWorkoutSummaryModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric because SplitTable and the stat formatters touch Android resources
@RunWith(RobolectricTestRunner::class)
class WorkoutSummaryViewModelTest {

    private fun viewModel(isRetail: Boolean, surveyUrl: String = SURVEY_URL) = WorkoutSummaryViewModel(
        systemModel = MockSystemModel(postWorkoutSurveyUrl = surveyUrl),
        uiBaseModel = MockUIBaseModel(isCommercial = !isRetail, isRetail = isRetail),
        navigationModel = MockNavigationModel(),
        idleTimerModel = MockIdleTimerModel(),
        workoutModel = MockWorkoutModel(),
        summaryModel = MockWorkoutSummaryModel(),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun retailKeepsTheInSummarySurvey() =
        assertEquals(SURVEY_URL, viewModel(isRetail = true).surveyUrl)

    @Test
    fun retailWithSurveyOffShowsNone() =
        assertEquals("", viewModel(isRetail = true, surveyUrl = "").surveyUrl)

    @Test
    fun commercialNeverShowsTheInSummarySurvey() =
        assertEquals("", viewModel(isRetail = false).surveyUrl)

    private companion object {
        const val SURVEY_URL = "https://www.surveymonkey.com/r/abc"
    }
}
