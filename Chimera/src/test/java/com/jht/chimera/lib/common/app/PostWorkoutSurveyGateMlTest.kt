package com.jht.chimera.lib.common.app

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric because the token host check parses with android.net.Uri
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PostWorkoutSurveyGateMlTest {

    private fun applies(
        url: String,
        isGuest: Boolean = false,
        hasCompletedPaceSurvey: Boolean = true,
        clubId: Int = 1964,
    ) = PostWorkoutSurveyGate.mlSurveyApplies(
        surveyUrl = url,
        isGuest = isGuest,
        hasCompletedPaceSurvey = hasCompletedPaceSurvey,
        clubId = clubId,
    )

    @Test
    fun `applies for an https jfit co survey`() =
        assertTrue(applies(url = "https://dev-matrixworkouts.jfit.co/survey/after-exercise"))

    @Test
    fun `applies for an https matrixfitness com survey`() =
        assertTrue(applies(url = "https://workouts.matrixfitness.com/survey/after-exercise"))

    @Test
    fun `skipped for a plain http survey`() =
        assertFalse(applies(url = "http://dev-matrixworkouts.jfit.co/survey/after-exercise"))

    @Test
    fun `skipped for a host that may not get jump tokens`() {
        assertFalse(applies(url = "https://example.com/survey"))
        assertFalse(applies(url = "https://matrixfitness.com.evil.com/survey"))
    }

    @Test
    fun `skipped when cleared`() = assertFalse(applies(url = ""))

    @Test
    fun `allowed host still needs a completed pace user and a club`() {
        val url = "https://workouts.matrixfitness.com/survey"
        assertFalse(applies(url = url, isGuest = true))
        assertFalse(applies(url = url, hasCompletedPaceSurvey = false))
        assertFalse(applies(url = url, clubId = 0))
    }

    @Test
    fun `allowed survey url passes through and others become empty`() {
        val url = "https://workouts.matrixfitness.com/survey"
        assertEquals(url, PostWorkoutSurveyGate.allowedMlSurveyUrl(url))
        assertEquals("", PostWorkoutSurveyGate.allowedMlSurveyUrl("https://example.com/survey"))
        assertEquals("", PostWorkoutSurveyGate.allowedMlSurveyUrl("http://jfit.co/survey"))
        assertEquals("", PostWorkoutSurveyGate.allowedMlSurveyUrl(""))
    }
}
