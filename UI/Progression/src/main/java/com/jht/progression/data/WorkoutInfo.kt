package com.jht.progression.data

import com.jht.data.MmIntervalList

/**
 * Contains necessary data to begin a progression workout
 */
data class WorkoutInfo(
    val intervalList: MmIntervalList, // Used for displaying intervals in progression workouts
    val workoutScript: String // The script used to run the workout
)