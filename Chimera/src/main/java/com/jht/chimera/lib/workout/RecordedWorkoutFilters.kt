package com.jht.chimera.lib.workout

import com.jht.enums.WorkoutId
import com.jht.enums.WorkoutId.Companion.fromId

/**
 * Utility for determining whether a recorded workout should be hidden from display.
 *
 * Contains predefined rules for filtering out certain workouts based on their [WorkoutId]
 * and associated [SavedWorkoutData], such as missing configuration parameters.
 */
object RecordedWorkoutFilters {

    /**
     * Maps specific [WorkoutId]s to predicates determining whether the associated workout should be hidden.
     *
     * Each predicate takes [SavedWorkoutData] as input and returns true if the workout
     * should be excluded from the [RecordedWorkoutDatabase] (e.g., due to incomplete configuration).
     */
    private val HiddenWorkouts: Map<WorkoutId?, (SavedWorkoutData) -> Boolean> = mapOf(

        // Hide all progression workouts
        WorkoutId.PROGRESSION_WORKOUT to { true },

        // Hide Sprint 8 workouts with missing startup parameters
        WorkoutId.SPRINT8 to { savedWorkoutData ->
            // Custom Sprint 8 workouts require sprint8Config and numberOfSprints parameters
            if (savedWorkoutData.startupParams?.sprint8Config != null && savedWorkoutData.startupParams?.numberOfSprints != null) {
                return@to false
            }

            // Standard Sprint 8 workouts require workoutLeve parameter
            if (savedWorkoutData.startupParams?.workoutLevel != null) {
                return@to false
            }

            // Sprint 8 workout is missing required parameters, hide it.
            else return@to true
        }
    )

    /**
     * Checks if a workout should be hidden based on its [workoutId] and [savedWorkoutData].
     *
     * @param savedWorkoutData The saved data associated with the workout.
     * @return `true` if the workout should be hidden, `false` otherwise.
     */
    fun isWorkoutHidden(savedWorkoutData: SavedWorkoutData): Boolean {
        val workoutId = savedWorkoutData.startupParams?.programTypeId?.let { fromId(it) }
        return HiddenWorkouts[workoutId]?.invoke(savedWorkoutData) ?: false
    }
}
