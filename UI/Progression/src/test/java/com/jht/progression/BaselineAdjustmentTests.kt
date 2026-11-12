package com.jht.progression

import androidx.compose.ui.graphics.Color
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.ProgramType
import com.jht.enums.UnitSystem
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IBaseline
import com.jht.progression.data.IEvent
import com.jht.progression.data.IWorkoutInterface
import com.jht.progression.data.Program
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.UserWorkoutMetrics
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.calculateExpectedWorkoutAverageSpeed
import com.jht.progression.data.calculateExpectedWorkoutTotals
import com.jht.progression.ui.summary.adjustBaseline
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BaselineAdjustmentTests {
    @Test
    fun testBaselineAdjustmentNone() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = false),
            TestEvent(completed = false),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentNone2() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = 0.5f

        // No adjustment because does not have 2 positive/negative in a row
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, -speedDiff),
            TestEvent(completed = true, 1f, -speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentNoneMissedWorkout() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = 0.5f

        // Should not adjust because of missed workout
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false, eventMissed = true),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentNoneWorkoutTooShort() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = 0.5f

        // Should not adjust because of missed workout
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 0.65f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentUp() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = 0.5f
        val expectedAdjustment = speedDiff * 0.7f

        // Adjustment should be 70% of the diff of the last 2 workouts
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline + expectedAdjustment, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentUpMultiple() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = 0.5f
        val expectedAdjustment = speedDiff * 0.7f

        // Adjustment should only be affected by last 2 completed workouts
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff * 2),
            TestEvent(completed = true, 1f, speedDiff * 2),
            TestEvent(completed = true, 1f, speedDiff * 2),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline + expectedAdjustment, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentDown() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = -1f
        val expectedAdjustment = speedDiff * 0.7f
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline + expectedAdjustment, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentDownMultiple() {
        val initialBaseline = 5f
        val program = getTestProgram()
        val speedDiff = -1f
        val expectedAdjustment = speedDiff * 0.7f
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff * 2),
            TestEvent(completed = true, 1f, speedDiff * 2),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(initialBaseline + expectedAdjustment, adjustedBaseline, 0.00001f)
    }

    @Test
    fun testBaselineAdjustmentDownMin() {
        val initialBaseline = 1.1f
        val program = getTestProgram()
        val speedDiff = -1f

        // Baseline should not reduce below min (1 kph)
        val programEvents = createProgramEvents(
            program,
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = true, 1f, speedDiff),
            TestEvent(completed = false),
            TestEvent(completed = false)
        )

        val adjustedBaseline = adjustBaseline(baseline = initialBaseline, programEvents = programEvents, program = program)
        assertEquals(1f, adjustedBaseline, 0.00001f)
    }

    data class TestEvent(
        val completed: Boolean,
        val workoutPercentComplete: Float = 0f,
        val workoutSpeedDiff: Float = 0f,
        val eventMissed: Boolean = false
    )

    private fun createProgramEvents(program: Program, vararg events: TestEvent): ProgramEvents {
        val currentTime = System.currentTimeMillis()
        val programEventsList = ArrayList<IEvent>()
        events.forEachIndexed { i: Int, event: TestEvent ->
            val intervals = program.workoutInterface.getWorkout(0, 0f).intervalList
            val actualTimeMs = getTestWorkoutTimeMs(intervals, event.workoutPercentComplete)
            val (totalTimeMs, _) = calculateExpectedWorkoutTotals(intervals, program.workoutInterface, 0f, actualTimeMs)
            val averageSpeed = calculateExpectedWorkoutAverageSpeed(
                program.workoutInterface.getWorkout(0, 0f).intervalList,
                program.workoutInterface,
                0f
            )
            val adjustedDistance = totalTimeMs / 3600000f * (averageSpeed + event.workoutSpeedDiff)
            val programEvent = WorkoutEvent(
                index = i,
                startTime = 0,
                endTime = if (event.eventMissed) currentTime - 1000L else currentTime + 1000000L,
                completed = event.completed,
                EventLabel.WORKOUT_TEMPO,
                workoutDistanceKm = adjustedDistance,
                workoutTimeMs = totalTimeMs
            )
            programEventsList.add(programEvent)
        }

        return ProgramEvents(programEventsList)
    }

    /**
     * Gets the total time for the test workout and returns a percentage of that
     */
    private fun getTestWorkoutTimeMs(intervals: MmIntervalList, percentCompleted: Float) : Int {
        var totalWorkoutTime = 0f
        for (interval in intervals.intervalList) {
            if (interval.intervalType == IntervalType.ZONE_1) continue
            totalWorkoutTime += interval.goalValue
        }
        return (totalWorkoutTime * percentCompleted).toInt()
    }


    private fun getTestProgram(): Program {
        val workoutInterface = object : IWorkoutInterface {
            override val raceUnitSystem: UnitSystem
                get() = TODO("Not yet implemented")
            override val maxRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")
            override val minRaceTimeInSeconds: Int
                get() = TODO("Not yet implemented")

            override fun getWorkout(programIndex: Int, baseline: Float, goalBaseline: Float): WorkoutInfo {
                val intervalList = ArrayList<ProgressionInterval>()
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_1,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_2,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_3,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_4,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_5,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )
                intervalList.add(
                    ProgressionInterval(
                        IntervalType.ZONE_1,
                        IntervalGoalType.TIME,
                        60000f
                    )
                )

                return WorkoutInfo(MmIntervalList(intervalList), "")
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
                    else -> 0f
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

        val baselineInterface = object : IBaseline {
            override fun getBaselineWorkout(): WorkoutInfo {
                TODO("Not yet implemented")
            }
        }

        return Program(ProgramType.Program5k, 0, workoutInterface, baselineInterface, Color.White)
    }
}