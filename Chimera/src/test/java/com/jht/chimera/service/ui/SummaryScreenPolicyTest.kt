package com.jht.chimera.service.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryScreenPolicyTest {

    @Test
    fun `survey and summary both use summary panels`() {
        assertEquals(true, SummaryScreenPolicy.usesSummaryPanels(screen = CommonScreens.WorkoutSummary))
        assertEquals(true, SummaryScreenPolicy.usesSummaryPanels(screen = CommonScreens.PostWorkoutSurvey))
    }

    @Test
    fun `other screens do not use summary panels`() {
        assertEquals(false, SummaryScreenPolicy.usesSummaryPanels(screen = CommonScreens.Workout))
        assertEquals(false, SummaryScreenPolicy.usesSummaryPanels(screen = CommonScreens.GenericIdleScreen))
        assertEquals(false, SummaryScreenPolicy.usesSummaryPanels(screen = null))
    }

    @Test
    fun `summary shows the launch overlay when no survey ran`() =
        assertEquals(true, SummaryScreenPolicy.showsLaunchOverlay(screen = CommonScreens.WorkoutSummary, surveyShown = false))

    @Test
    fun `summary after the survey does not show the overlay again`() =
        assertEquals(false, SummaryScreenPolicy.showsLaunchOverlay(screen = CommonScreens.WorkoutSummary, surveyShown = true))

    @Test
    fun `survey shows the launch overlay since it follows the workout`() =
        assertEquals(true, SummaryScreenPolicy.showsLaunchOverlay(screen = CommonScreens.PostWorkoutSurvey, surveyShown = false))

    @Test
    fun `non summary screens never show the summary overlay`() =
        assertEquals(false, SummaryScreenPolicy.showsLaunchOverlay(screen = CommonScreens.Workout, surveyShown = false))

    @Test
    fun `leaving the summary always runs the summary exit effects`() {
        assertEquals(true, SummaryScreenPolicy.runsSummaryExitEffects(screen = CommonScreens.WorkoutSummary, nextScreen = null))
        assertEquals(true, SummaryScreenPolicy.runsSummaryExitEffects(screen = CommonScreens.WorkoutSummary, nextScreen = CommonScreens.WorkoutSummary))
    }

    @Test
    fun `leaving the survey for the summary leaves the exit effects to the summary`() =
        assertEquals(false, SummaryScreenPolicy.runsSummaryExitEffects(screen = CommonScreens.PostWorkoutSurvey, nextScreen = CommonScreens.WorkoutSummary))

    @Test
    fun `leaving the survey for anything else runs the exit effects`() {
        // splash and app launches don't go through screen launches, so they show up as the survey itself or null
        listOf(null, CommonScreens.PostWorkoutSurvey, CommonScreens.OutOfOrder, CommonScreens.Workout, CommonScreens.Welcome).forEach { next ->
            assertEquals(true, SummaryScreenPolicy.runsSummaryExitEffects(screen = CommonScreens.PostWorkoutSurvey, nextScreen = next))
        }
    }

    @Test
    fun `other screens never run the summary exit effects`() {
        assertEquals(false, SummaryScreenPolicy.runsSummaryExitEffects(screen = null, nextScreen = null))
        assertEquals(false, SummaryScreenPolicy.runsSummaryExitEffects(screen = CommonScreens.Workout, nextScreen = null))
    }
    @Test
    fun `survey holds the post workout timeout`() =
        assertEquals(PostWorkoutTimeout.Hold, SummaryScreenPolicy.postWorkoutTimeout(screen = CommonScreens.PostWorkoutSurvey, surveyShown = false))

    @Test
    fun `summary after the survey restarts the post workout timeout`() =
        assertEquals(PostWorkoutTimeout.Restart, SummaryScreenPolicy.postWorkoutTimeout(screen = CommonScreens.WorkoutSummary, surveyShown = true))

    @Test
    fun `summary without a survey keeps the timeout started at workout stop`() =
        assertEquals(PostWorkoutTimeout.Unchanged, SummaryScreenPolicy.postWorkoutTimeout(screen = CommonScreens.WorkoutSummary, surveyShown = false))

    @Test
    fun `other screens leave the post workout timeout alone`() {
        assertEquals(PostWorkoutTimeout.Unchanged, SummaryScreenPolicy.postWorkoutTimeout(screen = CommonScreens.Workout, surveyShown = false))
        assertEquals(PostWorkoutTimeout.Unchanged, SummaryScreenPolicy.postWorkoutTimeout(screen = null, surveyShown = true))
    }
}
