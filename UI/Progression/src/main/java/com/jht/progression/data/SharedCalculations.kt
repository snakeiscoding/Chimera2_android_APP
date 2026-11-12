package com.jht.progression.data

import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.data.MmIntervalList
import com.jht.progression.ui.widgets.MAX_DURATION_LENGTH
import kotlin.math.roundToInt

/**
 * Calculates the expected workout time and distance from a list of workout intervals and the
 * baseline.
 *
 * @param intervals The intervals for the workout to forecast
 * @param workouts The workout interface to get the speed for each interval type
 * @param baseline The user baseline so we can get correct speeds
 * @param actualWorkoutTime Optional parameter for calculating speed for part of a workout
 * @param isTotalValue Optional parameter for including ZONE_1(warmup and cooldown) or not
 */
fun calculateExpectedWorkoutTotals(
    intervals: MmIntervalList,
    workouts: IWorkoutInterface,
    baseline: Float,
    actualWorkoutTime: Int = -1,
    isTotalValue: Boolean = false
): Pair<Int, Float> {
    var totalTimeMs = 0
    var totalDistanceKm = 0f
    for (interval in intervals.intervalList) {
        val type = interval.intervalType

        // We don't care about ZONE_1 (warmup/cooldown) in this calculation

        if (type == IntervalType.ZONE_1 && !isTotalValue) continue

        val speed = workouts.getIntervalTypeSpeed(type, baseline)
        val goalType = interval.goalType
        val goal = interval.goalValue

        val timeToRunInterval: Int = when (goalType) {
            IntervalGoalType.TIME -> goal.roundToInt()

            // Calculate how long it will take to run this interval distance at the expected speed
            IntervalGoalType.DISTANCE -> {
                val hoursToCompleteInterval = goal / speed
                (hoursToCompleteInterval * 3600000f).roundToInt()
            }
        }

        val timeNeeded = if (actualWorkoutTime >= 0) {
            // Get how much time is actually needed from this interval
            val totalTimeAfterInterval = totalTimeMs + timeToRunInterval
            if (totalTimeAfterInterval > actualWorkoutTime) {
                timeToRunInterval - (totalTimeAfterInterval - actualWorkoutTime)
            } else {
                timeToRunInterval
            }
        } else {
            timeToRunInterval
        }

        totalTimeMs += timeNeeded
        totalDistanceKm += (timeNeeded / 3600000f) * speed // Convert to hours then multiply by kph

        // We've reached the end of what the user ran, so do not need to calculate further
        if (timeNeeded < timeToRunInterval) break
    }

    return Pair(totalTimeMs, totalDistanceKm)
}

/**
 * Calculates the expected average speed for a workout based on the intervals and expected speeds
 * for each. Tested in CalculationTests.
 */
fun calculateExpectedWorkoutAverageSpeed(
    intervals: MmIntervalList,
    workouts: IWorkoutInterface,
    baseline: Float,
    actualWorkoutTime: Int = -1
): Float {
    val (totalTimeMs, distance) = calculateExpectedWorkoutTotals(intervals, workouts, baseline, actualWorkoutTime)
    val timeHours = totalTimeMs / 3600000f
    return distance / timeHours
}

/**
 * Converts a given number of seconds into a formatted string in the format HH:mm:ss.
 *
 * @param totalSeconds The total number of seconds to convert.
 * @return A string representing the time in HH:mm:ss format, where each component is two digits.
 */
fun secondsToHrMinSecString(totalSeconds: Int, delimiter: String = ":"): String {
    val hours = (totalSeconds / 3600).toString().padStart(2, '0')
    val minutes = ((totalSeconds % 3600) / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$hours$delimiter$minutes$delimiter$seconds"
}


/**
 * Converts race time from HH:MM:SS format to seconds.
 *
 * @param raceTime The race time in HHMMSS or HH:MM:SS format.
 * @return The race time in seconds.
 */
fun convertRaceTimeToSeconds(raceTime: String): Int {
    val formattedRaceTime = raceTime.replace(Regex("\\D"), "").padStart(MAX_DURATION_LENGTH, '0')
    val hours = formattedRaceTime.substring(0, 2).toIntOrNull() ?: 0
    val minutes = formattedRaceTime.substring(2, 4).toIntOrNull() ?: 0
    val seconds = formattedRaceTime.substring(4, 6).toIntOrNull() ?: 0
    return hours * 3600 + minutes * 60 + seconds
}