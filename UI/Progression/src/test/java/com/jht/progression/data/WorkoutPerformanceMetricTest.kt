package com.jht.progression.data

import androidx.compose.ui.graphics.Color
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.ProgramType
import com.jht.enums.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPerformanceMetricTest {
    private val expectedSpeedKph = 10f

    private fun getTestProgram(): Program {
        val workoutInterface = object : IWorkoutInterface {
            override val raceUnitSystem: UnitSystem
                get() = TODO("Not yet implemented")
            override val maxRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")
            override val minRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")

            override fun getWorkout(programIndex: Int, baseline: Float, goalBaseline: Float): WorkoutInfo {
                val intervals = listOf(
                    ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f),
                    ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 600000f),
                    ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f)
                )
                return WorkoutInfo(MmIntervalList(intervals), "")
            }

            override fun getIntervalTypeSpeed(
                intervalType: IntervalType,
                baseline: Float,
                clampSpeed: (Float) -> Float
            ): Float {
                return if (intervalType == IntervalType.ZONE_3) expectedSpeedKph else 0f
            }

            override fun shouldDisplayNextInterval(programIndex: Int): Boolean {
                TODO("Not yet implemented")
            }

            override fun shouldDisplayTips(programIndex: Int): Boolean {
                TODO("Not yet implemented")
            }

            override fun getRaceMetrics(userBaseline: Float): UserWorkoutMetrics {
                TODO("Not yet implemented")
            }

            override fun computeBaselineFromWorkoutTime(raceTimeSeconds: Int): Float {
                TODO("Not yet implemented")
            }
        }

        val baselineInterface = object : IBaseline {
            override fun getBaselineWorkout(): WorkoutInfo {
                TODO("Not yet implemented")
            }
        }

        return Program(ProgramType.Program5k, 0, workoutInterface, baselineInterface, Color.White)
    }

    private fun buildEvent(workoutTimeMs: Int, workoutDistanceKm: Float): WorkoutEvent {
        return WorkoutEvent(
            index = 0,
            startTime = 0,
            endTime = 0,
            completed = true,
            label = EventLabel.WORKOUT_TEMPO,
            workoutDistanceKm = workoutDistanceKm,
            workoutTimeMs = workoutTimeMs,
            baselineKph = 8f
        )
    }

    @Test
    fun `on pace workout has zero deviation`() {
        val metric = WorkoutPerformanceMetric(buildEvent(600000, 1.666667f), getTestProgram())
        assertEquals(0f, metric.avgWorkoutSpeedDeviation, 0.01f)
        assertEquals(0f, metric.workoutDistanceDeviation, 0.01f)
        assertEquals(0f, metric.totalWorkoutDeviation, 0.01f)
    }

    @Test
    fun `faster than expected workout has positive deviation`() {
        val metric = WorkoutPerformanceMetric(buildEvent(600000, 2.0f), getTestProgram())
        assertEquals(20f, metric.avgWorkoutSpeedDeviation, 0.01f)
        assertEquals(20f, metric.workoutDistanceDeviation, 0.01f)
        assertEquals(20f, metric.totalWorkoutDeviation, 0.01f)
    }

    @Test
    fun `slower than expected workout has negative deviation`() {
        val metric = WorkoutPerformanceMetric(buildEvent(600000, 1.0f), getTestProgram())
        assertEquals(-40f, metric.avgWorkoutSpeedDeviation, 0.01f)
        assertEquals(-40f, metric.workoutDistanceDeviation, 0.01f)
        assertEquals(-40f, metric.totalWorkoutDeviation, 0.01f)
    }

    @Test
    fun `zero actual workout time is treated as a fully missed workout`() {
        val metric = WorkoutPerformanceMetric(buildEvent(0, 5f), getTestProgram())
        assertEquals(-100f, metric.avgWorkoutSpeedDeviation, 0.01f)
        assertEquals(-100f, metric.workoutDistanceDeviation, 0.01f)
        assertEquals(-100f, metric.totalWorkoutDeviation, 0.01f)
    }
}
