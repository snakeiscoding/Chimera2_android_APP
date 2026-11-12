package com.jht.progression.data.program.program5k

import android.util.FloatMath
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.UnitSystem
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.progression.data.IWorkoutInterface
import com.jht.progression.data.UserWorkoutMetrics
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.program.ProgramScriptBuilder
import com.jht.progression.data.program.ProgramScriptBuilder.ScriptInterval
import com.jht.progression.data.program.estimateRaceMetrics
import com.jht.progression.data.program.getSpeedForIntervalType
import com.jht.progression.data.program.riegalBaselineEstimate
import kotlin.math.roundToInt

private const val RACE_5K_INDEX = 56

/**
 * Contains all the info needed to generate the workouts for a 5K program (given a baseline). Contains logic to
 * generate descriptions of workouts.
 */
class Program5k : IWorkoutInterface {
    override val raceUnitSystem: UnitSystem = UnitSystem.Metric

    // Max 5k runtime possible at 0.5 MPH is 6 hours 12 minutes and 50 seconds
    override val maxRaceTimeInSeconds: Int = 6 * 3600 + 12 * 60 + 50

    // Min 5k runtime possible at 12.5 MPH is 14 minute and 54 seconds
    override val minRaceTimeInSeconds: Int = 14 * 60 + 54

    override fun getWorkout(programIndex: Int, baseline: Float, goalBaseline: Float): WorkoutInfo {
        return if (programIndex == RACE_5K_INDEX) {
            getRaceWorkoutInfo(baseline, goalBaseline)
        } else {
            getWorkoutInfo(programIndex, baseline)
        }
    }

    private fun getWorkoutInfo(programIndex: Int, baseline: Float) : WorkoutInfo {
        val intervals: MmIntervalList = getWorkoutIntervals(programIndex)

        val zone1Speed = getIntervalTypeSpeed(IntervalType.ZONE_1, baseline)

        val workoutIntervals: MutableList<ScriptInterval> = mutableListOf()
        intervals.intervalList.forEachIndexed { i, interval ->
            if (i == 0 || i == intervals.intervalList.size - 1) return@forEachIndexed // this is a continue

            val scriptInterval = ScriptInterval(
                interval.goalValue.roundToInt(),
                getIntervalTypeSpeed(interval.intervalType, baseline),
                interval.intervalType
            )
            workoutIntervals.add(scriptInterval)
        }

        val scriptBuilder = ProgramScriptBuilder()
        return WorkoutInfo(
            intervalList = intervals,
            workoutScript = scriptBuilder.getScriptString(workoutIntervals, zone1Speed)
        )
    }

    private fun getWorkoutIntervals(programIndex: Int): MmIntervalList {
        val week = programIndex.floorDiv(7)
        // This will get the correct workout, based on the day for the program
        return when (programIndex % 7) {
            0 -> getLsdWorkoutIntervals(week)
            2 -> getIntervalWorkoutIntervals(week)
            4 -> getTempoWorkoutIntervals(week)
            else -> MmIntervalList(ArrayList())
        }
    }

    private val minToMs = 60000

    /**
     * Gets the long slow distance workout intervals
     */
    private fun getLsdWorkoutIntervals(week: Int): MmIntervalList {
        val intervals = ArrayList<ProgressionInterval>()
        when (week) {
            //this is Long-slow distance workout of week 1, the workout structure is zone3/4min,
            // zone2/1min,zone3/4min,zone2/1min,zone3/4min,zone2/1min,zone3/4min,zone2/1min,zone3/4min
            0 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (4 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (4 * minToMs).toFloat()))
            }

            1 -> {
                for (i in 0..4) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (4 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (4 * minToMs).toFloat()))
            }

            2 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (6 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (6 * minToMs).toFloat()))
            }

            3 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (8 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (8 * minToMs).toFloat()))
            }

            4 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (9 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (9 * minToMs).toFloat()))
            }

            5 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
            }

            6 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (14 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (14 * minToMs).toFloat()))
            }

            7 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
            }

        }
        addWarmupAndCooldownIntervals(intervals)
        return MmIntervalList(intervals)
    }

    private fun addWarmupAndCooldownIntervals(intervals: ArrayList<ProgressionInterval>) {
        intervals.add(0, ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
        intervals.add(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
    }

    /**
     * Gets interval workout intervals
     */
    private fun getIntervalWorkoutIntervals(week: Int): MmIntervalList {
        val intervals = ArrayList<ProgressionInterval>()
        when (week) {
            //This is Intervals workout of week 1, the workout structure is zone2/2min,zone4/1min,
            //zone2/2min,zone4/1min,zone2/2min,zone4/1min,zone2/2min,zone4/1min,zone2/2min,zone5/1min
            0 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
            }

            1 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            2 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            3 -> {
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..3) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            4 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..4) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            5 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..5) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            6 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..6) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }

            7 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                for (i in 0..5) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
            }
        }

        addWarmupAndCooldownIntervals(intervals)
        return MmIntervalList(intervals)
    }

    /**
     * Gets tempo workout intervals
     */
    private fun getTempoWorkoutIntervals(week: Int): MmIntervalList {
        val intervals = ArrayList<ProgressionInterval>()
        when (week) {
            //This is Tempo workout of week 1, the workout structure is zone4/5min,zone3/2min,zone4/5min,zone3/2min
            0 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
            }

            1 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
            }

            2 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (5 * minToMs).toFloat()))
            }

            3 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (6 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (6 * minToMs).toFloat()))
            }

            4 -> {
                for (i in 0..2) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (7 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (7 * minToMs).toFloat()))
            }

            5 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (10 * minToMs).toFloat()))
            }

            6 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (12 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (1 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (12 * minToMs).toFloat()))
            }

            7 -> {
                for (i in 0..1) {
                    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (11 * minToMs).toFloat()))
                    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, (2 * minToMs).toFloat()))
                }
                intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, (11 * minToMs).toFloat()))
            }
        }

        addWarmupAndCooldownIntervals(intervals)
        return MmIntervalList(intervals)
    }

    override fun getIntervalTypeSpeed(
        intervalType: IntervalType,
        baseline: Float,
        clampSpeed: (Float) -> Float
    ): Float {
        return getSpeedForIntervalType(intervalType, baseline, clampSpeed)
    }

    override fun shouldDisplayNextInterval(programIndex: Int): Boolean {
        return programIndex != RACE_5K_INDEX
    }

    override fun shouldDisplayTips(programIndex: Int): Boolean {
        return programIndex != RACE_5K_INDEX
    }

    private fun getRaceWorkoutInfo(baseline: Float, targetBaseline: Float): WorkoutInfo {
        val zone1Speed = getIntervalTypeSpeed(IntervalType.ZONE_1, baseline)
        val raceSpeed = calculateRaceSpeed(baseline, targetBaseline)

        val raceDistanceKm = 5
        val warmupCooldownTimeMs = 5 * 60 * 1000 // 5 minutes ms

        val builder = StringBuilder()

        builder
            .append("settings\n")
            .append("    workout_id = 99\n")
            .append("    allow_skip_warmup = 0\n")
            .append("    allow_skip_workout = 0\n")
            .append("    graph_future = 0\n")
            .append("    allow_user_goal_changes = 0\n")
            .append("input GOAL\n")
            .append("input DISTANCE\n")
            .append("export SPEED\n")
            .append("export INCLINE\n")
            .append("export GOAL\n")
            .append("export GOAL_TYPE\n")
            .append("export INTERVAL_GOAL\n")
            .append("export INTERVAL_GOAL_TYPE\n")
            .append("export INTERVAL_NUMBER\n")
            .append("stage \"warmup\"\n")
            .append("    interval\n")
            .append("        SPEED = $zone1Speed\n")
            .append("        INCLINE = 0\n")
            .append("        GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
            .append("        GOAL = $warmupCooldownTimeMs\n")
            .append("        INTERVAL_NUMBER = 0\n")
            .append("        INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
            .append("        INTERVAL_GOAL = $warmupCooldownTimeMs\n")
            .append("        when INTERVAL_TIME_MS >= GOAL\n")
            .append("            interval = interval + 1\n")
            .append("stage \"workout\"\n")
            .append("    interval\n")
            .append("        SPEED = $raceSpeed\n")
            .append("        GOAL_TYPE = GOAL_TYPE_DISTANCE_STAGE_CONSTANT\n")
            .append("        GOAL = $raceDistanceKm\n")
            .append("        INTERVAL_NUMBER = 1\n")
            .append("        INTERVAL_GOAL_TYPE = GOAL_TYPE_DISTANCE_STAGE_CONSTANT\n")
            .append("        INTERVAL_GOAL = 5\n")
            .append("        when STAGE_DISTANCE >= GOAL\n")
            .append("            interval = interval + 1\n")
            .append("stage \"cooldown\"\n")
            .append("    interval\n")
            .append("        SPEED = $zone1Speed\n")
            .append("        GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
            .append("        GOAL = $warmupCooldownTimeMs\n")
            .append("        INTERVAL_NUMBER = 2\n")
            .append("        INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
            .append("        INTERVAL_GOAL = $warmupCooldownTimeMs\n")
            .append("        when INTERVAL_TIME_MS >= GOAL\n")
            .append("            end\n")

        val raceScript = builder.toString()
        val intervals = ArrayList<ProgressionInterval>()
        intervals.add(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, warmupCooldownTimeMs.toFloat()))
        intervals.add(ProgressionInterval(IntervalType.RACE, IntervalGoalType.DISTANCE, goalValue = 5f))
        intervals.add(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, warmupCooldownTimeMs.toFloat()))

        return WorkoutInfo(MmIntervalList(intervals), raceScript)
    }

    /**
     * This method takes a user's baseline and estimates their workout metrics for a 5k race
     *
     * The Power regression formula for estimating the 5k pace calculation is y=4956.1461 * X^(-1.0604)
     */
    override fun getRaceMetrics(userBaseline: Float): UserWorkoutMetrics {
        return estimateRaceMetrics(userBaseline, 5f)
    }

    /**
     * This method takes a 5k race time (in seconds) and computes the baseline needed to achieve that race time.
     *
     * @param raceTimeSeconds the race time in seconds
     * @return the 5k baseline needed for the inputted race time.
     * @throws IllegalArgumentException if raceTimeSeconds is less than or equal to 0
     */
    override fun computeBaselineFromWorkoutTime(raceTimeSeconds: Int): Float {
        require(raceTimeSeconds > 0) { "Race time must be greater than 0 seconds." }
        return riegalBaselineEstimate(raceTimeSeconds.toFloat(), 5f)
    }
}