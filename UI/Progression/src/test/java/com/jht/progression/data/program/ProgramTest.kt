package com.jht.progression.data.program

import com.jht.progression.data.UserWorkoutMetrics
import com.jht.progression.data.program.program5k.Program5k
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class ProgramTest {
    @Test
    fun computeWorkoutMetricsFromBaseline() {
        val baseline5k = 8.989657f
        val expected5kMetrics = UserWorkoutMetrics(
            racePaceSecPerKm = 420,
            raceSpeedKph = 8.568523f,
            raceTimeSeconds = 2101
        )
        assertEquals(
            "Unexpected 5K workout metrics from baseline.",
            expected5kMetrics,
            Program5k().getRaceMetrics(baseline5k)
        )
    }

    @Test
    fun computeWorkoutMetricsFromInvalidBaseline() {
        val baseline = 0f
        val expected5kMetrics = UserWorkoutMetrics(
            racePaceSecPerKm = 0,
            raceSpeedKph = 0f,
            raceTimeSeconds = 0
        )
        assertEquals(
            "Unexpected workout metrics from invalid 5k baseline.",
            expected5kMetrics,
            Program5k().getRaceMetrics(baseline)
        )
    }

    @Test
    fun compute5kBaselineFromWorkoutTime() {
        val workoutTimeSeconds = 2100

        // this is the riegal formula for calculating baseline time from a 5k time
        val baselineTimeSec = (15f * 60f)
        val baselineDistanceKm = (baselineTimeSec / workoutTimeSeconds).pow(1f/1.06f) * 5
        val expected5kBaseline = (baselineDistanceKm / baselineTimeSec) * 3600f

        assertEquals(
            "Unexpected 5K baseline from workout time.",
            expected5kBaseline,
            Program5k().computeBaselineFromWorkoutTime(workoutTimeSeconds)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun compute5kBaselineFromInvalidWorkoutTime() {
        val workoutTime = 0
        Program5k().computeBaselineFromWorkoutTime(workoutTime)
    }

    @Test
    fun workoutTimeToBaselineToWorkoutTime() {
        val workoutTimeSeconds = 2100
        val baseline5k = Program5k().computeBaselineFromWorkoutTime(workoutTimeSeconds)
        assertEquals(
            "Failed to successfully convert workout time to baseline and back.",
            workoutTimeSeconds,
            Program5k().getRaceMetrics(baseline5k).raceTimeSeconds
        )
    }
}