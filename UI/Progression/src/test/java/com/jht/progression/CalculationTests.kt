package com.jht.progression

import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.UnitSystem
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.progression.data.IWorkoutInterface
import com.jht.progression.data.UserWorkoutMetrics
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.calculateExpectedWorkoutAverageSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CalculationTests {
    @Test
    fun expectedWorkoutTotalsSingleIntervalTest() {
        val intervals = getSingleInterval()
        val workouts = getTestWorkoutInterface()
        val calculatedSpeedFullWorkout =
            calculateExpectedWorkoutAverageSpeed(intervals, workouts, 0f, -1)

        assertEquals(2f, calculatedSpeedFullWorkout, 0.0000001f)
    }

    @Test
    fun expectedWorkoutTotalsMultipleIntervalsTest() {
        val intervals = getMultipleIntervals()
        val workouts = getTestWorkoutInterface()
        val calculatedSpeedFullWorkout =
            calculateExpectedWorkoutAverageSpeed(intervals, workouts, 0f, -1)

        assertEquals(3.25f, calculatedSpeedFullWorkout, 0.00001f)
    }

    @Test
    fun expectedWorkoutTotalsMultipleIntervalsEndedEarlyTest() {
        val intervals = getMultipleIntervals()
        val workouts = getTestWorkoutInterface()
        val calculatedSpeedFullWorkout =
            calculateExpectedWorkoutAverageSpeed(intervals, workouts, 0f, 15000)

        assertEquals(3f, calculatedSpeedFullWorkout, 0.00001f)
    }

    @Test
    fun expectedWorkoutWithDistanceTest() {
        val intervals = getMultipleIntervalsWithDistance()
        val workouts = getTestWorkoutInterface()
        val calculatedSpeedFullWorkout =
            calculateExpectedWorkoutAverageSpeed(intervals, workouts, 0f, -1)

        assertEquals(4f, calculatedSpeedFullWorkout, 0.00001f)
    }

    @Test
    fun expectedWorkoutWithDistanceEndedEarlyTest() {
        val intervals = getMultipleIntervalsWithDistance()
        val workouts = getTestWorkoutInterface()
        val calculatedSpeedFullWorkout =
            calculateExpectedWorkoutAverageSpeed(intervals, workouts, 0f, 7200)

        assertEquals(3.5f, calculatedSpeedFullWorkout, 0.00001f)
    }

    /**
     * Gets a test workout interface that can get test zone speeds
     */
    private fun getTestWorkoutInterface(): IWorkoutInterface {
        val workoutInterface = object : IWorkoutInterface {
            override val raceUnitSystem: UnitSystem
                get() = TODO("Not yet implemented")
            override val maxRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")
            override val minRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")

            override fun getWorkout(programIndex: Int, baseline: Float, goalBaseline: Float): WorkoutInfo {
                // Not needed for test
                TODO("Not yet implemented")
            }

            override fun getIntervalTypeSpeed(
                intervalType: IntervalType,
                baseline: Float,
                clampSpeed: (Float) -> Float
            ): Float {
                return when (intervalType) {
                    IntervalType.ZONE_1 -> 1f
                    IntervalType.ZONE_2 -> 2f
                    IntervalType.ZONE_3 -> 3f
                    IntervalType.ZONE_4 -> 4f
                    IntervalType.ZONE_5 -> 5f
                    IntervalType.WARMUP, IntervalType.COOLDOWN, IntervalType.ASSESSMENT, IntervalType.RACE -> 0f
                }
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

        return workoutInterface
    }

    private fun getSingleInterval(): MmIntervalList {
        val intervalList = ArrayList<ProgressionInterval>()
        intervalList.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 60000f))
        return MmIntervalList(intervalList)
    }

    private fun getMultipleIntervals(): MmIntervalList {
        val intervalList = ArrayList<ProgressionInterval>()
        intervalList.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 5000f))
        intervalList.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 5000f))
        intervalList.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 10000f))
        return MmIntervalList(intervalList)
    }

    private fun getMultipleIntervalsWithDistance(): MmIntervalList {
        val intervalList = ArrayList<ProgressionInterval>()
        intervalList.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 3600f))
        intervalList.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.DISTANCE, 0.01f)) // Should take 7200 ms
        intervalList.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 7200f))
        return MmIntervalList(intervalList)
    }
}