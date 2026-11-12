package com.jht.progression.data

import com.jht.enums.IntervalType
import com.jht.enums.UnitSystem
import com.jht.progression.userinfo.UserInfoManager

/**
 * Interface that allows the program to get needed information about workouts from a workout program.
 */
interface IWorkoutInterface {
    /**
     * Defines the unit system for the race.
     */
    val raceUnitSystem: UnitSystem

    /**
     * The maximum possible time, in seconds, to complete the race.
     */
    val maxRaceTimeInSeconds: Int

    /**
     * The minimum possible time, in seconds, to complete the race.
     */
    val minRaceTimeInSeconds: Int

    /**
     * The baseline value derived from the maximum race time in seconds.
     */
    val maxRaceTimeBaseline: Float
        get() = computeBaselineFromWorkoutTime(maxRaceTimeInSeconds)

    /**
     * The baseline value derived from the minimum race time in seconds.
     */
    val minRaceTimeBaseline: Float
        get() = computeBaselineFromWorkoutTime(minRaceTimeInSeconds)

    /**
     * Defines the range of workouts where the user's baseline is adjusted to help reach their goal race time.
     */
    val goalAdjustmentWorkoutRange: IntRange
        get() = 4..21

    /**
     * Checks if the user is allowed to modify their goal time setting.
     *
     * @param userInfoManager Used to access user-specific workout data.
     * @return Boolean indicating if goal time modification is allowed.
     */
    fun isGoalTimeChangePermitted(userInfoManager: UserInfoManager): Boolean {
        return userInfoManager.getNumGoalIncrementsRemaining() > 0
    }

    /**
     * Gets the workout script for a specific workout.
     * @param programIndex The index of the event
     * @param baseline The current user baseline
     * @param goalBaseline The goal for the user. Used for calculating the intended speed for a race.
     */
    fun getWorkout(programIndex: Int, baseline: Float, goalBaseline: Float = -1f) : WorkoutInfo

    /**
     * Gets the speed for a specific interval type for workouts
     */
    fun getIntervalTypeSpeed(intervalType: IntervalType, baseline: Float, clampSpeed: (Float) -> Float = { it }) : Float

    /**
     * Gets whether a workout should display the next interval box
     */
    fun shouldDisplayNextInterval(programIndex: Int) : Boolean

    /**
     * Gets whether a workout should display tips
     */
    fun shouldDisplayTips(programIndex: Int) : Boolean

    /**
     * Computes a user's estimated stats for a workout given their baseline
     */
    fun getRaceMetrics(userBaseline: Float) : UserWorkoutMetrics

    /**
     * Speed to hold for the race stage. Uses [targetBaseline] only when it is faster than
     * [baseline], giving the user the benefit of the doubt that they can run their goal pace.
     */
    fun calculateRaceSpeed(baseline: Float, targetBaseline: Float) : Float {
        val shouldUseTarget = targetBaseline > 0f && targetBaseline > baseline
        return getRaceMetrics(if (shouldUseTarget) targetBaseline else baseline).raceSpeedKph
    }

    /**
     * Computes the baseline needed to complete the workout in the time raceTimeSeconds provided.
     */
    fun computeBaselineFromWorkoutTime(raceTimeSeconds: Int) : Float
}

data class UserWorkoutMetrics(
    val racePaceSecPerKm: Int,
    val raceSpeedKph: Float,
    val raceTimeSeconds: Int
)