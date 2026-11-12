package com.jht.spinbikeapps.intervals

enum class IntervalPhase {
    Work,
    Rest,
    Other;
}

/** Whether each interval is measured by time or distance */
enum class IntervalGoal {
    /** Counts time down, accumulates distance */
    Time,

    /** Counts distance down, accumulates time */
    Distance;
}

data class IntervalData(
    /**
     * Pre-formatted time to display in the Previous Intervals table (e.g. "0:40"). All
     * formatting is done in IntervalsViewModel.
     */
    val intervalTime: String,

    /**
     * Pre-formatted distance to display in the Previous Intervals table (e.g. "0.11"). The distance
     * formatting is done in IntervalsViewModel.
     */
    val intervalDistance: String,

    /**
     * Used to determine whether data is Work, Rest, or Other (future possibilities :) )
     */
    val intervalType: IntervalPhase,
)