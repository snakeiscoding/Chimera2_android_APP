package com.jht.workoutdata.model

import com.jht.workoutdata.provider.WorkoutDataContract

/**
 * A single workout metric that can be read back as a time series.
 *
 * The provider is deliberately unaware of equipment frame types: callers push
 * only the fields relevant to their machine, and readers ask for whichever
 * metric they want. [columnName] maps the metric to its storage column so a
 * series query can project just that value.
 */
enum class WorkoutMetric(val columnName: String) {
    SPEED(WorkoutDataContract.COLUMN_SPEED_KPH),
    INCLINE(WorkoutDataContract.COLUMN_INCLINE_PERCENT),
    RESISTANCE(WorkoutDataContract.COLUMN_RESISTANCE),
    RPM(WorkoutDataContract.COLUMN_RPM),
    SPM(WorkoutDataContract.COLUMN_SPM),
    HEART_RATE(WorkoutDataContract.COLUMN_HEART_RATE),
    WATTS(WorkoutDataContract.COLUMN_WATTS),
}
