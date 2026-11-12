package com.jht.progression.data.program.program5k

import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.progression.data.program.getSpeedForIntervalType
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val RACE_INDEX = 56
private const val MIN_TO_MS = 60000f
private const val BASELINE_KPH = 8f

@RunWith(JUnitParamsRunner::class)
class Program5kTest {
    private val program = Program5k()

    @Test
    @Parameters(
        "0, 34",
        "7, 39",
        "14, 44",
        "21, 45",
        "28, 49",
        "35, 53",
        "42, 54",
        "49, 53"
    )
    fun `long slow distance workout total duration matches program week`(programIndex: Int, expectedMinutes: Float) {
        assertWorkoutDurationMinutes(programIndex, expectedMinutes)
    }

    @Test
    @Parameters(
        "2, 25",
        "9, 28",
        "16, 31",
        "23, 34",
        "30, 34",
        "37, 37",
        "44, 37",
        "51, 37"
    )
    fun `interval workout total duration matches program week`(programIndex: Int, expectedMinutes: Float) {
        assertWorkoutDurationMinutes(programIndex, expectedMinutes)
    }

    @Test
    @Parameters(
        "4, 24",
        "11, 29",
        "18, 36",
        "25, 40",
        "32, 44",
        "39, 44",
        "46, 48",
        "53, 47"
    )
    fun `tempo workout total duration matches program week`(programIndex: Int, expectedMinutes: Float) {
        assertWorkoutDurationMinutes(programIndex, expectedMinutes)
    }

    // programIndex is week*7 + dayOfWeek: LSD runs on day 0, interval on day 2, tempo on day 4
    private fun assertWorkoutDurationMinutes(programIndex: Int, expectedMinutes: Float) {
        val intervals = program.getWorkout(programIndex, baseline = BASELINE_KPH).intervalList.intervalList
        val totalMs = intervals.sumOf { it.goalValue.toDouble() }
        assertEquals(expectedMinutes, (totalMs / MIN_TO_MS).toFloat(), 0.001f)
        assertEquals(IntervalType.ZONE_1, intervals.first().intervalType)
        assertEquals(IntervalType.ZONE_1, intervals.last().intervalType)
    }

    @Test
    @Parameters("1", "3", "5", "6")
    fun `rest days have no workout intervals`(dayRemainder: Int) {
        val intervals = program.getWorkout(7 + dayRemainder, baseline = BASELINE_KPH).intervalList.intervalList
        assertTrue(intervals.isEmpty())
    }

    @Test
    fun `race workout uses a distance goal for the 5k race`() {
        val workout = program.getWorkout(RACE_INDEX, baseline = BASELINE_KPH)
        val intervals = workout.intervalList.intervalList

        assertEquals(3, intervals.size)
        assertEquals(IntervalType.ZONE_1, intervals[0].intervalType)
        assertEquals(IntervalType.RACE, intervals[1].intervalType)
        assertEquals(IntervalGoalType.DISTANCE, intervals[1].goalType)
        assertEquals(5f, intervals[1].goalValue)
        assertEquals(IntervalType.ZONE_1, intervals[2].intervalType)
        assertTrue(workout.workoutScript.contains("stage \"workout\""))
    }

    @Test
    @Parameters("0", "1", "27", "55", "57", "100")
    fun `shouldDisplayNextInterval and shouldDisplayTips are true away from the race`(programIndex: Int) {
        assertTrue(program.shouldDisplayNextInterval(programIndex))
        assertTrue(program.shouldDisplayTips(programIndex))
    }

    @Test
    fun `shouldDisplayNextInterval and shouldDisplayTips are false only at the race index`() {
        assertFalse(program.shouldDisplayNextInterval(RACE_INDEX))
        assertFalse(program.shouldDisplayTips(RACE_INDEX))
    }

    @Test
    fun `getIntervalTypeSpeed delegates to the shared zone speed calculation`() {
        val baseline = BASELINE_KPH
        assertEquals(
            getSpeedForIntervalType(IntervalType.ZONE_3, baseline) { it },
            program.getIntervalTypeSpeed(IntervalType.ZONE_3, baseline),
            0.00001f
        )
    }
}
