package com.jht.sprint8.model

/**
 * Represents a single statistic in the assessment workout summary.
 *
 * @property title The name of the statistic (e.g., "Average Speed", "Peak Incline").
 * @property value The numeric value of the statistic.
 * @property unit The unit of measurement associated with the value (e.g., "mph", "%"").
 */
data class AssessmentWorkoutSummaryStat(
    val title: String,
    val value: Float,
    val unit: String
)
