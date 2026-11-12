package com.jht.chimera.service.ui

import android.app.Application
import com.jht.chimera.lib.common.timer.ThirdPartyFeaturedWorkoutScreenTimeout
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric so the launch intent keeps the survey shown extra
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PostWorkoutTimeoutTest {
    private val timeout = mockk<ThirdPartyFeaturedWorkoutScreenTimeout>(relaxed = true)

    private fun data(screen: CommonScreens, surveyShown: Boolean = false) = LaunchScreenData(screen).apply {
        if (surveyShown) intent.putExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, true)
    }

    @Test
    fun `survey launch holds the post workout timeout`() {
        applyPostWorkoutTimeout(data = data(CommonScreens.PostWorkoutSurvey), timeout = timeout)
        verify { timeout.holdForSurvey() }
        confirmVerified(timeout)
    }

    @Test
    fun `summary after the survey restarts the post workout timeout`() {
        applyPostWorkoutTimeout(data = data(CommonScreens.WorkoutSummary, surveyShown = true), timeout = timeout)
        verify { timeout.restartAfterSurvey() }
        confirmVerified(timeout)
    }

    @Test
    fun `summary without the survey leaves the timeout alone`() {
        applyPostWorkoutTimeout(data = data(CommonScreens.WorkoutSummary), timeout = timeout)
        confirmVerified(timeout)
    }

    @Test
    fun `missing timeout is ignored`() {
        applyPostWorkoutTimeout(data = data(CommonScreens.PostWorkoutSurvey), timeout = null)
    }
}
