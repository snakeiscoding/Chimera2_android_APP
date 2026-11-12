package com.jht.progression.data.program

import com.jht.progression.data.UserWorkoutMetrics
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class ProgramCalculationsTest {
    @Test
    fun `estimateRaceMetrics computes pace speed and time from a baseline`() {
        // baseline is a 15-minute assessment speed (kph), riegal-projected to a 5k race pace of 7:00/km
        val baseline = 8.989657f
        val expected = UserWorkoutMetrics(racePaceSecPerKm = 420, raceSpeedKph = 8.568523f, raceTimeSeconds = 2101)
        assertEquals(expected, estimateRaceMetrics(baseline, 5f))
    }

    @Test
    fun `estimateRaceMetrics returns zeroed metrics for a non-positive baseline`() {
        val expected = UserWorkoutMetrics(0, 0f, 0)
        assertEquals(expected, estimateRaceMetrics(0f, 5f))
        assertEquals(expected, estimateRaceMetrics(-1f, 5f))
    }

    @Test
    fun `estimateRaceMetrics returns zeroed metrics for a non-positive race distance`() {
        val expected = UserWorkoutMetrics(0, 0f, 0)
        assertEquals(expected, estimateRaceMetrics(8f, 0f))
        assertEquals(expected, estimateRaceMetrics(8f, -5f))
    }

    @Test
    fun `riegalRaceTimeEstimate scales time by the distance ratio raised to the fatigue factor`() {
        val time1 = 900f
        val distance1 = 2f
        val distance2 = 5f
        val expected = time1 * (distance2 / distance1).pow(1.06f)
        assertEquals(expected, riegalRaceTimeEstimate(time1, distance1, distance2), 0.0001f)
    }

    @Test
    fun `riegalBaselineEstimate computes a baseline from race time and distance`() {
        val raceTimeSec = 2100f
        val raceDistanceKm = 5f
        val baselineTimeSec = 15f * 60f
        val baselineDistanceKm = (baselineTimeSec / raceTimeSec).pow(1f / 1.06f) * raceDistanceKm
        val expected = (baselineDistanceKm / baselineTimeSec) * 3600f
        assertEquals(expected, riegalBaselineEstimate(raceTimeSec, raceDistanceKm), 0.0001f)
    }
}
