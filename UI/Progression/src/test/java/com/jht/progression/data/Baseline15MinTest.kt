package com.jht.progression.data

import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Baseline15MinTest {
    private val baseline = Baseline15Min()

    @Test
    fun `getBaselineWorkout returns warmup assessment and cooldown intervals with expected durations`() {
        val intervals = baseline.getBaselineWorkout().intervalList.intervalList

        assertEquals(3, intervals.size)
        assertEquals(IntervalType.WARMUP, intervals[0].intervalType)
        assertEquals(300000f, intervals[0].goalValue)
        assertEquals(IntervalType.ASSESSMENT, intervals[1].intervalType)
        assertEquals(900000f, intervals[1].goalValue)
        assertEquals(IntervalType.COOLDOWN, intervals[2].intervalType)
        assertEquals(300000f, intervals[2].goalValue)
        assertTrue(intervals.all { it.goalType == IntervalGoalType.TIME })
    }

    @Test
    fun `getBaselineWorkout script includes settings warmup workout and cooldown stages in order`() {
        val script = baseline.getBaselineWorkout().workoutScript

        assertTrue(script.startsWith("settings\n"))
        val warmupIndex = script.indexOf("stage warmup")
        val workoutIndex = script.indexOf("stage workout")
        val cooldownIndex = script.indexOf("stage cooldown")
        assertTrue(warmupIndex in 0 until workoutIndex)
        assertTrue(workoutIndex < cooldownIndex)
    }

    @Test
    fun `getBaselineWorkout script sets a 5 minute warmup at a fixed speed`() {
        val script = baseline.getBaselineWorkout().workoutScript
        val warmupSection = script.substringAfter("stage warmup").substringBefore("stage workout")

        assertTrue(warmupSection.contains("GOAL = 300000"))
        assertTrue(warmupSection.contains("SPEED = 0.8"))
    }

    @Test
    fun `getBaselineWorkout script sets a 15 minute goal for the assessment stage`() {
        val script = baseline.getBaselineWorkout().workoutScript
        val workoutSection = script.substringAfter("stage workout").substringBefore("stage cooldown")

        assertTrue(workoutSection.contains("GOAL = 900000"))
    }

    @Test
    fun `getBaselineWorkout script derives cooldown speed from SPEED_IN`() {
        val script = baseline.getBaselineWorkout().workoutScript
        val cooldownSection = script.substringAfter("stage cooldown")

        assertTrue(cooldownSection.contains("GOAL = 300000"))
        assertTrue(cooldownSection.contains("SPEED = min(0.6 * SPEED_IN, 6.43738)"))
    }
}
