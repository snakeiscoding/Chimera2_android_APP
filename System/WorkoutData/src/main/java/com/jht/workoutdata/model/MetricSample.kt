package com.jht.workoutdata.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A lightweight (elapsedTimeSec, value) pair for a single [WorkoutMetric].
 *
 * Returned by series queries so a consumer that only wants to draw one line
 * (e.g. heart rate over time) doesn't have to carry full [WorkoutDataPoint]s
 * across the process boundary.
 */
@Parcelize
data class MetricSample(
    val elapsedTimeSec: Int,
    val value: Float,
) : Parcelable
