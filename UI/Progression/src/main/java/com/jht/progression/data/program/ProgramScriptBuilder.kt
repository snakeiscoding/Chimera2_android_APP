package com.jht.progression.data.program

import com.jht.enums.IntervalType

class ProgramScriptBuilder {
    private val warmupTimeMs = 300000
    private val cooldownTimeMs = 300000

    fun getScriptString(workoutIntervals: List<ScriptInterval>, zone1Speed: Float): String {
        val builder: StringBuilder = StringBuilder()
        builder.append(getWorkoutSettings())
        builder.append(getWarmup(zone1Speed))
        builder.append(getWorkout(workoutIntervals))
        builder.append(getCooldown(zone1Speed))
        return builder.toString()
    }

    private fun getWorkoutSettings(): String {
        return "settings\n" +
                "    workout_id = 99\n" +
                "    allow_skip_warmup = 0\n" +
                "    allow_skip_workout = 0\n" +
                "    graph_future = 1\n" +
                "    allow_user_goal_changes = 0\n" +
                "\n" +
                "input SPEED_IN\n" +
                "input GOAL\n" +
                "\n" +
                "export INCLINE\n" +
                "export GOAL\n" +
                "export GOAL_TYPE\n" +
                "export SPEED\n" +
                "export INTERVAL_GOAL_TYPE\n" +
                "export INTERVAL_GOAL\n" +
                "export INTERVAL_NUMBER\n" +
                "\n"
    }

    private fun getWarmup(warmupSpeed: Float): String {
            return "stage warmup\n" +
                    "  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n" +
                    "  GOAL = $warmupTimeMs\n" +
                    "  SPEED = $warmupSpeed\n" +
                    "  INCLINE = 0\n" +
                    "  INTERVAL_NUMBER = 0\n" +
                    "  interval\n" +
                    "    INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n" +
                    "    INTERVAL_GOAL = " + warmupTimeMs + "\n" +
                    "    when (STAGE_TIME_MS >= GOAL)\n" +
                    "      interval = interval + 1\n"
    }

    private fun getWorkout(workoutIntervals: List<ScriptInterval>): String {
        if (workoutIntervals.isNotEmpty()) {
            val workoutString = StringBuilder("stage workout\n")
            workoutString.append("  GOAL = " + getWorkoutTime(workoutIntervals) + "\n")
            workoutString.append("  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n")

            for (interval in workoutIntervals) {
                workoutString.append(
                    "  interval\n" +
                            "    SPEED = " + interval.speedKph + "\n" +
                            "    INTERVAL_NUMBER = INTERVAL_NUMBER + 1\n" +
                            "    INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n" +
                            "    INTERVAL_GOAL = " + interval.timeMs + "\n" +
                            "    when (INTERVAL_TIME_MS >= INTERVAL_GOAL)\n" +
                            "      interval = interval + 1\n"
                )
            }

            return workoutString.toString()
        } else {
            return ""
        }
    }

    private fun getWorkoutTime(intervals: List<ScriptInterval>): Int {
        var timeMs = 0
        for (interval in intervals) {
            timeMs += interval.timeMs
        }
        return timeMs
    }


    private fun getCooldown(cooldownSpeed: Float): String {
        return "stage cooldown\n" +
                "  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n" +
                "  GOAL = $cooldownTimeMs\n" +
                "  SPEED = $cooldownSpeed\n" +
                "  INTERVAL_NUMBER = INTERVAL_NUMBER + 1\n" +
                "  INTERVAL_GOAL_TYPE = GOAL_TYPE_TIME_CONSTANT\n" +
                "  INTERVAL_GOAL = 300000\n" +
                "  interval\n" +
                "    when (STAGE_TIME_MS >= GOAL)\n" +
                "      end\n"
    }

    data class ScriptInterval(val timeMs: Int, val speedKph: Float, val type: IntervalType)
}