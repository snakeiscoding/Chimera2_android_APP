package com.jht.workoutdata.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A single immutable snapshot of workout data for one second of a workout.
 *
 * Points are expected to be added once per second while the workout is running,
 * so [elapsedTimeSec] (cumulative workout time, in seconds) is both the natural
 * ordering key and the axis used for windowing ("last 30 minutes") and
 * down-sampling ("every 5 seconds"). It resets to 0 at the start of each workout
 * (when the history is cleared).
 *
 * The provider is unaware of equipment frame types: a caller fills only the
 * fields relevant to its machine and leaves the rest at their defaults. Readers
 * then ask for whichever metric they need.
 */
@Parcelize
data class WorkoutDataPoint(
    val elapsedTimeSec: Int,
    val workoutStage: Int = 0,
    val lapIndex: Int = 0,
    val splitIndex: Int = 0,
    val distanceKm: Float = 0f,
    val speedKph: Float = 0f,
    val inclinePercent: Float = 0f,
    val resistance: Int = 0,
    val rpm: Int = 0,
    val spm: Int = 0,
    val heartRate: Int = 0,
    val watts: Int = 0,
    val targetHeartRate: Int = 0,
    val targetWatts: Int = 0,
    val totalCalories: Float = 0f,
    val mets: Float = 0f,
    val totalFloors: Float = 0f,
    val totalSteps: Int = 0,
    val elevationMeters: Float = 0f,
) : Parcelable {

    /** Returns the value for [metric] from this point, as a Float for uniform graphing. */
    fun valueOf(metric: WorkoutMetric): Float = when (metric) {
        WorkoutMetric.SPEED -> speedKph
        WorkoutMetric.INCLINE -> inclinePercent
        WorkoutMetric.RESISTANCE -> resistance.toFloat()
        WorkoutMetric.RPM -> rpm.toFloat()
        WorkoutMetric.SPM -> spm.toFloat()
        WorkoutMetric.HEART_RATE -> heartRate.toFloat()
        WorkoutMetric.WATTS -> watts.toFloat()
    }

    /**
     * Fluent builder for Java callers (Kotlin callers can use named arguments on
     * the constructor directly). [elapsedTimeSec] is required; set only the
     * fields relevant to your equipment frame and leave the rest at their defaults.
     *
     * ```java
     * WorkoutDataPoint p = new WorkoutDataPoint.Builder(elapsedSec)
     *         .speedKph(speed)
     *         .heartRate(hr)
     *         .build();
     * ```
     */
    class Builder(private val elapsedTimeSec: Int) {
        private var workoutStage: Int = 0
        private var lapIndex: Int = 0
        private var splitIndex: Int = 0
        private var distanceKm: Float = 0f
        private var speedKph: Float = 0f
        private var inclinePercent: Float = 0f
        private var resistance: Int = 0
        private var rpm: Int = 0
        private var spm: Int = 0
        private var heartRate: Int = 0
        private var watts: Int = 0
        private var targetHeartRate: Int = 0
        private var targetWatts: Int = 0
        private var totalCalories: Float = 0f
        private var mets: Float = 0f
        private var totalFloors: Float = 0f
        private var totalSteps: Int = 0
        private var elevationMeters: Float = 0f

        fun workoutStage(v: Int) = apply { workoutStage = v }
        fun lapIndex(v: Int) = apply { lapIndex = v }
        fun splitIndex(v: Int) = apply { splitIndex = v }
        fun distanceKm(v: Float) = apply { distanceKm = v }
        fun speedKph(v: Float) = apply { speedKph = v }
        fun inclinePercent(v: Float) = apply { inclinePercent = v }
        fun resistance(v: Int) = apply { resistance = v }
        fun rpm(v: Int) = apply { rpm = v }
        fun spm(v: Int) = apply { spm = v }
        fun heartRate(v: Int) = apply { heartRate = v }
        fun watts(v: Int) = apply { watts = v }
        fun targetHeartRate(v: Int) = apply { targetHeartRate = v }
        fun targetWatts(v: Int) = apply { targetWatts = v }
        fun totalCalories(v: Float) = apply { totalCalories = v }
        fun mets(v: Float) = apply { mets = v }
        fun totalFloors(v: Float) = apply { totalFloors = v }
        fun totalSteps(v: Int) = apply { totalSteps = v }
        fun elevationMeters(v: Float) = apply { elevationMeters = v }

        fun build() = WorkoutDataPoint(
            elapsedTimeSec = elapsedTimeSec,
            workoutStage = workoutStage,
            lapIndex = lapIndex,
            splitIndex = splitIndex,
            distanceKm = distanceKm,
            speedKph = speedKph,
            inclinePercent = inclinePercent,
            resistance = resistance,
            rpm = rpm,
            spm = spm,
            heartRate = heartRate,
            watts = watts,
            targetHeartRate = targetHeartRate,
            targetWatts = targetWatts,
            totalCalories = totalCalories,
            mets = mets,
            totalFloors = totalFloors,
            totalSteps = totalSteps,
            elevationMeters = elevationMeters,
        )
    }
}
