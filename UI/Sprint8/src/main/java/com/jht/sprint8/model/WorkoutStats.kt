package com.jht.sprint8.model

/**
 * Holds statistical values related to different phases of a workout.
 * This includes speed or intensity ranges as well as incline ranges
 * for warmup, sprint, recovery, and cooldown phases.
 **/
data class WorkoutStats(
    val warmup: Float = 0f,
    val cooldown: Float = 0f,
    val sprintMin: Float = 0f,
    val sprintMax: Float = 0f,
    val recoveryMin: Float = 0f,
    val recoveryMax: Float = 0f,
    val warmupIncline: Float = 0f,
    val cooldownIncline: Float = 0f,
    val sprintInclineMin: Float = 0f,
    val sprintInclineMax: Float = 0f,
    val recoveryInclineMin: Float = 0f,
    val recoveryInclineMax: Float = 0f
)
