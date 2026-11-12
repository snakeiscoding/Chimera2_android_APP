package com.jht.progression.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.jht.progression.ui.setup.journey.EventStatus
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class ProgramEvents(val events: List<IEvent>) : Parcelable

@Keep
sealed interface IEvent : Parcelable {
    val index: Int
    val startTime: Long
    val endTime: Long
    val completed: Boolean
    val label: EventLabel
    fun getEventStatus(currentTime: Long): EventStatus {
        return if (completed) {
            EventStatus.COMPLETED
        } else if (startTime > currentTime) {
            EventStatus.FUTURE
        } else if (currentTime in startTime..endTime) {
            EventStatus.CURRENT
        } else if (this is WorkoutEvent) {
            EventStatus.MISSED
        } else {
            EventStatus.COMPLETED
        }
    }


    /**
     * Gets the zero indexed day number of the event from the index
     */
    fun getDayNum() : Int {
        return index % 7
    }
}

/**
 * Used by the UI to determine what text to display for the event
 */
@Keep
enum class EventLabel(val id: Int) {
    // Workout events
    WORKOUT_LONG_SLOW_DISTANCE(0),
    WORKOUT_INTERVAL(1),
    WORKOUT_TEMPO(2),

    // Non-workout events
    REST(3),
    CROSS(4),
    ACTIVE_RECOVERY(5),

    // Race event
    RACE(6),
}

@Parcelize
@Keep
data class WorkoutEvent(
    override val index: Int,
    override val startTime: Long,
    override val endTime: Long,
    override val completed: Boolean,
    override val label: EventLabel,

    // Data for workout history
    val workoutDistanceKm: Float = 0f,
    val workoutTimeMs: Int = 0,
    val totalDistanceKm: Float = 0f,
    val totalTimeSec: Int = 0,
    val totalCalories: Float = 0f,
    val totalAverageSpeedKph: Float = 0f,
    val totalPeakSpeedKph: Float = 0f,
    val totalAverageHR: Int = 0,
    val totalElevationMeters: Float = 0f,
    val baselineKph: Float = 0f,
    val goalTimeExisted: Boolean = false
) : IEvent, Parcelable

@Parcelize
@Keep
data class NonWorkoutEvent(
    override val index: Int,
    override val startTime: Long,
    override val endTime: Long,
    override val completed: Boolean,
    override val label: EventLabel
) : IEvent, Parcelable