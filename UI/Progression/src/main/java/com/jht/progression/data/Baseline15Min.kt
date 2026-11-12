package com.jht.progression.data

import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval

class Baseline15Min : IBaseline {
    /**
     * Gets the baseline script and intervals
     */
    override fun getBaselineWorkout(): WorkoutInfo {
        val intervals = getIntervals()

        val builder = StringBuilder()
        val script =
            builder
                .append("settings\n")
                .append("  workout_id = 99\n")
                .append("  allow_skip_warmup = 0\n")
                .append("  allow_skip_workout = 0\n")
                .append("  graph_future = 0\n")
                .append("  allow_user_goal_changes = 0\n")
                .append("\n")
                .append("input SPEED_IN\n")
                .append("input GOAL\n")
                .append("\n")
                .append("export INCLINE\n")
                .append("export GOAL\n")
                .append("export GOAL_TYPE\n")
                .append("export SPEED\n")
                .append("export RESISTANCE\n")
                .append("export INTERVAL_GOAL_TYPE\n")
                .append("export INTERVAL_GOAL\n")
                .append("export INTERVAL_NUMBER\n")
                .append("export PREVENT_INCLINE_CHANGES\n")
                .append("\n")
                .append("stage warmup\n")
                .append("  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
                .append("  GOAL = 300000\n")
                .append("  PREVENT_INCLINE_CHANGES = 0\n")
                .append("  interval\n")
                .append("    INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n")
                .append("    INTERVAL_GOAL = 300000\n")
                .append("    INTERVAL_NUMBER = 0\n")
                .append("    SPEED = 0.8\n")
                .append("    INCLINE = 0\n")
                .append("    RESISTANCE = 1\n")
                .append("    when INTERVAL_TIME_MS >= INTERVAL_GOAL\n")
                .append("      interval = interval + 1\n")
                .append("stage workout\n")
                .append("  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
                .append("  GOAL = 900000\n")
                .append("  PREVENT_INCLINE_CHANGES = 1\n")
                .append("  interval\n")
                .append("    INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n")
                .append("    INTERVAL_GOAL = 900000\n")
                .append("    INTERVAL_NUMBER = 1\n")
                .append("    INCLINE = 1\n")
                .append("    when INTERVAL_TIME_MS >= INTERVAL_GOAL\n")
                .append("      interval = interval + 1\n")
                .append("stage cooldown\n")
                .append("  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")
                .append("  GOAL = 300000\n")
                .append("  PREVENT_INCLINE_CHANGES = 0\n")
                .append("  interval\n")
                .append("    INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n")
                .append("    INTERVAL_GOAL = 300000\n")
                .append("    INTERVAL_NUMBER = 2\n")
                .append("    SPEED = min(0.6 * SPEED_IN, 6.43738)\n")
                .append("    INCLINE = 0\n")
                .append("    RESISTANCE = 1\n")
                .append("    when INTERVAL_TIME_MS >= INTERVAL_GOAL\n")
                .append("      end\n").toString()

        return WorkoutInfo(intervals, script)
    }

    /**
     * Gets a list of the intervals in the workout for display in the workout screen.
     */
    private fun getIntervals(): MmIntervalList {
        val intervalList = ArrayList<ProgressionInterval>()
        intervalList.add(ProgressionInterval(IntervalType.WARMUP, IntervalGoalType.TIME, 300000f))
        intervalList.add(ProgressionInterval(IntervalType.ASSESSMENT, IntervalGoalType.TIME, 900000f))
        intervalList.add(ProgressionInterval(IntervalType.COOLDOWN, IntervalGoalType.TIME, 300000f))
        return MmIntervalList(intervalList)
    }
}