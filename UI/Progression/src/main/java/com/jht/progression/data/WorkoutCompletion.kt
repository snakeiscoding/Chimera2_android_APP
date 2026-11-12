package com.jht.progression.data

/** Stats recorded for a finished workout, copied onto the event being completed. */
data class CompletedWorkoutStats(
    val workoutDistanceKm: Float = 0f,
    val workoutTimeMs: Int = 0,
    val totalTimeSec: Int = 0,
    val totalDistanceKm: Float = 0f,
    val totalCalories: Float = 0f,
    val totalAverageSpeedKph: Float = 0f,
    val totalPeakSpeedKph: Float = 0f,
    val totalAverageHR: Int = 0,
    val totalElevationMeters: Float = 0f,
    val baselineKph: Float = 0f,
    val goalTimeExisted: Boolean = false,
)

sealed interface WorkoutCompletionResult {
    data class Applied(val events: ProgramEvents) : WorkoutCompletionResult

    /**
     * The index points outside the event list, so nothing was changed. eventIndex and the
     * program are separate DataMatrix keys, so a session reset can clear just one of them.
     */
    data class IndexOutOfRange(val index: Int, val eventCount: Int) : WorkoutCompletionResult
}

/**
 * Marks the event at [index] complete, recording [stats] on it when it is a workout event.
 * Pure: callers persist the result only when it is [WorkoutCompletionResult.Applied].
 */
fun applyWorkoutCompletion(
    index: Int,
    programEvents: ProgramEvents,
    stats: CompletedWorkoutStats,
): WorkoutCompletionResult {
    val events = programEvents.events
    if (index !in events.indices) {
        return WorkoutCompletionResult.IndexOutOfRange(index, events.size)
    }

    val updatedEvent = when (val event = events[index]) {
        is WorkoutEvent -> event.copy(
            completed = true,
            workoutDistanceKm = stats.workoutDistanceKm,
            workoutTimeMs = stats.workoutTimeMs,
            totalTimeSec = stats.totalTimeSec,
            totalDistanceKm = stats.totalDistanceKm,
            totalCalories = stats.totalCalories,
            totalAverageSpeedKph = stats.totalAverageSpeedKph,
            totalPeakSpeedKph = stats.totalPeakSpeedKph,
            totalAverageHR = stats.totalAverageHR,
            totalElevationMeters = stats.totalElevationMeters,
            baselineKph = stats.baselineKph,
            goalTimeExisted = stats.goalTimeExisted,
        )
        is NonWorkoutEvent -> event.copy(completed = true)
    }

    return WorkoutCompletionResult.Applied(
        programEvents.copy(events = events.toMutableList().also { it[index] = updatedEvent })
    )
}
