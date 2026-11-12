package com.jht.chimera.lib.common.app

import com.jht.progression.MmActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostWorkoutSurveyGateTest {
    private val mmActivity = MmActivity::class.java.canonicalName!!

    private fun gate(applies: Boolean) = PostWorkoutSurveyGate(surveyApplies = { applies })

    @Test
    fun `routes through the survey when it applies`() =
        assertEquals(
            WorkoutSummaryDestination.Survey(target = "com.jht.seca2.SecaActivity"),
            gate(applies = true).route(target = "com.jht.seca2.SecaActivity", surveyShown = false),
        )

    @Test
    fun `keeps the default summary as an empty target`() =
        assertEquals(
            WorkoutSummaryDestination.Survey(target = ""),
            gate(applies = true).route(target = null, surveyShown = false),
        )

    @Test
    fun `goes straight to the summary when the survey does not apply`() =
        assertEquals(
            WorkoutSummaryDestination.Summary(target = "com.example.EndActivity", surveyShown = false),
            gate(applies = false).route(target = "com.example.EndActivity", surveyShown = false),
        )

    @Test
    fun `never loops back to the survey once shown`() {
        var asked = false
        val gate = PostWorkoutSurveyGate(surveyApplies = { asked = true; true })
        assertEquals(
            WorkoutSummaryDestination.Summary(target = "", surveyShown = true),
            gate.route(target = "", surveyShown = true),
        )
        assertFalse(asked)
    }

    @Test
    fun `progression returning to itself skips the survey`() =
        assertEquals(
            WorkoutSummaryDestination.Summary(target = mmActivity, surveyShown = false),
            gate(applies = true).route(target = mmActivity, surveyShown = false),
        )

    @Test
    fun `sprint8 and seca summaries still get the survey`() {
        listOf("com.jht.sprint8.sprint8workoutsummary.Sprint8WorkoutSummaryActivity", "com.jht.seca2.SecaActivity").forEach { target ->
            assertEquals(
                WorkoutSummaryDestination.Survey(target = target),
                gate(applies = true).route(target = target, surveyShown = false),
            )
        }
    }

    @Test
    fun `in summary survey only shows on retail with the setting on`() {
        assertTrue(PostWorkoutSurveyGate.showsInSummarySurvey(isRetail = true, enabled = true))
        assertFalse(PostWorkoutSurveyGate.showsInSummarySurvey(isRetail = true, enabled = false))
        assertFalse(PostWorkoutSurveyGate.showsInSummarySurvey(isRetail = false, enabled = true))
        assertFalse(PostWorkoutSurveyGate.showsInSummarySurvey(isRetail = false, enabled = false))
    }

    private fun applies(
        isRetail: Boolean = false,
        enabled: Boolean = true,
        url: String = "https://www.surveymonkey.com/r/abc",
        mlSurveyApplies: Boolean = false,
    ) = PostWorkoutSurveyGate.surveyApplies(isRetail = isRetail, enabled = enabled, url = url, mlSurveyApplies = mlSurveyApplies)

    @Test
    fun `applies on commercial when enabled with a url`() = assertTrue(applies())

    @Test
    fun `never applies on retail`() = assertFalse(applies(isRetail = true))

    @Test
    fun `does not apply when the setting is off`() = assertFalse(applies(enabled = false))

    @Test
    fun `does not apply with an empty url`() = assertFalse(applies(url = ""))

    @Test
    fun `does not apply with a blank url`() = assertFalse(applies(url = "   "))

    @Test
    fun `ml survey applies with the post survey url off`() =
        assertTrue(applies(enabled = false, url = "", mlSurveyApplies = true))

    @Test
    fun `ml survey applies alongside the post survey url`() =
        assertTrue(applies(mlSurveyApplies = true))

    @Test
    fun `ml survey never applies on retail`() =
        assertFalse(applies(isRetail = true, mlSurveyApplies = true))
}
