package com.jht.progression.data.program

import com.jht.enums.IntervalType
import com.jht.progression.data.UserWorkoutMetrics
import com.jht.progression.data.program.program5k.Program5k
import com.jht.unit.mphUnit
import com.jht.unit.secondsPerKmPaceUnit
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Gets the zone speed (kph) for a specific interval type scaled by the baseline. This is
 * clamped to the speeds supported by the console.
 *
 * Magic numbers come from calculating a line that goes through both points in form (m * x + b) where
 * x is the time it takes to complete a race. Output is pace adjustment from race pace in miles/min.
 *
 * View https://jhtnaee.atlassian.net/browse/CGC-17384 for more info on intended zone speeds.
 */
fun getSpeedForIntervalType(
    intervalType: IntervalType,
    baseline: Float,
    clampSpeed: (Float) -> Float
): Float {
    // Specifically use metrics for a 5k distance, we base zone speeds off of 5k pace
    val metrics = estimateRaceMetrics(baseline, 5f)
    val raceTimeMinutes : Float = metrics.raceTimeSeconds / 60f
    // kph (km/hour) = 3600 (hour, in seconds) / pace (seconds/km)
    val racePaceMinPerMi = metrics.racePaceSecPerKm.toFloat().secondsPerKmPaceUnit.toMinutesPerMilePace
    val intervalSpeed = when (intervalType) {
        IntervalType.ZONE_1 -> {
            // 1 mph at 90 minutes, 3.5 mph at 27 minutes
            // This zone calculates directly to mph, does not calculate adjustments from pace
            (-0.03968 * raceTimeMinutes + 4.5712).coerceIn(1.0, 3.5).toFloat().mphUnit.toKph
        }

        IntervalType.ZONE_2 -> {
            // 2 min slower pace at 25 minutes, 4 min slower at 35 minutes
            val paceReductionMinPerMi = (0.2 * raceTimeMinutes - 3).coerceIn(2.0, 4.0)
            val newPaceMinPerMi = racePaceMinPerMi + paceReductionMinPerMi
            (1 / newPaceMinPerMi * 60.0).toFloat().mphUnit.toKph
        }

        IntervalType.ZONE_3 -> {
            // 90 sec slower pace at 25 minutes, 2 min slower at 35 minutes
            val paceReductionMinPerMi = ( 0.05 * raceTimeMinutes + 0.25).coerceIn(1.5, 2.0)
            val newPaceMinPerMi = racePaceMinPerMi + paceReductionMinPerMi
            (1 / newPaceMinPerMi * 60.0).toFloat().mphUnit.toKph
        }

        IntervalType.ZONE_4 -> {
            // follows race pace exactly
            (1 / racePaceMinPerMi * 60.0).toFloat().mphUnit.toKph
        }

        IntervalType.ZONE_5 -> {
            // 2 min faster pace at 21 minutes, 1 min faster at 45 minutes
            val paceIncreaseMinMi = (-0.04167 * raceTimeMinutes + 2.87507).coerceIn(1.0, 2.0)
            val newPaceMinPerMi = racePaceMinPerMi - paceIncreaseMinMi
            (1 / newPaceMinPerMi * 60.0).toFloat().mphUnit.toKph
        }

        IntervalType.WARMUP,
        IntervalType.ASSESSMENT,
        IntervalType.COOLDOWN,
        IntervalType.RACE -> {
            0f // This uses preset speeds, does not need to be set here
        }
    }

    return clampSpeed(intervalSpeed)
}

/**
 * Estimates metrics about a race from a baseline
 */
fun estimateRaceMetrics(baseline: Float, raceDistanceKm: Float) : UserWorkoutMetrics {
    if (baseline <= 0f || raceDistanceKm <= 0f) {
        return UserWorkoutMetrics(0, 0f, 0)
    }

    val baselineDistance = baseline * (15f / 60f) // Gives km covered during 15 minute assessment
    val baselineTimeSec = 15f * 60f // How long the assessment time is
    val raceTimeSeconds = riegalRaceTimeEstimate(baselineTimeSec, baselineDistance, raceDistanceKm)
    val racePaceSecPerKm = raceTimeSeconds / raceDistanceKm
    val raceSpeedKph = raceDistanceKm / (raceTimeSeconds / 3600f)

    return UserWorkoutMetrics(
        // Round to nearest int to get correct 5k race time for manual assessment & goal time baselines
        racePaceSecPerKm = racePaceSecPerKm.roundToInt(),
        raceSpeedKph = raceSpeedKph,
        raceTimeSeconds = raceTimeSeconds.roundToInt()
    )
}

/**
 * Implementation of the Riegal formula to calculate how long a distance will take based on
 * another running distance. Formula is:
 * T2=T1*(D2/D1)^1.06
 * where T is time and D is distance for each race. 1.06 is the fatigue factor and can
 * be adjusted for different levels of runners, but we aren't doing that now.
 */
fun riegalRaceTimeEstimate(time1: Float, distance1: Float, distance2: Float) : Float {
    return time1 * (distance2 / distance1).pow(1.06f)
}

/**
 * Calculates a baseline based on a race distance and time using Riegal formula
 * T2=T1*(D2/D1)^1.06
 */
fun riegalBaselineEstimate(raceTimeSec: Float, raceDistanceKm: Float) : Float {
    // D2 = (T2/T1)^(1/1.06) * D1
    val baselineTimeSec = (15f * 60f)
    val baselineDistanceKm = (baselineTimeSec / raceTimeSec).pow(1f/1.06f) * raceDistanceKm
    val baselineKph = (baselineDistanceKm / baselineTimeSec) * 3600f
    return baselineKph
}