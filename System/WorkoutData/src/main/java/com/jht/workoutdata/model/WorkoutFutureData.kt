package com.jht.workoutdata.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A single point of a planned (future) workout profile: the value a metric is
 * scripted to take at [timeSec] seconds of elapsed workout time. Times are
 * fractional seconds so sub-second profile resolution is preserved.
 */
@Parcelize
data class FutureProfilePoint(
    val timeSec: Float,
    val value: Float,
) : Parcelable

/** The planned profile for one [metric] over the remainder of the workout. */
@Parcelize
data class FutureProfile(
    val metric: WorkoutMetric,
    val points: List<FutureProfilePoint>,
) : Parcelable

/**
 * The planned (future) workout profiles, one per scripted metric.
 *
 * Unlike history points this is replaced wholesale whenever the workout script
 * or terrain profile changes, and cleared with the history when a new workout
 * starts. Metrics without a scripted future simply have no entry.
 */
@Parcelize
data class WorkoutFutureData(
    val profiles: List<FutureProfile> = emptyList(),
) : Parcelable {

    /** Returns the profile for [metric], or null if that metric has no scripted future. */
    fun profileFor(metric: WorkoutMetric): FutureProfile? =
        profiles.firstOrNull { it.metric == metric }
}
